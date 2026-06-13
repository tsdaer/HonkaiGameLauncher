package screen.feature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.CodeHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text as FluentText
import io.ratex.RaTeXEngine
import io.ratex.measure
import io.ratex.compose.RaTeX
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import ui.fluent.theme.FluentTokens
import ui.settings.LocalAppUiSettings
import viewModel.DocsScreenModel
import java.io.File
import java.net.URI
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal data class InlineMarkdown(
    val content: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    companion object {
        val Empty = InlineMarkdown(AnnotatedString(""))
    }
}

internal fun buildInlineMarkdown(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
    linkListener: LinkInteractionListener? = null,
): InlineMarkdown {
    val inlineContent = linkedMapOf<String, InlineTextContent>()
    val content = buildAnnotatedString {
        appendInlineNode(node, source, style, inlineContent, linkListener)
    }

    return InlineMarkdown(content, inlineContent)
}


internal fun AnnotatedString.Builder.appendInlineNode(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
    inlineContent: MutableMap<String, InlineTextContent>,
    linkListener: LinkInteractionListener?,
) {
    when (node.type) {
        MarkdownElementTypes.STRONG -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            node.children.forEach { appendInlineNode(it, source, style, inlineContent, linkListener) }
        }

        MarkdownElementTypes.EMPH -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            node.children.forEach { appendInlineNode(it, source, style, inlineContent, linkListener) }
        }

        GFMElementTypes.STRIKETHROUGH -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
            node.children.forEach { appendInlineNode(it, source, style, inlineContent, linkListener) }
        }

        MarkdownElementTypes.CODE_SPAN -> {
            val code = extractMarkdownText(node, source)
                .trim()
                .trim('`')
            val contentId = "inline-code-${inlineContent.size}"
            inlineContent[contentId] = InlineTextContent(
                placeholder = Placeholder(
                    width = ((code.length.coerceAtLeast(1) * 7.5f) + 12f).sp,
                    height = 19.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                )
            ) {
                MarkdownInlineCode(code = code, style = style)
            }
            appendInlineContent(contentId, code)
        }

        MarkdownElementTypes.INLINE_LINK,
        MarkdownElementTypes.FULL_REFERENCE_LINK,
        MarkdownElementTypes.SHORT_REFERENCE_LINK,
        MarkdownElementTypes.AUTOLINK -> appendLinkNode(node, source, style, linkListener)

        MarkdownElementTypes.IMAGE -> {
            val label = node.children
                .firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
                ?.let { extractMarkdownText(it, source).trim('[', ']') }
                ?.ifBlank { null }
                ?: "image"
            withStyle(SpanStyle(color = style.mutedColor)) {
                append("[$label]")
            }
        }

        MarkdownTokenTypes.HARD_LINE_BREAK -> append("\n")

        MarkdownTokenTypes.WHITE_SPACE -> append(extractMarkdownText(node, source))

        MarkdownTokenTypes.EOL -> append(" ")

        MarkdownTokenTypes.TEXT,
        MarkdownTokenTypes.ATX_CONTENT,
        MarkdownTokenTypes.SETEXT_CONTENT,
        MarkdownTokenTypes.CODE_LINE -> append(extractMarkdownText(node, source))

        GFMTokenTypes.CELL -> append(extractMarkdownText(node, source).trimCellPipes())

        else -> {
            if (node.children.isEmpty()) {
                if (!node.isMarkdownSyntaxToken() && !node.isWhitespaceToken()) {
                    append(extractMarkdownText(node, source))
                }
            } else {
                node.children.forEach { appendInlineNode(it, source, style, inlineContent, linkListener) }
            }
        }
    }
}

@Composable
internal fun MarkdownInlineCode(
    code: String,
    style: DocsMarkdownStyle,
) {
    Box(
        modifier = Modifier
            .background(style.inlineCodeBackground, RoundedCornerShape(4.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        FluentText(
            text = code,
            style = TextStyle(
                color = style.textColor,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace,
            )
        )
    }
}



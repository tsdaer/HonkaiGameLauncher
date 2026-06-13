package screen.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import io.github.composefluent.component.Text as FluentText

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



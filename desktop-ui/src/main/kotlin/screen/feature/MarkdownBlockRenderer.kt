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

@Composable
internal fun rememberMarkdownAst(markdown: String): ASTNode {
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember(flavour) { MarkdownParser(flavour) }
    return remember(markdown, parser) {
        parser.buildMarkdownTreeFromString(markdown)
    }
}

@Composable
internal fun FluentMarkdownDocument(
    root: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        root.children.forEach { node ->
            MarkdownBlock(node = node, source = source, style = style)
        }
    }
}

@Composable
internal fun MarkdownBlock(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    when (node.type) {
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6,
        MarkdownElementTypes.SETEXT_1,
        MarkdownElementTypes.SETEXT_2 -> MarkdownHeading(node, source, style)

        MarkdownElementTypes.PARAGRAPH -> {
            val rawParagraph = extractMarkdownText(node, source)
            val displayMath = parseDisplayMath(rawParagraph)
            when {
                displayMath != null -> MarkdownMathBlock(latex = displayMath, style = style)

                else -> {
                    val table = parseMarkdownTable(rawParagraph)
                    if (table != null) {
                        MarkdownTable(table, style)
                    } else {
                        MarkdownTextBlock(
                            text = buildInlineMarkdown(node, source, style, LocalMarkdownLinkListener.current),
                            style = style.bodyTextStyle,
                        )
                    }
                }
            }
        }

        MarkdownElementTypes.CODE_FENCE,
        MarkdownElementTypes.CODE_BLOCK -> MarkdownCodeBlock(node, source, style)

        MarkdownElementTypes.BLOCK_QUOTE -> MarkdownQuoteBlock(node, source, style)

        MarkdownElementTypes.UNORDERED_LIST -> MarkdownList(node, source, style, ordered = false)

        MarkdownElementTypes.ORDERED_LIST -> MarkdownList(node, source, style, ordered = true)

        GFMElementTypes.TABLE -> MarkdownTable(node, source, style)

        MarkdownTokenTypes.HORIZONTAL_RULE -> MarkdownDivider(style)
    }
}

@Composable
internal fun MarkdownHeading(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    val level = headingLevel(node.type)
    val text = headingText(node, source)
    val headingStyle = style.headingTextStyle(level)

    val controller = LocalDocsReaderController.current
    val slug = LocalDocsHeadingSlugs.current[node.startOffset]
    val registerModifier = if (controller != null && slug != null) {
        Modifier.onGloballyPositioned { coords ->
            val contentTop = coords.positionInWindow().y -
                controller.viewportTopInWindow +
                controller.scrollState.value
            controller.registerHeading(slug, contentTop.roundToInt())
        }
    } else {
        Modifier
    }

    Column(
        modifier = registerModifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        FluentText(text = text, style = headingStyle)
        if (level == 2) {
            MarkdownDivider(style.copy(dividerColor = style.dividerColor.copy(alpha = 0.55f)))
        }
    }
}

@Composable
internal fun MarkdownTextBlock(
    text: InlineMarkdown,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val withMath = rememberInlineMath(text, style)
    FluentText(
        text = withMath.content,
        style = style,
        inlineContent = withMath.inlineContent,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable

internal fun MarkdownDivider(style: DocsMarkdownStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(style.dividerColor)
    )
}


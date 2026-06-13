package screen.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.math.roundToInt
import io.github.composefluent.component.Text as FluentText

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


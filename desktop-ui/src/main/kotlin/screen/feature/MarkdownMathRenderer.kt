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
internal fun MarkdownMathBlock(
    latex: String,
    style: DocsMarkdownStyle,
) {
    val hScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.subtleBackground, RoundedCornerShape(6.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(hScrollState)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            RaTeX(
                latex = latex,
                fontSize = 20.sp,
                displayMode = true,
                color = style.textColor,
            )
        }

        HorizontalScrollbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            adapter = rememberScrollbarAdapter(scrollState = hScrollState)
        )
    }
}

@Composable

internal fun rememberInlineMath(
    source: InlineMarkdown,
    textStyle: TextStyle,
): InlineMarkdown {
    val density = LocalDensity.current
    val fontSize = textStyle.fontSize
    val color = textStyle.color
    return remember(source, fontSize, color, density) {
        injectInlineMath(source, fontSize, color, density)
    }
}

internal fun injectInlineMath(
    source: InlineMarkdown,
    fontSize: TextUnit,
    color: Color,
    density: Density,
): InlineMarkdown {
    val text = source.content.text
    val matches = InlineMathRegex.findAll(text).toList()
    if (matches.isEmpty()) {
        return source
    }

    val fontSizePx = with(density) { fontSize.toPx() }
    val inlineContent = LinkedHashMap(source.inlineContent)
    var mathIndex = 0
    var cursor = 0

    val content = buildAnnotatedString {
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start < cursor) {
                continue
            }
            val latex = (match.groupValues[1].ifBlank { match.groupValues[2] }).trim()
            if (latex.isBlank()) {
                continue
            }

            if (start > cursor) {
                append(source.content.subSequence(cursor, start))
            }

            val displayList = runCatching {
                RaTeXEngine.parseBlocking(latex, displayMode = false, color = color)
            }.getOrNull()

            if (displayList == null) {
                append(source.content.subSequence(start, end))
            } else {
                val measured = displayList.measure(fontSizePx)
                val id = "inline-math-$mathIndex"
                mathIndex++
                inlineContent[id] = InlineTextContent(
                    placeholder = Placeholder(
                        width = with(density) { measured.widthPx.toDp().toSp() },
                        height = with(density) { measured.totalHeightPx.toDp().toSp() },
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    )
                ) {
                    RaTeX(displayList = displayList, fontSize = fontSize)
                }
                appendInlineContent(id, latex)
            }
            cursor = end
        }
        if (cursor < text.length) {
            append(source.content.subSequence(cursor, text.length))
        }
    }

    return InlineMarkdown(content, inlineContent)
}

internal fun parseDisplayMath(raw: String): String? {
    val trimmed = raw.trim()
    val inner = when {
        trimmed.length >= 4 && trimmed.startsWith("$$") && trimmed.endsWith("$$") ->
            trimmed.substring(2, trimmed.length - 2)

        trimmed.length >= 4 && trimmed.startsWith("\\[") && trimmed.endsWith("\\]") ->
            trimmed.substring(2, trimmed.length - 2)

        else -> return null
    }
    if (inner.contains("$$")) {
        return null
    }
    return inner.trim().takeIf { it.isNotBlank() }
}

internal val InlineMathRegex =
    Regex("""(?<!\$)\$(?!\$)(?!\s)([^$\n]+?)(?<!\s)\$(?!\$)(?!\d)|\\\(([^\n]+?)\\\)""")


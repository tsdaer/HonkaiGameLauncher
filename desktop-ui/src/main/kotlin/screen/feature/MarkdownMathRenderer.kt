package screen.feature

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ratex.RaTeXEngine
import io.ratex.compose.RaTeX
import io.ratex.measure

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


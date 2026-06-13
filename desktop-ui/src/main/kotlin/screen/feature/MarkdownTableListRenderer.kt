package screen.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import io.github.composefluent.component.Text as FluentText

@Composable
internal fun MarkdownQuoteBlock(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.subtleBackground, RoundedCornerShape(6.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
            .padding(vertical = 9.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(style.accentColor.copy(alpha = 0.58f), RoundedCornerShape(2.dp))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            node.children
                .filterNot { it.type == MarkdownTokenTypes.BLOCK_QUOTE || it.isWhitespaceToken() }
                .forEach { child -> MarkdownBlock(child, source, style.copy(textColor = style.mutedColor)) }
        }
    }
}

@Composable
internal fun MarkdownList(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
    ordered: Boolean,
) {
    val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FluentText(
                    text = if (ordered) "${index + 1}." else "•",
                    style = style.bodyTextStyle.copy(
                        color = style.mutedColor,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier.size(width = 22.dp, height = 22.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    item.children
                        .filterNot {
                            it.type == MarkdownTokenTypes.LIST_BULLET ||
                                it.type == MarkdownTokenTypes.LIST_NUMBER ||
                                it.type == MarkdownTokenTypes.EOL ||
                                it.isWhitespaceToken()
                        }
                        .forEach { child -> MarkdownBlock(child, source, style) }
                }
            }
        }
    }
}

@Composable
internal fun MarkdownTable(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    val rows = node.children.filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }
    if (rows.isEmpty()) return

    val linkListener = LocalMarkdownLinkListener.current
    val header = rows.firstOrNull()
        ?.children
        ?.filter { it.type == GFMTokenTypes.CELL }
        ?.map { buildInlineMarkdown(it, source, style, linkListener) }
        .orEmpty()
    val body = rows.drop(1).map { row ->
        row.children
            .filter { it.type == GFMTokenTypes.CELL }
            .map { buildInlineMarkdown(it, source, style, linkListener) }
    }

    MarkdownTable(
        table = MarkdownTableData(
            header = header,
            rows = body,
            alignments = List(header.size) { TextAlign.Start }
        ),
        style = style,
    )
}

@Composable
internal fun MarkdownTable(
    table: MarkdownTableData,
    style: DocsMarkdownStyle,
) {
    if (table.header.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
            .background(style.subtleBackground.copy(alpha = 0.42f), RoundedCornerShape(6.dp))
    ) {
        MarkdownTableRow(
            cells = table.header,
            alignments = table.alignments,
            style = style,
            header = true,
        )
        table.rows.forEach { row ->
            MarkdownTableRow(
                cells = row,
                alignments = table.alignments,
                style = style,
                header = false,
            )
        }
    }
}

@Composable
internal fun MarkdownTableRow(
    cells: List<InlineMarkdown>,
    alignments: List<TextAlign>,
    style: DocsMarkdownStyle,
    header: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        val columnCount = maxOf(cells.size, alignments.size, 1)
        repeat(columnCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(0.5.dp, style.borderColor)
                    .background(if (header) style.subtleBackground else Color.Transparent)
                    .padding(horizontal = 9.dp, vertical = 7.dp)
            ) {
                MarkdownTextBlock(
                    text = cells.getOrNull(index) ?: InlineMarkdown.Empty,
                    style = style.bodyTextStyle.copy(
                        fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = alignments.getOrElse(index) { TextAlign.Start }
                    )
                )
            }
        }
    }
}

internal data class MarkdownTableData(
    val header: List<InlineMarkdown>,
    val rows: List<List<InlineMarkdown>>,
    val alignments: List<TextAlign>,
)

internal fun parseMarkdownTable(raw: String): MarkdownTableData? {
    val lines = raw
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (lines.size < 2 || !lines[0].contains("|") || !lines[1].contains("|")) {
        return null
    }

    val separator = splitMarkdownTableRow(lines[1])
    if (separator.isEmpty() || separator.any { !it.isMarkdownTableSeparatorCell() }) {
        return null
    }

    val header = splitMarkdownTableRow(lines[0])
    if (header.isEmpty()) {
        return null
    }

    val alignments = separator.map { cell ->
        val trimmed = cell.trim()
        when {
            trimmed.startsWith(":") && trimmed.endsWith(":") -> TextAlign.Center
            trimmed.endsWith(":") -> TextAlign.End
            else -> TextAlign.Start
        }
    }
    val rows = lines.drop(2)
        .filter { it.contains("|") }
        .map { splitMarkdownTableRow(it).map { cell -> InlineMarkdown(AnnotatedString(cell)) } }

    return MarkdownTableData(
        header = header.map { InlineMarkdown(AnnotatedString(it)) },
        rows = rows,
        alignments = alignments
    )
}

internal fun splitMarkdownTableRow(line: String): List<String> {
    return line
        .trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { it.trim() }
}

internal fun String.isMarkdownTableSeparatorCell(): Boolean {
    val trimmed = trim()
    if (trimmed.length < 3) {
        return false
    }

    val body = trimmed
        .removePrefix(":")
        .removeSuffix(":")
        .trim()

    return body.length >= 3 && body.all { it == '-' }
}


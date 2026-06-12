package screen.feature

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.File
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.File
import compose.icons.feathericons.Folder
import compose.icons.feathericons.RefreshCw
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.docsDirectoryLabel
import honkaigamelauncher.desktop_ui.generated.resources.docsEmptyDirectory
import honkaigamelauncher.desktop_ui.generated.resources.docsErrorRead
import honkaigamelauncher.desktop_ui.generated.resources.docsErrorUnresolvedLink
import honkaigamelauncher.desktop_ui.generated.resources.docsGamePluginsSection
import honkaigamelauncher.desktop_ui.generated.resources.docsGeneralSection
import honkaigamelauncher.desktop_ui.generated.resources.docsMissingDirectory
import honkaigamelauncher.desktop_ui.generated.resources.docsMissingGamePath
import honkaigamelauncher.desktop_ui.generated.resources.docsOverviewTitle
import honkaigamelauncher.desktop_ui.generated.resources.docsRefresh
import honkaigamelauncher.desktop_ui.generated.resources.screen_doc
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import org.jetbrains.compose.resources.stringResource
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import screen.IScreenInterface
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.theme.FluentTokens
import viewModel.DocEntry
import viewModel.DocSection
import viewModel.DocsLoadStatus
import viewModel.DocsScreenModel
import java.io.File
import java.net.URI
import io.github.composefluent.component.Text as FluentText

class DocsScreen : Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return "docs"
    }

    override fun getIcon(): ImageVector {
        return EvaIcons.Fill.File
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_doc)
    }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { DocsScreenModel() }
        val listState = rememberLazyListState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DocsOverview(screenModel = screenModel, icon = getIcon())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.92f)
                        .fillMaxHeight()
                ) {
                    FluentCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 10.dp)
                    ) {
                        if (screenModel.documents.isEmpty()) {
                            DocsListEmptyState(screenModel = screenModel)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val generalDocs = screenModel.documents.filter { it.section == DocSection.General }
                                val pluginDocs = screenModel.documents.filter { it.section == DocSection.GamePlugins }

                                if (generalDocs.isNotEmpty()) {
                                    item {
                                        DocsSectionHeader(stringResource(Res.string.docsGeneralSection))
                                    }
                                    items(generalDocs, key = { it.absolutePath }) { entry ->
                                        DocListItem(
                                            entry = entry,
                                            selected = screenModel.selectedDocument?.absolutePath == entry.absolutePath,
                                            onClick = { screenModel.selectDocument(entry.absolutePath) },
                                        )
                                    }
                                }

                                if (pluginDocs.isNotEmpty()) {
                                    item {
                                        DocsSectionHeader(stringResource(Res.string.docsGamePluginsSection))
                                    }
                                    items(pluginDocs, key = { it.absolutePath }) { entry ->
                                        DocListItem(
                                            entry = entry,
                                            selected = screenModel.selectedDocument?.absolutePath == entry.absolutePath,
                                            onClick = { screenModel.selectDocument(entry.absolutePath) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )
                }

                FluentCard(
                    modifier = Modifier
                        .weight(1.7f)
                        .fillMaxHeight()
                ) {
                    when {
                        screenModel.selectedDocument == null || screenModel.loadStatus != DocsLoadStatus.Ready -> {
                            DocsReaderPlaceholder(screenModel = screenModel)
                        }

                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ReaderHeader(screenModel = screenModel)

                                if (screenModel.linkErrorMessage.isNotBlank()) {
                                    InlineWarning(
                                        text = stringResource(
                                            Res.string.docsErrorUnresolvedLink,
                                            screenModel.linkErrorMessage
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    val scrollState = rememberScrollState()

                                    MarkdownPreview(
                                        screenModel = screenModel,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                            .padding(end = 12.dp)
                                    )

                                    VerticalScrollbar(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .padding(vertical = 4.dp),
                                        adapter = rememberScrollbarAdapter(scrollState)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownPreview(
    screenModel: DocsScreenModel,
    modifier: Modifier = Modifier,
) {
    val currentDocumentPath = screenModel.selectedDocument?.absolutePath
    val markdownContent = remember(screenModel.markdownContent, currentDocumentPath) {
        rewriteMarkdownResourceLinks(
            markdown = screenModel.markdownContent,
            currentDocumentPath = currentDocumentPath,
        )
    }
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember(flavour) { MarkdownParser(flavour) }
    val astRoot = remember(markdownContent, parser) {
        parser.buildMarkdownTreeFromString(markdownContent)
    }

    FluentMarkdownDocument(
        root = astRoot,
        source = markdownContent,
        style = rememberDocsMarkdownStyle(),
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun FluentMarkdownDocument(
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
private fun MarkdownBlock(
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
            val table = parseMarkdownTable(extractMarkdownText(node, source))
            if (table != null) {
                MarkdownTable(table, style)
            } else {
                MarkdownTextBlock(
                    text = buildInlineMarkdown(node, source, style),
                    style = style.bodyTextStyle,
                )
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
private fun MarkdownHeading(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    val level = headingLevel(node.type)
    val text = headingText(node, source)
    val headingStyle = style.headingTextStyle(level)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FluentText(text = text, style = headingStyle)
        if (level == 2) {
            MarkdownDivider(style.copy(dividerColor = style.dividerColor.copy(alpha = 0.55f)))
        }
    }
}

@Composable
private fun MarkdownTextBlock(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    FluentText(
        text = text,
        style = style,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun MarkdownCodeBlock(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    val code = when (node.type) {
        MarkdownElementTypes.CODE_FENCE -> {
            val content = node.children
                .filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
                .joinToString("") { extractMarkdownText(it, source) }
                .trimEnd()

            content.ifBlank {
                extractFencedCodeText(extractMarkdownText(node, source))
            }
        }

        else -> extractMarkdownText(node, source).trimEnd()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.codeBackground, RoundedCornerShape(6.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        FluentText(
            text = code,
            style = style.codeBlockTextStyle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MarkdownQuoteBlock(
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
private fun MarkdownList(
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
private fun MarkdownTable(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    val rows = node.children.filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }
    if (rows.isEmpty()) return

    val header = rows.firstOrNull()
        ?.children
        ?.filter { it.type == GFMTokenTypes.CELL }
        ?.map { buildInlineMarkdown(it, source, style) }
        .orEmpty()
    val body = rows.drop(1).map { row ->
        row.children
            .filter { it.type == GFMTokenTypes.CELL }
            .map { buildInlineMarkdown(it, source, style) }
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
private fun MarkdownTable(
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
private fun MarkdownTableRow(
    cells: List<AnnotatedString>,
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
                    text = cells.getOrNull(index) ?: AnnotatedString(""),
                    style = style.bodyTextStyle.copy(
                        fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = alignments.getOrElse(index) { TextAlign.Start }
                    )
                )
            }
        }
    }
}

private data class MarkdownTableData(
    val header: List<AnnotatedString>,
    val rows: List<List<AnnotatedString>>,
    val alignments: List<TextAlign>,
)

private fun parseMarkdownTable(raw: String): MarkdownTableData? {
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
        .map { splitMarkdownTableRow(it).map(::AnnotatedString) }

    return MarkdownTableData(
        header = header.map(::AnnotatedString),
        rows = rows,
        alignments = alignments
    )
}

private fun extractFencedCodeText(raw: String): String {
    val lines = raw.lines()
    if (lines.size <= 2) {
        return raw.trim('`', '~').trim()
    }

    return lines
        .drop(1)
        .dropLastWhile { it.trim().startsWith("```") || it.trim().startsWith("~~~") || it.isBlank() }
        .joinToString("\n")
        .trimEnd()
}

private fun splitMarkdownTableRow(line: String): List<String> {
    return line
        .trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { it.trim() }
}

private fun String.isMarkdownTableSeparatorCell(): Boolean {
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

@Composable
private fun MarkdownDivider(style: DocsMarkdownStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(style.dividerColor)
    )
}

@Composable
private fun rememberDocsMarkdownStyle(): DocsMarkdownStyle {
    val textColor = FluentTheme.colors.text.text.primary
    val mutedColor = FluentTokens.ColorToken.LogLevel.veryVerbose
    val accentColor = FluentTokens.ColorToken.accent
    val unknownColor = FluentTokens.ColorToken.LogLevel.unknown

    return remember(textColor, mutedColor, accentColor, unknownColor) {
        DocsMarkdownStyle(
            textColor = textColor,
            mutedColor = mutedColor,
            accentColor = accentColor,
            codeBackground = unknownColor.copy(alpha = 0.045f),
            inlineCodeBackground = unknownColor.copy(alpha = 0.065f),
            borderColor = unknownColor.copy(alpha = 0.12f),
            dividerColor = unknownColor.copy(alpha = 0.16f),
            subtleBackground = unknownColor.copy(alpha = 0.035f),
        )
    }
}

private data class DocsMarkdownStyle(
    val textColor: Color,
    val mutedColor: Color,
    val accentColor: Color,
    val codeBackground: Color,
    val inlineCodeBackground: Color,
    val borderColor: Color,
    val dividerColor: Color,
    val subtleBackground: Color,
) {
    val bodyTextStyle: TextStyle
        get() = TextStyle(
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )

    val codeBlockTextStyle: TextStyle
        get() = TextStyle(
            color = textColor,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = FontFamily.Monospace,
        )

    fun headingTextStyle(level: Int): TextStyle {
        val size = when (level) {
            1 -> 22.sp
            2 -> 18.sp
            3 -> 16.sp
            4 -> 14.sp
            else -> 14.sp
        }
        return TextStyle(
            color = if (level >= 5) mutedColor else textColor,
            fontSize = size,
            lineHeight = (size.value * 1.32f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun buildInlineMarkdown(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
): AnnotatedString {
    return buildAnnotatedString {
        appendInlineNode(node, source, style)
    }
}

private fun AnnotatedString.Builder.appendInlineNode(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    when (node.type) {
        MarkdownElementTypes.STRONG -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            node.children.forEach { appendInlineNode(it, source, style) }
        }

        MarkdownElementTypes.EMPH -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            node.children.forEach { appendInlineNode(it, source, style) }
        }

        GFMElementTypes.STRIKETHROUGH -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
            node.children.forEach { appendInlineNode(it, source, style) }
        }

        MarkdownElementTypes.CODE_SPAN -> {
            val code = extractMarkdownText(node, source)
                .trim()
                .trim('`')
            withStyle(
                SpanStyle(
                    color = style.textColor,
                    background = style.inlineCodeBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
            ) {
                append(code)
            }
        }

        MarkdownElementTypes.INLINE_LINK,
        MarkdownElementTypes.FULL_REFERENCE_LINK,
        MarkdownElementTypes.SHORT_REFERENCE_LINK,
        MarkdownElementTypes.AUTOLINK -> appendLinkNode(node, source, style)

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

        MarkdownTokenTypes.EOL -> append(" ")

        MarkdownTokenTypes.TEXT,
        MarkdownTokenTypes.ATX_CONTENT,
        MarkdownTokenTypes.SETEXT_CONTENT,
        MarkdownTokenTypes.CODE_LINE,
        GFMTokenTypes.CELL -> append(extractMarkdownText(node, source).trimCellPipes())

        else -> {
            if (node.children.isEmpty()) {
                if (!node.isMarkdownSyntaxToken() && !node.isWhitespaceToken()) {
                    append(extractMarkdownText(node, source))
                }
            } else {
                node.children.forEach { appendInlineNode(it, source, style) }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendLinkNode(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    val label = node.children
        .firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
        ?.let { extractMarkdownText(it, source).trim('[', ']') }
        ?.ifBlank { null }
        ?: extractMarkdownText(node, source)

    withStyle(
        SpanStyle(
            color = style.accentColor,
            textDecoration = TextDecoration.None,
            fontWeight = FontWeight.Medium,
        )
    ) {
        append(label)
    }
}

private fun headingLevel(type: IElementType): Int {
    return when (type) {
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.SETEXT_1 -> 1
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.SETEXT_2 -> 2
        MarkdownElementTypes.ATX_3 -> 3
        MarkdownElementTypes.ATX_4 -> 4
        MarkdownElementTypes.ATX_5 -> 5
        MarkdownElementTypes.ATX_6 -> 6
        else -> 6
    }
}

private fun headingText(node: ASTNode, source: String): String {
    return node.children
        .firstOrNull {
            it.type == MarkdownTokenTypes.ATX_CONTENT ||
                it.type == MarkdownTokenTypes.SETEXT_CONTENT
        }
        ?.let { extractMarkdownText(it, source).trim() }
        ?: extractMarkdownText(node, source)
            .trim()
            .trimStart('#')
            .trim()
            .trimEnd('#')
            .trim()
}

private fun extractMarkdownText(node: ASTNode, source: String): String {
    return source.substring(node.startOffset, node.endOffset)
}

private fun ASTNode.isWhitespaceToken(): Boolean {
    return type == MarkdownTokenTypes.WHITE_SPACE || type == MarkdownTokenTypes.EOL
}

private fun ASTNode.isMarkdownSyntaxToken(): Boolean {
    return type == MarkdownTokenTypes.EMPH ||
        type == MarkdownTokenTypes.BACKTICK ||
        type == MarkdownTokenTypes.ESCAPED_BACKTICKS ||
        type == MarkdownTokenTypes.LBRACKET ||
        type == MarkdownTokenTypes.RBRACKET ||
        type == MarkdownTokenTypes.LPAREN ||
        type == MarkdownTokenTypes.RPAREN ||
        type == MarkdownTokenTypes.EXCLAMATION_MARK ||
        type == MarkdownTokenTypes.COLON ||
        type == MarkdownTokenTypes.LT ||
        type == MarkdownTokenTypes.GT ||
        type == MarkdownTokenTypes.ATX_HEADER ||
        type == MarkdownTokenTypes.SETEXT_1 ||
        type == MarkdownTokenTypes.SETEXT_2 ||
        type == MarkdownTokenTypes.LIST_BULLET ||
        type == MarkdownTokenTypes.LIST_NUMBER ||
        type == MarkdownTokenTypes.LINK_ID ||
        type == MarkdownTokenTypes.LINK_TITLE ||
        type == MarkdownTokenTypes.CODE_FENCE_START ||
        type == MarkdownTokenTypes.CODE_FENCE_END ||
        type == MarkdownTokenTypes.FENCE_LANG ||
        type == GFMTokenTypes.TILDE ||
        type == GFMTokenTypes.TABLE_SEPARATOR
}

private fun String.trimCellPipes(): String {
    return trim().trim('|').trim()
}

private fun rewriteMarkdownResourceLinks(
    markdown: String,
    currentDocumentPath: String?,
): String {
    return MarkdownResourceLinkRegex.replace(markdown) { match ->
        val prefix = match.groupValues[1]
        val label = match.groupValues[2]
        val rawValue = match.groupValues[3]
        val suffix = match.groupValues[4]
        val rewritten = rewriteMarkdownResourceLink(rawValue, currentDocumentPath)

        "$prefix$label]($rewritten$suffix)"
    }
}

private fun rewriteMarkdownResourceLink(
    rawValue: String,
    currentDocumentPath: String?,
): String {
    val value = rawValue.trim()
    if (value.isBlank() || value.startsWith("#") || value.hasKnownScheme()) {
        return rawValue
    }

    if (value.isMarkdownDocumentLink()) {
        return rawValue
    }

    return resolveMarkdownResource(currentDocumentPath, value)
        ?.toURI()
        ?.toASCIIString()
        ?: rawValue
}

private fun resolveMarkdownResource(currentDocumentPath: String?, rawLink: String): File? {
    val currentFile = currentDocumentPath?.let(::File) ?: return null
    val cleaned = rawLink.substringBefore('#').trim()
    if (cleaned.isBlank()) {
        return null
    }

    val normalized = runCatching { URI(cleaned).path }.getOrDefault(cleaned)
    return if (normalized.startsWith("/")) {
        File(currentFile.parentFile, normalized.removePrefix("/")).normalize()
    } else {
        File(currentFile.parentFile, normalized).normalize()
    }.takeIf { it.exists() && it.isFile }
}

private fun String.isMarkdownDocumentLink(): Boolean {
    val path = runCatching { URI(this).path }
        .getOrDefault(substringBefore('#').substringBefore('?'))

    return path.endsWith(".md", ignoreCase = true)
}

private fun String.hasKnownScheme(): Boolean {
    val lowerValue = lowercase()
    return lowerValue.startsWith("http://") ||
        lowerValue.startsWith("https://") ||
        lowerValue.startsWith("file:") ||
        lowerValue.startsWith("data:") ||
        lowerValue.startsWith("mailto:")
}

private val MarkdownResourceLinkRegex = Regex("""(!?\[)([^\]]*)]\(([^)\s]+)([^)]*)\)""")

@Composable
private fun DocsOverview(
    screenModel: DocsScreenModel,
    icon: ImageVector,
) {
    val statusColor = docsStatusColor(screenModel.loadStatus)
    val docsPath = screenModel.docsDirectory.ifBlank { "-" }

    FluentCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.24f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FluentText(
                        text = stringResource(Res.string.screen_doc),
                        style = FluentTheme.typography.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FluentText(
                        text = "(${screenModel.documents.size})",
                        style = FluentTheme.typography.caption,
                        color = FluentTokens.ColorToken.LogLevel.veryVerbose,
                        maxLines = 1
                    )
                    FluentText(
                        text = docsStatusText(screenModel),
                        style = FluentTheme.typography.caption,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FluentText(
                    text = "${stringResource(Res.string.docsDirectoryLabel)}: $docsPath",
                    style = FluentTheme.typography.caption,
                    color = FluentTokens.ColorToken.LogLevel.veryVerbose,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FluentButton(
                onClick = { screenModel.refresh() },
                disabled = screenModel.isLoading,
                iconOnly = true
            ) {
                Icon(
                    imageVector = compose.icons.FeatherIcons.RefreshCw,
                    contentDescription = stringResource(Res.string.docsRefresh)
                )
            }
        }
    }
}

@Composable
private fun DocsSectionHeader(title: String) {
    FluentText(
        text = title,
        style = FluentTheme.typography.bodyStrong,
        color = FluentTokens.ColorToken.LogLevel.veryVerbose,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun DocListItem(
    entry: DocEntry,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = if (selected) FluentTokens.ColorToken.accent else FluentTokens.ColorToken.LogLevel.unknown
    val title = if (entry.isDefault) {
        stringResource(Res.string.docsOverviewTitle)
    } else {
        entry.title
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                accentColor.copy(alpha = if (selected) 0.10f else 0.04f),
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                accentColor.copy(alpha = if (selected) 0.26f else 0.10f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (entry.section == DocSection.GamePlugins) {
                    compose.icons.FeatherIcons.Folder
                } else {
                    compose.icons.FeatherIcons.File
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            FluentText(
                text = title,
                style = FluentTheme.typography.bodyStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FluentText(
                text = entry.relativePath,
                style = FluentTheme.typography.caption,
                color = FluentTokens.ColorToken.LogLevel.veryVerbose,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DocsListEmptyState(screenModel: DocsScreenModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FluentText(
            text = docsStatusText(screenModel),
            style = FluentTheme.typography.body,
            color = docsStatusColor(screenModel.loadStatus)
        )
    }
}

@Composable
private fun ReaderHeader(screenModel: DocsScreenModel) {
    val entry = screenModel.selectedDocument ?: return
    val title = if (entry.isDefault) {
        stringResource(Res.string.docsOverviewTitle)
    } else {
        entry.title
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FluentText(
            text = title,
            style = FluentTheme.typography.title
        )
        FluentText(
            text = entry.relativePath,
            style = FluentTheme.typography.caption,
            color = FluentTokens.ColorToken.LogLevel.veryVerbose
        )
    }
}

@Composable
private fun DocsReaderPlaceholder(screenModel: DocsScreenModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        docsStatusColor(screenModel.loadStatus).copy(alpha = 0.10f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        docsStatusColor(screenModel.loadStatus).copy(alpha = 0.18f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = compose.icons.FeatherIcons.AlertCircle,
                    contentDescription = null,
                    tint = docsStatusColor(screenModel.loadStatus),
                    modifier = Modifier.size(26.dp)
                )
            }

            FluentText(
                text = docsStatusText(screenModel),
                style = FluentTheme.typography.body,
                color = docsStatusColor(screenModel.loadStatus),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (screenModel.loadStatus == DocsLoadStatus.Error && screenModel.errorMessage.isNotBlank()) {
                FluentText(
                    text = stringResource(Res.string.docsErrorRead, screenModel.errorMessage),
                    style = FluentTheme.typography.caption,
                    color = FluentTokens.ColorToken.LogLevel.error
                )
            }
        }
    }
}

@Composable
private fun InlineWarning(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                FluentTokens.ColorToken.LogLevel.warning.copy(alpha = 0.10f),
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                FluentTokens.ColorToken.LogLevel.warning.copy(alpha = 0.18f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = compose.icons.FeatherIcons.AlertCircle,
            contentDescription = null,
            tint = FluentTokens.ColorToken.LogLevel.warning,
            modifier = Modifier.size(16.dp)
        )
        FluentText(
            text = text,
            style = FluentTheme.typography.caption,
            color = FluentTokens.ColorToken.LogLevel.warning
        )
    }
}

@Composable
private fun docsStatusText(screenModel: DocsScreenModel): String {
    return when (screenModel.loadStatus) {
        DocsLoadStatus.MissingGamePath -> stringResource(Res.string.docsMissingGamePath)
        DocsLoadStatus.MissingDocsDirectory -> stringResource(Res.string.docsMissingDirectory)
        DocsLoadStatus.Empty -> stringResource(Res.string.docsEmptyDirectory)
        DocsLoadStatus.Ready -> screenModel.selectedDocument?.relativePath
            ?: stringResource(Res.string.screen_doc)
        DocsLoadStatus.Error -> stringResource(Res.string.docsErrorRead, screenModel.errorMessage.ifBlank { "-" })
    }
}

private fun docsStatusColor(status: DocsLoadStatus): Color {
    return when (status) {
        DocsLoadStatus.Ready -> FluentTokens.ColorToken.accent
        DocsLoadStatus.MissingGamePath,
        DocsLoadStatus.MissingDocsDirectory,
        DocsLoadStatus.Empty -> FluentTokens.ColorToken.LogLevel.warning
        DocsLoadStatus.Error -> FluentTokens.ColorToken.LogLevel.error
    }
}

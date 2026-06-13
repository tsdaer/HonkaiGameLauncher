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
internal fun MarkdownPreview(
    screenModel: DocsScreenModel,
    markdown: String,
    controller: DocsReaderController,
    headingSlugs: Map<Int, String>,
    modifier: Modifier = Modifier,
) {
    val astRoot = rememberMarkdownAst(markdown)

    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val linkListener = remember(screenModel, controller, uriHandler, scope) {
        LinkInteractionListener { annotation ->
            val href = (annotation as? LinkAnnotation.Clickable)?.tag ?: return@LinkInteractionListener
            when {
                href.startsWith("#") ->
                    scope.launch { controller.scrollToAnchor(href.removePrefix("#")) }

                href.hasKnownScheme() ->
                    runCatching { uriHandler.openUri(href) }

                else -> {
                    if (!screenModel.openLinkedDocument(href)) {
                        runCatching { uriHandler.openUri(href) }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalDocsReaderController provides controller,
        LocalDocsHeadingSlugs provides headingSlugs,
        LocalMarkdownLinkListener provides linkListener,
    ) {
        FluentMarkdownDocument(
            root = astRoot,
            source = markdown,
            style = rememberDocsMarkdownStyle(),
            modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

internal data class DocTocItem(
    val level: Int,
    val title: String,
    val slug: String,
)

@Stable
internal class DocsReaderController(
    val scrollState: ScrollState,
) {
    var viewportTopInWindow: Float = 0f

    private val headingOffsets = mutableStateMapOf<String, Int>()

    fun registerHeading(slug: String, contentTopPx: Int) {
        headingOffsets[slug] = contentTopPx.coerceAtLeast(0)
    }

    val registeredCount: Int
        get() = headingOffsets.size

    fun resolveAnchor(anchor: String): Int? {
        headingOffsets[anchor]?.let { return it }
        return headingOffsets[slugifyHeading(anchor)]
    }

    suspend fun scrollToAnchor(anchor: String): Boolean {
        val target = resolveAnchor(anchor) ?: return false
        scrollState.animateScrollTo((target - ANCHOR_TOP_PADDING_PX).coerceAtLeast(0))
        return true
    }

    private companion object {
        const val ANCHOR_TOP_PADDING_PX = 8
    }
}

private val LocalDocsReaderController = compositionLocalOf<DocsReaderController?> { null }
private val LocalDocsHeadingSlugs = compositionLocalOf<Map<Int, String>> { emptyMap() }
private val LocalMarkdownLinkListener = compositionLocalOf<LinkInteractionListener?> { null }

/**
 * GitHub-style heading slug. Keeps unicode letters/digits (so CJK headings work),
 * maps spaces/hyphens to '-', keeps '_', and drops other punctuation.
 */
internal fun slugifyHeading(raw: String): String {
    val builder = StringBuilder()
    raw.trim().lowercase().forEach { ch ->
        when {
            ch.isLetterOrDigit() -> builder.append(ch)
            ch == ' ' || ch == '-' -> builder.append('-')
            ch == '_' -> builder.append('_')
        }
    }
    return builder.toString().replace(MultiHyphenRegex, "-").trim('-')
}

/**
 * Parse [markdown] once to build the table of contents and a stable
 * heading-start-offset → slug map (slugs deduplicated in document order).
 */
internal fun extractDocHeadings(markdown: String): Pair<List<DocTocItem>, Map<Int, String>> {
    if (markdown.isBlank()) {
        return emptyList<DocTocItem>() to emptyMap()
    }
    val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(markdown)
    val items = mutableListOf<DocTocItem>()
    val slugByOffset = linkedMapOf<Int, String>()
    val usedSlugs = mutableMapOf<String, Int>()

    fun visit(node: ASTNode) {
        val level = headingLevelOrNull(node.type)
        if (level != null) {
            val title = headingText(node, markdown)
            if (title.isNotBlank()) {
                val base = slugifyHeading(title).ifBlank { "section" }
                val seen = usedSlugs.getOrElse(base) { 0 }
                usedSlugs[base] = seen + 1
                val slug = if (seen == 0) base else "$base-$seen"
                items.add(DocTocItem(level = level, title = title, slug = slug))
                slugByOffset[node.startOffset] = slug
            }
        }
        node.children.forEach(::visit)
    }
    visit(root)
    return items to slugByOffset
}

@Composable
private fun rememberMarkdownAst(markdown: String): ASTNode {
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember(flavour) { MarkdownParser(flavour) }
    return remember(markdown, parser) {
        parser.buildMarkdownTreeFromString(markdown)
    }
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
private fun MarkdownHeading(
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
private fun MarkdownTextBlock(
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
private fun MarkdownCodeBlock(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    val fencedCode = if (node.type == MarkdownElementTypes.CODE_FENCE) {
        parseFencedCodeBlock(extractMarkdownText(node, source))
    } else {
        null
    }
    val astCodeContent = node.children
        .filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
        .joinToString("") { extractMarkdownText(it, source) }
        .trimEnd()
    val code = fencedCode?.code
        ?: astCodeContent
            .ifBlank { extractMarkdownText(node, source).trimEnd() }
            .let(::trimFencedCodeContent)
    val language = fencedCode?.language ?: if (node.type == MarkdownElementTypes.CODE_FENCE) {
        extractFenceLanguage(node, source)
    } else {
        null
    }
    val trailingMarkdown = fencedCode?.trailingMarkdown.orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (language.equals("mermaid", ignoreCase = true)) {
            MarkdownMermaidBlock(code = code, style = style)
        } else {
            MarkdownCodeBlockContent(
                code = code,
                language = language,
                style = style
            )
        }

        if (trailingMarkdown.isNotBlank()) {
            MarkdownTrailingDocument(markdown = trailingMarkdown, style = style)
        }
    }
}

@Composable
private fun MarkdownCodeBlockContent(
    code: String,
    language: String?,
    style: DocsMarkdownStyle,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.codeBackground, RoundedCornerShape(6.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!language.isNullOrBlank()) {
                MarkdownCodeLanguageBadge(language = language, style = style)
            }
            SyntaxHighlightedCodeText(
                code = code,
                language = language,
                style = style,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MarkdownTrailingDocument(
    markdown: String,
    style: DocsMarkdownStyle,
) {
    val root = rememberMarkdownAst(markdown)

    FluentMarkdownDocument(
        root = root,
        source = markdown,
        style = style,
    )
}

@Composable
private fun MarkdownCodeLanguageBadge(
    language: String,
    style: DocsMarkdownStyle,
) {
    FluentText(
        text = language.uppercase(),
        style = TextStyle(
            color = style.mutedColor,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = Modifier
            .background(style.subtleBackground, RoundedCornerShape(4.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun MarkdownMathBlock(
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
private fun MarkdownMermaidBlock(
    code: String,
    style: DocsMarkdownStyle,
) {
    val classDiagram = remember(code) { parseMermaidClassDiagram(code) }
    val graphDiagram = remember(code) {
        if (classDiagram == null) parseMermaidGraph(code) else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.subtleBackground, RoundedCornerShape(6.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(style.accentColor.copy(alpha = 0.76f), RoundedCornerShape(50))
            )
            FluentText(
                text = "Mermaid diagram",
                style = style.bodyTextStyle.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = style.textColor,
                )
            )
        }

        when {
            classDiagram != null -> MermaidClassDiagram(diagram = classDiagram, style = style)
            graphDiagram != null -> MermaidFlowchart(graph = graphDiagram, style = style)
            else -> {
                FluentText(
                    text = "This Mermaid diagram is shown as source because native rendering currently supports classDiagram and graph/flowchart.",
                    style = style.bodyTextStyle.copy(color = style.mutedColor)
                )
                SyntaxHighlightedCodeText(
                    code = code,
                    language = "mermaid",
                    style = style,
                    fallbackStyle = style.codeBlockTextStyle.copy(color = style.mutedColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MermaidClassDiagram(
    diagram: MermaidClassDiagramData,
    style: DocsMarkdownStyle,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (diagram.relations.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(style.codeBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                diagram.relations.forEach { relation ->
                    MermaidClassRelation(relation = relation, style = style)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            diagram.classes.forEach { item ->
                MermaidClassCard(item = item, style = style)
            }
        }
    }
}

@Composable
private fun MermaidClassRelation(
    relation: MermaidClassRelationData,
    style: DocsMarkdownStyle,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FluentText(
            text = relation.from,
            style = style.codeBlockTextStyle.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        FluentText(
            text = relation.arrow,
            style = style.bodyTextStyle.copy(color = style.accentColor, fontWeight = FontWeight.SemiBold),
        )
        FluentText(
            text = relation.to,
            style = style.codeBlockTextStyle.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        if (!relation.label.isNullOrBlank()) {
            FluentText(
                text = relation.label,
                style = style.bodyTextStyle.copy(color = style.mutedColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MermaidClassCard(
    item: MermaidClassData,
    style: DocsMarkdownStyle,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.subtleBackground, RoundedCornerShape(6.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(style.accentColor.copy(alpha = 0.10f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FluentText(
                text = item.name,
                style = style.bodyTextStyle.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            item.stereotypes.forEach { stereotype ->
                MermaidClassBadge(text = stereotype, style = style)
            }
        }

        if (item.members.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item.members.forEach { member ->
                    FluentText(
                        text = member,
                        style = style.codeBlockTextStyle.copy(color = style.textColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MermaidClassBadge(
    text: String,
    style: DocsMarkdownStyle,
) {
    FluentText(
        text = text,
        style = TextStyle(
            color = style.mutedColor,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = Modifier
            .background(style.codeBackground, RoundedCornerShape(4.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun MermaidFlowchart(
    graph: MermaidGraphData,
    style: DocsMarkdownStyle,
) {
    val layout = remember(graph) { computeMermaidGraphLayout(graph) }
    val maxLayer = layout.values.maxOfOrNull { it.first } ?: 0
    val maxIndex = layout.values.maxOfOrNull { it.second } ?: 0

    val cellWidth = 140
    val cellHeight = 52
    val hGap = 60
    val vGap = 50

    val isVertical = graph.direction == MermaidGraphDirection.TD
    val totalCols = if (isVertical) maxIndex + 1 else maxLayer + 1
    val totalRows = if (isVertical) maxLayer + 1 else maxIndex + 1
    val totalWidth = totalCols * cellWidth + (totalCols - 1).coerceAtLeast(0) * hGap
    val totalHeight = totalRows * cellHeight + (totalRows - 1).coerceAtLeast(0) * vGap

    val nodeRects = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val density = LocalDensity.current
    val hScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(hScrollState)
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { totalWidth.toDp() + 24.dp },
                        height = with(density) { totalHeight.toDp() + 24.dp }
                    )
                    .padding(12.dp)
            ) {
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                graph.edges.forEach { edge ->
                    val fromRect = nodeRects[edge.fromId] ?: return@forEach
                    val toRect = nodeRects[edge.toId] ?: return@forEach
                    drawMermaidEdge(fromRect, toRect, edge, style, isVertical)
                }
            }

            graph.nodes.forEach { node ->
                val pos = layout[node.id] ?: return@forEach
                val col = if (isVertical) pos.second else pos.first
                val row = if (isVertical) pos.first else pos.second
                val xPx = col * (cellWidth + hGap)
                val yPx = row * (cellHeight + vGap)

                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { xPx.toDp() },
                            y = with(density) { yPx.toDp() }
                        )
                        .size(
                            width = with(density) { cellWidth.toDp() },
                            height = with(density) { cellHeight.toDp() }
                        )
                        .onGloballyPositioned { coords ->
                            nodeRects[node.id] = coords.boundsInParent()
                        }
                ) {
                    MermaidFlowchartNode(node = node, style = style)
                }
            }

            graph.edges.filter { it.label != null }.forEach { edge ->
                val fromPos = layout[edge.fromId] ?: return@forEach
                val toPos = layout[edge.toId] ?: return@forEach
                val fromCol = if (isVertical) fromPos.second else fromPos.first
                val fromRow = if (isVertical) fromPos.first else fromPos.second
                val toCol = if (isVertical) toPos.second else toPos.first
                val toRow = if (isVertical) toPos.first else toPos.second
                val midX = ((fromCol + toCol) * (cellWidth + hGap) + cellWidth) / 2
                val midY = ((fromRow + toRow) * (cellHeight + vGap) + cellHeight) / 2

                FluentText(
                    text = edge.label!!,
                    style = TextStyle(
                        color = style.mutedColor,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                    modifier = Modifier
                        .offset(
                            x = with(density) { midX.toDp() - 16.dp },
                            y = with(density) { midY.toDp() - 8.dp }
                        )
                        .background(style.subtleBackground, RoundedCornerShape(3.dp))
                        .border(0.5.dp, style.borderColor, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
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
private fun MermaidFlowchartNode(
    node: MermaidGraphNodeData,
    style: DocsMarkdownStyle,
) {
    val shape = when (node.shape) {
        MermaidNodeShape.RECT -> RoundedCornerShape(4.dp)
        MermaidNodeShape.ROUND_RECT -> RoundedCornerShape(12.dp)
        MermaidNodeShape.STADIUM -> RoundedCornerShape(50)
        MermaidNodeShape.CIRCLE -> androidx.compose.foundation.shape.CircleShape
        MermaidNodeShape.DIAMOND -> androidx.compose.foundation.shape.CutCornerShape(50)
        MermaidNodeShape.HEXAGON -> androidx.compose.foundation.shape.CutCornerShape(30)
        MermaidNodeShape.ASYMMETRIC -> RoundedCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 0.dp)
        MermaidNodeShape.SUBROUTINE -> RoundedCornerShape(2.dp)
        MermaidNodeShape.CYLINDER -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    }

    val borderColor = when (node.shape) {
        MermaidNodeShape.DIAMOND -> style.accentColor.copy(alpha = 0.5f)
        else -> style.borderColor
    }
    val bgColor = when (node.shape) {
        MermaidNodeShape.DIAMOND -> style.accentColor.copy(alpha = 0.08f)
        else -> style.subtleBackground
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        FluentText(
            text = node.label,
            style = style.bodyTextStyle.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMermaidEdge(
    from: androidx.compose.ui.geometry.Rect,
    to: androidx.compose.ui.geometry.Rect,
    edge: MermaidGraphEdgeData,
    style: DocsMarkdownStyle,
    isVertical: Boolean,
) {
    val start = if (isVertical) {
        androidx.compose.ui.geometry.Offset(from.center.x, from.bottom)
    } else {
        androidx.compose.ui.geometry.Offset(from.right, from.center.y)
    }
    val end = if (isVertical) {
        androidx.compose.ui.geometry.Offset(to.center.x, to.top)
    } else {
        androidx.compose.ui.geometry.Offset(to.left, to.center.y)
    }

    val color = style.mutedColor.copy(alpha = 0.7f)
    val strokeWidth = when (edge.style) {
        MermaidEdgeStyle.THICK -> 3f
        else -> 1.5f
    }
    val pathEffect = when (edge.style) {
        MermaidEdgeStyle.DOTTED -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
        else -> null
    }

    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        pathEffect = pathEffect,
    )

    if (edge.hasArrow) {
        drawMermaidArrowhead(end, start, color, strokeWidth)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMermaidArrowhead(
    tip: androidx.compose.ui.geometry.Offset,
    from: androidx.compose.ui.geometry.Offset,
    color: Color,
    strokeWidth: Float,
) {
    val arrowSize = 8f + strokeWidth
    val dx = tip.x - from.x
    val dy = tip.y - from.y
    val len = kotlin.math.sqrt(dx * dx + dy * dy)
    if (len < 1f) return

    val ux = dx / len
    val uy = dy / len
    val px = -uy
    val py = ux

    val base = androidx.compose.ui.geometry.Offset(
        tip.x - ux * arrowSize,
        tip.y - uy * arrowSize
    )
    val left = androidx.compose.ui.geometry.Offset(
        base.x + px * arrowSize * 0.5f,
        base.y + py * arrowSize * 0.5f
    )
    val right = androidx.compose.ui.geometry.Offset(
        base.x - px * arrowSize * 0.5f,
        base.y - py * arrowSize * 0.5f
    )

    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path = path, color = color)
}

@Composable
private fun SyntaxHighlightedCodeText(
    code: String,
    language: String?,
    style: DocsMarkdownStyle,
    modifier: Modifier = Modifier,
    fallbackStyle: TextStyle = style.codeBlockTextStyle,
) {
    val isDarkTheme = LocalAppUiSettings.current.isDarkTheme
    val syntaxLanguage = remember(language) { language?.toSyntaxLanguage() }
    val syntaxTheme = remember(isDarkTheme) { SyntaxThemes.themes(isDarkTheme).values.firstOrNull() }
    val highlights = remember(code, syntaxLanguage, syntaxTheme) {
        if (syntaxLanguage != null && syntaxTheme != null) {
            Highlights.Builder(code = code)
                .language(syntaxLanguage)
                .theme(syntaxTheme)
                .build()
        } else {
            null
        }
    }
    val highlightedCode = remember(code, highlights) {
        highlights
            ?.getHighlights()
            ?.toHighlightedCode(code)
            ?: AnnotatedString(code)
    }

    if (highlights == null) {
        FluentText(
            text = code,
            style = fallbackStyle,
            modifier = modifier
        )
    } else {
        FluentText(
            text = highlightedCode,
            style = fallbackStyle,
            modifier = modifier
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

private data class MarkdownTableData(
    val header: List<InlineMarkdown>,
    val rows: List<List<InlineMarkdown>>,
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
        .map { splitMarkdownTableRow(it).map { cell -> InlineMarkdown(AnnotatedString(cell)) } }

    return MarkdownTableData(
        header = header.map { InlineMarkdown(AnnotatedString(it)) },
        rows = rows,
        alignments = alignments
    )
}

private data class FencedCodeBlock(
    val language: String?,
    val code: String,
    val trailingMarkdown: String,
)

private data class MermaidClassDiagramData(
    val classes: List<MermaidClassData>,
    val relations: List<MermaidClassRelationData>,
)

private data class MermaidClassData(
    val name: String,
    val members: List<String>,
    val stereotypes: List<String>,
)

private data class MermaidClassRelationData(
    val from: String,
    val arrow: String,
    val to: String,
    val label: String?,
)

private enum class MermaidGraphDirection { TD, LR }

private enum class MermaidNodeShape {
    RECT, ROUND_RECT, STADIUM, CIRCLE, DIAMOND, HEXAGON, ASYMMETRIC, SUBROUTINE, CYLINDER
}

private data class MermaidGraphNodeData(
    val id: String,
    val label: String,
    val shape: MermaidNodeShape,
)

private enum class MermaidEdgeStyle { SOLID, DOTTED, THICK }

private data class MermaidGraphEdgeData(
    val fromId: String,
    val toId: String,
    val label: String?,
    val style: MermaidEdgeStyle,
    val hasArrow: Boolean,
)

private data class MermaidGraphData(
    val direction: MermaidGraphDirection,
    val nodes: List<MermaidGraphNodeData>,
    val edges: List<MermaidGraphEdgeData>,
)

private fun parseMermaidClassDiagram(raw: String): MermaidClassDiagramData? {
    val lines = raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith("%%") }
        .toList()

    if (lines.none { it == "classDiagram" || it.startsWith("classDiagram ") }) {
        return null
    }

    val classes = linkedMapOf<String, MermaidClassBuilder>()
    val relations = mutableListOf<MermaidClassRelationData>()
    var currentClass: MermaidClassBuilder? = null

    fun classBuilder(name: String): MermaidClassBuilder {
        return classes.getOrPut(name) { MermaidClassBuilder(name) }
    }

    lines.drop(1).forEach { line ->
        when {
            currentClass != null -> {
                if (line == "}") {
                    currentClass = null
                } else {
                    currentClass.members.add(line)
                }
            }

            line.startsWith("class ") && line.endsWith("{") -> {
                val name = line
                    .removePrefix("class ")
                    .removeSuffix("{")
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?: return@forEach
                currentClass = classBuilder(name)
            }

            line.startsWith("class ") -> {
                val name = line
                    .removePrefix("class ")
                    .substringBefore(' ')
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?: return@forEach
                classBuilder(name)
            }

            line.startsWith("<<") && line.contains(">>") -> {
                val stereotype = line.substringAfter("<<").substringBefore(">>").trim()
                val name = line.substringAfter(">>").trim()
                if (stereotype.isNotBlank() && name.isNotBlank()) {
                    classBuilder(name).stereotypes.add(stereotype)
                }
            }

            line.startsWith("click ") -> Unit

            line.contains(" : ") && !line.substringBefore(" : ").trim().contains(' ') -> {
                val name = line.substringBefore(" : ").trim()
                val member = line.substringAfter(" : ").trim()
                if (name.isNotBlank() && member.isNotBlank()) {
                    classBuilder(name).members.add(member)
                }
            }

            else -> parseMermaidClassRelation(line)?.let { relation ->
                relations.add(relation)
                classBuilder(relation.from)
                classBuilder(relation.to)
            }
        }
    }

    val parsedClasses = classes.values.map { it.toData() }
    if (parsedClasses.isEmpty() && relations.isEmpty()) {
        return null
    }

    return MermaidClassDiagramData(
        classes = parsedClasses,
        relations = relations,
    )
}

private fun parseMermaidClassRelation(line: String): MermaidClassRelationData? {
    val match = MermaidClassRelationRegex.matchEntire(line) ?: return null
    return MermaidClassRelationData(
        from = match.groupValues[1],
        arrow = match.groupValues[2],
        to = match.groupValues[3],
        label = match.groupValues.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() },
    )
}

private class MermaidClassBuilder(
    val name: String,
) {
    val members = mutableListOf<String>()
    val stereotypes = mutableListOf<String>()

    fun toData(): MermaidClassData {
        return MermaidClassData(
            name = name,
            members = members,
            stereotypes = stereotypes.distinct(),
        )
    }
}

private fun parseMermaidGraph(raw: String): MermaidGraphData? {
    val lines = raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith("%%") }
        .toList()

    val headerLine = lines.firstOrNull() ?: return null
    val headerMatch = MermaidGraphHeaderRegex.matchEntire(headerLine) ?: return null
    val direction = when (headerMatch.groupValues[1].uppercase()) {
        "LR", "RL" -> MermaidGraphDirection.LR
        else -> MermaidGraphDirection.TD
    }

    val nodeMap = linkedMapOf<String, MermaidGraphNodeBuilder>()
    val edges = mutableListOf<MermaidGraphEdgeData>()

    fun nodeBuilder(id: String): MermaidGraphNodeBuilder {
        return nodeMap.getOrPut(id) { MermaidGraphNodeBuilder(id) }
    }

    lines.drop(1).forEach { line ->
        if (line.startsWith("subgraph ") || line == "end" ||
            line.startsWith("style ") || line.startsWith("linkStyle ") ||
            line.startsWith("classDef ") || line.startsWith("class ") ||
            line.startsWith("click ")
        ) {
            return@forEach
        }

        val segments = splitMermaidGraphEdges(line)
        if (segments.size < 2) {
            val nodeMatch = MermaidGraphNodeDeclRegex.matchEntire(line)
            if (nodeMatch != null) {
                val id = nodeMatch.groupValues[1]
                val (shape, label) = parseMermaidNodeShapeAndLabel(nodeMatch.groupValues[2], id)
                val builder = nodeBuilder(id)
                builder.label = label
                builder.shape = shape
            }
            return@forEach
        }

        for (i in 0 until segments.size - 1) {
            val fromPart = segments[i]
            val toPart = segments[i + 1]

            val fromNode = parseMermaidGraphNodeRef(fromPart.nodeText)
            val toNode = parseMermaidGraphNodeRef(toPart.nodeText)

            val fromBuilder = nodeBuilder(fromNode.id)
            if (fromNode.label != null) fromBuilder.label = fromNode.label
            if (fromNode.shape != MermaidNodeShape.RECT) fromBuilder.shape = fromNode.shape

            val toBuilder = nodeBuilder(toNode.id)
            if (toNode.label != null) toBuilder.label = toNode.label
            if (toNode.shape != MermaidNodeShape.RECT) toBuilder.shape = toNode.shape

            edges.add(
                MermaidGraphEdgeData(
                    fromId = fromNode.id,
                    toId = toNode.id,
                    label = fromPart.edgeLabel,
                    style = fromPart.edgeStyle,
                    hasArrow = fromPart.edgeHasArrow,
                )
            )
        }
    }

    val nodes = nodeMap.values.map { it.toData() }
    if (nodes.isEmpty()) return null

    return MermaidGraphData(
        direction = direction,
        nodes = nodes,
        edges = edges,
    )
}

private class MermaidGraphNodeBuilder(val id: String) {
    var label: String? = null
    var shape: MermaidNodeShape = MermaidNodeShape.RECT

    fun toData(): MermaidGraphNodeData {
        return MermaidGraphNodeData(
            id = id,
            label = label ?: id,
            shape = shape,
        )
    }
}

private data class MermaidGraphSegment(
    val nodeText: String,
    val edgeLabel: String?,
    val edgeStyle: MermaidEdgeStyle,
    val edgeHasArrow: Boolean,
)

private fun splitMermaidGraphEdges(line: String): List<MermaidGraphSegment> {
    val segments = mutableListOf<MermaidGraphSegment>()
    var remaining = line.trim()

    while (remaining.isNotBlank()) {
        val edgeMatch = MermaidGraphEdgeRegex.find(remaining)
        if (edgeMatch == null) {
            segments.add(MermaidGraphSegment(remaining.trim(), null, MermaidEdgeStyle.SOLID, true))
            break
        }

        val nodeText = remaining.substring(0, edgeMatch.range.first).trim()
        val edgeStr = edgeMatch.groupValues[0]
        val labelFromPipe = edgeMatch.groupValues[1].takeIf { it.isNotBlank() }
            ?: edgeMatch.groupValues[2].takeIf { it.isNotBlank() }
            ?: edgeMatch.groupValues[3].takeIf { it.isNotBlank() }
        val labelFromInline = edgeMatch.groupValues[4].takeIf { it.isNotBlank() }
        val label = labelFromPipe ?: labelFromInline

        val style = when {
            edgeStr.contains("=") -> MermaidEdgeStyle.THICK
            edgeStr.contains(".") -> MermaidEdgeStyle.DOTTED
            else -> MermaidEdgeStyle.SOLID
        }
        val hasArrow = edgeStr.contains(">")

        if (nodeText.isNotBlank()) {
            segments.add(MermaidGraphSegment(nodeText, label, style, hasArrow))
        }

        remaining = remaining.substring(edgeMatch.range.last + 1).trim()
    }

    return segments
}

private data class MermaidGraphNodeRef(
    val id: String,
    val label: String?,
    val shape: MermaidNodeShape,
)

private fun parseMermaidGraphNodeRef(text: String): MermaidGraphNodeRef {
    val trimmed = text.trim().removeSuffix(";").trim()
    val idEnd = trimmed.indexOfFirst { it in "([{<>/" }
    if (idEnd <= 0) {
        return MermaidGraphNodeRef(trimmed, null, MermaidNodeShape.RECT)
    }

    val id = trimmed.substring(0, idEnd).trim()
    val rest = trimmed.substring(idEnd)
    val (shape, label) = parseMermaidNodeShapeAndLabel(rest, id)
    return MermaidGraphNodeRef(id, label, shape)
}

private fun parseMermaidNodeShapeAndLabel(shapeText: String, fallbackLabel: String): Pair<MermaidNodeShape, String> {
    val t = shapeText.trim()
    return when {
        t.startsWith("((") && t.endsWith("))") ->
            MermaidNodeShape.CIRCLE to t.removeSurrounding("((", "))").trim()
        t.startsWith("([") && t.endsWith("])") ->
            MermaidNodeShape.STADIUM to t.removeSurrounding("([", "])").trim()
        t.startsWith("[(") && t.endsWith(")]") ->
            MermaidNodeShape.CYLINDER to t.removeSurrounding("[(", ")]").trim()
        t.startsWith("[[") && t.endsWith("]]") ->
            MermaidNodeShape.SUBROUTINE to t.removeSurrounding("[[", "]]").trim()
        t.startsWith("{{") && t.endsWith("}}") ->
            MermaidNodeShape.HEXAGON to t.removeSurrounding("{{", "}}").trim()
        t.startsWith("{") && t.endsWith("}") ->
            MermaidNodeShape.DIAMOND to t.removeSurrounding("{", "}").trim()
        t.startsWith("(") && t.endsWith(")") ->
            MermaidNodeShape.ROUND_RECT to t.removeSurrounding("(", ")").trim()
        t.startsWith(">") && t.endsWith("]") ->
            MermaidNodeShape.ASYMMETRIC to t.removePrefix(">").removeSuffix("]").trim()
        t.startsWith("[/") && t.endsWith("\\]") ->
            MermaidNodeShape.RECT to t.removePrefix("[/").removeSuffix("\\]").trim()
        t.startsWith("[\\") && t.endsWith("/]") ->
            MermaidNodeShape.RECT to t.removePrefix("[\\").removeSuffix("/]").trim()
        t.startsWith("[") && t.endsWith("]") ->
            MermaidNodeShape.RECT to t.removeSurrounding("[", "]").trim()
        else -> MermaidNodeShape.RECT to fallbackLabel
    }
}

private fun computeMermaidGraphLayout(data: MermaidGraphData): Map<String, Pair<Int, Int>> {
    val nodeIds = data.nodes.map { it.id }
    val adjacency = mutableMapOf<String, MutableList<String>>()
    val inDegree = mutableMapOf<String, Int>()
    nodeIds.forEach { id ->
        adjacency[id] = mutableListOf()
        inDegree[id] = 0
    }

    data.edges.forEach { edge ->
        if (edge.fromId != edge.toId && edge.fromId in adjacency && edge.toId in adjacency) {
            adjacency[edge.fromId]!!.add(edge.toId)
            inDegree[edge.toId] = (inDegree[edge.toId] ?: 0) + 1
        }
    }

    val layers = mutableMapOf<String, Int>()
    val queue = ArrayDeque<String>()

    nodeIds.filter { (inDegree[it] ?: 0) == 0 }.forEach { id ->
        queue.add(id)
        layers[id] = 0
    }

    val visited = mutableSetOf<String>()
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (current in visited) continue
        visited.add(current)

        val currentLayer = layers[current] ?: 0
        adjacency[current]?.forEach { neighbor ->
            val newLayer = currentLayer + 1
            if (newLayer > (layers[neighbor] ?: 0)) {
                layers[neighbor] = newLayer
            }
            inDegree[neighbor] = (inDegree[neighbor] ?: 1) - 1
            if ((inDegree[neighbor] ?: 0) <= 0 && neighbor !in visited) {
                queue.add(neighbor)
            }
        }
    }

    nodeIds.filter { it !in visited }.forEach { id ->
        layers[id] = (layers.values.maxOrNull() ?: 0) + 1
        visited.add(id)
    }

    val layerGroups = nodeIds.groupBy { layers[it] ?: 0 }
    val result = mutableMapOf<String, Pair<Int, Int>>()

    layerGroups.forEach { (layer, nodesInLayer) ->
        nodesInLayer.forEachIndexed { index, nodeId ->
            result[nodeId] = layer to index
        }
    }

    return result
}

private fun parseFencedCodeBlock(raw: String): FencedCodeBlock? {
    val lines = raw.lines()
    val openingLine = lines.firstOrNull()?.trimStart() ?: return null
    val fenceMarker = openingLine
        .takeWhile { it == '`' || it == '~' }
        .takeIf { it.length >= 3 }
        ?: return null
    val language = openingLine
        .removePrefix(fenceMarker)
        .trim()
        .substringBefore(' ')
        .takeIf { it.isNotBlank() }
    val contentLines = lines.drop(1)
    val closingFenceIndex = contentLines.indexOfFirst { line ->
        line.trimStart().startsWith(fenceMarker)
    }
    val codeLines = if (closingFenceIndex >= 0) {
        contentLines.take(closingFenceIndex)
    } else {
        contentLines
    }
    val trailingLines = if (closingFenceIndex >= 0) {
        contentLines.drop(closingFenceIndex + 1)
    } else {
        emptyList()
    }

    return FencedCodeBlock(
        language = language,
        code = codeLines.joinToString("\n").trimEnd(),
        trailingMarkdown = trailingLines.joinToString("\n").trimStart()
    )
}

private fun trimFencedCodeContent(raw: String): String {
    return raw.lines()
        .takeWhile { line ->
            val trimmed = line.trimStart()
            !trimmed.startsWith("```") && !trimmed.startsWith("~~~")
        }
        .joinToString("\n")
        .trimEnd()
}

private fun extractFenceLanguage(node: ASTNode, source: String): String? {
    return node.children
        .firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }
        ?.let { extractMarkdownText(it, source).trim() }
        ?.takeIf { it.isNotBlank() }
        ?: extractMarkdownText(node, source)
            .lineSequence()
            .firstOrNull()
            ?.trim()
            ?.trimStart('`', '~')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.substringBefore(' ')
}

private fun String.toSyntaxLanguage(): SyntaxLanguage? {
    val normalized = trim()
        .lowercase()
        .removePrefix("language-")
    if (normalized.isBlank() || normalized == "text" || normalized == "plain" || normalized == "plaintext") {
        return null
    }

    val aliases = mapOf(
        "c++" to "cpp",
        "cplusplus" to "cpp",
        "c#" to "csharp",
        "cs" to "csharp",
        "f#" to "fsharp",
        "fs" to "fsharp",
        "js" to "javascript",
        "mjs" to "javascript",
        "cjs" to "javascript",
        "jsx" to "javascript",
        "ts" to "typescript",
        "tsx" to "typescript",
        "kt" to "kotlin",
        "kts" to "kotlin",
        "py" to "python",
        "rb" to "ruby",
        "rs" to "rust",
        "sh" to "shell",
        "bash" to "shell",
        "zsh" to "shell",
        "ps1" to "powershell",
        "pwsh" to "powershell",
        "yml" to "yaml",
        "md" to "markdown",
        "html" to "xml",
        "xhtml" to "xml",
    )
    val candidates = listOfNotNull(normalized, aliases[normalized]).distinct()

    return candidates.firstNotNullOfOrNull { candidate ->
        SyntaxLanguage.getByName(candidate)
    }
}

private fun List<CodeHighlight>.toHighlightedCode(code: String): AnnotatedString {
    return buildAnnotatedString {
        append(code)
        forEach { highlight ->
            val start = highlight.location.start.coerceIn(0, code.length)
            val end = highlight.location.end.coerceIn(start, code.length)
            if (start == end) {
                return@forEach
            }

            when (highlight) {
                is BoldHighlight -> addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    start = start,
                    end = end,
                )

                is ColorHighlight -> addStyle(
                    SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f)),
                    start = start,
                    end = end,
                )
            }
        }
    }
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
    val isDarkTheme = LocalAppUiSettings.current.isDarkTheme
    val textColor = FluentTheme.colors.text.text.primary
    val mutedColor = if (isDarkTheme) {
        FluentTokens.ColorToken.LogLevel.unknown
    } else {
        FluentTokens.ColorToken.LogLevel.veryVerbose
    }
    val accentColor = FluentTokens.ColorToken.accent
    val unknownColor = FluentTokens.ColorToken.LogLevel.unknown

    return remember(isDarkTheme, textColor, mutedColor, accentColor, unknownColor) {
        DocsMarkdownStyle(
            textColor = textColor,
            mutedColor = mutedColor,
            accentColor = accentColor,
            codeBackground = unknownColor.copy(alpha = if (isDarkTheme) 0.18f else 0.045f),
            inlineCodeBackground = unknownColor.copy(alpha = if (isDarkTheme) 0.24f else 0.065f),
            borderColor = unknownColor.copy(alpha = if (isDarkTheme) 0.34f else 0.12f),
            dividerColor = unknownColor.copy(alpha = if (isDarkTheme) 0.28f else 0.16f),
            subtleBackground = unknownColor.copy(alpha = if (isDarkTheme) 0.12f else 0.035f),
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

private data class InlineMarkdown(
    val content: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    companion object {
        val Empty = InlineMarkdown(AnnotatedString(""))
    }
}

private fun buildInlineMarkdown(
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

@Composable
private fun rememberInlineMath(
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

private fun injectInlineMath(
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

private fun parseDisplayMath(raw: String): String? {
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

private fun AnnotatedString.Builder.appendInlineNode(
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
private fun MarkdownInlineCode(
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

private fun AnnotatedString.Builder.appendLinkNode(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
    linkListener: LinkInteractionListener?,
) {
    val label = node.children
        .firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
        ?.let { extractMarkdownText(it, source).trim('[', ']') }
        ?.ifBlank { null }
        ?: extractMarkdownText(node, source)

    val linkSpan = SpanStyle(
        color = style.accentColor,
        textDecoration = TextDecoration.None,
        fontWeight = FontWeight.Medium,
    )
    val destination = linkDestination(node, source)

    if (destination == null || linkListener == null) {
        withStyle(linkSpan) {
            append(label)
        }
        return
    }

    withLink(
        LinkAnnotation.Clickable(
            tag = destination,
            styles = TextLinkStyles(
                style = linkSpan,
                hoveredStyle = linkSpan.copy(textDecoration = TextDecoration.Underline),
                pressedStyle = linkSpan.copy(textDecoration = TextDecoration.Underline),
            ),
            linkInteractionListener = linkListener,
        )
    ) {
        append(label)
    }
}

private fun linkDestination(node: ASTNode, source: String): String? {
    node.children
        .firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
        ?.let { return extractMarkdownText(it, source).trim().trim('<', '>').ifBlank { null } }

    if (node.type == MarkdownElementTypes.AUTOLINK) {
        return extractMarkdownText(node, source).trim().trim('<', '>').ifBlank { null }
    }

    return null
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

private fun headingLevelOrNull(type: IElementType): Int? {
    return when (type) {
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6,
        MarkdownElementTypes.SETEXT_1,
        MarkdownElementTypes.SETEXT_2 -> headingLevel(type)
        else -> null
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

internal fun rewriteMarkdownResourceLinks(
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
private val MultiHyphenRegex = Regex("-+")
private val InlineMathRegex =
    Regex("""(?<!\$)\$(?!\$)(?!\s)([^$\n]+?)(?<!\s)\$(?!\$)(?!\d)|\\\(([^\n]+?)\\\)""")
private val MermaidClassRelationRegex = Regex("""^([^\s:]+)\s+([<o*|.]*[-.]+[>|o*]*|[<o*|]+[-.]+[>|o*]*)\s+([^\s:]+)\s*(?::\s*(.*))?$""")
private val MermaidGraphHeaderRegex = Regex("""^(?:graph|flowchart)\s*(\w*)$""", RegexOption.IGNORE_CASE)
private val MermaidGraphNodeDeclRegex = Regex("""^([A-Za-z_]\w*)(.*)$""")
private val MermaidGraphEdgeRegex = Regex("""(?:-->\|([^|]*)\||==>\|([^|]*)\||-\.?->\|([^|]*)\||--\s+([^-].*?)\s+-->|-->|---|-\.->|-\.-|==>|===)""")

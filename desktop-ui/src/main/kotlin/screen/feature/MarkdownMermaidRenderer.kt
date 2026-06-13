package screen.feature

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composefluent.component.Text as FluentText

@Composable
internal fun MarkdownMermaidBlock(
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
internal fun MermaidClassDiagram(
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
internal fun MermaidClassRelation(
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
internal fun MermaidClassCard(
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
internal fun MermaidClassBadge(
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
internal fun MermaidFlowchart(
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
internal fun MermaidFlowchartNode(
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


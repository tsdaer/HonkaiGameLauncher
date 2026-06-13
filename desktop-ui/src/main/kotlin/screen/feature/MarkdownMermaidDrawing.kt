package screen.feature

import androidx.compose.ui.graphics.Color

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMermaidEdge(
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

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMermaidArrowhead(
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

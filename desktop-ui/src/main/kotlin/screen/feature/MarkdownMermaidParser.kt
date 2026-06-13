package screen.feature

internal data class MermaidClassDiagramData(
    val classes: List<MermaidClassData>,
    val relations: List<MermaidClassRelationData>,
)

internal data class MermaidClassData(
    val name: String,
    val members: List<String>,
    val stereotypes: List<String>,
)

internal data class MermaidClassRelationData(
    val from: String,
    val arrow: String,
    val to: String,
    val label: String?,
)

internal enum class MermaidGraphDirection { TD, LR }

internal enum class MermaidNodeShape {
    RECT, ROUND_RECT, STADIUM, CIRCLE, DIAMOND, HEXAGON, ASYMMETRIC, SUBROUTINE, CYLINDER
}

internal data class MermaidGraphNodeData(
    val id: String,
    val label: String,
    val shape: MermaidNodeShape,
)

internal enum class MermaidEdgeStyle { SOLID, DOTTED, THICK }

internal data class MermaidGraphEdgeData(
    val fromId: String,
    val toId: String,
    val label: String?,
    val style: MermaidEdgeStyle,
    val hasArrow: Boolean,
)

internal data class MermaidGraphData(
    val direction: MermaidGraphDirection,
    val nodes: List<MermaidGraphNodeData>,
    val edges: List<MermaidGraphEdgeData>,
)

internal fun parseMermaidClassDiagram(raw: String): MermaidClassDiagramData? {
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

internal fun parseMermaidClassRelation(line: String): MermaidClassRelationData? {
    val match = MermaidClassRelationRegex.matchEntire(line) ?: return null
    return MermaidClassRelationData(
        from = match.groupValues[1],
        arrow = match.groupValues[2],
        to = match.groupValues[3],
        label = match.groupValues.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() },
    )
}

internal class MermaidClassBuilder(
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

internal fun parseMermaidGraph(raw: String): MermaidGraphData? {
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

internal class MermaidGraphNodeBuilder(val id: String) {
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

internal data class MermaidGraphSegment(
    val nodeText: String,
    val edgeLabel: String?,
    val edgeStyle: MermaidEdgeStyle,
    val edgeHasArrow: Boolean,
)

internal fun splitMermaidGraphEdges(line: String): List<MermaidGraphSegment> {
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

internal data class MermaidGraphNodeRef(
    val id: String,
    val label: String?,
    val shape: MermaidNodeShape,
)

internal fun parseMermaidGraphNodeRef(text: String): MermaidGraphNodeRef {
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

internal fun parseMermaidNodeShapeAndLabel(shapeText: String, fallbackLabel: String): Pair<MermaidNodeShape, String> {
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

internal fun computeMermaidGraphLayout(data: MermaidGraphData): Map<String, Pair<Int, Int>> {
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


internal val MermaidClassRelationRegex = Regex("""^([^\s:]+)\s+([<o*|.]*[-.]+[>|o*]*|[<o*|]+[-.]+[>|o*]*)\s+([^\s:]+)\s*(?::\s*(.*))?$""")
internal val MermaidGraphHeaderRegex = Regex("""^(?:graph|flowchart)\s*(\w*)$""", RegexOption.IGNORE_CASE)
internal val MermaidGraphNodeDeclRegex = Regex("""^([A-Za-z_]\w*)(.*)$""")
internal val MermaidGraphEdgeRegex = Regex("""(?:-->\|([^|]*)\||==>\|([^|]*)\||-\.?->\|([^|]*)\||--\s+([^-].*?)\s+-->|-->|---|-\.->|-\.-|==>|===)""")


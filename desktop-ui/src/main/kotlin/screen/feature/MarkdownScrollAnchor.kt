package screen.feature

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode

/** 文档目录项 */
internal data class DocTocItem(
    val level: Int,
    val title: String,
    val slug: String,
)

/**
 * 文档阅读器滚动控制器。
 *
 * 管理 Markdown 标题的位置注册和锚点跳转。
 * 标题通过 [registerHeading] 注册其相对于滚动顶部的偏移量，
 * 然后 [scrollToAnchor] 可根据 slug 平滑滚动到目标位置。
 *
 * @property scrollState 关联的 Compose [ScrollState]
 */
@Stable
internal class DocsReaderController(
    val scrollState: ScrollState,
) {
    /** 阅读器视口在窗口中的 Y 坐标，用于坐标转换 */
    var viewportTopInWindow: Float = 0f

    /** slug → 内容顶部偏移量（px）的映射 */
    private val headingOffsets = mutableStateMapOf<String, Int>()

    fun registerHeading(slug: String, contentTopPx: Int) {
        headingOffsets[slug] = contentTopPx.coerceAtLeast(0)
    }

    /** 已注册的标题数量（用于触发 LaunchedEffect） */
    val registeredCount: Int
        get() = headingOffsets.size

    /** 根据锚点查找标题偏移量，先精确匹配再尝试 slug 化匹配 */
    fun resolveAnchor(anchor: String): Int? {
        headingOffsets[anchor]?.let { return it }
        return headingOffsets[slugifyHeading(anchor)]
    }

    /** 滚动到指定锚点，失败返回 false */
    suspend fun scrollToAnchor(anchor: String): Boolean {
        val target = resolveAnchor(anchor) ?: return false
        scrollState.animateScrollTo((target - ANCHOR_TOP_PADDING_PX).coerceAtLeast(0))
        return true
    }

    private companion object {
        const val ANCHOR_TOP_PADDING_PX = 8
    }
}

/**
 * GitHub 风格标题 slug。
 * 保留 Unicode 字母/数字（支持 CJK 标题），空格/连字符映射为 '-'，
 * 保留下划线，删除其他标点。
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
 * 构建文档目录 (TOC) 和标题起始偏移量 → slug 映射。
 *
 * 直接扫描源文本行（而非 AST），因为代码块内的 `#` 不应被当作标题。
 * 通过追踪 fenced code block 状态来跳过代码块内的行。
 */
internal fun extractDocHeadings(markdown: String): Pair<List<DocTocItem>, Map<Int, String>> {
    if (markdown.isBlank()) {
        return emptyList<DocTocItem>() to emptyMap()
    }
    val source = normalizeMarkdownLineEndings(markdown)
    val items = mutableListOf<DocTocItem>()
    val slugByOffset = linkedMapOf<Int, String>()
    val usedSlugs = mutableMapOf<String, Int>()
    var activeFence: String? = null
    var previousTextLine: PendingSetextHeading? = null

    fun addHeading(level: Int, title: String, startOffset: Int) {
        val normalizedTitle = title.trim().trimEnd('#').trim()
        if (normalizedTitle.isBlank()) {
            return
        }
        val base = slugifyHeading(normalizedTitle).ifBlank { "section" }
        val seen = usedSlugs.getOrElse(base) { 0 }
        usedSlugs[base] = seen + 1
        val slug = if (seen == 0) base else "$base-$seen"
        items.add(DocTocItem(level = level, title = normalizedTitle, slug = slug))
        slugByOffset[startOffset] = slug
    }

    forEachMarkdownLine(source) { line, startOffset ->
        val trimmedStart = line.trimStart()
        val indent = line.length - trimmedStart.length
        val fence = if (indent <= MAX_FENCE_INDENT) fenceMarkerOrNull(trimmedStart) else null
        if (fence != null) {
            val openFence = activeFence
            if (openFence == null) {
                activeFence = fence
            } else if (trimmedStart.startsWith(openFence)) {
                activeFence = null
            }
            previousTextLine = null
            return@forEachMarkdownLine
        }

        if (activeFence != null) {
            return@forEachMarkdownLine
        }

        val atxHeading = parseAtxHeading(trimmedStart)
        if (atxHeading != null) {
            addHeading(
                level = atxHeading.level,
                title = atxHeading.title,
                startOffset = startOffset + indent,
            )
            previousTextLine = null
            return@forEachMarkdownLine
        }

        val setextLevel = parseSetextLevel(trimmedStart)
        if (setextLevel != null) {
            previousTextLine?.let { pending ->
                addHeading(
                    level = setextLevel,
                    title = pending.title,
                    startOffset = pending.startOffset,
                )
            }
            previousTextLine = null
            return@forEachMarkdownLine
        }

        previousTextLine = line
            .takeIf { it.isNotBlank() && !it.startsWith(" ") && !it.startsWith("\t") }
            ?.let { PendingSetextHeading(title = it.trim(), startOffset = startOffset) }
    }

    return items to slugByOffset
}

/** 根据 Markdown 元素类型获取标题级别（1-6） */
internal fun headingLevel(type: IElementType): Int {
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

/** 返回标题级别，非标题类型返回 null */
internal fun headingLevelOrNull(type: IElementType): Int? {
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

/** 从标题 AST 节点提取纯文本内容 */
internal fun headingText(node: ASTNode, source: String): String {
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


internal val MultiHyphenRegex = Regex("-+")

private const val MAX_FENCE_INDENT = 3

private data class ParsedAtxHeading(
    val level: Int,
    val title: String,
)

private data class PendingSetextHeading(
    val title: String,
    val startOffset: Int,
)

/** 逐行遍历 Markdown 文本，回调传入行内容和行在源文本中的起始偏移量 */
private fun forEachMarkdownLine(markdown: String, block: (line: String, startOffset: Int) -> Unit) {
    var start = 0
    while (start <= markdown.length) {
        val end = markdown.indexOf('\n', start).let { if (it == -1) markdown.length else it }
        block(markdown.substring(start, end), start)
        if (end == markdown.length) {
            break
        }
        start = end + 1
    }
}

/** 判断一行是否为 fenced code block 标记（``` 或 ~~~），并返回标记字符串 */
private fun fenceMarkerOrNull(trimmedLine: String): String? {
    val markerChar = trimmedLine.firstOrNull()?.takeIf { it == '`' || it == '~' } ?: return null
    val marker = trimmedLine.takeWhile { it == markerChar }
    return marker.takeIf { it.length >= 3 }
}

/** 解析 ATX 标题（`### Title`），返回级别和标题文本 */
private fun parseAtxHeading(trimmedLine: String): ParsedAtxHeading? {
    val marker = trimmedLine.takeWhile { it == '#' }
    if (marker.isEmpty() || marker.length > 6) {
        return null
    }
    val remainder = trimmedLine.drop(marker.length)
    if (remainder.isNotEmpty() && !remainder.first().isWhitespace()) {
        return null
    }
    return ParsedAtxHeading(
        level = marker.length,
        title = remainder.trim(),
    )
}

/** 解析 Setext 标题下划线（`===` → 级别 1，`---` → 级别 2） */
private fun parseSetextLevel(trimmedLine: String): Int? {
    if (trimmedLine.isEmpty()) {
        return null
    }
    return when {
        trimmedLine.all { it == '=' } -> 1
        trimmedLine.all { it == '-' } -> 2
        else -> null
    }
}

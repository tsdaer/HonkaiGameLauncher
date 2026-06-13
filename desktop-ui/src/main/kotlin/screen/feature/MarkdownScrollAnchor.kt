package screen.feature

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

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


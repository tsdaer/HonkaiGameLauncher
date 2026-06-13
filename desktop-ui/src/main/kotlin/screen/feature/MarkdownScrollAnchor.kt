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


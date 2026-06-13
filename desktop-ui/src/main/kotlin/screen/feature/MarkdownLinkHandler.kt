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

internal val LocalDocsReaderController = compositionLocalOf<DocsReaderController?> { null }
internal val LocalDocsHeadingSlugs = compositionLocalOf<Map<Int, String>> { emptyMap() }
internal val LocalMarkdownLinkListener = compositionLocalOf<LinkInteractionListener?> { null }

internal fun AnnotatedString.Builder.appendLinkNode(
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

internal fun linkDestination(node: ASTNode, source: String): String? {
    node.children
        .firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
        ?.let { return extractMarkdownText(it, source).trim().trim('<', '>').ifBlank { null } }

    if (node.type == MarkdownElementTypes.AUTOLINK) {
        return extractMarkdownText(node, source).trim().trim('<', '>').ifBlank { null }
    }

    return null
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

internal fun rewriteMarkdownResourceLink(
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

internal fun resolveMarkdownResource(currentDocumentPath: String?, rawLink: String): File? {
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

internal fun String.isMarkdownDocumentLink(): Boolean {
    val path = runCatching { URI(this).path }
        .getOrDefault(substringBefore('#').substringBefore('?'))

    return path.endsWith(".md", ignoreCase = true)
}

internal fun String.hasKnownScheme(): Boolean {
    val lowerValue = lowercase()
    return lowerValue.startsWith("http://") ||
        lowerValue.startsWith("https://") ||
        lowerValue.startsWith("file:") ||
        lowerValue.startsWith("data:") ||
        lowerValue.startsWith("mailto:")
}


internal val MarkdownResourceLinkRegex = Regex("""(!?\[)([^\]]*)]\(([^)\s]+)([^)]*)\)""")


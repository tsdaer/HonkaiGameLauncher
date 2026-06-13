package screen.feature

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import java.io.File
import java.net.URI

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


package screen.feature

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import java.io.File
import java.net.URI

/** Markdown 阅读器的 CompositionLocal（供标题注册和锚点跳转） */
internal val LocalDocsReaderController = compositionLocalOf<DocsReaderController?> { null }
/** 标题起始偏移量 → slug 映射的 CompositionLocal */
internal val LocalDocsHeadingSlugs = compositionLocalOf<Map<Int, String>> { emptyMap() }
/** Markdown 链接点击监听器的 CompositionLocal */
internal val LocalMarkdownLinkListener = compositionLocalOf<LinkInteractionListener?> { null }

/**
 * 在 [AnnotatedString.Builder] 中追加一个可交互的 Markdown 链接。
 * 提取链接文本和 destination，通过 [LinkInteractionListener] 处理点击。
 */
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

/** 从 Markdown 链接节点提取目标 URL */
internal fun linkDestination(node: ASTNode, source: String): String? {
    node.children
        .firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
        ?.let { return extractMarkdownText(it, source).trim().trim('<', '>').ifBlank { null } }

    if (node.type == MarkdownElementTypes.AUTOLINK) {
        return extractMarkdownText(node, source).trim().trim('<', '>').ifBlank { null }
    }

    return null
}

/**
 * 重写 Markdown 中的资源链接（图片等）为 file:// 绝对路径。
 * 使 Markdown 中相对路径引用的本地图片可在渲染中正常显示。
 */
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

/** 单条资源链接重写 */
internal fun rewriteMarkdownResourceLink(
    rawValue: String,
    currentDocumentPath: String?,
): String {
    val value = rawValue.trim()
    // 不处理锚点链接、外部链接和 .md 文档链接
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

/** 将资源相对路径解析为本地 File 对象 */
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

/** 检查链接是否指向 .md 文件 */
internal fun String.isMarkdownDocumentLink(): Boolean {
    val path = runCatching { URI(this).path }
        .getOrDefault(substringBefore('#').substringBefore('?'))

    return path.endsWith(".md", ignoreCase = true)
}

/** 检查 URL 是否有已知协议前缀 */
internal fun String.hasKnownScheme(): Boolean {
    val lowerValue = lowercase()
    return lowerValue.startsWith("http://") ||
        lowerValue.startsWith("https://") ||
        lowerValue.startsWith("file:") ||
        lowerValue.startsWith("data:") ||
        lowerValue.startsWith("mailto:")
}

/** Markdown 资源链接正则：匹配 `![label](url)` 或 `[label](url)` 格式 */
internal val MarkdownResourceLinkRegex = Regex("""(!?\[)([^\]]*)]\(([^)\s]+)([^)]*)\)""")

package core.docs

import java.io.File
import java.net.URI

/**
 * 文档链接解析结果（密封接口）。
 *
 * 对 Markdown 文档中链接的解析结果进行类型安全建模。
 *
 * @see Ignored   无需处理的链接（外部 URL、空白等）
 * @see Resolved  成功解析到目标文档的链接
 * @see Unresolved 未能匹配到目标文档的链接
 */
sealed interface DocsLinkResolution {
    /**
     * 忽略的链接。
     * 包括：
     * - 空白或仅含锚点的链接
     * - HTTP/HTTPS 外部链接
     * - 非 `.md` 文件链接
     * - 当前文档为 null 时无法解析的链接
     */
    data object Ignored : DocsLinkResolution

    /**
     * 成功解析的链接。
     *
     * @property target 目标 [DocEntry] 文档条目
     * @property anchor 锚点片段（`#` 之后的部分），无锚点时返回 null
     */
    data class Resolved(
        val target: DocEntry,
        val anchor: String?,
    ) : DocsLinkResolution

    /**
     * 无法解析的链接。
     * 链接指向的 `.md` 文件不在当前文档索引中。
     *
     * @property rawHref 原始 href 属性值，用于调试或日志
     */
    data class Unresolved(
        val rawHref: String,
    ) : DocsLinkResolution
}

/**
 * 文档链接解析器。
 *
 * 负责将 Markdown 文档中的链接引用解析为具体的 [DocEntry]。
 *
 * ## 解析流程
 * 1. 提取 href 中的路径部分（去掉 `#` 锚点）
 * 2. 过滤外部链接（http/https）和空白链接
 * 3. 将相对路径结合当前文档目录解析为绝对路径
 * 4. 在文档列表中查找匹配的 `.md` 文件
 * 5. 返回对应的 [DocsLinkResolution]
 *
 * 此解析器不关心 Markdown 语法，只处理提取出的 href 字符串。
 */
class DocsLinkResolver {
    /**
     * 解析文档链接。
     *
     * @param rawHref         链接的原始 href 属性值（可能包含 `#` 锚点）
     * @param currentDocument 当前正在查看的文档条目，用于计算相对路径的基准目录
     * @param documents       所有文档条目列表，用于查找链接目标
     * @return [DocsLinkResolution] 解析结果
     */
    fun resolve(
        rawHref: String,
        currentDocument: DocEntry?,
        documents: List<DocEntry>,
    ): DocsLinkResolution {
        // 分离路径和锚点：`path/to/file.md#section` → 路径=`path/to/file.md`, 锚点=`section`
        val href = rawHref.substringBefore('#').trim()
        val fragment = rawHref.substringAfter('#', "").trim()

        // 外部链接或空白链接 → 忽略
        if (href.isBlank() || href.startsWith("http://", true) || href.startsWith("https://", true)) {
            return DocsLinkResolution.Ignored
        }

        // 无当前文档 → 无法解析相对路径
        val currentFile = currentDocument?.let { File(it.absolutePath) }
            ?: return DocsLinkResolution.Ignored

        // URL 解码 href（处理 %20 等编码），然后基于当前文档目录解析为绝对路径
        val decodedHref = runCatching { URI(href).path }.getOrDefault(href)
        val resolvedFile = File(currentFile.parentFile, decodedHref)
            .normalize()
            .takeIf { it.extension.equals("md", ignoreCase = true) }
            ?: return DocsLinkResolution.Ignored   // 非 .md 文件

        // 在文档列表中查找匹配的文档条目
        val target = documents.firstOrNull { File(it.absolutePath).normalize() == resolvedFile }
            ?: return DocsLinkResolution.Unresolved(rawHref)

        return DocsLinkResolution.Resolved(
            target = target,
            anchor = fragment.ifBlank { null },
        )
    }
}

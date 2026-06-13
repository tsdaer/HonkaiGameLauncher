package core.docs

import core.platform.AppSettingsRepository
import java.io.File

/**
 * 文档加载状态枚举。
 *
 * 表示从游戏目录扫描和读取 Markdown 文档过程中所处的状态。
 *
 * @property MissingGamePath    未设置游戏路径或路径无效
 * @property MissingDocsDirectory 游戏路径有效，但 `honkai_rts/docs` 目录不存在
 * @property Empty              文档目录存在，但其中不包含任何 `.md` 文件
 * @property Ready              成功加载文档列表和内容
 * @property Error              加载过程中发生异常（如文件读取失败）
 */
enum class DocsLoadStatus {
    MissingGamePath,
    MissingDocsDirectory,
    Empty,
    Ready,
    Error,
}

/**
 * 文档分区枚举。
 *
 * 用于在文档列表中区分不同类型的文档，UI 层可根据分区分组展示。
 *
 * @property General    通用文档，位于 `honkai_rts/docs/` 根目录下
 * @property GamePlugins 游戏插件文档，位于 `honkai_rts/docs/GamePlugins/` 子目录下
 */
enum class DocSection {
    General,
    GamePlugins,
}

/**
 * 单条文档条目。
 *
 * 代表一个已扫描到的 `.md` 文档文件及其元数据。
 *
 * @property title        显示标题：Default.md 为 "Default"，其余取文件名（不含扩展名）
 * @property absolutePath 文档文件的绝对路径
 * @property relativePath 相对于 `honkai_rts/docs/` 的路径（使用正斜杠分隔）
 * @property section      文档分区：[DocSection.General] 或 [DocSection.GamePlugins]
 * @property isDefault    是否为默认文档（`Default.md`）
 */
data class DocEntry(
    val title: String,
    val absolutePath: String,
    val relativePath: String,
    val section: DocSection,
    val isDefault: Boolean = false,
)

/**
 * 文档加载完整结果。
 *
 * 一次 load 操作的结果，包含文档列表、选中文档、Markdown 内容及状态信息。
 *
 * @property documents       扫描到的所有文档条目列表
 * @property selectedDocument 当前选中的文档条目
 * @property markdownContent  选中文档的 Markdown 原始内容
 * @property status           加载状态
 * @property docsDirectory    文档目录的绝对路径
 * @property errorMessage     错误描述（仅在 status=Error 时有值）
 */
data class DocsLoadResult(
    val documents: List<DocEntry> = emptyList(),
    val selectedDocument: DocEntry? = null,
    val markdownContent: String = "",
    val status: DocsLoadStatus,
    val docsDirectory: String = "",
    val errorMessage: String = "",
)

/**
 * 单文档读取结果。
 *
 * 读取单个文档文件后的结果，不含文档列表信息。
 *
 * @property content      文档的 Markdown 原始内容
 * @property status       加载状态
 * @property errorMessage 错误描述（仅在 status=Error 时有值）
 */
data class DocumentReadResult(
    val content: String = "",
    val status: DocsLoadStatus,
    val errorMessage: String = "",
)

/**
 * 文档索引服务。
 *
 * 负责扫描游戏目录下的 `honkai_rts/docs/` 中的 Markdown 文档，
 * 构建文档列表索引，并可按需读取单个文档内容。
 *
 * ## 文档扫描规则
 * 1. 基于游戏路径解析出 `{游戏目录}/honkai_rts/docs/` 作为文档根目录
 * 2. 递归遍历所有 `.md` 文件
 * 3. 按以下规则排序：
 *    - Default.md 优先显示
 *    - GamePlugins/ 下的文档分入 [DocSection.GamePlugins] 分区
 *    - 同分区内按相对路径字母序排列
 *
 * ## 选中规则
 * - 优先匹配上一次选中的文档（通过 [previousSelection] 相对路径匹配）
 * - 否则选择 Default.md
 * - 若没有 Default.md，选择排序后的第一篇文档
 */
class DocsIndexService {
    /**
     * 加载文档索引并读取选中文档内容。
     *
     * @param path               游戏可执行文件路径或目录路径
     * @param previousSelection 上一次选中的文档相对路径，用于恢复选中状态；可为 null
     * @return 包含文档列表、选中文档和 Markdown 内容的 [DocsLoadResult]
     */
    fun load(path: String?, previousSelection: String?): DocsLoadResult {
        val normalizedPath = AppSettingsRepository.normalizeGamePath(path)
            ?: return DocsLoadResult(status = DocsLoadStatus.MissingGamePath)

        return runCatching {
            val gameFile = File(normalizedPath)
            // 如果输入是文件路径，取其父目录作为游戏目录；如果是目录则直接使用
            val gameDirectory = if (gameFile.isDirectory) {
                gameFile
            } else {
                gameFile.parentFile
            } ?: return DocsLoadResult(status = DocsLoadStatus.MissingGamePath)

            // 文档根目录：{游戏目录}/honkai_rts/docs/
            val docsRoot = File(File(gameDirectory, "honkai_rts"), "docs")
            if (!docsRoot.exists() || !docsRoot.isDirectory) {
                return DocsLoadResult(
                    status = DocsLoadStatus.MissingDocsDirectory,
                    docsDirectory = docsRoot.absolutePath,
                )
            }

            // 递归扫描所有 .md 文件
            val entries = docsRoot
                .walkTopDown()
                .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
                .map { file -> file.toDocEntry(docsRoot) }
                .sortedWith(
                    compareByDescending<DocEntry> { it.isDefault }
                        .thenBy { it.section.ordinal }
                        .thenBy { it.relativePath.lowercase() }
                )
                .toList()

            if (entries.isEmpty()) {
                return DocsLoadResult(
                    status = DocsLoadStatus.Empty,
                    docsDirectory = docsRoot.absolutePath,
                )
            }

            // 选中逻辑：上次选中 > Default.md > 第一篇
            val selectedEntry = entries.firstOrNull { it.relativePath == previousSelection }
                ?: entries.firstOrNull { it.isDefault }
                ?: entries.first()
            val readResult = read(selectedEntry)

            DocsLoadResult(
                documents = entries,
                selectedDocument = selectedEntry,
                markdownContent = readResult.content,
                status = readResult.status,
                docsDirectory = docsRoot.absolutePath,
                errorMessage = readResult.errorMessage,
            )
        }.getOrElse { error ->
            // 顶层异常：如路径不可访问等
            DocsLoadResult(
                status = DocsLoadStatus.Error,
                errorMessage = error.message ?: error::class.simpleName.orEmpty(),
            )
        }
    }

    /**
     * 读取单个文档条目的全文内容。
     *
     * @param entry 要读取的文档条目
     * @return 包含 Markdown 文本内容的 [DocumentReadResult]
     */
    fun read(entry: DocEntry): DocumentReadResult {
        return runCatching {
            DocumentReadResult(
                content = File(entry.absolutePath).readText(),
                status = DocsLoadStatus.Ready,
            )
        }.getOrElse { error ->
            DocumentReadResult(
                status = DocsLoadStatus.Error,
                errorMessage = error.message ?: error::class.simpleName.orEmpty(),
            )
        }
    }

    /**
     * 将 [File] 转换为 [DocEntry]。
     *
     * 计算相对路径，确定文档分区和是否为默认文档。
     *
     * @param docsRoot 文档根目录，用于计算相对路径
     * @return 对应的文档条目
     */
    private fun File.toDocEntry(docsRoot: File): DocEntry {
        val relativePath = relativeTo(docsRoot).invariantSeparatorsPath
        val isGamePluginDoc = relativePath.startsWith("GamePlugins/", ignoreCase = true)
        val isDefault = relativePath.equals(DEFAULT_DOC_FILE, ignoreCase = true)

        return DocEntry(
            title = if (isDefault) DEFAULT_DOC_TITLE else nameWithoutExtension,
            absolutePath = absolutePath,
            relativePath = relativePath,
            section = if (isGamePluginDoc) DocSection.GamePlugins else DocSection.General,
            isDefault = isDefault,
        )
    }

    private companion object {
        /** 默认文档文件名，优先显示 */
        const val DEFAULT_DOC_FILE = "Default.md"

        /** 默认文档的显示标题 */
        const val DEFAULT_DOC_TITLE = "Default"
    }
}

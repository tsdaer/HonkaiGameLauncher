package core.docs

import core.platform.AppSettingsRepository
import java.io.File

enum class DocsLoadStatus {
    MissingGamePath,
    MissingDocsDirectory,
    Empty,
    Ready,
    Error,
}

enum class DocSection {
    General,
    GamePlugins,
}

data class DocEntry(
    val title: String,
    val absolutePath: String,
    val relativePath: String,
    val section: DocSection,
    val isDefault: Boolean = false,
)

data class DocsLoadResult(
    val documents: List<DocEntry> = emptyList(),
    val selectedDocument: DocEntry? = null,
    val markdownContent: String = "",
    val status: DocsLoadStatus,
    val docsDirectory: String = "",
    val errorMessage: String = "",
)

data class DocumentReadResult(
    val content: String = "",
    val status: DocsLoadStatus,
    val errorMessage: String = "",
)

class DocsIndexService {
    fun load(path: String?, previousSelection: String?): DocsLoadResult {
        val normalizedPath = AppSettingsRepository.normalizeGamePath(path)
            ?: return DocsLoadResult(status = DocsLoadStatus.MissingGamePath)

        return runCatching {
            val gameFile = File(normalizedPath)
            val gameDirectory = if (gameFile.isDirectory) {
                gameFile
            } else {
                gameFile.parentFile
            } ?: return DocsLoadResult(status = DocsLoadStatus.MissingGamePath)

            val docsRoot = File(File(gameDirectory, "honkai_rts"), "docs")
            if (!docsRoot.exists() || !docsRoot.isDirectory) {
                return DocsLoadResult(
                    status = DocsLoadStatus.MissingDocsDirectory,
                    docsDirectory = docsRoot.absolutePath,
                )
            }

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
            DocsLoadResult(
                status = DocsLoadStatus.Error,
                errorMessage = error.message ?: error::class.simpleName.orEmpty(),
            )
        }
    }

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
        const val DEFAULT_DOC_FILE = "Default.md"
        const val DEFAULT_DOC_TITLE = "Default"
    }
}

package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

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

class DocsScreenModel(
    val settings: Settings = Settings(),
) : ScreenModel {

    var gamePath by mutableStateOf(settings.getString("gamePath", "null"))
        private set

    var docsDirectory by mutableStateOf("")
        private set

    var documents by mutableStateOf<List<DocEntry>>(emptyList())
        private set

    var selectedDocument by mutableStateOf<DocEntry?>(null)
        private set

    var markdownContent by mutableStateOf("")
        private set

    var loadStatus by mutableStateOf(DocsLoadStatus.MissingGamePath)
        private set

    var errorMessage by mutableStateOf("")
        private set

    var linkErrorMessage by mutableStateOf("")
        private set

    var pendingAnchor by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        refresh()
    }

    fun consumePendingAnchor(): String? {
        val anchor = pendingAnchor
        pendingAnchor = null
        return anchor
    }

    fun refresh() {
        gamePath = settings.getString("gamePath", "null")
        screenModelScope.launch {
            isLoading = true
            val previousSelection = selectedDocument?.relativePath
            val result = withContext(Dispatchers.IO) {
                loadDocuments(gamePath, previousSelection)
            }
            applyLoadResult(result)
            isLoading = false
        }
    }

    fun selectDocument(path: String) {
        pendingAnchor = null
        val target = documents.firstOrNull { it.absolutePath == path || it.relativePath == path } ?: return
        screenModelScope.launch {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                readDocument(target)
            }
            selectedDocument = target
            markdownContent = result.content
            loadStatus = result.status
            errorMessage = result.errorMessage
            linkErrorMessage = ""
            isLoading = false
        }
    }

    fun openLinkedDocument(rawHref: String): Boolean {
        val href = rawHref.substringBefore('#').trim()
        val fragment = rawHref.substringAfter('#', "").trim()
        if (href.isBlank() || href.startsWith("http://", true) || href.startsWith("https://", true)) {
            return false
        }

        val currentFile = selectedDocument?.let { File(it.absolutePath) } ?: return false
        val decodedHref = runCatching { URI(href).path }.getOrDefault(href)
        val resolvedFile = File(currentFile.parentFile, decodedHref)
            .normalize()
            .takeIf { it.extension.equals("md", ignoreCase = true) }
            ?: return false

        val target = documents.firstOrNull { File(it.absolutePath).normalize() == resolvedFile }
        return if (target != null) {
            selectDocument(target.absolutePath)
            pendingAnchor = fragment.ifBlank { null }
            true
        } else {
            linkErrorMessage = rawHref
            true
        }
    }

    private fun applyLoadResult(result: DocsLoadResult) {
        documents = result.documents
        docsDirectory = result.docsDirectory
        selectedDocument = result.selectedDocument
        markdownContent = result.markdownContent
        loadStatus = result.status
        errorMessage = result.errorMessage
        linkErrorMessage = ""
    }

    private fun loadDocuments(path: String, previousSelection: String?): DocsLoadResult {
        if (path == "null" || path.isBlank()) {
            return DocsLoadResult(status = DocsLoadStatus.MissingGamePath)
        }

        return runCatching {
            val gameFile = File(path)
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
            val readResult = readDocument(selectedEntry)

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

    private fun readDocument(entry: DocEntry): DocumentReadResult {
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

private data class DocsLoadResult(
    val documents: List<DocEntry> = emptyList(),
    val selectedDocument: DocEntry? = null,
    val markdownContent: String = "",
    val status: DocsLoadStatus,
    val docsDirectory: String = "",
    val errorMessage: String = "",
)

private data class DocumentReadResult(
    val content: String = "",
    val status: DocsLoadStatus,
    val errorMessage: String = "",
)

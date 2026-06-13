package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import core.docs.DocEntry
import core.docs.DocsIndexService
import core.docs.DocsLoadResult
import core.docs.DocsLoadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

data class DocsUiState(
    val gamePath: String = "null",
    val docsDirectory: String = "",
    val documents: List<DocEntry> = emptyList(),
    val selectedDocument: DocEntry? = null,
    val markdownContent: String = "",
    val loadStatus: DocsLoadStatus = DocsLoadStatus.MissingGamePath,
    val errorMessage: String = "",
    val linkErrorMessage: String = "",
    val pendingAnchor: String? = null,
    val isLoading: Boolean = false,
)

class DocsScreenModel(
    val settings: Settings = Settings(),
    private val docsIndexService: DocsIndexService = DocsIndexService(),
) : ScreenModel {

    var uiState by mutableStateOf(
        DocsUiState(gamePath = settings.getString("gamePath", "null"))
    )
        private set

    val gamePath: String
        get() = uiState.gamePath

    val docsDirectory: String
        get() = uiState.docsDirectory

    val documents: List<DocEntry>
        get() = uiState.documents

    val selectedDocument: DocEntry?
        get() = uiState.selectedDocument

    val markdownContent: String
        get() = uiState.markdownContent

    val loadStatus: DocsLoadStatus
        get() = uiState.loadStatus

    val errorMessage: String
        get() = uiState.errorMessage

    val linkErrorMessage: String
        get() = uiState.linkErrorMessage

    val pendingAnchor: String?
        get() = uiState.pendingAnchor

    val isLoading: Boolean
        get() = uiState.isLoading

    init {
        refresh()
    }

    fun consumePendingAnchor(): String? {
        val anchor = pendingAnchor
        uiState = uiState.copy(pendingAnchor = null)
        return anchor
    }

    fun refresh() {
        val currentGamePath = settings.getString("gamePath", "null")
        val previousSelection = selectedDocument?.relativePath
        uiState = uiState.copy(
            gamePath = currentGamePath,
            isLoading = true,
        )
        screenModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                docsIndexService.load(currentGamePath, previousSelection)
            }
            applyLoadResult(result)
        }
    }

    fun selectDocument(path: String) {
        val target = documents.firstOrNull { it.absolutePath == path || it.relativePath == path } ?: return
        uiState = uiState.copy(
            pendingAnchor = null,
            isLoading = true,
        )
        screenModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                docsIndexService.read(target)
            }
            uiState = uiState.copy(
                selectedDocument = target,
                markdownContent = result.content,
                loadStatus = result.status,
                errorMessage = result.errorMessage,
                linkErrorMessage = "",
                isLoading = false,
            )
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
            uiState = uiState.copy(pendingAnchor = fragment.ifBlank { null })
            true
        } else {
            uiState = uiState.copy(linkErrorMessage = rawHref)
            true
        }
    }

    private fun applyLoadResult(result: DocsLoadResult) {
        uiState = uiState.copy(
            documents = result.documents,
            docsDirectory = result.docsDirectory,
            selectedDocument = result.selectedDocument,
            markdownContent = result.markdownContent,
            loadStatus = result.status,
            errorMessage = result.errorMessage,
            linkErrorMessage = "",
            isLoading = false,
        )
    }

}

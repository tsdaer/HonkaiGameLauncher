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

class DocsScreenModel(
    val settings: Settings = Settings(),
    private val docsIndexService: DocsIndexService = DocsIndexService(),
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
                docsIndexService.load(gamePath, previousSelection)
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
                docsIndexService.read(target)
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

}

package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.docs.DocEntry
import core.docs.DocsIndexService
import core.docs.DocsLinkResolution
import core.docs.DocsLinkResolver
import core.docs.DocsLoadResult
import core.docs.DocsLoadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DocsUiState(
    val gamePath: String? = null,
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
    private val settingsStore: AppSettingsStore = SharedAppSettingsStore.instance,
    private val docsIndexService: DocsIndexService = DocsIndexService(),
    private val docsLinkResolver: DocsLinkResolver = DocsLinkResolver(),
) : ScreenModel {

    var uiState by mutableStateOf(
        DocsUiState(gamePath = settingsStore.state.value.gamePath)
    )
        private set

    private var refreshJob: Job? = null

    init {
        screenModelScope.launch {
            settingsStore.state
                .map { it.gamePath }
                .distinctUntilChanged()
                .collectLatest { refresh(it) }
        }
    }

    fun consumePendingAnchor(): String? {
        val anchor = uiState.pendingAnchor
        uiState = uiState.copy(pendingAnchor = null)
        return anchor
    }

    fun refresh() {
        refresh(settingsStore.state.value.gamePath)
    }

    private fun refresh(currentGamePath: String?) {
        refreshJob?.cancel()
        val previousSelection = uiState.selectedDocument?.relativePath
        uiState = uiState.copy(
            gamePath = currentGamePath,
            isLoading = true,
        )
        refreshJob = screenModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                docsIndexService.load(currentGamePath, previousSelection)
            }
            applyLoadResult(result)
        }
    }

    fun selectDocument(path: String) {
        val target = uiState.documents.firstOrNull { it.absolutePath == path || it.relativePath == path } ?: return
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
        return when (val resolution = docsLinkResolver.resolve(rawHref, uiState.selectedDocument, uiState.documents)) {
            DocsLinkResolution.Ignored -> false
            is DocsLinkResolution.Resolved -> {
                selectDocument(resolution.target.absolutePath)
                uiState = uiState.copy(pendingAnchor = resolution.anchor)
                true
            }
            is DocsLinkResolution.Unresolved -> {
                uiState = uiState.copy(linkErrorMessage = resolution.rawHref)
                true
            }
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

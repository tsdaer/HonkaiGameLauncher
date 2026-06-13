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

/**
 * 文档页 UI 状态。
 *
 * @property gamePath         当前游戏路径
 * @property docsDirectory    文档根目录绝对路径
 * @property documents        所有文档条目列表
 * @property selectedDocument 当前选中的文档条目
 * @property markdownContent  当前文档的 Markdown 原始内容
 * @property loadStatus       加载状态
 * @property errorMessage     错误消息
 * @property linkErrorMessage 链接解析失败时的目标 href
 * @property pendingAnchor    待滚动到的锚点（文档间导航时携带）
 * @property isLoading        是否正在加载
 */
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

/**
 * 文档页 ScreenModel。
 *
 * 管理文档索引加载、文档选择和文档间链接导航。
 *
 * ## 数据流
 * - 监听 [AppSettingsStore] 的 gamePath 变化自动刷新文档列表
 * - 刷新时尽量保持上次选中文档的选中状态
 * - 文档间导航通过 [openLinkedDocument] → [DocsLinkResolver] 实现
 */
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

    /**
     * 消费待处理的锚点。
     * Markdown 渲染完成后调用此方法获取目标锚点并清空 [pendingAnchor]。
     *
     * @return 待跳转的锚点 slug，无待处理锚点时返回 null
     */
    fun consumePendingAnchor(): String? {
        val anchor = uiState.pendingAnchor
        uiState = uiState.copy(pendingAnchor = null)
        return anchor
    }

    /** 刷新文档列表 */
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

    /**
     * 选择一篇文档并加载其内容。
     *
     * @param path 文档的绝对路径或相对路径
     */
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

    /**
     * 处理文档内链接点击。
     *
     * @param rawHref 链接的原始 href 值
     * @return true 表示链接被内部处理（跳转到其他文档），false 表示应交给外部浏览器
     */
    fun openLinkedDocument(rawHref: String): Boolean {
        return when (val resolution = docsLinkResolver.resolve(rawHref, uiState.selectedDocument, uiState.documents)) {
            DocsLinkResolution.Ignored -> false
            is DocsLinkResolution.Resolved -> {
                selectDocument(resolution.target.absolutePath)
                // 携带锚点，渲染完成后自动滚动
                uiState = uiState.copy(pendingAnchor = resolution.anchor)
                true
            }
            is DocsLinkResolution.Unresolved -> {
                // 链接指向了未知文档，显示错误提示
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

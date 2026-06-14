package screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.docs.DocEntry
import core.docs.DocsIndexService
import core.docs.PluginDocsLinker
import core.plugin.GamePluginConfig
import core.plugin.PluginConfigService
import core.plugin.PluginLoadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import navigation.FeatureLinkIntents
import ui.settings.AppSettingsStore
import ui.settings.SharedAppSettingsStore

/**
 * 插件页 UI 状态。
 */
data class PluginUiState(
    val gamePath: String? = null,
    val configPath: String = "",
    val pluginDirectory: String = "",
    val plugins: List<GamePluginConfig> = emptyList(),
    val pluginDocumentByPluginName: Map<String, DocEntry> = emptyMap(),
    val selectedPluginName: String? = null,
    val loadStatus: PluginLoadStatus = PluginLoadStatus.MissingGamePath,
    val errorMessage: String = "",
    val isLoading: Boolean = false,
)

/**
 * 插件页 ScreenModel。
 *
 * 监听 gamePath 变化自动加载插件配置。
 * 加载操作在 IO 线程执行以避免阻塞 UI。
 */
class PluginScreenModel(
    private val settingsStore: AppSettingsStore = SharedAppSettingsStore.instance,
    private val pluginConfigService: PluginConfigService = PluginConfigService(),
    private val docsIndexService: DocsIndexService = DocsIndexService(),
    private val pluginDocsLinker: PluginDocsLinker = PluginDocsLinker(),
) : ScreenModel {

    var uiState by mutableStateOf(
        PluginUiState(gamePath = settingsStore.state.value.gamePath)
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

    fun refresh() {
        refresh(settingsStore.state.value.gamePath)
    }

    private fun refresh(currentGamePath: String?) {
        refreshJob?.cancel()
        uiState = uiState.copy(
            gamePath = currentGamePath,
            isLoading = true,
        )
        refreshJob = screenModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val pluginResult = pluginConfigService.load(currentGamePath)
                val docsResult = docsIndexService.load(currentGamePath, null)
                val links = pluginDocsLinker.link(pluginResult.plugins, docsResult.documents)
                PluginRefreshResult(
                    pluginResult = pluginResult,
                    pluginDocumentByPluginName = links.pluginDocumentByPluginName
                        .mapNotNull { (pluginName, document) -> document?.let { pluginName to it } }
                        .toMap(),
                )
            }
            val selectedPluginName = FeatureLinkIntents.consumePluginSelection()
                ?.takeIf { targetName -> result.pluginResult.plugins.any { it.name == targetName } }
            uiState = uiState.copy(
                plugins = result.pluginResult.plugins,
                pluginDocumentByPluginName = result.pluginDocumentByPluginName,
                selectedPluginName = selectedPluginName,
                pluginDirectory = result.pluginResult.pluginDirectory,
                configPath = result.pluginResult.configPath,
                loadStatus = result.pluginResult.status,
                errorMessage = result.pluginResult.errorMessage,
                isLoading = false,
            )
        }
    }
}

private data class PluginRefreshResult(
    val pluginResult: core.plugin.PluginLoadResult,
    val pluginDocumentByPluginName: Map<String, DocEntry>,
)

package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
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

/**
 * 插件页 UI 状态。
 */
data class PluginUiState(
    val gamePath: String? = null,
    val configPath: String = "",
    val pluginDirectory: String = "",
    val plugins: List<GamePluginConfig> = emptyList(),
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
                pluginConfigService.load(currentGamePath)
            }
            uiState = uiState.copy(
                plugins = result.plugins,
                pluginDirectory = result.pluginDirectory,
                configPath = result.configPath,
                loadStatus = result.status,
                errorMessage = result.errorMessage,
                isLoading = false,
            )
        }
    }
}

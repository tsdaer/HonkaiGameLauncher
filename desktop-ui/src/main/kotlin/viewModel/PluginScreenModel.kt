package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.platform.AppSettingsRepository
import core.plugin.GamePluginConfig
import core.plugin.PluginConfigService
import core.plugin.PluginLoadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PluginUiState(
    val gamePath: String? = null,
    val configPath: String = "",
    val pluginDirectory: String = "",
    val plugins: List<GamePluginConfig> = emptyList(),
    val loadStatus: PluginLoadStatus = PluginLoadStatus.MissingGamePath,
    val errorMessage: String = "",
    val isLoading: Boolean = false,
)

class PluginScreenModel(
    private val settingsRepository: AppSettingsRepository = SettingsAppSettingsRepository(),
    private val pluginConfigService: PluginConfigService = PluginConfigService(),
) : ScreenModel {

    var uiState by mutableStateOf(
        PluginUiState(gamePath = settingsRepository.getGamePath())
    )
        private set

    init {
        refresh()
    }

    fun refresh() {
        val currentGamePath = settingsRepository.getGamePath()
        uiState = uiState.copy(
            gamePath = currentGamePath,
            isLoading = true,
        )
        screenModelScope.launch {
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

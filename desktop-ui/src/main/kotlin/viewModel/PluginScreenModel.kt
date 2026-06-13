package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import core.plugin.GamePluginConfig
import core.plugin.PluginConfigService
import core.plugin.PluginLoadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PluginUiState(
    val gamePath: String = "null",
    val configPath: String = "",
    val pluginDirectory: String = "",
    val plugins: List<GamePluginConfig> = emptyList(),
    val loadStatus: PluginLoadStatus = PluginLoadStatus.MissingGamePath,
    val errorMessage: String = "",
    val isLoading: Boolean = false,
)

class PluginScreenModel(
    val settings: Settings = Settings(),
    private val pluginConfigService: PluginConfigService = PluginConfigService(),
) : ScreenModel {

    var uiState by mutableStateOf(
        PluginUiState(gamePath = settings.getString("gamePath", "null"))
    )
        private set

    val gamePath: String
        get() = uiState.gamePath

    val configPath: String
        get() = uiState.configPath

    val pluginDirectory: String
        get() = uiState.pluginDirectory

    val plugins: List<GamePluginConfig>
        get() = uiState.plugins

    val loadStatus: PluginLoadStatus
        get() = uiState.loadStatus

    val errorMessage: String
        get() = uiState.errorMessage

    val isLoading: Boolean
        get() = uiState.isLoading

    init {
        refresh()
    }

    fun refresh() {
        val currentGamePath = settings.getString("gamePath", "null")
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

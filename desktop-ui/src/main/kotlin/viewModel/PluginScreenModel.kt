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

class PluginScreenModel(
    val settings: Settings = Settings(),
    private val pluginConfigService: PluginConfigService = PluginConfigService(),
) : ScreenModel {

    var gamePath by mutableStateOf(settings.getString("gamePath", "null"))
        private set

    var configPath by mutableStateOf("")
        private set

    var pluginDirectory by mutableStateOf("")
        private set

    var plugins by mutableStateOf<List<GamePluginConfig>>(emptyList())
        private set

    var loadStatus by mutableStateOf(PluginLoadStatus.MissingGamePath)
        private set

    var errorMessage by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        refresh()
    }

    fun refresh() {
        gamePath = settings.getString("gamePath", "null")
        screenModelScope.launch {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                pluginConfigService.load(gamePath)
            }
            plugins = result.plugins
            pluginDirectory = result.pluginDirectory
            configPath = result.configPath
            loadStatus = result.status
            errorMessage = result.errorMessage
            isLoading = false
        }
    }
}

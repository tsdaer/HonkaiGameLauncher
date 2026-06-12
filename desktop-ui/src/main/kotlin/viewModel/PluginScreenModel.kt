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

data class GamePluginConfig(
    val category: String,
    val createdBy: String,
    val createdByUrl: String,
    val defaultEnable: Boolean,
    val description: String,
    val friendlyName: String,
    val gameFeatureName: String,
    val mountOrder: Int?,
    val name: String,
    val pakPath: String,
    val resolvedPakPath: String?,
) {
    val isBuiltIn: Boolean = !pakPath.endsWith(".pak", ignoreCase = true)
}

enum class PluginLoadStatus {
    Ready,
    MissingGamePath,
    MissingConfig,
    Error,
}

class PluginScreenModel(
    val settings: Settings = Settings(),
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
                loadPluginConfigs(gamePath)
            }
            plugins = result.plugins
            pluginDirectory = result.pluginDirectory
            configPath = result.configPath
            loadStatus = result.status
            errorMessage = result.errorMessage
            isLoading = false
        }
    }

    private fun loadPluginConfigs(path: String): PluginLoadResult {
        if (path == "null" || path.isBlank()) {
            return PluginLoadResult(status = PluginLoadStatus.MissingGamePath)
        }

        return runCatching {
            val gameFile = File(path)
            val gameDirectory = if (gameFile.isDirectory) {
                gameFile
            } else {
                gameFile.parentFile
            } ?: return PluginLoadResult(status = PluginLoadStatus.MissingGamePath)

            val pluginsDirectory = File(File(gameDirectory, "honkai_rts"), "GamePlugins")
            val configFile = File(pluginsDirectory, "GamePluginConfigs.toml")

            if (!configFile.exists()) {
                return PluginLoadResult(
                    status = PluginLoadStatus.MissingConfig,
                    pluginDirectory = pluginsDirectory.absolutePath,
                    configPath = configFile.absolutePath,
                )
            }

            val parsedPlugins = parsePluginConfigs(
                content = configFile.readText(),
                pluginDirectory = pluginsDirectory,
            )

            PluginLoadResult(
                plugins = parsedPlugins,
                status = PluginLoadStatus.Ready,
                pluginDirectory = pluginsDirectory.absolutePath,
                configPath = configFile.absolutePath,
            )
        }.getOrElse { error ->
            PluginLoadResult(
                status = PluginLoadStatus.Error,
                errorMessage = error.message ?: error::class.simpleName.orEmpty(),
            )
        }
    }

    private fun parsePluginConfigs(content: String, pluginDirectory: File): List<GamePluginConfig> {
        val pluginMaps = mutableListOf<Map<String, String>>()
        var currentPlugin: MutableMap<String, String>? = null

        fun flushCurrent() {
            currentPlugin?.let { pluginMaps += it.toMap() }
            currentPlugin = null
        }

        content.lineSequence().forEach { rawLine ->
            val line = stripInlineComment(rawLine).trim()
            if (line.isBlank()) return@forEach

            if (line == "[[PluginConfigs]]") {
                flushCurrent()
                currentPlugin = mutableMapOf()
                return@forEach
            }

            val separatorIndex = line.indexOf('=')
            if (separatorIndex <= 0 || currentPlugin == null) return@forEach

            val key = line.substring(0, separatorIndex).trim()
            val value = line.substring(separatorIndex + 1).trim().trimTomlString()
            currentPlugin?.set(key, value)
        }

        flushCurrent()

        return pluginMaps.map { values ->
            val pakPath = values["PakPath"].orEmpty()
            GamePluginConfig(
                category = values["Category"].orEmpty(),
                createdBy = values["CreatedBy"].orEmpty(),
                createdByUrl = values["CreatedByURL"].orEmpty(),
                defaultEnable = values["DefaultEnable"]?.toBooleanStrictOrNull() ?: false,
                description = values["Description"].orEmpty(),
                friendlyName = values["FriendlyName"].orEmpty(),
                gameFeatureName = values["GameFeatureName"].orEmpty(),
                mountOrder = values["MountOrder"]?.toIntOrNull(),
                name = values["Name"].orEmpty(),
                pakPath = pakPath,
                resolvedPakPath = if (pakPath.endsWith(".pak", ignoreCase = true)) {
                    File(pluginDirectory, pakPath).absolutePath
                } else {
                    null
                },
            )
        }
    }

    private fun stripInlineComment(line: String): String {
        var inSingleQuotedString = false
        var inDoubleQuotedString = false

        line.forEachIndexed { index, char ->
            when (char) {
                '\'' -> if (!inDoubleQuotedString) inSingleQuotedString = !inSingleQuotedString
                '"' -> if (!inSingleQuotedString) inDoubleQuotedString = !inDoubleQuotedString
                '#' -> if (!inSingleQuotedString && !inDoubleQuotedString) return line.substring(0, index)
            }
        }

        return line
    }

    private fun String.trimTomlString(): String {
        return if (length >= 2 && ((first() == '\'' && last() == '\'') || (first() == '"' && last() == '"'))) {
            substring(1, lastIndex)
        } else {
            this
        }
    }
}

private data class PluginLoadResult(
    val plugins: List<GamePluginConfig> = emptyList(),
    val status: PluginLoadStatus,
    val pluginDirectory: String = "",
    val configPath: String = "",
    val errorMessage: String = "",
)

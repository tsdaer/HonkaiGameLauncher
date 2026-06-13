package core.plugin

import core.service.GamePathService
import java.io.File

enum class PluginLoadStatus {
    Ready,
    MissingGamePath,
    MissingConfig,
    Error,
}

data class PluginLoadResult(
    val plugins: List<GamePluginConfig> = emptyList(),
    val status: PluginLoadStatus,
    val pluginDirectory: String = "",
    val configPath: String = "",
    val errorMessage: String = "",
)

class PluginConfigService(
    private val parser: PluginConfigParser = PluginConfigParser(),
) {
    fun load(path: String?): PluginLoadResult {
        if (path.isNullOrBlank() || path == GamePathService.NO_GAME_PATH_SENTINEL) {
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

            PluginLoadResult(
                plugins = parser.parse(
                    content = configFile.readText(),
                    pluginDirectory = pluginsDirectory,
                ),
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
}

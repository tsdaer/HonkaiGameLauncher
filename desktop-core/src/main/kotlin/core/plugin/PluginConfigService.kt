package core.plugin

import core.platform.AppSettingsRepository
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
        val normalizedPath = AppSettingsRepository.normalizeGamePath(path)
            ?: return PluginLoadResult(status = PluginLoadStatus.MissingGamePath)

        return runCatching {
            val gameFile = File(normalizedPath)
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

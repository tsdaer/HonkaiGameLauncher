package core.service

import java.io.File

data class GamePathSnapshot(
    val executableExists: Boolean = false,
    val gameFileName: String = "",
    val gameDirectory: String = "",
    val pluginConfigPath: String = "",
    val pluginCount: Int = 0,
    val message: String = "",
)

class GamePathService {
    fun inspect(path: String?): GamePathSnapshot {
        if (path.isNullOrBlank() || path == NO_GAME_PATH_SENTINEL) {
            return GamePathSnapshot(message = "missing-game-path")
        }

        val gameFile = File(path)
        val gameDirectory = when {
            gameFile.isDirectory -> gameFile
            else -> gameFile.parentFile
        }

        if (!gameFile.exists() || gameDirectory == null) {
            return GamePathSnapshot(
                gameFileName = gameFile.name,
                gameDirectory = gameDirectory?.absolutePath.orEmpty(),
                message = "missing-executable",
            )
        }

        val configFile = File(File(gameDirectory, "honkai_rts"), "GamePlugins/GamePluginConfigs.toml")
        val pluginCount = if (configFile.exists()) {
            runCatching {
                configFile.readLines().count { it.trim() == PLUGIN_CONFIG_HEADER }
            }.getOrDefault(0)
        } else {
            0
        }

        return GamePathSnapshot(
            executableExists = gameFile.isFile,
            gameFileName = gameFile.name,
            gameDirectory = gameDirectory.absolutePath,
            pluginConfigPath = configFile.takeIf { it.exists() }?.absolutePath.orEmpty(),
            pluginCount = pluginCount,
        )
    }

    companion object {
        const val NO_GAME_PATH_SENTINEL = "null"
        const val PLUGIN_CONFIG_HEADER = "[[PluginConfigs]]"
    }
}

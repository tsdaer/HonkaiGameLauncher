package core.service

import core.platform.AppSettingsRepository
import java.io.File

/**
 * 游戏路径快照。
 *
 * 包含对指定游戏路径进行检查后得到的状态信息，
 * UI 层可据此判断路径是否有效，以及展示路径相关的元数据。
 *
 * @property executableExists 可执行文件是否存在且为文件（非目录）
 * @property gameFileName     游戏可执行文件名（如 `HonkaiRTS.exe`）
 * @property gameDirectory    游戏所在目录的绝对路径
 * @property pluginConfigPath 插件配置文件 `GamePluginConfigs.toml` 的绝对路径（若存在）
 * @property pluginCount      插件配置文件中的插件数量（统计 `[[PluginConfigs]]` 段头出现次数）
 * @property message          状态消息 key：空字符串表示正常，"missing-game-path" 表示未设置路径，
 *                            "missing-executable" 表示可执行文件不存在
 */
data class GamePathSnapshot(
    val executableExists: Boolean = false,
    val gameFileName: String = "",
    val gameDirectory: String = "",
    val pluginConfigPath: String = "",
    val pluginCount: Int = 0,
    val message: String = "",
)

/**
 * 游戏路径检查服务。
 *
 * 负责验证用户设置的游戏路径是否有效，并收集路径相关的元数据。
 *
 * ## 检查流程
 * 1. 归一化游戏路径（过滤无效/哨兵值）
 * 2. 判断路径类型（文件 vs 目录）
 * 3. 验证可执行文件是否存在
 * 4. 定位并统计插件配置文件信息
 *
 * 注意：此服务只做检查，不修改任何状态。
 */
class GamePathService {
    /**
     * 检查游戏路径并生成状态快照。
     *
     * @param path 游戏 exe 路径或目录路径；null / 空白 / 哨兵值 "null" 被视为未设置
     * @return 包含检查结果的 [GamePathSnapshot]
     */
    fun inspect(path: String?): GamePathSnapshot {
        val normalizedPath = AppSettingsRepository.normalizeGamePath(path)
            ?: return GamePathSnapshot(message = "missing-game-path")

        if (normalizedPath.isBlank()) {
            return GamePathSnapshot(message = "missing-game-path")
        }

        val gameFile = File(normalizedPath)
        // 确定游戏目录：如果路径指向文件则取其父目录，否则直接使用该目录
        val gameDirectory = when {
            gameFile.isDirectory -> gameFile
            else -> gameFile.parentFile
        }

        // 可执行文件不存在
        if (!gameFile.exists() || gameDirectory == null) {
            return GamePathSnapshot(
                gameFileName = gameFile.name,
                gameDirectory = gameDirectory?.absolutePath.orEmpty(),
                message = "missing-executable",
            )
        }

        // 定位插件配置文件并统计插件数量
        val configFile = File(File(gameDirectory, "honkai_rts"), "GamePlugins/GamePluginConfigs.toml")
        val pluginCount = if (configFile.exists()) {
            runCatching {
                // 统计 [[PluginConfigs]] 段头数量，每个段头对应一个插件
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
        /** 插件配置段头标记，用于统计插件数量 */
        const val PLUGIN_CONFIG_HEADER = "[[PluginConfigs]]"
    }
}

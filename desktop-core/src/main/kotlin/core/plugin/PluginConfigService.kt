package core.plugin

import core.platform.AppSettingsRepository
import java.io.File

/**
 * 插件加载状态枚举。
 *
 * @property Ready           已成功加载插件配置
 * @property MissingGamePath 未设置游戏路径或路径无效
 * @property MissingConfig   插件配置文件 `GamePluginConfigs.toml` 不存在
 * @property Error           加载或解析过程中发生异常
 */
enum class PluginLoadStatus {
    Ready,
    MissingGamePath,
    MissingConfig,
    Error,
}

/**
 * 插件加载结果。
 *
 * @property plugins         解析出的所有插件配置列表
 * @property status          加载状态
 * @property pluginDirectory 插件目录的绝对路径（`honkai_rts/GamePlugins/`）
 * @property configPath      配置文件 `GamePluginConfigs.toml` 的绝对路径
 * @property errorMessage    错误描述（仅在 status=Error 时有值）
 */
data class PluginLoadResult(
    val plugins: List<GamePluginConfig> = emptyList(),
    val status: PluginLoadStatus,
    val pluginDirectory: String = "",
    val configPath: String = "",
    val errorMessage: String = "",
)

/**
 * 插件配置服务。
 *
 * 负责从游戏目录加载和解析 `honkai_rts/GamePlugins/GamePluginConfigs.toml` 配置文件。
 *
 * ## 工作流程
 * 1. 归一化游戏路径（过滤无效/哨兵值）
 * 2. 定位游戏目录：如果路径指向文件则取父目录
 * 3. 定位配置文件：`{游戏目录}/honkai_rts/GamePlugins/GamePluginConfigs.toml`
 * 4. 读取文件内容并通过 [PluginConfigParser] 解析
 * 5. 返回结构化的 [PluginLoadResult]
 *
 * @property parser 插件配置解析器实例，默认使用 [PluginConfigParser]
 */
class PluginConfigService(
    private val parser: PluginConfigParser = PluginConfigParser(),
) {
    /**
     * 加载插件配置。
     *
     * @param path 游戏可执行文件路径或目录路径；null 或无效值将返回 MissingGamePath 状态
     * @return 包含解析结果的 [PluginLoadResult]
     */
    fun load(path: String?): PluginLoadResult {
        val normalizedPath = AppSettingsRepository.normalizeGamePath(path)
            ?: return PluginLoadResult(status = PluginLoadStatus.MissingGamePath)

        return runCatching {
            val gameFile = File(normalizedPath)
            // 如果路径指向文件，以父目录作为游戏根目录
            val gameDirectory = if (gameFile.isDirectory) {
                gameFile
            } else {
                gameFile.parentFile
            } ?: return PluginLoadResult(status = PluginLoadStatus.MissingGamePath)

            // 插件目录和配置文件路径
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

package core.plugin

import java.io.File

/**
 * 游戏插件配置数据类。
 *
 * 表示从 `GamePluginConfigs.toml` 中解析出的单个插件配置段（`[[PluginConfigs]]`）。
 *
 * @property category        插件分类标签
 * @property createdBy       创建者名称
 * @property createdByUrl    创建者网站 URL
 * @property defaultEnable   默认是否启用
 * @property description     插件描述文本
 * @property friendlyName    用户友好的显示名称
 * @property gameFeatureName 游戏侧使用的功能名称
 * @property mountOrder      挂载优先级，数值越小越先挂载；null 表示无指定
 * @property name            插件内部名称（唯一标识）
 * @property pakPath         原始 Pak 路径（来自配置文件）
 * @property resolvedPakPath 解析后的 Pak 文件绝对路径；若非 .pak 文件则为 null
 *
 * @property isBuiltIn 计算属性：pakPath 不以 `.pak` 结尾时视为内置插件
 */
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
    /**
     * 是否为内置插件。
     * 内置插件的 pakPath 不以 `.pak` 结尾（通常是引擎内置功能名称而非文件路径）。
     */
    val isBuiltIn: Boolean = !pakPath.endsWith(".pak", ignoreCase = true)
}

/**
 * 插件配置解析器。
 *
 * 负责解析 TOML 格式的 `GamePluginConfigs.toml` 文件。
 *
 * ## 解析策略
 * 由于当前不引入完整的 TOML 库，采用轻量级行解析方式：
 * 1. 以 `[[PluginConfigs]]` 作为配置段分隔符，每遇到一个新的段头即开启一个新插件配置
 * 2. 每行按 `key = value` 格式解析键值对
 * 3. 行内 `#` 开头的注释会被去除（但保留引号字符串内的 `#`）
 * 4. 字符串值的外层引号会被剥离
 *
 * ## 限制
 * - 不支持多行字符串
 * - 不支持嵌套表格或数组
 * - 不支持 `#` 注释在同一行的等号后（会被当作值的一部分）
 */
class PluginConfigParser {
    /**
     * 解析 TOML 配置内容。
     *
     * @param content         TOML 文件原始文本内容
     * @param pluginDirectory 插件目录路径，用于解析 .pak 文件的绝对路径
     * @return 解析出的插件配置列表，按文件中的出现顺序排列
     */
    fun parse(content: String, pluginDirectory: File): List<GamePluginConfig> {
        val pluginMaps = mutableListOf<Map<String, String>>()
        var currentPlugin: MutableMap<String, String>? = null

        // 将当前正在构建的插件配置加入结果列表
        fun flushCurrent() {
            currentPlugin?.let { pluginMaps += it.toMap() }
            currentPlugin = null
        }

        content.lineSequence().forEach { rawLine ->
            // 去除行内注释后再 trim
            val line = stripInlineComment(rawLine).trim()
            if (line.isBlank()) return@forEach

            // 遇到新的配置段头 [[PluginConfigs]]
            if (line == PLUGIN_CONFIG_HEADER) {
                flushCurrent()
                currentPlugin = mutableMapOf()
                return@forEach
            }

            // 解析 key = value
            val separatorIndex = line.indexOf('=')
            if (separatorIndex <= 0 || currentPlugin == null) return@forEach

            val key = line.substring(0, separatorIndex).trim()
            val value = line.substring(separatorIndex + 1).trim().trimTomlString()
            currentPlugin?.set(key, value)
        }

        // 不要忘记最后一个插件配置段
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
                // 如果是 .pak 文件，解析为相对于插件目录的绝对路径；否则为内置插件
                resolvedPakPath = if (pakPath.endsWith(".pak", ignoreCase = true)) {
                    File(pluginDirectory, pakPath).absolutePath
                } else {
                    null
                },
            )
        }
    }

    /**
     * 去除行内注释。
     *
     * 从行首扫描，遇到首个不在引号字符串内的 `#` 时，截断该行。
     * 这确保 `name = "# not a comment"` 中的 `#` 不会被误判为注释。
     *
     * @param line 原始行文本（已去除首尾空白）
     * @return 去除注释后的文本
     */
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

    /**
     * 去除 TOML 字符串值的外层引号。
     *
     * 如果字符串被单引号或双引号包裹（首尾字符相同），则剥离外层引号返回内部内容；
     * 否则原样返回。
     *
     * @return 去引号后的字符串内容
     */
    private fun String.trimTomlString(): String {
        return if (length >= 2 && ((first() == '\'' && last() == '\'') || (first() == '"' && last() == '"'))) {
            substring(1, lastIndex)
        } else {
            this
        }
    }

    private companion object {
        /** TOML 配置段头标记 */
        const val PLUGIN_CONFIG_HEADER = "[[PluginConfigs]]"
    }
}

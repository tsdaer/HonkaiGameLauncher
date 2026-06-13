package core.plugin

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

class PluginConfigParser {
    fun parse(content: String, pluginDirectory: File): List<GamePluginConfig> {
        val pluginMaps = mutableListOf<Map<String, String>>()
        var currentPlugin: MutableMap<String, String>? = null

        fun flushCurrent() {
            currentPlugin?.let { pluginMaps += it.toMap() }
            currentPlugin = null
        }

        content.lineSequence().forEach { rawLine ->
            val line = stripInlineComment(rawLine).trim()
            if (line.isBlank()) return@forEach

            if (line == PLUGIN_CONFIG_HEADER) {
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

    private companion object {
        const val PLUGIN_CONFIG_HEADER = "[[PluginConfigs]]"
    }
}

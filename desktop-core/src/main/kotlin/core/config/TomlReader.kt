package core.config

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.TableType
import com.akuleshov7.ktoml.tree.nodes.TomlTable

/**
 * 基于 [ktoml](https://github.com/orchestr7/ktoml) 的只读 TOML 查询器。
 *
 * 将 ktoml 解析得到的 [TomlFile] AST 压平为「点分路径 → 值」的映射，
 * 对外提供与原手写解析器一致的访问器（[string] / [int] / [double] / [boolean]），
 * 以便在不改动调用方的前提下替换底层解析实现。
 *
 * ## 路径规则
 * - 顶层键 `Language` → 路径 `"Language"`
 * - 表 `[Resolution]` 下的 `X` → 路径 `"Resolution.X"`
 * - 点分键 `a.b.c = 1` 被 ktoml 视作表 `[a.b]` 的键 `c`，压平后同样得到 `"a.b.c"`
 *
 * 因此源文件无论写成表头还是点分键，得到的查询路径都一致。
 *
 * 此类仅用于**读取**。写入（保留未知键、注释与格式）仍由 [GameSettingsService] 内的
 * 逐行编辑实现负责。
 */
class TomlReader private constructor(
    private val values: Map<String, Any>,
    private val tableNames: List<String>,
) {
    /** 读取字符串值；非字符串内容回退为其字符串表示。 */
    fun string(path: String): String? = when (val raw = values[path]) {
        null -> null
        is String -> raw
        else -> raw.toString()
    }

    /** 读取整数值；支持 ktoml 的 [Long] 内容与字符串数字。 */
    fun int(path: String): Int? = when (val raw = values[path]) {
        is Long -> raw.toInt()
        is Int -> raw
        is Double -> raw.toInt()
        is String -> raw.toIntOrNull()
        else -> null
    }

    /** 读取浮点值；支持 [Long] / [Double] 内容与字符串数字。 */
    fun double(path: String): Double? = when (val raw = values[path]) {
        is Double -> raw
        is Long -> raw.toDouble()
        is Int -> raw.toDouble()
        is String -> raw.toDoubleOrNull()
        else -> null
    }

    /** 读取布尔值。 */
    fun boolean(path: String): Boolean? = when (val raw = values[path]) {
        is Boolean -> raw
        is String -> raw.toBooleanStrictOrNull()
        else -> null
    }

    /** 所有原始（非数组）表的点分全名，供动态表枚举使用。 */
    fun tableNames(): List<String> = tableNames

    /**
     * 枚举所有同时包含 `.R/.G/.B/.A` 四个通道的表，构造为 RGBA 颜色配置。
     *
     * 用于读取诸如 `[AttackLineColor]` 这类**动态表名**的颜色设置，
     * 缺失的通道按默认值填充（A 默认 1.0，其余 0.0）。
     */
    fun readRgbaColorSections(): List<GameColorSetting> {
        return tableNames
            .filter { section ->
                listOf("R", "G", "B", "A").all { channel -> double("$section.$channel") != null }
            }
            .map { section ->
                GameColorSetting(
                    name = section,
                    value = GameRgbaColor(
                        red = double("$section.R") ?: 0.0,
                        green = double("$section.G") ?: 0.0,
                        blue = double("$section.B") ?: 0.0,
                        alpha = double("$section.A") ?: 1.0,
                    ),
                )
            }
    }

    companion object {
        /**
         * 解析 TOML 文本为可查询的 [TomlReader]。
         *
         * @param content TOML 原始文本（支持 `\n` 与 `\r\n` 换行）
         */
        fun parse(content: String): TomlReader {
            val file = TomlParser(TomlInputConfig()).parseString(content)
            val values = mutableMapOf<String, Any>()
            val tableNames = mutableListOf<String>()
            flatten(file, prefix = "", values = values, tableNames = tableNames)
            return TomlReader(values, tableNames)
        }

        /**
         * 深度优先遍历 AST：原始表记录其点分全名，键值对以「前缀 + 键名」入表。
         *
         * 数组表（`[[...]]`）及其元素不参与压平——本读取器面向的是固定结构的配置。
         */
        private fun flatten(
            node: TomlNode,
            prefix: String,
            values: MutableMap<String, Any>,
            tableNames: MutableList<String>,
        ) {
            node.children.forEach { child ->
                when (child) {
                    is TomlTable -> {
                        if (child.type != TableType.PRIMITIVE) return@forEach
                        val tablePath = if (prefix.isEmpty()) child.name else "$prefix.${child.name}"
                        tableNames.add(tablePath)
                        flatten(child, tablePath, values, tableNames)
                    }

                    is TomlKeyValuePrimitive -> {
                        val keyName = child.key.last()
                        val path = if (prefix.isEmpty()) keyName else "$prefix.$keyName"
                        values[path] = child.value.content
                    }

                    else -> flatten(child, prefix, values, tableNames)
                }
            }
        }
    }
}

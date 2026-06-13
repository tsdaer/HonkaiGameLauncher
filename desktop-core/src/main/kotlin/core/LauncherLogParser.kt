package core

import kotlinx.serialization.json.Json

/**
 * 游戏日志 JSON 解析器（单例）。
 *
 * 负责将游戏通过 HTTP 回传的原始 JSON 文本解析为 [LauncherLogEntry] 列表。
 *
 * ## 输入格式支持
 * - **数组格式**：`[{"type":1,...}, {...}]` → 反序列化为列表
 * - **单条格式**：`{"type":1,...}` → 包裹为单元素列表
 *
 * ## 错误处理
 * 解析过程使用 [runCatching] 包裹，通过 [Result] 类型返回，
 * 调用方可以安全地获取成功值或处理失败情况。
 */
object LauncherLogParser {
    /**
     * JSON 解析器实例，配置为忽略未知键。
     * 这确保游戏侧新增字段时不会导致反序列化失败，
     * 仅解析启动器关心的已知字段。
     */
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * 解析游戏回传的 JSON 日志文本。
     *
     * 自动检测输入格式：
     * - 以 `[` 开头 → 当作 JSON 数组解析，返回多条日志
     * - 其他 → 当作单个 JSON 对象解析，返回单条日志列表
     *
     * @param text 游戏回传的原始 JSON 字符串
     * @return 成功则包含 [LauncherLogEntry] 列表的 [Result.success]，
     *         失败则返回 [Result.failure]
     */
    fun parse(text: String): Result<List<LauncherLogEntry>> = runCatching {
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            // 数组格式：批量日志
            json.decodeFromString<List<LauncherLogEntry>>(trimmed)
        } else {
            // 单条格式：包裹为列表
            listOf(json.decodeFromString<LauncherLogEntry>(trimmed))
        }
    }
}

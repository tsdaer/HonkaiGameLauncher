package core

import kotlinx.serialization.Serializable

/**
 * 游戏回传的单条日志条目。
 *
 * 由游戏客户端通过 [GameService] 的 `POST /game/status` 接口以 JSON 形式回传，
 * 经 [LauncherLogParser] 反序列化后生成。
 *
 * 所有字段均为可序列化字段，直接映射游戏日志 JSON 结构。
 *
 * @property type     日志类型编码（整数），由游戏侧定义
 * @property category 日志分类标签，如 "System"、"Combat" 等
 * @property time     日志时间戳字符串，格式由游戏侧定义
 * @property message  日志正文内容
 */
@Serializable
data class LauncherLogEntry(
    val type: Int,
    val category: String,
    val time: String,
    val message: String
)

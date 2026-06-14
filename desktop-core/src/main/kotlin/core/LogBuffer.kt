package core

/**
 * 日志屏幕上显示的日志条目。
 *
 * @property id  自增唯一 ID，用于 Compose LazyColumn 的 key
 * @property log 原始 [LauncherLogEntry]
 */
data class LogScreenEntry(
    val id: Long,
    val log: LauncherLogEntry,
)

/** 日志筛选 UI 状态 */
data class LogUiState(
    val selectedType: Int? = null,
    val selectedCategory: String? = null,
    val autoScroll: Boolean = true,
)

/** [LogBuffer] 的状态快照，用于批量更新 UI */
data class LogBufferSnapshot(
    val logs: List<LogScreenEntry> = emptyList(),
    val filteredLogs: List<LogScreenEntry> = emptyList(),
    val availableTypes: List<Int> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val uiState: LogUiState = LogUiState(),
)

/**
 * 日志缓冲区。
 *
 * 环形缓冲区实现，维护最多 [maxLogCount] 条日志。
 * 支持按 type 和 category 筛选，自动滚动，以及溢出裁剪。
 *
 * ## 设计要点
 * - 使用 [typeCounts] 和 [categoryCounts] 维护筛选选项的可用性
 * - 溢出时移除最早的条目并同步计数（decrement）
 * - 当筛选选项在数据中不再存在时自动重置筛选器
 */
class LogBuffer(
    private val maxLogCount: Int,
) {
    private val logs = mutableListOf<LogScreenEntry>()
    private val typeCounts = mutableMapOf<Int, Int>()
    private val categoryCounts = mutableMapOf<String, Int>()
    private var nextLogId = 0L
    private var uiState = LogUiState()

    /** 当前状态快照（每次调用重新计算过滤结果） */
    val snapshot: LogBufferSnapshot
        get() = LogBufferSnapshot(
            logs = logs.toList(),
            filteredLogs = logs.filter(::matchesFilter),
            availableTypes = typeCounts.keys.sorted(),
            availableCategories = categoryCounts.keys.sorted(),
            uiState = uiState,
        )

    /** 追加新日志，触发溢出裁剪 */
    fun append(newLogs: List<LauncherLogEntry>): LogBufferSnapshot {
        if (newLogs.isEmpty()) return snapshot

        val entries = newLogs.map { log ->
            LogScreenEntry(
                id = nextLogId++,
                log = log,
            )
        }

        entries.forEach { entry ->
            typeCounts.increment(entry.log.type)
            categoryCounts.increment(entry.log.category)
        }

        logs.addAll(entries)
        trimOverflow()
        return snapshot
    }

    /** 清空所有日志和筛选状态 */
    fun clear(): LogBufferSnapshot {
        logs.clear()
        typeCounts.clear()
        categoryCounts.clear()
        uiState = uiState.copy(
            selectedType = null,
            selectedCategory = null,
        )
        return snapshot
    }

    fun selectType(type: Int?): LogBufferSnapshot {
        uiState = uiState.copy(selectedType = type)
        resetMissingFilters()
        return snapshot
    }

    fun selectCategory(category: String?): LogBufferSnapshot {
        uiState = uiState.copy(selectedCategory = category)
        resetMissingFilters()
        return snapshot
    }

    fun toggleAutoScroll(): LogBufferSnapshot {
        uiState = uiState.copy(autoScroll = !uiState.autoScroll)
        return snapshot
    }

    /** 移除超出上限的最早条目，同步更新计数 */
    private fun trimOverflow() {
        val overflow = logs.size - maxLogCount
        if (overflow <= 0) return

        val removedEntries = logs.take(overflow)
        repeat(overflow) {
            logs.removeAt(0)
        }

        removedEntries.forEach { entry ->
            typeCounts.decrement(entry.log.type)
            categoryCounts.decrement(entry.log.category)
        }

        resetMissingFilters()
    }

    private fun matchesFilter(entry: LogScreenEntry): Boolean {
        val log = entry.log
        return (uiState.selectedType == null || log.type == uiState.selectedType) &&
                (uiState.selectedCategory == null || log.category == uiState.selectedCategory)
    }

    /** 如果当前筛选的 type/category 在数据中已不存在，重置为 null */
    private fun resetMissingFilters() {
        if (uiState.selectedType != null && uiState.selectedType !in typeCounts) {
            uiState = uiState.copy(selectedType = null)
        }
        if (uiState.selectedCategory != null && uiState.selectedCategory !in categoryCounts) {
            uiState = uiState.copy(selectedCategory = null)
        }
    }

    private fun <T> MutableMap<T, Int>.increment(key: T) {
        this[key] = (this[key] ?: 0) + 1
    }

    private fun <T> MutableMap<T, Int>.decrement(key: T) {
        val count = this[key] ?: return
        if (count <= 1) {
            remove(key)
        } else {
            this[key] = count - 1
        }
    }
}

package viewModel

import core.LauncherLogEntry

data class LogScreenEntry(
    val id: Long,
    val log: LauncherLogEntry,
)

data class LogUiState(
    val selectedType: Int? = null,
    val selectedCategory: String? = null,
    val autoScroll: Boolean = true,
)

data class LogBufferSnapshot(
    val logs: List<LogScreenEntry> = emptyList(),
    val filteredLogs: List<LogScreenEntry> = emptyList(),
    val availableTypes: List<Int> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val uiState: LogUiState = LogUiState(),
)

class LogBuffer(
    private val maxLogCount: Int,
) {
    private val logs = mutableListOf<LogScreenEntry>()
    private val typeCounts = mutableMapOf<Int, Int>()
    private val categoryCounts = mutableMapOf<String, Int>()
    private var nextLogId = 0L
    private var uiState = LogUiState()

    val snapshot: LogBufferSnapshot
        get() = LogBufferSnapshot(
            logs = logs.toList(),
            filteredLogs = logs.filter(::matchesFilter),
            availableTypes = typeCounts.keys.sorted(),
            availableCategories = categoryCounts.keys.sorted(),
            uiState = uiState,
        )

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

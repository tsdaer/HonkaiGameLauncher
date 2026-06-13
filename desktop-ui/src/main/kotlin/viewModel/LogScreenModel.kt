package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.platform.AppSettingsRepository
import core.LauncherLogEntry
import core.RuntimeServices
import kotlinx.coroutines.launch

data class LogScreenEntry(
    val id: Long,
    val log: LauncherLogEntry,
)

data class LogUiState(
    val selectedType: Int? = null,
    val selectedCategory: String? = null,
    val autoScroll: Boolean = true,
)

class LogScreenModel (
    settingsRepository: AppSettingsRepository = SettingsAppSettingsRepository(),
) : ScreenModel {
    private val maxLogCount = settingsRepository.getLogMaxEntries(DEFAULT_MAX_LOG_COUNT)
        .coerceAtLeast(MIN_LOG_COUNT)

    // Compose 观察用的可变列表
    val logs = mutableStateListOf<LogScreenEntry>()

    // 当前筛选结果。新增日志时按增量维护，避免每次重组都全量过滤。
    val filteredLogs = mutableStateListOf<LogScreenEntry>()

    val availableTypes = mutableStateListOf<Int>()
    val availableCategories = mutableStateListOf<String>()

    private val typeCounts = mutableMapOf<Int, Int>()
    private val categoryCounts = mutableMapOf<String, Int>()
    private var nextLogId = 0L

    var uiState by mutableStateOf(LogUiState())
        private set

    init {
        screenModelScope.launch {
            RuntimeServices.gameService.logEvents.collect(::appendLogs)
        }
    }

    fun clear() {
        logs.clear()
        filteredLogs.clear()
        availableTypes.clear()
        availableCategories.clear()
        typeCounts.clear()
        categoryCounts.clear()
    }

    fun selectType(type: Int?) {
        if (uiState.selectedType == type) return
        uiState = uiState.copy(selectedType = type)
        rebuildFilteredLogs()
    }

    fun selectCategory(category: String?) {
        if (uiState.selectedCategory == category) return
        uiState = uiState.copy(selectedCategory = category)
        rebuildFilteredLogs()
    }

    fun toggleAutoScroll() {
        uiState = uiState.copy(autoScroll = !uiState.autoScroll)
    }

    private fun appendLogs(newLogs: List<LauncherLogEntry>) {
        if (newLogs.isEmpty()) return

        val entries = newLogs.map { log ->
            LogScreenEntry(
                id = nextLogId++,
                log = log,
            )
        }

        entries.forEach { entry ->
            typeCounts.increment(entry.log.type, availableTypes)
            categoryCounts.increment(entry.log.category, availableCategories)
        }

        logs.addAll(entries)
        filteredLogs.addAll(entries.filter(::matchesFilter))
        trimOverflow()
    }

    private fun trimOverflow() {
        val overflow = logs.size - maxLogCount
        if (overflow <= 0) return

        val removedEntries = logs.take(overflow)
        val removedIds = removedEntries.mapTo(HashSet(overflow)) { it.id }

        logs.removeRange(0, overflow)
        filteredLogs.removeAll { it.id in removedIds }

        removedEntries.forEach { entry ->
            typeCounts.decrement(entry.log.type, availableTypes)
            categoryCounts.decrement(entry.log.category, availableCategories)
        }

        resetMissingFilters()
    }

    private fun rebuildFilteredLogs() {
        filteredLogs.clear()
        filteredLogs.addAll(logs.filter(::matchesFilter))
    }

    private fun matchesFilter(entry: LogScreenEntry): Boolean {
        val log = entry.log
        return (uiState.selectedType == null || log.type == uiState.selectedType) &&
                (uiState.selectedCategory == null || log.category == uiState.selectedCategory)
    }

    private fun resetMissingFilters() {
        var needsRebuild = false
        if (uiState.selectedType != null && uiState.selectedType !in typeCounts) {
            uiState = uiState.copy(selectedType = null)
            needsRebuild = true
        }
        if (uiState.selectedCategory != null && uiState.selectedCategory !in categoryCounts) {
            uiState = uiState.copy(selectedCategory = null)
            needsRebuild = true
        }
        if (needsRebuild) {
            rebuildFilteredLogs()
        }
    }

    private fun <T : Comparable<T>> MutableMap<T, Int>.increment(key: T, options: MutableList<T>) {
        val count = this[key] ?: 0
        this[key] = count + 1
        if (count == 0) {
            options.add(key)
            options.sort()
        }
    }

    private fun <T> MutableMap<T, Int>.decrement(key: T, options: MutableList<T>) {
        val count = this[key] ?: return
        if (count <= 1) {
            remove(key)
            options.remove(key)
        } else {
            this[key] = count - 1
        }
    }

    private companion object {
        const val DEFAULT_MAX_LOG_COUNT = 10_000
        const val MIN_LOG_COUNT = 100
    }
}

package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.LauncherLogEntry
import core.RuntimeServices
import kotlinx.coroutines.launch

/**
 * 日志页 ScreenModel。
 *
 * 收集 [RuntimeServices.gameService.logEvents] 的日志流，
 * 通过 [LogBuffer] 管理缓冲区、筛选和自动滚动逻辑。
 *
 * 使用 Compose 的 [mutableStateListOf] 暴露可观察列表，
 * UI 层直接监听变化。
 */
class LogScreenModel (
    settingsStore: AppSettingsStore = SharedAppSettingsStore.instance,
) : ScreenModel {
    /** 日志上限，不低于 [MIN_LOG_COUNT] */
    private val maxLogCount = settingsStore.state.value.logMaxEntries
        .coerceAtLeast(MIN_LOG_COUNT)
    private val buffer = LogBuffer(maxLogCount)

    /** 所有日志条目的 Compose 可观察列表 */
    val logs = mutableStateListOf<LogScreenEntry>()

    /** 经过筛选后的日志条目列表 */
    val filteredLogs = mutableStateListOf<LogScreenEntry>()

    /** 当前可用的日志类型列表（供筛选下拉框） */
    val availableTypes = mutableStateListOf<Int>()
    /** 当前可用的日志分类列表（供筛选下拉框） */
    val availableCategories = mutableStateListOf<String>()

    var uiState by mutableStateOf(LogUiState())
        private set

    init {
        // 收集游戏日志事件
        screenModelScope.launch {
            RuntimeServices.gameService.logEvents.collect(::appendLogs)
        }
    }

    fun clear() {
        applySnapshot(buffer.clear())
    }

    fun selectType(type: Int?) {
        if (uiState.selectedType == type) return
        applySnapshot(buffer.selectType(type))
    }

    fun selectCategory(category: String?) {
        if (uiState.selectedCategory == category) return
        applySnapshot(buffer.selectCategory(category))
    }

    fun toggleAutoScroll() {
        applySnapshot(buffer.toggleAutoScroll())
    }

    private fun appendLogs(newLogs: List<LauncherLogEntry>) {
        applySnapshot(buffer.append(newLogs))
    }

    /** 将 [LogBuffer] 返回的快照同步到 Compose 可观察状态 */
    private fun applySnapshot(snapshot: LogBufferSnapshot) {
        logs.replaceWith(snapshot.logs)
        filteredLogs.replaceWith(snapshot.filteredLogs)
        availableTypes.replaceWith(snapshot.availableTypes)
        availableCategories.replaceWith(snapshot.availableCategories)
        uiState = snapshot.uiState
    }

    private companion object {
        /** 日志最小保留数，防止用户设置过小值 */
        const val MIN_LOG_COUNT = 100
    }
}

/** 替换 MutableList 内容的高效方法：清空后添加 */
private fun <T> MutableList<T>.replaceWith(items: List<T>) {
    clear()
    addAll(items)
}

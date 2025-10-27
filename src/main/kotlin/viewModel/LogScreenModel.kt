package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.russhwolf.settings.Settings
import core.LauncherLogEntry
import gameService

class LogScreenModel (
    val settings: Settings = Settings(),
) : ScreenModel {
    // Compose 观察用的可变列表
    val logs = mutableStateListOf<LauncherLogEntry>()

    // 当前选中的日志类型（null 表示全部）
    var selectedType by mutableStateOf<Int?>(null)

    // 当前选中的分类（null 表示全部）
    var selectedCategory by mutableStateOf<String?>(null)

    // 自动滚动
    var autoScroll by mutableStateOf(true)

    init {
        gameService.addLogListener { newLogs ->
            logs.addAll(newLogs)
            if (logs.size > 500) logs.removeRange(0, logs.size - 500)
        }
    }

    fun clear() {
        logs.clear()
    }

    // 提供动态筛选后的日志
    fun filteredLogs(): List<LauncherLogEntry> {
        return logs.filter { log ->
            (selectedType == null || log.type == selectedType) &&
                    (selectedCategory == null || log.category == selectedCategory)
        }
    }

    // 从日志提取可选类型 / 分类列表
    fun availableTypes(): List<Int> = logs.map { it.type }.distinct().sorted()
    fun availableCategories(): List<String> = logs.map { it.category }.distinct().sorted()

    fun toggleAutoScroll() {
        autoScroll = !autoScroll
    }
}
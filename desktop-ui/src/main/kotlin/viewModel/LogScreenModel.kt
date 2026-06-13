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

class LogScreenModel (
    settingsStore: AppSettingsStore = SharedAppSettingsStore.instance,
) : ScreenModel {
    private val maxLogCount = settingsStore.state.value.logMaxEntries
        .coerceAtLeast(MIN_LOG_COUNT)
    private val buffer = LogBuffer(maxLogCount)

    // Compose 观察用的可变列表
    val logs = mutableStateListOf<LogScreenEntry>()

    val filteredLogs = mutableStateListOf<LogScreenEntry>()

    val availableTypes = mutableStateListOf<Int>()
    val availableCategories = mutableStateListOf<String>()

    var uiState by mutableStateOf(LogUiState())
        private set

    init {
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

    private fun applySnapshot(snapshot: LogBufferSnapshot) {
        logs.replaceWith(snapshot.logs)
        filteredLogs.replaceWith(snapshot.filteredLogs)
        availableTypes.replaceWith(snapshot.availableTypes)
        availableCategories.replaceWith(snapshot.availableCategories)
        uiState = snapshot.uiState
    }

    private companion object {
        const val MIN_LOG_COUNT = 100
    }
}

private fun <T> MutableList<T>.replaceWith(items: List<T>) {
    clear()
    addAll(items)
}

package viewModel

import core.platform.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppSettingsState(
    val gamePath: String? = null,
    val logMaxEntries: Int = AppSettingsStore.DEFAULT_LOG_MAX_ENTRIES,
)

class AppSettingsStore(
    private val repository: AppSettingsRepository = SettingsAppSettingsRepository(),
    private val defaultLogMaxEntries: Int = DEFAULT_LOG_MAX_ENTRIES,
) {
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    fun setGamePath(path: String?) {
        repository.setGamePath(path)
        refresh()
    }

    fun refresh() {
        _state.update { loadState() }
    }

    private fun loadState(): AppSettingsState {
        return AppSettingsState(
            gamePath = repository.getGamePath(),
            logMaxEntries = repository.getLogMaxEntries(defaultLogMaxEntries),
        )
    }

    companion object {
        const val DEFAULT_LOG_MAX_ENTRIES = 10_000
    }
}

object SharedAppSettingsStore {
    val instance: AppSettingsStore by lazy {
        AppSettingsStore()
    }
}

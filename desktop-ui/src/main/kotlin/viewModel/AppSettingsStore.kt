package viewModel

import core.platform.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 应用设置的状态快照。
 *
 * 由 [AppSettingsStore] 维护并通过 [StateFlow] 暴露，
 * 供各 ScreenModel 订阅以响应设置变更。
 *
 * @property gamePath      当前设置的游戏 exe 路径（可能为 null 或哨兵值）
 * @property logMaxEntries 日志最大保留条目数
 */
data class AppSettingsState(
    val gamePath: String? = null,
    val logMaxEntries: Int = AppSettingsStore.DEFAULT_LOG_MAX_ENTRIES,
)

/**
 * 应用设置存储器。
 *
 * 在 [AppSettingsRepository] 接口之上提供响应式的 [StateFlow] 状态，
 * 是 core 层接口与 UI 层 ScreenModel 之间的桥梁。
 *
 * 所有修改操作（如 [setGamePath]）会立即写入底层 repository 并刷新状态流。
 */
class AppSettingsStore(
    private val repository: AppSettingsRepository = SettingsAppSettingsRepository(),
    private val defaultLogMaxEntries: Int = DEFAULT_LOG_MAX_ENTRIES,
) {
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    /**
     * 设置游戏 exe 路径并刷新状态。
     *
     * @param path 要设置的路径，null 表示清除
     */
    fun setGamePath(path: String?) {
        repository.setGamePath(path)
        refresh()
    }

    /** 从底层 repository 重新加载并发布最新状态 */
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
        /** 默认日志最大条目数：10,000 */
        const val DEFAULT_LOG_MAX_ENTRIES = 10_000
    }
}

/**
 * 全局共享的 [AppSettingsStore] 单例持有者。
 * 所有 ScreenModel 通过此对象获取同一个 store 实例。
 */
object SharedAppSettingsStore {
    val instance: AppSettingsStore by lazy {
        AppSettingsStore()
    }
}

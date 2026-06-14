package core.webengine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * WebEngine 初始化控制器。
 *
 * 管理初始化生命周期、进度状态、重试逻辑和竞态保护。
 * 通过 [state] 暴露响应式的 [WebEngineState] 状态流，不依赖任何 UI 框架。
 *
 * @property runtime 平台相关的初始化运行时实现
 * @property scope   用于启动初始化协程的作用域
 */
class WebEngineController(
    private val runtime: WebEngineRuntime,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(WebEngineState())

    /** 当前 WebEngine 初始化状态流 */
    val state: StateFlow<WebEngineState> = _state.asStateFlow()

    private var initAttempt = 0
    private var initialized = false
    private var completedAttempt: Int? = null

    /** 触发 WebEngine 初始化（幂等，多次调用安全） */
    fun ensureInitialized() {
        if (!initialized) {
            initialized = true
            doInit()
        }
    }

    /** 重试 WebEngine 初始化 */
    fun retry() {
        initAttempt++
        doInit()
    }

    private fun doInit() {
        val currentAttempt = initAttempt

        scope.launch {
            completedAttempt = null
            _state.value = WebEngineState(
                ready = false,
                phase = WebEnginePhase.Initializing,
                progress = null,
                error = null,
                restartRequired = false,
            )

            val result = try {
                runtime.initialize { phase, progress ->
                    updateProgress(currentAttempt, phase, progress)
                }
            } catch (throwable: Throwable) {
                WebEngineInitializationResult.Failed(
                    throwable.message ?: throwable.javaClass.simpleName
                )
            }

            if (currentAttempt != initAttempt) return@launch
            completedAttempt = currentAttempt

            when (result) {
                WebEngineInitializationResult.Ready -> {
                    _state.value = WebEngineState(
                        ready = true,
                        phase = WebEnginePhase.Ready,
                        progress = 1f,
                        error = null,
                        restartRequired = false,
                    )
                }

                WebEngineInitializationResult.RestartRequired -> {
                    _state.update {
                        it.copy(
                            ready = false,
                            error = null,
                            restartRequired = true,
                        )
                    }
                }

                is WebEngineInitializationResult.Failed -> {
                    _state.update {
                        it.copy(
                            ready = false,
                            error = result.message,
                            restartRequired = false,
                        )
                    }
                }
            }
        }
    }

    private fun updateProgress(attempt: Int, phase: WebEnginePhase, progress: Float?) {
        scope.launch {
            if (attempt == initAttempt && completedAttempt != attempt) {
                _state.update { it.copy(phase = phase, progress = progress) }
            }
        }
    }
}

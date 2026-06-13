package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.cef.CefApp
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * KCEF WebEngine 初始化服务（单例）。
 *
 * 提供对嵌入式 Chromium 引擎（KCEF）的懒初始化和管理。
 * 所有 WebView 页面通过此单例获取引擎的就绪状态、进度和错误信息。
 *
 * ## 设计要点
 * - 单次初始化：[ensureInitialized] 多次调用安全，内部只初始化一次
 * - 进度报告：通过 [phase] / [progress] StateFlow 暴露初始化各阶段状态
 * - 错误恢复：支持 [retry] 重试失败的初始化
 * - 重启感知：引擎在下载/安装后可能需要应用重启，通过 [restartRequired] 提示用户
 *
 * ## 初始化流程
 * Checking → Downloading → DownloadFinishing → Extracting → Installing → Initializing → Ready
 */
object WebEngineService {

    private val controller = WebEngineController(
        runtime = KcefWebEngineRuntime(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    )

    /** WebEngine 是否已就绪（可用于加载网页） */
    val ready get() = controller.ready
    /** 当前初始化阶段 */
    val phase get() = controller.phase
    /** 初始化进度（0.0-1.0），仅在 Downloading/Ready 阶段有值 */
    val progress get() = controller.progress
    /** 初始化失败时的错误信息 */
    val error get() = controller.error
    /** 是否需要重启应用以完成引擎安装 */
    val restartRequired get() = controller.restartRequired

    /** 触发 WebEngine 初始化（幂等，多次调用安全） */
    fun ensureInitialized() {
        controller.ensureInitialized()
    }

    /** 重试 WebEngine 初始化 */
    fun retry() {
        controller.retry()
    }
}

/**
 * WebEngine 初始化控制器。
 *
 * 管理初始化生命周期、进度状态、重试逻辑和竞态保护。
 *
 * @property runtime 平台相关的初始化运行时实现
 * @property scope   用于启动初始化协程的作用域
 */
internal class WebEngineController(
    private val runtime: WebEngineRuntime,
    private val scope: CoroutineScope,
) {

    var ready by mutableStateOf(false)
        private set

    var phase by mutableStateOf(WebEnginePhase.Initializing)
        private set

    var progress by mutableStateOf<Float?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var restartRequired by mutableStateOf(false)
        private set

    private var initAttempt = 0
    private var initialized = false
    private var completedAttempt: Int? = null

    fun ensureInitialized() {
        if (!initialized) {
            initialized = true
            doInit()
        }
    }

    fun retry() {
        initAttempt++
        doInit()
    }

    private fun doInit() {
        val currentAttempt = initAttempt

        scope.launch {
            ready = false
            error = null
            restartRequired = false
            phase = WebEnginePhase.Initializing
            progress = null
            completedAttempt = null

            val result = try {
                runtime.initialize { phase, progress ->
                    updateProgress(currentAttempt, phase, progress)
                }
            } catch (throwable: Throwable) {
                WebEngineInitializationResult.Failed(
                    throwable.message ?: throwable.javaClass.simpleName
                )
            }

            when (result) {
                WebEngineInitializationResult.Ready -> {
                    if (currentAttempt == initAttempt) {
                        completedAttempt = currentAttempt
                        restartRequired = false
                        error = null
                        ready = true
                        phase = WebEnginePhase.Ready
                        progress = 1f
                    }
                }

                WebEngineInitializationResult.RestartRequired -> {
                    if (currentAttempt == initAttempt) {
                        completedAttempt = currentAttempt
                        restartRequired = true
                        error = null
                        ready = false
                    }
                }

                is WebEngineInitializationResult.Failed -> {
                    if (currentAttempt == initAttempt) {
                        completedAttempt = currentAttempt
                        restartRequired = false
                        error = result.message
                        ready = false
                    }
                }
            }
        }
    }

    private fun updateProgress(attempt: Int, phase: WebEnginePhase, progress: Float?) {
        scope.launch {
            if (attempt == initAttempt && completedAttempt != attempt) {
                this@WebEngineController.phase = phase
                this@WebEngineController.progress = progress
            }
        }
    }
}

/** WebEngine 初始化进度回调接口（函数式） */
internal fun interface WebEngineProgressSink {
    fun update(phase: WebEnginePhase, progress: Float?)
}

/**
 * WebEngine 运行时抽象。
 *
 * 定义引擎初始化的平台相关操作，便于测试时 mock 替换。
 */
internal interface WebEngineRuntime {
    suspend fun initialize(progressSink: WebEngineProgressSink): WebEngineInitializationResult
}

/** WebEngine 初始化结果（密封接口） */
internal sealed interface WebEngineInitializationResult {
    /** 初始化成功，引擎就绪 */
    data object Ready : WebEngineInitializationResult
    /** 需要重启应用以完成安装 */
    data object RestartRequired : WebEngineInitializationResult
    /** 初始化失败 */
    data class Failed(val message: String) : WebEngineInitializationResult
}

/**
 * 基于 KCEF（Chromium Embedded Framework）的 [WebEngineRuntime] 实现。
 *
 * 调用 `KCEF.init()` 进行引擎定位/下载/安装。初始化在主线程分派，
 * 文件下载和 CEF 初始化在 IO 线程执行。
 *
 * 安装数据存储在 `%LOCALAPPDATA%/HonkaiGameLauncher/kcef-{name}` 下。
 */
internal class KcefWebEngineRuntime : WebEngineRuntime {

    override suspend fun initialize(progressSink: WebEngineProgressSink): WebEngineInitializationResult {
        var initError: String? = null
        var needsRestart = false

        withContext(Dispatchers.IO) {
            KCEF.init(
                builder = {
                    installDir(kcefDataDir("bundle"))
                    settings {
                        cachePath = kcefDataDir("cache").absolutePath
                    }
                    progress {
                        onLocating {
                            progressSink.update(WebEnginePhase.Checking, null)
                        }
                        onDownloading { dl ->
                            val normalized = dl.coerceIn(0f, 1f)
                            progressSink.update(
                                if (normalized >= 0.999f) {
                                    WebEnginePhase.DownloadFinishing
                                } else {
                                    WebEnginePhase.Downloading
                                },
                                normalized,
                            )
                        }
                        onExtracting {
                            progressSink.update(WebEnginePhase.Extracting, null)
                        }
                        onInstall {
                            progressSink.update(WebEnginePhase.Installing, null)
                        }
                        onInitializing {
                            progressSink.update(WebEnginePhase.Initializing, null)
                        }
                        onInitialized {
                            progressSink.update(WebEnginePhase.Ready, 1f)
                        }
                    }
                },
                onError = { throwable ->
                    initError = throwable?.message
                        ?: throwable?.javaClass?.simpleName
                        ?: "Unknown error"
                },
                onRestartRequired = {
                    needsRestart = true
                }
            )
        }

        return when {
            initError != null -> WebEngineInitializationResult.Failed(initError.orEmpty())
            needsRestart -> WebEngineInitializationResult.RestartRequired
            waitForCefInitialized() -> WebEngineInitializationResult.Ready
            else -> WebEngineInitializationResult.Failed("CEF initialization timed out")
        }
    }

    private suspend fun waitForCefInitialized(): Boolean {
        return withTimeoutOrNull(15_000.milliseconds) {
            while (CefApp.getState() != CefApp.CefAppState.INITIALIZED) {
                delay(50.milliseconds)
            }
            true
        } ?: false
    }

    private fun kcefDataDir(name: String): File {
        val baseDir = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let { localAppData ->
            File(localAppData, "HonkaiGameLauncher")
        } ?: File(System.getProperty("user.home"), ".HonkaiGameLauncher")

        return File(baseDir, "kcef-$name")
    }
}

/**
 * WebEngine 初始化阶段枚举。
 *
 * 反映 KCEF 引擎从检测到就绪的完整初始化流程。
 */
enum class WebEnginePhase {
    /** 检测已安装的引擎 */
    Checking,
    /** 下载引擎包 */
    Downloading,
    /** 下载完成，即将解压 */
    DownloadFinishing,
    /** 解压引擎包 */
    Extracting,
    /** 安装引擎 */
    Installing,
    /** 初始化 CEF 运行时 */
    Initializing,
    /** 引擎就绪，可加载网页 */
    Ready
}

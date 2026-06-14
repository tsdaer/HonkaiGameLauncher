package ui.webengine

import core.webengine.WebEngineController
import core.webengine.WebEngineInitializationResult
import core.webengine.WebEnginePhase
import core.webengine.WebEngineProgressSink
import core.webengine.WebEngineRuntime
import core.webengine.WebEngineState
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.cef.CefApp
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * KCEF WebEngine 初始化服务（单例）。
 *
 * 提供对嵌入式 Chromium 引擎（KCEF）的懒初始化和管理。
 * 所有 WebView 页面通过此单例的 [state] 流获取引擎就绪状态、进度和错误信息。
 *
 * 初始化生命周期与竞态逻辑由 desktop-core 的 [WebEngineController] 负责，
 * 此处只提供 KCEF 平台运行时（[KcefWebEngineRuntime]）和单例作用域。
 *
 * ## 初始化流程
 * Checking → Downloading → DownloadFinishing → Extracting → Installing → Initializing → Ready
 */
object WebEngineService {

    private val controller = WebEngineController(
        runtime = KcefWebEngineRuntime(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    )

    /** WebEngine 初始化状态流 */
    val state: StateFlow<WebEngineState> = controller.state

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

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

object WebEngineService {

    private val controller = WebEngineController(
        runtime = KcefWebEngineRuntime(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    )

    val ready get() = controller.ready
    val phase get() = controller.phase
    val progress get() = controller.progress
    val error get() = controller.error
    val restartRequired get() = controller.restartRequired

    fun ensureInitialized() {
        controller.ensureInitialized()
    }

    fun retry() {
        controller.retry()
    }
}

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

internal fun interface WebEngineProgressSink {
    fun update(phase: WebEnginePhase, progress: Float?)
}

internal interface WebEngineRuntime {
    suspend fun initialize(progressSink: WebEngineProgressSink): WebEngineInitializationResult
}

internal sealed interface WebEngineInitializationResult {
    data object Ready : WebEngineInitializationResult
    data object RestartRequired : WebEngineInitializationResult
    data class Failed(val message: String) : WebEngineInitializationResult
}

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

enum class WebEnginePhase {
    Checking,
    Downloading,
    DownloadFinishing,
    Extracting,
    Installing,
    Initializing,
    Ready
}

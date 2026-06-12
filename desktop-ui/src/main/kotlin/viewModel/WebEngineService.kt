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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var initAttempt = 0
    private var initialized = false

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

            var initError: String? = null
            var needsRestart = false

            try {
                withContext(Dispatchers.IO) {
                    KCEF.init(
                        builder = {
                            installDir(kcefDataDir("bundle"))
                            settings {
                                cachePath = kcefDataDir("cache").absolutePath
                            }
                            progress {
                                onLocating {
                                    updateProgress(currentAttempt) {
                                        phase = WebEnginePhase.Checking
                                        progress = null
                                    }
                                }
                                onDownloading { dl ->
                                    updateProgress(currentAttempt) {
                                        val normalized = dl.coerceIn(0f, 1f)
                                        phase = if (normalized >= 0.999f) {
                                            WebEnginePhase.DownloadFinishing
                                        } else {
                                            WebEnginePhase.Downloading
                                        }
                                        progress = normalized
                                    }
                                }
                                onExtracting {
                                    updateProgress(currentAttempt) {
                                        phase = WebEnginePhase.Extracting
                                        progress = null
                                    }
                                }
                                onInstall {
                                    updateProgress(currentAttempt) {
                                        phase = WebEnginePhase.Installing
                                        progress = null
                                    }
                                }
                                onInitializing {
                                    updateProgress(currentAttempt) {
                                        phase = WebEnginePhase.Initializing
                                        progress = null
                                    }
                                }
                                onInitialized {
                                    updateProgress(currentAttempt) {
                                        phase = WebEnginePhase.Ready
                                        progress = 1f
                                    }
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

                val cefReady = if (initError == null && !needsRestart) {
                    waitForCefInitialized()
                } else {
                    false
                }

                if (currentAttempt == initAttempt) {
                    restartRequired = needsRestart
                    error = initError ?: if (!needsRestart && !cefReady) {
                        "CEF initialization timed out"
                    } else {
                        null
                    }
                    ready = !needsRestart && initError == null && cefReady
                }
            } catch (throwable: Throwable) {
                if (currentAttempt == initAttempt) {
                    error = throwable.message ?: throwable.javaClass.simpleName
                }
            }
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

    private fun updateProgress(attempt: Int, update: () -> Unit) {
        scope.launch {
            if (attempt == initAttempt) {
                update()
            }
        }
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

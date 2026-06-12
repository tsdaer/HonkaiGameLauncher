package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.cef.CefApp
import java.io.File

class WebScreenModel : ScreenModel {

    var webEngineReady by mutableStateOf(false)
        private set

    var webEnginePhase by mutableStateOf(WebEnginePhase.Initializing)
        private set

    var webEngineProgress by mutableStateOf<Float?>(null)
        private set

    var webEngineError by mutableStateOf<String?>(null)
        private set

    var webEngineRestartRequired by mutableStateOf(false)
        private set

    var address by mutableStateOf(HOME_URL)
        private set

    private var initAttempt = 0

    init {
        initializeWebEngine()
    }

    fun retryWebEngineInit() {
        initAttempt++
        initializeWebEngine()
    }

    fun updateAddress(value: String) {
        address = value
    }

    fun updateAddressFromLoadedUrl(value: String?) {
        if (!value.isNullOrBlank()) {
            address = value
        }
    }

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return HOME_URL
        }
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun prepareLoadUrl(rawUrl: String): String {
        val targetUrl = normalizeUrl(rawUrl)
        address = targetUrl
        return targetUrl
    }

    private fun initializeWebEngine() {
        val currentAttempt = initAttempt

        screenModelScope.launch {
            webEngineReady = false
            webEngineError = null
            webEngineRestartRequired = false
            webEnginePhase = WebEnginePhase.Initializing
            webEngineProgress = null

            var initError: String? = null
            var restartRequired = false

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
                                    updateWebEngineProgress(currentAttempt) {
                                        webEnginePhase = WebEnginePhase.Checking
                                        webEngineProgress = null
                                    }
                                }
                                onDownloading { progress ->
                                    updateWebEngineProgress(currentAttempt) {
                                        val normalizedProgress = progress.coerceIn(0f, 1f)
                                        webEnginePhase = if (normalizedProgress >= 0.999f) {
                                            WebEnginePhase.DownloadFinishing
                                        } else {
                                            WebEnginePhase.Downloading
                                        }
                                        webEngineProgress = normalizedProgress
                                    }
                                }
                                onExtracting {
                                    updateWebEngineProgress(currentAttempt) {
                                        webEnginePhase = WebEnginePhase.Extracting
                                        webEngineProgress = null
                                    }
                                }
                                onInstall {
                                    updateWebEngineProgress(currentAttempt) {
                                        webEnginePhase = WebEnginePhase.Installing
                                        webEngineProgress = null
                                    }
                                }
                                onInitializing {
                                    updateWebEngineProgress(currentAttempt) {
                                        webEnginePhase = WebEnginePhase.Initializing
                                        webEngineProgress = null
                                    }
                                }
                                onInitialized {
                                    updateWebEngineProgress(currentAttempt) {
                                        webEnginePhase = WebEnginePhase.Ready
                                        webEngineProgress = 1f
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
                            restartRequired = true
                        }
                    )
                }

                val cefReady = if (initError == null && !restartRequired) {
                    waitForCefInitialized()
                } else {
                    false
                }

                if (currentAttempt == initAttempt) {
                    webEngineRestartRequired = restartRequired
                    webEngineError = initError ?: if (!restartRequired && !cefReady) {
                        "CEF initialization timed out"
                    } else {
                        null
                    }
                    webEngineReady = !restartRequired && initError == null && cefReady
                }
            } catch (throwable: Throwable) {
                if (currentAttempt == initAttempt) {
                    webEngineError = throwable.message ?: throwable.javaClass.simpleName
                }
            }
        }
    }

    private suspend fun waitForCefInitialized(): Boolean {
        return withTimeoutOrNull(15_000) {
            while (CefApp.getState() != CefApp.CefAppState.INITIALIZED) {
                delay(50)
            }
            true
        } ?: false
    }

    private fun updateWebEngineProgress(attempt: Int, update: () -> Unit) {
        screenModelScope.launch {
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

    companion object {
        const val HOME_URL = "https://www.honkai-rts.com"
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

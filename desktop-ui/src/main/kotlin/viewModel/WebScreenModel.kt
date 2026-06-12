package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel

class WebScreenModel : ScreenModel {

    val webEngineReady get() = WebEngineService.ready
    val webEnginePhase get() = WebEngineService.phase
    val webEngineProgress get() = WebEngineService.progress
    val webEngineError get() = WebEngineService.error
    val webEngineRestartRequired get() = WebEngineService.restartRequired

    var address by mutableStateOf(HOME_URL)
        private set

    init {
        WebEngineService.ensureInitialized()
    }

    fun retryWebEngineInit() {
        WebEngineService.retry()
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

    companion object {
        const val HOME_URL = "https://www.honkai-rts.com"
    }
}

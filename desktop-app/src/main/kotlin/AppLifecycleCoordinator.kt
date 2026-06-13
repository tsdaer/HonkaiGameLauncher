import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface DesktopGameService {
    fun start()
    fun stop()
}

class RuntimeDesktopGameService(
    private val gameService: core.GameService = core.RuntimeServices.gameService,
) : DesktopGameService {
    override fun start() {
        gameService.start()
    }

    override fun stop() {
        gameService.stop()
    }
}

class AppLifecycleCoordinator(
    private val gameService: DesktopGameService = RuntimeDesktopGameService(),
) {
    var isVisible by mutableStateOf(true)
        private set

    private var serviceStarted = false
    private var exited = false

    fun start() {
        if (serviceStarted) return

        gameService.start()
        serviceStarted = true
    }

    fun showWindow() {
        isVisible = true
    }

    fun toggleWindowVisibility() {
        isVisible = !isVisible
    }

    fun hideWindow() {
        isVisible = false
    }

    fun exit(onDispose: () -> Unit, onExitProcess: () -> Unit) {
        if (exited) return

        exited = true
        gameService.stop()
        onDispose()
        onExitProcess()
    }
}

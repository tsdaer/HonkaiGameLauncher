package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.GameConnectionStatus
import core.RuntimeServices
import core.platform.AppSettingsRepository
import core.platform.FileSystemGateway
import core.platform.ProcessLauncher
import core.service.GamePathService
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

enum class HomeLaunchStatus {
    MissingGamePath,
    MissingExecutable,
    Ready,
    Launching,
    Running,
    Error,
}

data class HomeUiState(
    val gamePath: String? = null,
    val gameDirectory: String = "",
    val gameFileName: String = "",
    val pluginConfigPath: String = "",
    val pluginCount: Int = 0,
    val launchStatus: HomeLaunchStatus = HomeLaunchStatus.MissingGamePath,
    val lastLaunchTime: String = "",
    val statusMessage: String = "",
    val gameConnectionStatus: GameConnectionStatus = GameConnectionStatus.Stopped,
)

class HomeScreenModel(
    private val settingsRepository: AppSettingsRepository = SettingsAppSettingsRepository(),
    private val gamePathService: GamePathService = GamePathService(),
    private val processLauncher: ProcessLauncher = ProcessLauncher(),
    private val fileSystemGateway: FileSystemGateway = FileSystemGateway(),
) : ScreenModel {

    var uiState by mutableStateOf(
        HomeUiState(
            gamePath = settingsRepository.getGamePath(),
            gameConnectionStatus = RuntimeServices.gameService.connectionStatus.value,
        )
    )
        private set

    private var awaitConnectionJob: Job? = null

    init {
        screenModelScope.launch {
            RuntimeServices.gameService.connectionStatus.collect(::onConnectionStatusChanged)
        }
        refresh()
    }

    private fun onConnectionStatusChanged(status: GameConnectionStatus) {
        uiState = uiState.copy(gameConnectionStatus = status)
        if (status == GameConnectionStatus.Connected) {
            awaitConnectionJob?.cancel()
        }
        refresh()
    }

    fun refresh() {
        val currentGamePath = settingsRepository.getGamePath()
        val snapshot = gamePathService.inspect(currentGamePath)
        val hasCurrentGamePath = !currentGamePath.isNullOrBlank()
        val nextLaunchStatus = when {
            !hasCurrentGamePath -> HomeLaunchStatus.MissingGamePath
            !snapshot.executableExists -> HomeLaunchStatus.MissingExecutable
            uiState.gameConnectionStatus == GameConnectionStatus.Connected -> HomeLaunchStatus.Running
            uiState.launchStatus == HomeLaunchStatus.Launching -> HomeLaunchStatus.Launching
            else -> HomeLaunchStatus.Ready
        }
        uiState = uiState.copy(
            gamePath = currentGamePath,
            gameDirectory = snapshot.gameDirectory,
            gameFileName = snapshot.gameFileName,
            pluginConfigPath = snapshot.pluginConfigPath,
            pluginCount = snapshot.pluginCount,
            launchStatus = nextLaunchStatus,
            statusMessage = snapshot.message,
        )
    }

    fun selectGamePath() {
        screenModelScope.launch {
            val file = FileKit.openFilePicker()
            if (file != null) {
                settingsRepository.setGamePath(file.path)
                refresh()
            }
        }
    }

    fun launchGame() {
        refresh()
        if (uiState.launchStatus != HomeLaunchStatus.Ready && uiState.launchStatus != HomeLaunchStatus.Running) return
        val executablePath = uiState.gamePath ?: return

        screenModelScope.launch {
            uiState = uiState.copy(
                launchStatus = HomeLaunchStatus.Launching,
                statusMessage = "",
            )
            val result = withContext(Dispatchers.IO) {
                processLauncher.launch(executablePath)
            }

            result.fold(
                onSuccess = {
                    val nextLaunchStatus = if (uiState.gameConnectionStatus == GameConnectionStatus.Connected) {
                        HomeLaunchStatus.Running
                    } else {
                        HomeLaunchStatus.Launching
                    }
                    uiState = uiState.copy(
                        lastLaunchTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        launchStatus = nextLaunchStatus,
                        statusMessage = "",
                    )
                    waitForGameConnection()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        launchStatus = HomeLaunchStatus.Error,
                        statusMessage = error.message ?: error::class.simpleName.orEmpty(),
                    )
                }
            )
        }
    }

    private fun waitForGameConnection() {
        awaitConnectionJob?.cancel()
        awaitConnectionJob = screenModelScope.launch {
            delay(GAME_CONNECTION_WAIT_TIMEOUT_MS.milliseconds)
            if (uiState.launchStatus == HomeLaunchStatus.Launching &&
                uiState.gameConnectionStatus != GameConnectionStatus.Connected
            ) {
                uiState = uiState.copy(launchStatus = HomeLaunchStatus.Ready)
            }
        }
    }

    fun openGameDirectory() {
        val directory = uiState.gameDirectory.takeIf { it.isNotBlank() } ?: return

        screenModelScope.launch {
            val errorMessage = withContext(Dispatchers.IO) {
                fileSystemGateway.openDirectory(directory).exceptionOrNull()
            }?.let { error ->
                error.message ?: error::class.simpleName.orEmpty()
            }
            if (errorMessage != null) {
                uiState = uiState.copy(
                    launchStatus = HomeLaunchStatus.Error,
                    statusMessage = errorMessage,
                )
            }
        }
    }

    override fun onDispose() {
        awaitConnectionJob?.cancel()
        super.onDispose()
    }

    private companion object {
        const val GAME_CONNECTION_WAIT_TIMEOUT_MS = 20_000L
    }
}

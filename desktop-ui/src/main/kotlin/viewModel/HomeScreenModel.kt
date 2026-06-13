package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import core.GameConnectionStatus
import core.RuntimeServices
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
import java.awt.Desktop
import java.io.File
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
    val gamePath: String = "null",
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
    val settings: Settings = Settings(),
    private val gamePathService: GamePathService = GamePathService(),
    private val processLauncher: ProcessLauncher = ProcessLauncher(),
) : ScreenModel {

    var uiState by mutableStateOf(
        HomeUiState(
            gamePath = settings.getString("gamePath", "null"),
            gameConnectionStatus = RuntimeServices.gameService.connectionStatus.value,
        )
    )
        private set

    val gamePath: String
        get() = uiState.gamePath

    val gameDirectory: String
        get() = uiState.gameDirectory

    val gameFileName: String
        get() = uiState.gameFileName

    val pluginConfigPath: String
        get() = uiState.pluginConfigPath

    val pluginCount: Int
        get() = uiState.pluginCount

    val launchStatus: HomeLaunchStatus
        get() = uiState.launchStatus

    val lastLaunchTime: String
        get() = uiState.lastLaunchTime

    val statusMessage: String
        get() = uiState.statusMessage

    val gameConnectionStatus: GameConnectionStatus
        get() = uiState.gameConnectionStatus

    private var awaitConnectionJob: Job? = null

    val hasGamePath: Boolean
        get() = gamePath != "null" && gamePath.isNotBlank()

    val hasPluginConfig: Boolean
        get() = pluginConfigPath.isNotBlank()

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
        val currentGamePath = settings.getString("gamePath", "null")
        val snapshot = gamePathService.inspect(currentGamePath)
        val hasCurrentGamePath = currentGamePath != "null" && currentGamePath.isNotBlank()
        val nextLaunchStatus = when {
            uiState.gameConnectionStatus == GameConnectionStatus.Connected -> HomeLaunchStatus.Running
            !hasCurrentGamePath -> HomeLaunchStatus.MissingGamePath
            !snapshot.executableExists -> HomeLaunchStatus.MissingExecutable
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
                settings.putString("gamePath", file.path)
                refresh()
            }
        }
    }

    fun launchGame() {
        refresh()
        if (launchStatus != HomeLaunchStatus.Ready && launchStatus != HomeLaunchStatus.Running) return

        screenModelScope.launch {
            uiState = uiState.copy(
                launchStatus = HomeLaunchStatus.Launching,
                statusMessage = "",
            )
            val result = withContext(Dispatchers.IO) {
                processLauncher.launch(gamePath)
            }

            result.fold(
                onSuccess = {
                    val nextLaunchStatus = if (gameConnectionStatus == GameConnectionStatus.Connected) {
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
            if (launchStatus == HomeLaunchStatus.Launching &&
                gameConnectionStatus != GameConnectionStatus.Connected
            ) {
                uiState = uiState.copy(launchStatus = HomeLaunchStatus.Ready)
            }
        }
    }

    fun openGameDirectory() {
        val directory = File(gameDirectory)
        if (!directory.exists()) return

        screenModelScope.launch(Dispatchers.IO) {
            val errorMessage = runCatching {
                Desktop.getDesktop().open(directory)
            }.exceptionOrNull()?.let { error ->
                error.message ?: error::class.simpleName.orEmpty()
            }
            if (errorMessage != null) {
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(
                        launchStatus = HomeLaunchStatus.Error,
                        statusMessage = errorMessage,
                    )
                }
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

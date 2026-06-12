package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import core.GameConnectionStatus
import core.RuntimeServices
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

class HomeScreenModel(
    val settings: Settings = Settings(),
) : ScreenModel {

    var gamePath by mutableStateOf(settings.getString("gamePath", "null"))
        private set

    var gameDirectory by mutableStateOf("")
        private set

    var gameFileName by mutableStateOf("")
        private set

    var pluginConfigPath by mutableStateOf("")
        private set

    var pluginCount by mutableStateOf(0)
        private set

    var launchStatus by mutableStateOf(HomeLaunchStatus.MissingGamePath)
        private set

    var lastLaunchTime by mutableStateOf("")
        private set

    var statusMessage by mutableStateOf("")
        private set

    var gameConnectionStatus by mutableStateOf(RuntimeServices.gameService.connectionStatus)
        private set

    private val connectionListener: (GameConnectionStatus) -> Unit = { status ->
        gameConnectionStatus = status
        if (status == GameConnectionStatus.Connected) {
            awaitConnectionJob?.cancel()
        }
        refresh()
    }

    private var awaitConnectionJob: Job? = null

    val hasGamePath: Boolean
        get() = gamePath != "null" && gamePath.isNotBlank()

    val hasPluginConfig: Boolean
        get() = pluginConfigPath.isNotBlank()

    init {
        RuntimeServices.gameService.addConnectionListener(connectionListener)
        refresh()
    }

    fun refresh() {
        gamePath = settings.getString("gamePath", "null")
        val snapshot = inspectGamePath(gamePath)
        gameDirectory = snapshot.gameDirectory
        gameFileName = snapshot.gameFileName
        pluginConfigPath = snapshot.pluginConfigPath
        pluginCount = snapshot.pluginCount
        launchStatus = when {
            gameConnectionStatus == GameConnectionStatus.Connected -> HomeLaunchStatus.Running
            !hasGamePath -> HomeLaunchStatus.MissingGamePath
            !snapshot.executableExists -> HomeLaunchStatus.MissingExecutable
            launchStatus == HomeLaunchStatus.Launching -> HomeLaunchStatus.Launching
            else -> HomeLaunchStatus.Ready
        }
        statusMessage = snapshot.message
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
            launchStatus = HomeLaunchStatus.Launching
            statusMessage = ""
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val executable = File(gamePath)
                    ProcessBuilder(executable.absolutePath)
                        .directory(executable.parentFile)
                        .start()
                }
            }

            result.fold(
                onSuccess = {
                    lastLaunchTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    launchStatus = if (gameConnectionStatus == GameConnectionStatus.Connected) {
                        HomeLaunchStatus.Running
                    } else {
                        HomeLaunchStatus.Launching
                    }
                    statusMessage = ""
                    waitForGameConnection()
                },
                onFailure = { error ->
                    launchStatus = HomeLaunchStatus.Error
                    statusMessage = error.message ?: error::class.simpleName.orEmpty()
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
                launchStatus = HomeLaunchStatus.Ready
            }
        }
    }

    fun openGameDirectory() {
        val directory = File(gameDirectory)
        if (!directory.exists()) return

        screenModelScope.launch(Dispatchers.IO) {
            runCatching {
                Desktop.getDesktop().open(directory)
            }.onFailure { error ->
                statusMessage = error.message ?: error::class.simpleName.orEmpty()
                launchStatus = HomeLaunchStatus.Error
            }
        }
    }

    private fun inspectGamePath(path: String): HomeSnapshot {
        if (path == "null" || path.isBlank()) {
            return HomeSnapshot(message = "missing-game-path")
        }

        val gameFile = File(path)
        val gameDirectory = when {
            gameFile.isDirectory -> gameFile
            else -> gameFile.parentFile
        }

        if (!gameFile.exists() || gameDirectory == null) {
            return HomeSnapshot(
                gameFileName = gameFile.name,
                gameDirectory = gameDirectory?.absolutePath.orEmpty(),
                message = "missing-executable",
            )
        }

        val configFile = File(File(gameDirectory, "honkai_rts"), "GamePlugins/GamePluginConfigs.toml")
        val pluginCount = if (configFile.exists()) {
            runCatching {
                configFile.readLines().count { it.trim() == "[[PluginConfigs]]" }
            }.getOrDefault(0)
        } else {
            0
        }

        return HomeSnapshot(
            executableExists = gameFile.isFile,
            gameFileName = gameFile.name,
            gameDirectory = gameDirectory.absolutePath,
            pluginConfigPath = configFile.takeIf { it.exists() }?.absolutePath.orEmpty(),
            pluginCount = pluginCount,
        )
    }

    override fun onDispose() {
        awaitConnectionJob?.cancel()
        RuntimeServices.gameService.removeConnectionListener(connectionListener)
        super.onDispose()
    }

    private companion object {
        const val GAME_CONNECTION_WAIT_TIMEOUT_MS = 20_000L
    }
}

private data class HomeSnapshot(
    val executableExists: Boolean = false,
    val gameFileName: String = "",
    val gameDirectory: String = "",
    val pluginConfigPath: String = "",
    val pluginCount: Int = 0,
    val message: String = "",
)

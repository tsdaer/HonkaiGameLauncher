package screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.GameConnectionStatus
import core.RuntimeServices
import core.platform.FileSystemGateway
import core.platform.ProcessLauncher
import core.service.GamePathService
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.settings.AppSettingsStore
import ui.settings.SharedAppSettingsStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

/**
 * 首页启动状态枚举。
 *
 * 表示游戏路径和进程的当前状态。
 */
enum class HomeLaunchStatus {
    /** 未设置游戏路径 */
    MissingGamePath,
    /** 设置了路径但 exe 文件不存在 */
    MissingExecutable,
    /** 可以启动 */
    Ready,
    /** 正在启动中 */
    Launching,
    /** 游戏正在运行（已收到连接） */
    Running,
    /** 启动失败 */
    Error,
}

/**
 * 首页 UI 状态数据类。
 *
 * @property gamePath             游戏 exe 路径
 * @property gameDirectory        游戏所在目录的绝对路径
 * @property gameFileName         游戏可执行文件名
 * @property pluginConfigPath     插件配置文件路径（若存在）
 * @property pluginCount          已加载的插件数量
 * @property launchStatus         启动状态
 * @property lastLaunchTime       上次启动时间（格式化字符串）
 * @property statusMessage        状态消息（错误提示等）
 * @property gameConnectionStatus 游戏连接状态（来自 GameService）
 */
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

/**
 * 首页 ScreenModel。
 *
 * 管理游戏路径选择、路径状态检查、游戏启动和目录打开等交互逻辑。
 *
 * ## 数据流
 * 1. [init] 中收集 [gameConnectionStatus] 和 [settingsStore.state] 的变化
 * 2. 设置变更时自动 refresh 路径状态
 * 3. 连接状态变更时更新 UI 并刷新
 *
 * ## 启动流程
 * 1. 验证当前状态为 Ready 或 Running
 * 2. 在 IO 协程中调用 [ProcessLauncher.launch]
 * 3. 成功后记录启动时间，等待游戏连接
 * 4. 20 秒内未收到连接则回退为 Ready
 */
class HomeScreenModel(
    private val settingsStore: AppSettingsStore = SharedAppSettingsStore.instance,
    private val gamePathService: GamePathService = GamePathService(),
    private val processLauncher: ProcessLauncher = ProcessLauncher(),
    private val fileSystemGateway: FileSystemGateway = FileSystemGateway(),
    private val gameConnectionStatus: StateFlow<GameConnectionStatus> = RuntimeServices.gameService.connectionStatus,
) : ScreenModel {

    var uiState by mutableStateOf(
        HomeUiState(
            gamePath = settingsStore.state.value.gamePath,
            gameConnectionStatus = gameConnectionStatus.value,
        )
    )
        private set

    /** 等待游戏连接的 Job，20 秒超时 */
    private var awaitConnectionJob: Job? = null

    init {
        // 收集游戏连接状态变化
        screenModelScope.launch {
            gameConnectionStatus.collect(::onConnectionStatusChanged)
        }
        // 收集游戏路径设置变化
        screenModelScope.launch {
            settingsStore.state
                .map { it.gamePath }
                .distinctUntilChanged()
                .collectLatest { refresh(it) }
        }
    }

    /** 连接状态变化时更新 UI 并刷新路径状态 */
    private fun onConnectionStatusChanged(status: GameConnectionStatus) {
        uiState = uiState.copy(gameConnectionStatus = status)
        if (status == GameConnectionStatus.Connected) {
            awaitConnectionJob?.cancel()
        }
        refresh()
    }

    /** 使用当前的 gamePath 刷新路径状态 */
    fun refresh() {
        refresh(settingsStore.state.value.gamePath)
    }

    private fun refresh(currentGamePath: String?) {
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

    /** 打开文件选择器让用户选择游戏 exe */
    fun selectGamePath() {
        screenModelScope.launch {
            val file = FileKit.openFilePicker()
            if (file != null) {
                settingsStore.setGamePath(file.path)
            }
        }
    }

    /**
     * 启动游戏进程。
     * 仅当状态为 Ready 或 Running 时可执行。
     * 启动后在 IO 线程执行以保持 UI 流畅。
     */
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
                    // 等待游戏连接
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

    /**
     * 等待游戏连接的超时协程。
     * 启动游戏后 20 秒内未收到连接，则将状态回退为 Ready。
     */
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

    /** 在系统文件管理器中打开游戏目录 */
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
        /** 等待游戏连接的超时时间：20 秒 */
        const val GAME_CONNECTION_WAIT_TIMEOUT_MS = 20_000L
    }
}

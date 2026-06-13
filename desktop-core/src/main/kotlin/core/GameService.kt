package core

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

/**
 * 游戏连接状态枚举。
 *
 * 表示启动器与本机游戏客户端之间 HTTP 日志回传通道的连接生命周期。
 *
 * @property Stopped        服务已停止，未监听任何端口
 * @property Waiting        服务正在监听端口，但尚未收到游戏日志回传
 * @property Connected      已收到游戏日志回传，表示游戏正在运行并连接
 */
enum class GameConnectionStatus {
    Stopped,
    Waiting,
    Connected,
}

/**
 * 游戏服务启动结果。
 *
 * @property port     服务实际绑定的本机端口号（127.0.0.1）
 * @property portFile 写入端口信息的临时 JSON 文件，供游戏侧读取
 */
data class GameServiceStartResult(
    val port: Int,
    val portFile: File,
)

/**
 * 游戏通信服务。
 *
 * 在本机启动一个基于 Ktor Netty 的 HTTP 服务端，监听 127.0.0.1 随机端口，
 * 接收游戏客户端通过 `POST /game/status` 回传的日志 JSON 数据。
 *
 * 核心职责：
 * - 动态分配空闲端口并写入临时端口文件，供游戏进程读取后发起回连
 * - 解析游戏回传日志，并通过 StateFlow / SharedFlow / 回调三种方式分发给订阅者
 * - 连接超时看门狗：超过 [GAME_CONNECTION_TIMEOUT_MS] 未收到日志回传时，自动将状态回退为 Waiting
 * - 应用退出时清理端口文件和停止服务
 *
 * ## 构造方式
 * - 无参构造：使用默认协程作用域（SupervisorJob + Dispatchers.Default）
 *   和默认端口文件路径（系统临时目录下的 `honkai_rts_launcher_port.json`）
 * - 双参构造：由调用方提供协程作用域和/或端口文件，适合测试场景
 *
 * ## 状态分发方式
 * | 方式                        | 类型                          | 说明               |
 * |-----------------------------|-------------------------------|-------------------|
 * | [connectionStatus]          | [StateFlow]                   | 连接状态（可收集）     |
 * | [logEvents]                 | [SharedFlow]                  | 批量日志事件（可收集）  |
 * | [addLogListener]            | 回调                          | 日志监听器（旧式回调）  |
 * | [addConnectionListener]     | 回调                          | 连接状态监听器（旧式）  |
 */
class GameService private constructor(
    private val serviceScope: CoroutineScope,
    private val cancelServiceScopeOnClose: Boolean,
    private val portFile: File,
) {
    /**
     * 无参构造。
     * 使用默认协程作用域（SupervisorJob + Dispatchers.Default），
     * close() 时会自动取消该作用域。
     * 端口文件写入系统临时目录下的 `honkai_rts_launcher_port.json`。
     */
    constructor() : this(
        serviceScope = defaultServiceScope(),
        cancelServiceScopeOnClose = true,
        portFile = defaultPortFile(),
    )

    /**
     * 双参构造（供测试使用）。
     * 由调用方提供协程作用域和/或端口文件，close() 时不会取消传入的作用域。
     *
     * @param serviceScope 协程作用域，用于启动看门狗等异步任务
     * @param portFile     端口信息写入的目标文件，游戏侧从此文件读取端口号
     */
    constructor(
        serviceScope: CoroutineScope,
        portFile: File = defaultPortFile(),
    ) : this(
        serviceScope = serviceScope,
        cancelServiceScopeOnClose = false,
        portFile = portFile,
    )

    /** Ktor 嵌入式 Netty 服务端实例，null 表示未启动 */
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    /** 当前监听的端口号，-1 表示未启动 */
    private var port: Int = -1

    /** 连接超时看门狗协程的 Job，用于取消 */
    private var connectionWatchdog: Job? = null

    /**
     * 最后一次收到游戏日志信号的时间戳（毫秒）。
     * 使用 @Volatile 保证多线程可见性，因为 Ktor 请求线程和看门狗协程可能在不同线程。
     */
    @Volatile
    private var lastGameSignalAt: Long = 0L

    /** 连接状态的 MutableStateFlow 内部存储 */
    private val mutableConnectionStatus = MutableStateFlow(GameConnectionStatus.Stopped)

    /** 日志事件的 MutableSharedFlow 内部存储，缓冲区容量 64 用于应对突发日志 */
    private val mutableLogEvents = MutableSharedFlow<List<LauncherLogEntry>>(extraBufferCapacity = 64)

    /** 只读连接状态流，供 UI 层收集 */
    val connectionStatus: StateFlow<GameConnectionStatus> = mutableConnectionStatus.asStateFlow()

    /** 只读日志事件流，供 UI 层收集 */
    val logEvents: SharedFlow<List<LauncherLogEntry>> = mutableLogEvents.asSharedFlow()

    /** 日志监听器列表（线程安全），用于旧式回调分发 */
    private val logListeners = CopyOnWriteArrayList<(List<LauncherLogEntry>) -> Unit>()

    /** 连接状态监听器列表（线程安全），用于旧式回调分发 */
    private val connectionListeners = CopyOnWriteArrayList<(GameConnectionStatus) -> Unit>()

    /**
     * 注册日志监听器（回调方式）。
     * 每次游戏回传日志解析成功时会调用此监听器。
     *
     * @param listener 接收解析后的日志条目列表的回调函数
     */
    fun addLogListener(listener: (List<LauncherLogEntry>) -> Unit) {
        logListeners += listener
    }

    /**
     * 移除日志监听器。
     *
     * @param listener 之前通过 [addLogListener] 注册的回调函数
     */
    fun removeLogListener(listener: (List<LauncherLogEntry>) -> Unit) {
        logListeners -= listener
    }

    /** 向所有日志监听器分发日志，由内部调用 */
    private fun notifyLogListeners(logs: List<LauncherLogEntry>) {
        logListeners.forEach { it.invoke(logs) }
    }

    /**
     * 注册连接状态监听器（回调方式）。
     * 注册时会立即回调当前状态，确保订阅者始终拿到最新状态。
     *
     * @param listener 接收连接状态的回调函数
     */
    fun addConnectionListener(listener: (GameConnectionStatus) -> Unit) {
        connectionListeners += listener
        // 立即通知当前状态，确保订阅者同步到最新值
        listener(connectionStatus.value)
    }

    /**
     * 移除连接状态监听器。
     *
     * @param listener 之前通过 [addConnectionListener] 注册的回调函数
     */
    fun removeConnectionListener(listener: (GameConnectionStatus) -> Unit) {
        connectionListeners -= listener
    }

    /**
     * 更新连接状态，同时通过 MutableStateFlow 和回调通知订阅者。
     * 如果新状态与当前状态相同则跳过，避免重复通知。
     *
     * @param status 新的连接状态
     */
    private fun updateConnectionStatus(status: GameConnectionStatus) {
        if (mutableConnectionStatus.value == status) return

        mutableConnectionStatus.value = status
        connectionListeners.forEach { it.invoke(status) }
    }

    /**
     * 启动游戏通信服务（线程安全）。
     *
     * 流程：
     * 1. 如果已启动则直接返回当前端口信息（幂等）
     * 2. 在 127.0.0.1 上找一个空闲端口
     * 3. 配置 Ktor 路由：`POST /game/status` 接收游戏日志并解析
     * 4. 以非阻塞模式启动 Netty 服务端
     * 5. 将端口号写入临时 JSON 文件供游戏侧读取
     * 6. 状态切换为 Waiting，启动连接超时看门狗
     *
     * @return 包含端口号和端口文件路径的启动结果
     */
    @Synchronized
    fun start(): GameServiceStartResult {
        // 幂等：已启动则直接返回
        if (server != null) {
            return GameServiceStartResult(port, portFile)
        }
        port = findFreePort()
        lastGameSignalAt = 0L

        // 配置 Ktor 嵌入式 Netty 服务端，监听 127.0.0.1
        val nextServer = embeddedServer(Netty, port, "127.0.0.1") {
            routing {
                post("/game/status") {
                    // 读取游戏回传的原始字节
                    val bytes = call.receiveChannel().toByteArray()
                    val text = bytes.toString(Charsets.UTF_8)

                    try {
                        // 更新时间戳并切换为 Connected 状态
                        lastGameSignalAt = System.currentTimeMillis()
                        updateConnectionStatus(GameConnectionStatus.Connected)

                        // 解析日志 JSON（支持单条或数组）
                        val parsed = LauncherLogParser.parse(text).getOrThrow()

                        println("Received ${parsed.size} log(s) from game.")
                        // 通过 SharedFlow 和回调双通道分发
                        mutableLogEvents.emit(parsed)
                        notifyLogListeners(parsed)
                        call.respondText("OK")
                    } catch (e: Exception) {
                        println("Log parse error: ${e.message}")
                    }
                }
            }
        }

        // 非阻塞启动，不等待服务端停止
        nextServer.start(wait = false)
        server = nextServer
        writePortFile(port)
        updateConnectionStatus(GameConnectionStatus.Waiting)
        startConnectionWatchdog()
        println("Launcher service started on port $port")
        return GameServiceStartResult(port, portFile)
    }

    /**
     * 停止游戏通信服务（线程安全）。
     *
     * 依次执行：
     * 1. 取消看门狗协程
     * 2. 停止 Netty 服务端（1 秒优雅关闭，2 秒强制关闭）
     * 3. 清理端口信息和端口文件
     * 4. 状态切换为 Stopped
     */
    @Synchronized
    fun stop() {
        connectionWatchdog?.cancel()
        connectionWatchdog = null
        server?.stop(1000, 2000)
        server = null
        port = -1
        deletePortFile()
        updateConnectionStatus(GameConnectionStatus.Stopped)
    }

    /**
     * 关闭服务并可选地取消协程作用域。
     * 如果使用无参构造创建（[cancelServiceScopeOnClose] = true），
     * 则关闭时会取消默认的协程作用域，适合应用退出场景。
     */
    fun close() {
        stop()
        if (cancelServiceScopeOnClose) {
            serviceScope.cancel()
        }
    }

    /**
     * 启动连接超时看门狗。
     *
     * 每隔 5 秒检查一次：如果当前状态为 Connected 但距离上次收到日志已超过
     * [GAME_CONNECTION_TIMEOUT_MS]（15 秒），则将状态回退为 Waiting。
     * 这表示游戏可能已经退出但未正常通知启动器。
     */
    private fun startConnectionWatchdog() {
        connectionWatchdog?.cancel()
        connectionWatchdog = serviceScope.launch {
            while (true) {
                delay(5000.milliseconds)
                val elapsed = System.currentTimeMillis() - lastGameSignalAt
                if (connectionStatus.value == GameConnectionStatus.Connected && elapsed > GAME_CONNECTION_TIMEOUT_MS) {
                    updateConnectionStatus(GameConnectionStatus.Waiting)
                }
            }
        }
    }

    /**
     * 在 127.0.0.1 上查找一个空闲 TCP 端口。
     * 通过绑定到端口 0 让操作系统自动分配，获取后立即释放。
     *
     * @return 空闲端口号
     */
    private fun findFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }

    /**
     * 将端口信息写入临时 JSON 文件。
     * 格式：`{"launcher_port": <port>, "ts": <timestamp>}`
     * 游戏进程启动后读取该文件来获取通信端口。
     *
     * @param port 当前监听的端口号
     */
    private fun writePortFile(port: Int) {
        portFile.writeText("""{"launcher_port":$port,"ts":${System.currentTimeMillis()}}""")
    }

    /**
     * 删除端口信息文件。
     * 使用 [runCatching] 包裹以防止文件不存在或无权限时抛出异常。
     */
    private fun deletePortFile() {
        runCatching {
            if (portFile.exists()) {
                portFile.delete()
            }
        }
    }

    private companion object {
        /** 游戏连接超时时间：15 秒。超过此时间未收到日志回传则认为连接已断开 */
        const val GAME_CONNECTION_TIMEOUT_MS = 15_000L

        /** 端口信息文件名，写入系统临时目录 */
        const val PORT_FILE_NAME = "honkai_rts_launcher_port.json"

        /**
         * 创建默认协程作用域。
         * 使用 SupervisorJob 确保子协程异常不会取消兄弟协程，
         * 使用 Dispatchers.Default 执行 CPU 密集型任务。
         */
        fun defaultServiceScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /**
         * 返回默认端口文件路径。
         * 位于系统临时目录（java.io.tmpdir）下。
         */
        fun defaultPortFile(): File = File(System.getProperty("java.io.tmpdir"), PORT_FILE_NAME)
    }
}

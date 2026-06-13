package core

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds

enum class GameConnectionStatus {
    Stopped,
    Waiting,
    Connected,
}

class GameService {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var port: Int = -1
    private var connectionWatchdog: Job? = null

    @Volatile
    private var lastGameSignalAt: Long = 0L

    @Volatile
    var connectionStatus: GameConnectionStatus = GameConnectionStatus.Stopped
        private set

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    // ✅ 定义事件监听器集合
    private val logListeners = CopyOnWriteArrayList<(List<LauncherLogEntry>) -> Unit>()
    private val connectionListeners = CopyOnWriteArrayList<(GameConnectionStatus) -> Unit>()

    fun addLogListener(listener: (List<LauncherLogEntry>) -> Unit) {
        logListeners += listener
    }

    fun removeLogListener(listener: (List<LauncherLogEntry>) -> Unit) {
        logListeners -= listener
    }

    private fun notifyLogListeners(logs: List<LauncherLogEntry>) {
        uiScope.launch {
            logListeners.forEach { it.invoke(logs) }
        }
    }

    fun addConnectionListener(listener: (GameConnectionStatus) -> Unit) {
        connectionListeners += listener
        uiScope.launch {
            listener(connectionStatus)
        }
    }

    fun removeConnectionListener(listener: (GameConnectionStatus) -> Unit) {
        connectionListeners -= listener
    }

    private fun updateConnectionStatus(status: GameConnectionStatus) {
        if (connectionStatus == status) return

        connectionStatus = status
        uiScope.launch {
            connectionListeners.forEach { it.invoke(status) }
        }
    }

    fun start() {
        if (server != null) return
        port = findFreePort()
        writePortFile(port)
        updateConnectionStatus(GameConnectionStatus.Waiting)
        startConnectionWatchdog()

        // 在后台线程启动 HTTP 服务
        server = embeddedServer(Netty, port,"127.0.0.1") {
            routing {
                post("/game/status") {
                    val bytes = call.receiveChannel().toByteArray()
                    val text  = bytes.toString(Charsets.UTF_8)

                    // 尝试解析 JSON
                    try {
                        lastGameSignalAt = System.currentTimeMillis()
                        updateConnectionStatus(GameConnectionStatus.Connected)

                        val parsed = LauncherLogParser.parse(text).getOrThrow()

                        println("Received ${parsed.size} log(s) from game.")
                        notifyLogListeners(parsed)
                    } catch (e: Exception) {
                        println("Log parse error: ${e.message}")
                    }
                }
            }
        }
        thread(isDaemon = true, name = "LauncherService") {
            server!!.start(wait = true)
        }
        println("Launcher service started on port $port")
    }

    fun stop() {
        connectionWatchdog?.cancel()
        connectionWatchdog = null
        server?.stop(1000, 2000)
        server = null
        updateConnectionStatus(GameConnectionStatus.Stopped)
    }

    private fun startConnectionWatchdog() {
        connectionWatchdog?.cancel()
        connectionWatchdog = serviceScope.launch {
            while (true) {
                delay(5000.milliseconds)
                val elapsed = System.currentTimeMillis() - lastGameSignalAt
                if (connectionStatus == GameConnectionStatus.Connected && elapsed > GAME_CONNECTION_TIMEOUT_MS) {
                    updateConnectionStatus(GameConnectionStatus.Waiting)
                }
            }
        }
    }

    private fun findFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }

    private fun writePortFile(port: Int) {
        val file = File(System.getProperty("java.io.tmpdir"), "honkai_rts_launcher_port.json")
        file.writeText("""{"launcher_port":$port,"ts":${System.currentTimeMillis()}}""")
    }

    private companion object {
        const val GAME_CONNECTION_TIMEOUT_MS = 15_000L
    }
}

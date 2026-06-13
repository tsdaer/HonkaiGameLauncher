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

enum class GameConnectionStatus {
    Stopped,
    Waiting,
    Connected,
}

data class GameServiceStartResult(
    val port: Int,
    val portFile: File,
)

class GameService private constructor(
    private val serviceScope: CoroutineScope,
    private val cancelServiceScopeOnClose: Boolean,
    private val portFile: File,
) {
    constructor() : this(
        serviceScope = defaultServiceScope(),
        cancelServiceScopeOnClose = true,
        portFile = defaultPortFile(),
    )

    constructor(
        serviceScope: CoroutineScope,
        portFile: File = defaultPortFile(),
    ) : this(
        serviceScope = serviceScope,
        cancelServiceScopeOnClose = false,
        portFile = portFile,
    )

    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var port: Int = -1
    private var connectionWatchdog: Job? = null

    @Volatile
    private var lastGameSignalAt: Long = 0L

    private val mutableConnectionStatus = MutableStateFlow(GameConnectionStatus.Stopped)
    private val mutableLogEvents = MutableSharedFlow<List<LauncherLogEntry>>(extraBufferCapacity = 64)

    val connectionStatus: StateFlow<GameConnectionStatus> = mutableConnectionStatus.asStateFlow()
    val logEvents: SharedFlow<List<LauncherLogEntry>> = mutableLogEvents.asSharedFlow()

    private val logListeners = CopyOnWriteArrayList<(List<LauncherLogEntry>) -> Unit>()
    private val connectionListeners = CopyOnWriteArrayList<(GameConnectionStatus) -> Unit>()

    fun addLogListener(listener: (List<LauncherLogEntry>) -> Unit) {
        logListeners += listener
    }

    fun removeLogListener(listener: (List<LauncherLogEntry>) -> Unit) {
        logListeners -= listener
    }

    private fun notifyLogListeners(logs: List<LauncherLogEntry>) {
        logListeners.forEach { it.invoke(logs) }
    }

    fun addConnectionListener(listener: (GameConnectionStatus) -> Unit) {
        connectionListeners += listener
        listener(connectionStatus.value)
    }

    fun removeConnectionListener(listener: (GameConnectionStatus) -> Unit) {
        connectionListeners -= listener
    }

    private fun updateConnectionStatus(status: GameConnectionStatus) {
        if (mutableConnectionStatus.value == status) return

        mutableConnectionStatus.value = status
        connectionListeners.forEach { it.invoke(status) }
    }

    @Synchronized
    fun start(): GameServiceStartResult {
        if (server != null) {
            return GameServiceStartResult(port, portFile)
        }
        port = findFreePort()
        lastGameSignalAt = 0L

        val nextServer = embeddedServer(Netty, port, "127.0.0.1") {
            routing {
                post("/game/status") {
                    val bytes = call.receiveChannel().toByteArray()
                    val text = bytes.toString(Charsets.UTF_8)

                    try {
                        lastGameSignalAt = System.currentTimeMillis()
                        updateConnectionStatus(GameConnectionStatus.Connected)

                        val parsed = LauncherLogParser.parse(text).getOrThrow()

                        println("Received ${parsed.size} log(s) from game.")
                        mutableLogEvents.emit(parsed)
                        notifyLogListeners(parsed)
                        call.respondText("OK")
                    } catch (e: Exception) {
                        println("Log parse error: ${e.message}")
                    }
                }
            }
        }

        nextServer.start(wait = false)
        server = nextServer
        writePortFile(port)
        updateConnectionStatus(GameConnectionStatus.Waiting)
        startConnectionWatchdog()
        println("Launcher service started on port $port")
        return GameServiceStartResult(port, portFile)
    }

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

    fun close() {
        stop()
        if (cancelServiceScopeOnClose) {
            serviceScope.cancel()
        }
    }

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

    private fun findFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }

    private fun writePortFile(port: Int) {
        portFile.writeText("""{"launcher_port":$port,"ts":${System.currentTimeMillis()}}""")
    }

    private fun deletePortFile() {
        runCatching {
            if (portFile.exists()) {
                portFile.delete()
            }
        }
    }

    private companion object {
        const val GAME_CONNECTION_TIMEOUT_MS = 15_000L
        const val PORT_FILE_NAME = "honkai_rts_launcher_port.json"

        fun defaultServiceScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun defaultPortFile(): File = File(System.getProperty("java.io.tmpdir"), PORT_FILE_NAME)
    }
}

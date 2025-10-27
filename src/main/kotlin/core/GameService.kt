package core

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.net.ServerSocket
import kotlin.concurrent.thread

class GameService {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var port: Int = -1

    private val uiScope = CoroutineScope(Dispatchers.Main)

    // ✅ 定义事件监听器集合
    private val logListeners = mutableListOf<(List<LauncherLogEntry>) -> Unit>()

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

    fun start() {
        if (server != null) return
        port = findFreePort()
        writePortFile(port)

        // 在后台线程启动 HTTP 服务
        server = embeddedServer(Netty, port,"127.0.0.1") {
            routing {
                post("/game/status") {
                    val bytes = call.receiveChannel().toByteArray()
                    val text  = bytes.toString(Charsets.UTF_8)

                    // 尝试解析 JSON
                    try {
                        val parsed = if (text.trim().startsWith("[")) {
                            Json.decodeFromString<List<LauncherLogEntry>>(text)
                        } else {
                            listOf(Json.decodeFromString<LauncherLogEntry>(text))
                        }

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
        server?.stop(1000, 2000)
        server = null
    }

    private fun findFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }

    private fun writePortFile(port: Int) {
        val file = File(System.getProperty("java.io.tmpdir"), "honkai_rts_launcher_port.json")
        file.writeText("""{"launcher_port":$port,"ts":${System.currentTimeMillis()}}""")
    }
}
package core

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.net.ServerSocket
import kotlin.concurrent.thread

class GameService {
    private var server: ApplicationEngine? = null
    private var port: Int = -1

    private val logs = MutableStateFlow("")


    public fun start() {
        if (server != null) return
        port = findFreePort()
        writePortFile(port)

        // 在后台线程启动 HTTP 服务
        server = embeddedServer(Netty, port) {
            routing {
                post("/game/status") {
                    val body = call.receiveText()
                    println("Game says: $body")
                    call.respondText("""{"ok":true}""")
                }
            }
        }.engine
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
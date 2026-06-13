package core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class GameServiceTest {
    @Test
    fun `start is idempotent and stop clears lifecycle state`() = runTest {
        withTempPortFile { portFile ->
            val service = GameService(backgroundScope, portFile)

            try {
                val firstStart = service.start()
                val secondStart = service.start()

                assertEquals(firstStart, secondStart)
                assertEquals(GameConnectionStatus.Waiting, service.connectionStatus.value)
                assertTrue(portFile.exists())
                assertTrue(portFile.readText().contains(""""launcher_port":${firstStart.port}"""))

                service.stop()

                assertEquals(GameConnectionStatus.Stopped, service.connectionStatus.value)
                assertFalse(portFile.exists())
            } finally {
                service.stop()
            }
        }
    }

    @Test
    fun `game status endpoint emits connection status and log flow events`() = runTest {
        withTempPortFile { portFile ->
            val service = GameService(backgroundScope, portFile)
            val receivedLogs = CompletableDeferred<List<LauncherLogEntry>>()
            val connectedStatus = CompletableDeferred<GameConnectionStatus>()
            val listenerLogs = CompletableDeferred<List<LauncherLogEntry>>()
            val listenerStatuses = mutableListOf<GameConnectionStatus>()

            service.addLogListener { logs ->
                listenerLogs.complete(logs)
            }
            service.addConnectionListener { status ->
                listenerStatuses += status
            }

            backgroundScope.launch {
                receivedLogs.complete(service.logEvents.first())
            }
            backgroundScope.launch {
                connectedStatus.complete(service.connectionStatus.first { it == GameConnectionStatus.Connected })
            }

            try {
                val startResult = service.start()
                postStatus(
                    port = startResult.port,
                    body = """{"type":1,"category":"Game","time":"10:00","message":"Ready"}""",
                )

                val flowLogs = withTimeout(5_000.milliseconds) { receivedLogs.await() }
                val compatibilityLogs = withTimeout(5_000.milliseconds) { listenerLogs.await() }

                assertEquals(GameConnectionStatus.Connected, withTimeout(5_000.milliseconds) { connectedStatus.await() })
                assertEquals("Ready", flowLogs.single().message)
                assertEquals(flowLogs, compatibilityLogs)
                assertEquals(
                    listOf(
                        GameConnectionStatus.Stopped,
                        GameConnectionStatus.Waiting,
                        GameConnectionStatus.Connected,
                    ),
                    listenerStatuses,
                )
            } finally {
                service.stop()
            }
        }
    }

    private suspend fun postStatus(port: Int, body: String) = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        repeat(30) {
            try {
                val connection = URI("http://127.0.0.1:$port/game/status")
                    .toURL()
                    .openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                connection.inputStream.close()
                connection.disconnect()

                if (responseCode in 200..299) {
                    return@withContext
                }

                lastError = IllegalStateException("Unexpected response code $responseCode")
            } catch (error: Exception) {
                lastError = error
                Thread.sleep(50)
            }
        }

        throw AssertionError("Unable to post game status: ${lastError?.message}")
    }

    private inline fun withTempPortFile(block: (File) -> Unit) {
        val tempDir = createTempDirectory("game-service-test").toFile()

        try {
            block(File(tempDir, "launcher-port.json"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

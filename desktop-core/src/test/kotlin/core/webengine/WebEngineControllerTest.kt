package core.webengine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WebEngineControllerTest {

    @Test
    fun `successful initialization publishes progress and ready state`() = runTest {
        val runtime = FakeWebEngineRuntime(
            WebEngineInitializationResult.Ready,
            WebEnginePhase.Checking to null,
            WebEnginePhase.Downloading to 0.42f,
        )
        val controller = createController(runtime)

        controller.ensureInitialized()
        advanceUntilIdle()

        val state = controller.state.value
        assertTrue(state.ready)
        assertEquals(WebEnginePhase.Ready, state.phase)
        assertEquals(1f, state.progress)
        assertNull(state.error)
        assertFalse(state.restartRequired)
    }

    @Test
    fun `failed initialization exposes stable error message`() = runTest {
        val controller = createController(
            FakeWebEngineRuntime(WebEngineInitializationResult.Failed("network unavailable"))
        )

        controller.ensureInitialized()
        advanceUntilIdle()

        val state = controller.state.value
        assertFalse(state.ready)
        assertEquals("network unavailable", state.error)
        assertFalse(state.restartRequired)
    }

    @Test
    fun `restart required does not mark engine ready`() = runTest {
        val controller = createController(
            FakeWebEngineRuntime(WebEngineInitializationResult.RestartRequired)
        )

        controller.ensureInitialized()
        advanceUntilIdle()

        val state = controller.state.value
        assertFalse(state.ready)
        assertTrue(state.restartRequired)
        assertNull(state.error)
    }

    @Test
    fun `retry ignores stale result from previous initialization attempt`() = runTest {
        val firstAttempt = CompletableDeferred<WebEngineInitializationResult>()
        val runtime = QueuedWebEngineRuntime(
            firstAttempt,
            CompletableDeferred(WebEngineInitializationResult.Ready),
        )
        val controller = createController(runtime)

        controller.ensureInitialized()
        advanceUntilIdle()
        controller.retry()
        advanceUntilIdle()

        firstAttempt.complete(WebEngineInitializationResult.Failed("old failure"))
        advanceUntilIdle()

        val state = controller.state.value
        assertTrue(state.ready)
        assertNull(state.error)
        assertFalse(state.restartRequired)
    }

    @Test
    fun `ensure initialized only starts runtime once`() = runTest {
        val runtime = FakeWebEngineRuntime(WebEngineInitializationResult.Ready)
        val controller = createController(runtime)

        controller.ensureInitialized()
        controller.ensureInitialized()
        advanceUntilIdle()

        assertEquals(1, runtime.initializeCalls)
    }

    private fun TestScope.createController(runtime: WebEngineRuntime): WebEngineController {
        return WebEngineController(
            runtime = runtime,
            scope = this,
        )
    }

    private class FakeWebEngineRuntime(
        private val result: WebEngineInitializationResult,
        private vararg val progressUpdates: Pair<WebEnginePhase, Float?>,
    ) : WebEngineRuntime {
        var initializeCalls = 0
            private set

        override suspend fun initialize(progressSink: WebEngineProgressSink): WebEngineInitializationResult {
            initializeCalls++
            progressUpdates.forEach { (phase, progress) ->
                progressSink.update(phase, progress)
            }
            return result
        }
    }

    private class QueuedWebEngineRuntime(
        private vararg val results: CompletableDeferred<WebEngineInitializationResult>,
    ) : WebEngineRuntime {
        private var index = 0

        override suspend fun initialize(progressSink: WebEngineProgressSink): WebEngineInitializationResult {
            return results[index++].await()
        }
    }
}

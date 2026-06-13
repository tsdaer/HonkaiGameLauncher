import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLifecycleCoordinatorTest {

    @Test
    fun `start launches game service only once`() {
        val gameService = FakeDesktopGameService()
        val coordinator = AppLifecycleCoordinator(gameService)

        coordinator.start()
        coordinator.start()

        assertEquals(1, gameService.startCalls)
        assertEquals(0, gameService.stopCalls)
    }

    @Test
    fun `window visibility actions keep tray behavior explicit`() {
        val coordinator = AppLifecycleCoordinator(FakeDesktopGameService())

        assertTrue(coordinator.isVisible)

        coordinator.hideWindow()
        assertFalse(coordinator.isVisible)

        coordinator.showWindow()
        assertTrue(coordinator.isVisible)

        coordinator.toggleWindowVisibility()
        assertFalse(coordinator.isVisible)
    }

    @Test
    fun `exit stops service disposes compose and exits process once`() {
        val gameService = FakeDesktopGameService()
        val coordinator = AppLifecycleCoordinator(gameService)
        var disposeCalls = 0
        var exitCalls = 0

        coordinator.exit(
            onDispose = { disposeCalls++ },
            onExitProcess = { exitCalls++ },
        )
        coordinator.exit(
            onDispose = { disposeCalls++ },
            onExitProcess = { exitCalls++ },
        )

        assertEquals(1, gameService.stopCalls)
        assertEquals(1, disposeCalls)
        assertEquals(1, exitCalls)
    }

    private class FakeDesktopGameService : DesktopGameService {
        var startCalls = 0
            private set

        var stopCalls = 0
            private set

        override fun start() {
            startCalls++
        }

        override fun stop() {
            stopCalls++
        }
    }
}

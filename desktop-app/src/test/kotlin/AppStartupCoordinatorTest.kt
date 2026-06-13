import kotlin.test.Test
import kotlin.test.assertEquals

class AppStartupCoordinatorTest {

    @Test
    fun `initialize configures environment and registers screens once`() {
        var configureCalls = 0
        var registerCalls = 0
        val coordinator = AppStartupCoordinator(
            configureEnvironment = { configureCalls++ },
            registerScreens = { registerCalls++ },
        )

        coordinator.initialize()
        coordinator.initialize()

        assertEquals(1, configureCalls)
        assertEquals(1, registerCalls)
    }
}

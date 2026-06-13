import navigation.registerNavigation
import java.io.PrintStream

class AppStartupCoordinator(
    private val configureEnvironment: () -> Unit = ::configureDesktopEnvironment,
    private val registerScreens: () -> Unit = ::registerNavigation,
) {
    private var initialized = false

    fun initialize() {
        if (initialized) return

        configureEnvironment()
        registerScreens()
        initialized = true
    }
}

private fun configureDesktopEnvironment() {
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")
    System.setOut(PrintStream(System.out, true, "UTF-8"))
    System.setErr(PrintStream(System.err, true, "UTF-8"))
}

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.navigator.Navigator
import honkaigamelauncher.desktop_ui.generated.resources.*
import com.kdroid.composetray.tray.api.Tray
import navigation.SharedScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.components.AppWindowTitleBar
import ui.components.NavigationBar
import ui.fluent.theme.AppFluentTheme
import ui.fluent.theme.FluentTokens
import ui.settings.LocalAppUiSettings
import kotlin.system.exitProcess

@Composable
fun MainView(
    darkTheme: Boolean
) {
    NavigationBar(darkTheme = darkTheme)
}

@OptIn(ExperimentalVoyagerApi::class)
fun main() = application {

    remember { AppStartupCoordinator().also { it.initialize() } }
    val lifecycleCoordinator = remember { AppLifecycleCoordinator().also { it.start() } }
    val uiSettingsController = remember { AppUiSettingsController() }

    val appIcon = painterResource(Res.drawable.logo)

    LaunchedEffect(uiSettingsController.languageCode) {
        uiSettingsController.applyCurrentLanguage()
    }

    val state = rememberWindowState(size = DpSize(1280.dp, 720.dp))

    CompositionLocalProvider(
        LocalAppUiSettings provides uiSettingsController.asAppUiSettings()
    ) {
        val openWindowText = stringResource(Res.string.openWindow)
        val exitApplicationText = stringResource(Res.string.exit)

        Tray(
            iconContent = {
                Image(
                    painter = appIcon,
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize()
                )
            },
            tooltip = stringResource(Res.string.appTitle),
            primaryAction = { lifecycleCoordinator.toggleWindowVisibility() }
        ) {
            Item(label = openWindowText, onClick = {
                lifecycleCoordinator.showWindow()
            })
            Divider()
            Item(label = exitApplicationText, onClick = {
                lifecycleCoordinator.exit(
                    onDispose = { dispose() },
                    onExitProcess = { exitProcess(0) },
                )
            })
        }

        Window(
            onCloseRequest = { lifecycleCoordinator.hideWindow() },
            visible = lifecycleCoordinator.isVisible,
            state = state,
            title = stringResource(Res.string.appTitle),
            transparent = true,
            undecorated = true,
            icon = appIcon,
            resizable = state.placement == WindowPlacement.Floating
        ) {
            AppFluentTheme(darkTheme = uiSettingsController.isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (uiSettingsController.isDarkTheme) FluentTokens.ColorToken.windowBackgroundDark
                            else FluentTokens.ColorToken.windowBackgroundLight
                        )
                ) {
                    Navigator(rememberScreen(SharedScreen.Home)) { navigator ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            AppWindowTitleBar(
                                state = state,
                                onCloseRequest = { lifecycleCoordinator.hideWindow() }
                            )
                            Box(modifier = Modifier.fillMaxSize()) {
                                MainView(darkTheme = uiSettingsController.isDarkTheme)
                            }
                        }
                    }
                }
            }
        }
    }
}

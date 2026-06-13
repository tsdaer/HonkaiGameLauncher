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
import com.russhwolf.settings.Settings
import localization.changeLanguage
import navigation.SharedScreen
import navigation.registerNavigation
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.components.AppWindowTitleBar
import ui.components.NavigationBar
import ui.fluent.theme.AppFluentTheme
import ui.fluent.theme.FluentTokens
import ui.settings.AppNavigationStyle
import ui.settings.AppUiSettings
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

    System.setProperty("file.encoding", "UTF-8")
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))
    System.setErr(java.io.PrintStream(System.err, true, "UTF-8"))

    val lifecycleCoordinator = remember { AppLifecycleCoordinator().also { it.start() } }
    val settings = remember { Settings() }

    val appIcon = painterResource(Res.drawable.logo)
    var isDarkTheme by remember { mutableStateOf(settings.getBoolean("isDarkTheme", false)) }

    var languageCode by remember { mutableStateOf(settings.getString("languageCode", "zh")) }
    var navigationStyle by remember {
        mutableStateOf(AppNavigationStyle.fromKey(settings.getString("navigationStyle", AppNavigationStyle.LeftCompact.key)))
    }

    LaunchedEffect(languageCode) {
        changeLanguage(languageCode)
    }

    var openWindowStr = ""
    var exitApplicationStr = ""

    val state = rememberWindowState(size = DpSize(1280.dp, 720.dp))

    registerNavigation()

    Tray(
        iconContent = {
            Image(
                painter = appIcon,
                contentDescription = "",
                modifier = Modifier.fillMaxSize()
            )
        },
        tooltip = stringResource(Res.string.appTitle),
        primaryAction = { lifecycleCoordinator.toggleWindowVisibility() })
    {
        Item(label = openWindowStr, onClick = {
            lifecycleCoordinator.showWindow()
        })
        Divider()
        Item(label = exitApplicationStr, onClick = {
            lifecycleCoordinator.exit(
                onDispose = { dispose() },
                onExitProcess = { exitProcess(0) },
            )
        })
    }

    CompositionLocalProvider(
        LocalAppUiSettings provides AppUiSettings(
            isDarkTheme = isDarkTheme,
            languageCode = languageCode,
            navigationStyle = navigationStyle,
            onThemeChanged = {
                isDarkTheme = !isDarkTheme
                settings.putBoolean("isDarkTheme", isDarkTheme)
            },
            onLanguageChanged = { language ->
                if (languageCode != language) {
                    languageCode = language
                    settings.putString("languageCode", language)
                    changeLanguage(language)
                }
            },
            onNavigationStyleChanged = { style ->
                if (navigationStyle != style) {
                    navigationStyle = style
                    settings.putString("navigationStyle", style.key)
                }
            }
        )
    ) {
        openWindowStr = stringResource(Res.string.openWindow)
        exitApplicationStr = stringResource(Res.string.exit)

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
            System.setProperty("awt.useSystemAAFontSettings", "on")
            System.setProperty("swing.aatext", "true")
            AppFluentTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isDarkTheme) FluentTokens.ColorToken.windowBackgroundDark
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
                                MainView(darkTheme = isDarkTheme)
                            }
                        }
                    }
                }
            }
        }
    }
}

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import honkaigamelauncher.desktop_ui.generated.resources.*
import com.kdroid.composetray.tray.api.Tray
import core.RuntimeServices
import localization.changeLanguage
import navigation.SharedScreen
import navigation.registerNavigation
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import ui.components.AppWindowTitleBar
import ui.components.NavigationBar
import ui.fluent.components.FluentText
import ui.fluent.theme.AppFluentTheme
import ui.fluent.theme.FluentTokens
import kotlin.system.exitProcess

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainView(
    darkTheme: Boolean
) {
    val navigator = LocalNavigator.current
    val data = navigator?.let { it.lastItem as? IScreenInterface }
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationBar(darkTheme = darkTheme)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = FluentTokens.Spacing.large,
                    end = FluentTokens.Spacing.xLarge,
                    top = FluentTokens.Spacing.xLarge,
                    bottom = FluentTokens.Spacing.xLarge
                )
        ) {
            data?.let {
                FluentText(
                    text = it.getTitle(),
                    fontSize = 22.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            navigator?.let {
                val slowAnimation = tween<IntOffset>(durationMillis = 300)
                val slowAnimation2 = tween<Float>(durationMillis = 300)
                AnimatedContent(
                    targetState = navigator.lastItem.key,
                    transitionSpec = {
                        (slideInVertically(animationSpec = slowAnimation) { fullHeight -> (0.1 * fullHeight).toInt() } + fadeIn(animationSpec = slowAnimation2))
                            .togetherWith(slideOutVertically(animationSpec = slowAnimation) { fullHeight -> (-0.1 * fullHeight).toInt() } + fadeOut(slowAnimation2))
                    },
                    contentKey = { navigator.lastItem.key }
                ) {
                    CurrentScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalVoyagerApi::class)
fun main() = application {

    System.setProperty("file.encoding", "UTF-8")
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))
    System.setErr(java.io.PrintStream(System.err, true, "UTF-8"))

    RuntimeServices.gameService.start()

    var isVisible by remember { mutableStateOf(true) }
    val appIcon = painterResource(Res.drawable.logo)
    var isDarkTheme by remember { mutableStateOf(false) }

    val localLocalization = staticCompositionLocalOf { "zh" }
    var languageCode by remember { mutableStateOf("zh") }

    var openWindowStr = ""
    var exitApplicationStr = ""

    val state = rememberWindowState(size = DpSize(1280.dp, 720.dp))

    registerNavigation()

    Tray(
        iconContent = {
            androidx.compose.material.Icon(
                appIcon,
                contentDescription = "",
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize()
            )
        },
        tooltip = stringResource(Res.string.appTitle),
        primaryAction = { isVisible = !isVisible })
    {
        Item(label = openWindowStr, onClick = {
            isVisible = true
        })
        Divider()
        Item(label = exitApplicationStr, onClick = {
            RuntimeServices.gameService.stop()
            dispose()
            exitProcess(0)
        })
    }

    CompositionLocalProvider(localLocalization provides languageCode)
    {
        openWindowStr = stringResource(Res.string.openWindow)
        exitApplicationStr = stringResource(Res.string.exit)

        Window(
            onCloseRequest = { isVisible = false },
            visible = isVisible,
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
                                onCloseRequest = { isVisible = false },
                                onThemeChanged = { isDarkTheme = !isDarkTheme },
                                onLanguageChanged = {
                                    if (languageCode == "en") {
                                        languageCode = "zh"
                                        changeLanguage("zh")
                                    } else {
                                        languageCode = "en"
                                        changeLanguage("en")
                                    }
                                },
                                isDarkTheme = isDarkTheme
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

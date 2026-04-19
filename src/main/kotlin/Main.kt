import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import com.honkai_rts.honkaigamelauncher.generated.resources.*
import com.kdroid.composetray.tray.api.Tray
import core.GameService
import localization.changeLanguage
import navigation.SharedScreen
import navigation.registerNavigation
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import ui.components.AppWindowTitleBar
import ui.components.NavigationBar
import ui.theme.DarkColorScheme
import ui.theme.HarmonyTypography
import ui.theme.LightColorScheme
import kotlin.system.exitProcess

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainView() {
    val navigator = LocalNavigator.current
    val data = navigator?.let { it.lastItem as? IScreenInterface }
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row {
                NavigationBar()
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colors.background,
                        border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(0.1f)),
                        modifier = Modifier.fillMaxSize()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(8.dp, 0.dp, 0.dp, 0.dp),
                                ambientColor = MaterialTheme.colors.primary,
                                spotColor = MaterialTheme.colors.primary
                            ),
                        shape = RoundedCornerShape(8.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(60.dp)) {

                            data?.let {
                                Text(
                                    text = it.getTitle(),
                                    style = MaterialTheme.typography.h5,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }

                            navigator?.let {
                                val slowAnimation = tween<IntOffset>(durationMillis = 300)
                                val slowAnimation2 = tween<Float>(durationMillis = 300)
                                AnimatedContent(
                                    targetState = navigator.lastItem.key,
                                    transitionSpec = {
                                        (slideInVertically(animationSpec = slowAnimation) { fullHeight -> (0.1*fullHeight).toInt() } + fadeIn(animationSpec = slowAnimation2)).
                                        togetherWith(slideOutVertically(animationSpec = slowAnimation) { fullHeight -> (-0.1*fullHeight).toInt() } + fadeOut(slowAnimation2)) },
                                    contentKey = { navigator.lastItem.key }) {
                                    CurrentScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val gameService = GameService()

@OptIn(ExperimentalVoyagerApi::class)
fun main() = application {

    System.setProperty("file.encoding", "UTF-8")
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))
    System.setErr(java.io.PrintStream(System.err, true, "UTF-8"))

    gameService.start()

    var isVisible by remember { mutableStateOf(true) }
    val appIcon = painterResource(Res.drawable.logo)
    var isDarkTheme by remember { mutableStateOf(false) }
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    val localLocalization = staticCompositionLocalOf { "zh" }
    var languageCode by remember { mutableStateOf("zh") }

    var openWindowStr = ""
    var exitApplicationStr = ""

    val state = rememberWindowState(size = DpSize(1280.dp, 720.dp))

    registerNavigation()

    Tray(
        iconContent = {
            Icon(
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
            gameService.stop()
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
            MaterialTheme(colors = colorScheme, typography = HarmonyTypography()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    val isFloating = state.placement == WindowPlacement.Floating

                    Surface(
                        modifier = Modifier.padding(if (isFloating) 5.dp else 0.dp)
                            .shadow(
                                elevation = if (isFloating) 3.dp else 0.dp, // 全屏时关闭阴影
                                shape = RoundedCornerShape(if (isFloating) 10.dp else 0.dp)
                            ),
                        border = if (isFloating) BorderStroke(1.dp, MaterialTheme.colors.background) else null,
                        color = MaterialTheme.colors.background,
                        shape = RoundedCornerShape(if (isFloating) 10.dp else 0.dp)
                    ) {
                        Box {
                            Navigator(rememberScreen(SharedScreen.Home)) { navigator ->
                                Scaffold(
                                    topBar = {
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
                                            isDarkTheme = !isDarkTheme
                                        )
                                    },
                                    content = { MainView() }
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}

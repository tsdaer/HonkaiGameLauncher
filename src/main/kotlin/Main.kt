import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

import com.kdroid.composetray.tray.api.Tray

import com.honkai_rts.honkaigamelauncher.generated.resources.Res
import com.honkai_rts.honkaigamelauncher.generated.resources.appTitle
import com.honkai_rts.honkaigamelauncher.generated.resources.exit
import com.honkai_rts.honkaigamelauncher.generated.resources.logo
import com.honkai_rts.honkaigamelauncher.generated.resources.openWindow
import service.navigation.SharedScreen
import util.IScreenInterface

import service.navigation.registerNavigation

import ui.theme.DarkColorScheme
import ui.theme.LightColorScheme
import ui.components.AppWindowTitleBar
import ui.components.NavigationBar
import ui.theme.HarmonyTypography
import util.changeLanguage
import kotlin.system.exitProcess

@Composable
fun MainView(){
    val navigator = LocalNavigator.current
    val data = navigator?.let { it.lastItem as? IScreenInterface }
    Box(modifier = Modifier.fillMaxSize()){
        Column {
            Row{
                NavigationBar()
                Box(modifier = Modifier.fillMaxSize())
                {
                    Surface(
                        color = MaterialTheme.colors.background,
                        border = BorderStroke(1.dp,MaterialTheme.colors.primary.copy(0.1f)),
                        modifier = Modifier.fillMaxSize()
                            .shadow(elevation = 8.dp,
                                shape = RoundedCornerShape(8.dp,0.dp,0.dp,0.dp),
                                ambientColor = MaterialTheme.colors.primary,
                                spotColor = MaterialTheme.colors.primary),
                        shape = RoundedCornerShape(8.dp,0.dp,0.dp,0.dp))
                    {
                        Column(modifier = Modifier.fillMaxSize().padding(60.dp)) {

                            data?.let { Text(text = it.getTitle(), style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold,modifier = Modifier.padding(bottom = 10.dp)) }
                            CurrentScreen()
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {

    var isVisible by remember { mutableStateOf(true) }
    val appIcon = painterResource(Res.drawable.logo)
    val state = rememberWindowState(size = DpSize(1280.dp, 720.dp))
    var isDarkTheme by remember { mutableStateOf(false) }
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    val localLocalization = staticCompositionLocalOf { "zh" }
    var languageCode by remember { mutableStateOf("zh") }

    registerNavigation()

    var openWindowStr = ""
    var exitApplicationStr = ""


    Tray(
        iconContent = {
            Icon(
                appIcon,
                contentDescription = "",
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize()) },
        tooltip = stringResource(Res.string.appTitle),
        primaryAction = {isVisible = !isVisible})
    {
        Item(label =   openWindowStr , onClick = {
            isVisible = true
        })
        Divider()
        Item(label = exitApplicationStr, onClick = {
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
            icon = appIcon
        ) {
            System.setProperty("awt.useSystemAAFontSettings", "on")
            System.setProperty("swing.aatext", "true")
            /**
            window.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    if (state.placement == WindowPlacement.Floating) {
                        window.shape = RoundRectangle2D.Double(
                            0.0, 0.0, window.width.toDouble(), window.height.toDouble(), 16.0, 16.0
                        )
                    }
                }
            })
            **/

            MaterialTheme(colors = colorScheme, typography = HarmonyTypography()) {
                Surface(
                    modifier = Modifier.padding(5.dp)
                        .shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(10.dp)),
                    border = BorderStroke(1.dp, MaterialTheme.colors.background),
                    color = MaterialTheme.colors.background,
                    shape = RoundedCornerShape(10.dp) //窗口现在有圆角
                ) {

                    Box {
                        Navigator(rememberScreen(SharedScreen.Home)){ navigator ->
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
                                content = {MainView()}
                            )
                        }
                    }
                }
            }
        }
    }
}

package navigation

import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.registry.ScreenProvider
import cafe.adriel.voyager.core.registry.ScreenRegistry
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.File
import compose.icons.evaicons.fill.Globe
import compose.icons.evaicons.fill.Home
import compose.icons.feathericons.Activity
import compose.icons.feathericons.Settings
import compose.icons.lineawesomeicons.PuzzlePieceSolid
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_doc
import honkaigamelauncher.desktop_ui.generated.resources.screen_home
import honkaigamelauncher.desktop_ui.generated.resources.screen_log
import honkaigamelauncher.desktop_ui.generated.resources.screen_plugin
import honkaigamelauncher.desktop_ui.generated.resources.screen_website
import honkaigamelauncher.desktop_ui.generated.resources.setting
import org.jetbrains.compose.resources.StringResource
import screen.HomeScreen
import screen.SettingScreen
import screen.feature.DocsScreen
import screen.feature.LogScreen
import screen.feature.PluginScreen
import screen.feature.WebScreen

sealed class SharedScreen : ScreenProvider {
    object Home : SharedScreen()
    object Setting : SharedScreen()
    object Plugin : SharedScreen()
    object Docs : SharedScreen()
    object Web : SharedScreen()
    object Log : SharedScreen()
}

data class ScreenDescriptor(
    val provider: SharedScreen,
    val route: String,
    val titleKey: StringResource,
    val icon: ImageVector,
    val showInNavigation: Boolean,
    val order: Int,
)

val screenDescriptors = listOf(
    ScreenDescriptor(
        provider = SharedScreen.Home,
        route = "/home",
        titleKey = Res.string.screen_home,
        icon = EvaIcons.Fill.Home,
        showInNavigation = true,
        order = 0,
    ),
    ScreenDescriptor(
        provider = SharedScreen.Plugin,
        route = "/plugin",
        titleKey = Res.string.screen_plugin,
        icon = compose.icons.LineAwesomeIcons.PuzzlePieceSolid,
        showInNavigation = true,
        order = 10,
    ),
    ScreenDescriptor(
        provider = SharedScreen.Docs,
        route = "/docs",
        titleKey = Res.string.screen_doc,
        icon = EvaIcons.Fill.File,
        showInNavigation = true,
        order = 20,
    ),
    ScreenDescriptor(
        provider = SharedScreen.Web,
        route = "/web",
        titleKey = Res.string.screen_website,
        icon = EvaIcons.Fill.Globe,
        showInNavigation = true,
        order = 30,
    ),
    ScreenDescriptor(
        provider = SharedScreen.Log,
        route = "/log",
        titleKey = Res.string.screen_log,
        icon = compose.icons.FeatherIcons.Activity,
        showInNavigation = true,
        order = 40,
    ),
    ScreenDescriptor(
        provider = SharedScreen.Setting,
        route = "/setting",
        titleKey = Res.string.setting,
        icon = compose.icons.FeatherIcons.Settings,
        showInNavigation = false,
        order = 100,
    ),
).sortedBy { it.order }

val featureScreens = screenDescriptors
    .filter { it.showInNavigation && it.provider != SharedScreen.Home }

fun screenDescriptorFor(provider: SharedScreen): ScreenDescriptor {
    return screenDescriptors.first { it.provider == provider }
}

fun screenRoute(provider: SharedScreen): String {
    return screenDescriptorFor(provider).route
}

fun registerNavigation() {
    ScreenRegistry {
        register<SharedScreen.Home> { HomeScreen() }
        register<SharedScreen.Setting> { SettingScreen() }

        register<SharedScreen.Plugin> { PluginScreen() }
        register<SharedScreen.Docs> { DocsScreen() }
        register<SharedScreen.Web> { WebScreen() }
        register<SharedScreen.Log> { LogScreen() }
    }
}

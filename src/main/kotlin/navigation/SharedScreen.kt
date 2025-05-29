package navigation

import cafe.adriel.voyager.core.registry.ScreenProvider
import cafe.adriel.voyager.core.registry.ScreenRegistry
import screen.HomeScreen
import screen.SettingScreen
import screen.feature.DocsScreen
import screen.feature.PluginScreen
import screen.feature.WebScreen

//导航模块声明了所有可达的界面
sealed class SharedScreen : ScreenProvider {
    object Home : SharedScreen()
    object Setting : SharedScreen()

    //更多界面

    object Plugin : SharedScreen()
    object Docs : SharedScreen()
    object Web : SharedScreen()
}

fun registerNavigation() {
    ScreenRegistry {
        register<SharedScreen.Home> { HomeScreen() }
        register<SharedScreen.Setting> { SettingScreen() }

        register<SharedScreen.Plugin> { PluginScreen() }
        register<SharedScreen.Docs> { DocsScreen() }
        register<SharedScreen.Web> { WebScreen() }
    }
}

val featureScreens = listOf(
    SharedScreen.Plugin,
    SharedScreen.Docs,
    SharedScreen.Web
    // Add more screens here as needed
    // SharedScreen.AnotherFeature,
    // SharedScreen.YetAnotherFeature
)

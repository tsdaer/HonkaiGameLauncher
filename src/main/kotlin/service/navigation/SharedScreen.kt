package service.navigation

import cafe.adriel.voyager.core.registry.ScreenProvider
import cafe.adriel.voyager.core.registry.ScreenRegistry
import screen.HomeScreen
import screen.SettingScreen
import screen.feature.PluginScreen

//导航模块声明了所有可达的界面
sealed class SharedScreen: ScreenProvider {
    object Home: SharedScreen()
    object Setting: SharedScreen()

    object Plugin: SharedScreen()
}

fun registerNavigation() {
    ScreenRegistry {
        register<SharedScreen.Home> { HomeScreen() }
        register<SharedScreen.Setting> { SettingScreen() }

        register<SharedScreen.Plugin> { PluginScreen() }
    }
}

val featureScreens = listOf(
    SharedScreen.Plugin,
    // Add more screens here as needed
    // SharedScreen.AnotherFeature,
    // SharedScreen.YetAnotherFeature
)
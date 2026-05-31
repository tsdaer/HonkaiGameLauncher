package navigation

import cafe.adriel.voyager.core.registry.ScreenRegistry
import cafe.adriel.voyager.navigator.Navigator

object NavigationService {
    private val routeMap by lazy {
        SharedScreen::class.sealedSubclasses
            .associate { clazz ->
                val screen = clazz.objectInstance as SharedScreen
                "/${screen::class.simpleName!!.lowercase()}" to screen
            }
    }

    fun navigateTo(url: String,navigator:Navigator) {
        routeMap[url]?.let { provider -> navigator.push(ScreenRegistry.get(provider))
        }
    }
}
package navigation

import cafe.adriel.voyager.core.registry.ScreenRegistry
import cafe.adriel.voyager.navigator.Navigator

object NavigationService {
    private val routeMap by lazy {
        screenDescriptors
            .flatMap { descriptor ->
                listOf(
                    descriptor.route to descriptor.provider,
                    descriptor.route.removePrefix("/") to descriptor.provider,
                )
            }
            .toMap()
    }

    fun navigateTo(url: String, navigator: Navigator) {
        routeMap[url]?.let { provider ->
            navigator.push(ScreenRegistry.get(provider))
        }
    }
}

package navigation

import cafe.adriel.voyager.core.registry.ScreenRegistry
import cafe.adriel.voyager.navigator.Navigator

/**
 * 导航服务（单例）。
 *
 * 提供基于 URL 字符串的页面导航能力。通过预构建的路由映射表，
 * 将 `/home`、`home` 等变体映射到 [SharedScreen] provider。
 *
 * 路由映射基于 [screenDescriptors] 构建，支持带/不带前缀斜杠两种形式。
 */
object NavigationService {
    /**
     * 路由 → ScreenProvider 的映射表。
     * 惰性初始化，同时注册 `/route` 和 `route` 两种形式以兼容不同调用方。
     */
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

    /**
     * 通过 URL 字符串导航到目标页面。
     *
     * @param url       目标路由字符串，如 `"/plugin"` 或 `"plugin"`
     * @param navigator Voyager Navigator 实例
     */
    fun navigateTo(url: String, navigator: Navigator) {
        routeMap[url]?.let { provider ->
            navigator.push(ScreenRegistry.get(provider))
        }
    }
}

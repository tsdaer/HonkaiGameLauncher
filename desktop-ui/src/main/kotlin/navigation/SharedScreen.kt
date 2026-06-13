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

/**
 * 共享页面标识符（密封类）。
 *
 * 每个对象代表一个全局唯一的页面，作为 Voyager [ScreenProvider] 注册和路由解析的 key。
 * 新增页面时需在此添加对应对象，并在 [screenDescriptors] 和 [registerNavigation] 中注册。
 */
sealed class SharedScreen : ScreenProvider {
    /** 首页 */
    object Home : SharedScreen()
    /** 设置页 */
    object Setting : SharedScreen()
    /** 插件配置页 */
    object Plugin : SharedScreen()
    /** 文档中心页 */
    object Docs : SharedScreen()
    /** 内置网页工具页 */
    object Web : SharedScreen()
    /** 游戏日志页 */
    object Log : SharedScreen()
}

/**
 * 页面描述符。
 *
 * 每个页面在导航系统中的唯一配置入口，包含路由、标题、图标、可见性和排序信息。
 *
 * @property provider        [SharedScreen] 唯一标识
 * @property route          稳定路由路径（如 `/home`、`/plugin`）
 * @property titleKey       标题本地化资源 key
 * @property icon           导航栏图标
 * @property showInNavigation 是否在导航栏中显示（false = 隐藏页面，如设置页）
 * @property order          导航栏排序权重（数值越小越靠前）
 */
data class ScreenDescriptor(
    val provider: SharedScreen,
    val route: String,
    val titleKey: StringResource,
    val icon: ImageVector,
    val showInNavigation: Boolean,
    val order: Int,
)

/**
 * 所有页面的描述符列表（按 [order] 排序）。
 * 这是导航栏渲染和路由解析的唯一数据源。
 */
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
        showInNavigation = false,   // 设置页不在主导航栏显示，通过其他入口访问
        order = 100,
    ),
).sortedBy { it.order }

/** 功能页面列表（排除首页），供特殊导航场景使用 */
val featureScreens = screenDescriptors
    .filter { it.showInNavigation && it.provider != SharedScreen.Home }

/**
 * 根据 [SharedScreen] provider 获取对应的 [ScreenDescriptor]。
 *
 * @throws NoSuchElementException 如果 provider 未注册
 */
fun screenDescriptorFor(provider: SharedScreen): ScreenDescriptor {
    return screenDescriptors.first { it.provider == provider }
}

/**
 * 获取 provider 对应的稳定路由路径。
 * 页面实现类应通过此函数返回其 [getUrl]。
 */
fun screenRoute(provider: SharedScreen): String {
    return screenDescriptorFor(provider).route
}

/**
 * 将所有页面注册到 Voyager [ScreenRegistry]。
 * 应用启动时必须调用此函数一次。
 *
 * 新增页面时在此处添加 `register<SharedScreen.NewPage> { NewPageScreen() }`
 */
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

package screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 页面接口。
 *
 * 所有业务页面的公共契约，扩展自 Voyager [cafe.adriel.voyager.core.screen.Screen]。
 *
 * 定义了 URL、图标、标题、动画控制等元数据方法，由导航系统和窗口标题栏消费。
 */
interface IScreenInterface {

    /**
     * 返回该页面的稳定路由 URL。
     * 实现时应委托给 [navigation.screenRoute] 传入对应的 [SharedScreen]。
     */
    fun getUrl(): String

    /**
     * 返回该页面在导航栏和标题栏中使用的图标。
     */
    fun getIcon(): ImageVector

    /**
     * 是否隐藏窗口自定义标题栏中的页面标题文本。
     * 默认 false（显示标题）。
     */
    fun hideTitle(): Boolean {
        return false
    }

    /**
     * 是否禁用页面过渡动画。
     * 默认 false（使用动画）。
     */
    fun diableAnimation(): Boolean {
        return false
    }

    /**
     * 获取页面标题字符串（Composable 上下文）。
     * 实现时应通过 [org.jetbrains.compose.resources.stringResource] 获取本地化文本。
     */
    @Composable
    fun getTitle(): String

}

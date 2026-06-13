/**
 * 崩坏 RTS 桌面启动器 — 应用入口。
 *
 * 本文件是 Compose Desktop 应用的启动点，负责：
 * - 初始化启动协调器（[AppStartupCoordinator]），配置 JVM 环境并注册页面导航
 * - 创建生命周期协调器（[AppLifecycleCoordinator]），管理窗口可见性、托盘行为和游戏通信服务启停
 * - 构建 UI 设置控制器（[AppUiSettingsController]），提供主题 / 语言 / 导航风格的持久化与注入
 * - 组装系统托盘（Tray）、应用窗口（Window）、Fluent 主题和 Voyager 导航器
 *
 * 顶层结构：
 * ```
 * main()
 *  ├── Application
 *  │    ├── AppStartupCoordinator      ← 一次性初始化
 *  │    ├── AppLifecycleCoordinator    ← 窗口/托盘/服务生命周期
 *  │    ├── AppUiSettingsController   ← 响应式 UI 设置
 *  │    ├── CompositionLocalProvider   ← 注入 AppUiSettings
 *  │    ├── Tray                       ← 系统托盘图标与右键菜单
 *  │    └── Window                     ← 主窗口
 *  │         ├── AppFluentTheme        ← Fluent Design 主题切换
 *  │         ├── Navigator             ← Voyager 页面导航
 *  │         ├── AppWindowTitleBar     ← 自定义标题栏
 *  │         └── NavigationBar         ← 左侧/顶部导航栏
 * ```
 *
 * 平台限制：
 * - 当前默认面向 Windows x86_64
 * - 托盘实现依赖 compose-nativetray
 */

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.navigator.Navigator
import honkaigamelauncher.desktop_ui.generated.resources.*
import com.kdroid.composetray.tray.api.Tray
import navigation.SharedScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.components.AppWindowTitleBar
import ui.components.NavigationBar
import ui.fluent.theme.AppFluentTheme
import ui.fluent.theme.FluentTokens
import ui.settings.LocalAppUiSettings
import kotlin.system.exitProcess

/**
 * 主窗口内容 —— 导航栏。
 *
 * 根据 [darkTheme] 参数将当前主题传递给 [NavigationBar]，
 * 使其内部导航项和背景随主题变化。
 *
 * @param darkTheme 是否使用深色主题，来自 [AppUiSettingsController.isDarkTheme]
 */
@Composable
fun MainView(
    darkTheme: Boolean
) {
    NavigationBar(darkTheme = darkTheme)
}

/**
 * 应用入口函数。
 *
 * 创建 Compose Desktop 的 [application] 块，依次完成：
 *
 * **1. 初始化阶段**
 * - [AppStartupCoordinator.initialize]：设置 UTF-8 编码、AWT 字体抗锯齿、注册 Voyager 导航页面。
 *   通过 `remember` 确保在 Compose 重组中只执行一次。
 *
 * **2. 生命周期阶段**
 * - [AppLifecycleCoordinator.start]：启动 Ktor/Netty 游戏通信服务（监听 127.0.0.1 随机端口）。
 *   通过 `remember` 持有同一实例，窗口关闭时隐藏而非退出，托盘右键菜单可真正退出。
 *
 * **3. UI 设置阶段**
 * - [AppUiSettingsController]：从 multiplatform-settings 持久化存储恢复主题 / 语言 / 导航风格，
 *   并通过 `CompositionLocalProvider` 注入到整个组件树。
 * - [LaunchedEffect] 在语言 code 变化时调用 `changeLanguage()` 切换 JVM 默认 Locale。
 *
 * **4. 窗口与托盘**
 * - [Tray]：系统托盘图标，双击切换窗口可见性；右键菜单包含"打开窗口"与"退出应用"。
 *   退出时依次执行 `gameService.stop()` → `dispose()` → `exitProcess(0)`。
 * - [Window]：1280×720 起始尺寸，透明无装饰（自定义标题栏），可缩放（仅浮动模式）。
 *   关闭按钮触发 `hideWindow()` 而非退出。
 *
 * **5. 内容渲染**
 * - [AppFluentTheme]：根据 `isDarkTheme` 切换亮/暗 Fluent Design 主题。
 * - [Navigator]：Voyager 导航容器，首页为 [SharedScreen.Home]。
 * - [AppWindowTitleBar]：自定义拖拽区 + 最小化/最大化/关闭按钮。
 * - [MainView]：主内容区 —— 导航栏。
 *
 * @see AppStartupCoordinator
 * @see AppLifecycleCoordinator
 * @see AppUiSettingsController
 */
@OptIn(ExperimentalVoyagerApi::class)
fun main() = application {

    // === 初始化阶段 ========================================================
    // 启动协调器：设置 JVM 系统属性 + 注册页面导航，remember 确保单次执行
    remember { AppStartupCoordinator().also { it.initialize() } }
    // 生命周期协调器：管理窗口可见性、托盘操作、游戏服务启停
    val lifecycleCoordinator = remember { AppLifecycleCoordinator().also { it.start() } }
    // UI 设置控制器：主题 / 语言 / 导航风格的读写与注入
    val uiSettingsController = remember { AppUiSettingsController() }

    val appIcon = painterResource(Res.drawable.logo)

    // 语言切换响应：当 languageCode 变化时，调用 changeLanguage() 更新 JVM 默认 Locale
    LaunchedEffect(uiSettingsController.languageCode) {
        uiSettingsController.applyCurrentLanguage()
    }

    val state = rememberWindowState(size = DpSize(1280.dp, 720.dp))

    // === UI 组合阶段 ========================================================
    // 通过 CompositionLocalProvider 将 AppUiSettings 注入整个组件树
    CompositionLocalProvider(
        LocalAppUiSettings provides uiSettingsController.asAppUiSettings()
    ) {
        val openWindowText = stringResource(Res.string.openWindow)
        val exitApplicationText = stringResource(Res.string.exit)

        // 系统托盘：图标 + 双击主操作 + 右键菜单
        Tray(
            iconContent = {
                Image(
                    painter = appIcon,
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize()
                )
            },
            tooltip = stringResource(Res.string.appTitle),
            // 双击托盘图标：切换窗口可见性
            primaryAction = { lifecycleCoordinator.toggleWindowVisibility() }
        ) {
            // 右键菜单项 —— "打开窗口"
            Item(label = openWindowText, onClick = {
                lifecycleCoordinator.showWindow()
            })
            Divider()
            // 右键菜单项 —— "退出"
            Item(label = exitApplicationText, onClick = {
                lifecycleCoordinator.exit(
                    onDispose = { dispose() },
                    onExitProcess = { exitProcess(0) },
                )
            })
        }

        // 主窗口：透明无边框，自定义标题栏
        Window(
            // 关闭按钮 → 隐藏窗口（最小化到托盘），而非退出进程
            onCloseRequest = { lifecycleCoordinator.hideWindow() },
            visible = lifecycleCoordinator.isVisible,
            state = state,
            title = stringResource(Res.string.appTitle),
            transparent = true,
            undecorated = true,
            icon = appIcon,
            // 窗口仅在浮动模式下可自由缩放
            resizable = state.placement == WindowPlacement.Floating
        ) {
            // Fluent Design 主题：根据用户设定切换亮/暗
            AppFluentTheme(darkTheme = uiSettingsController.isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (uiSettingsController.isDarkTheme) FluentTokens.ColorToken.windowBackgroundDark
                            else FluentTokens.ColorToken.windowBackgroundLight
                        )
                ) {
                    // Voyager Navigator：管理页面栈和路由导航
                    Navigator(rememberScreen(SharedScreen.Home)) { navigator ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 自定义窗口标题栏（拖拽区 + 窗口控制按钮）
                            AppWindowTitleBar(
                                state = state,
                                onCloseRequest = { lifecycleCoordinator.hideWindow() }
                            )
                            // 主内容区：导航栏
                            Box(modifier = Modifier.fillMaxSize()) {
                                MainView(darkTheme = uiSettingsController.isDarkTheme)
                            }
                        }
                    }
                }
            }
        }
    }
}

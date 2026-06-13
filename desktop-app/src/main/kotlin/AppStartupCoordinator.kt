/**
 * 应用启动协调器 —— 在 Compose Desktop 应用启动前执行一次性初始化任务。
 *
 * 本文件包含：
 * - [AppStartupCoordinator]：统一入口，保证初始化只执行一次
 * - [configureDesktopEnvironment]：设置 JVM 系统属性以优化桌面渲染
 *
 * 初始化顺序：
 * 1. 配置桌面环境（UTF-8 编码、字体抗锯齿等 JVM 属性）
 * 2. 注册所有 Voyager 导航页面
 *
 * 设计要点：
 * - 构造函数接受可注入的 [configureEnvironment] 和 [registerScreens] lambda，
 *   方便单元测试验证初始化行为（见 AppStartupCoordinatorTest）
 * - 通过 `initialized` 标志位保证幂等性：多次调用 `initialize()` 不会重复执行
 */

import navigation.registerNavigation
import java.io.PrintStream

/**
 * 应用启动协调器。
 *
 * 在主 Composable 创建之前完成的初始化工作：
 * - 设置 JVM 桌面渲染环境（UTF-8 输出编码、AWT 字体抗锯齿）
 * - 注册 Voyager 共享页面和路由映射
 *
 * 通过 [remember] 持有实例，在 Compose 重组中保持稳定。
 *
 * @param configureEnvironment JVM 环境配置函数，默认调用 [configureDesktopEnvironment]
 * @param registerScreens 导航页面注册函数，默认调用 [registerNavigation]
 * @see MainKt.main
 */
class AppStartupCoordinator(
    private val configureEnvironment: () -> Unit = ::configureDesktopEnvironment,
    private val registerScreens: () -> Unit = ::registerNavigation,
) {
    /**
     * 是否已完成初始化。
     * 防止在 Compose 重组或多次调用中重复执行初始化逻辑。
     */
    private var initialized = false

    /**
     * 执行一次性初始化。
     *
     * 幂等操作：多次调用仅首次生效。
     * 依次执行：
     * 1. [configureEnvironment] — 设置 UTF-8 编码、AWT 字体属性
     * 2. [registerScreens] — 注册 Voyager 导航页面和路由
     */
    fun initialize() {
        if (initialized) return

        configureEnvironment()
        registerScreens()
        initialized = true
    }
}

/**
 * 配置 JVM 桌面运行环境。
 *
 * 在 Compose Desktop 窗口创建前设置以下系统属性，确保中英文文本渲染和 UI 质量：
 *
 * | 属性 | 值 | 作用 |
 * |------|----|------|
 * | `file.encoding` | `UTF-8` | 统一文件读写编码，避免中文路径乱码 |
 * | `awt.useSystemAAFontSettings` | `on` | 启用系统级 AWT 字体抗锯齿 |
 * | `swing.aatext` | `true` | 启用 Swing 文本抗锯齿 |
 *
 * 同时将 [System.out] 和 [System.err] 重定向为 UTF-8 编码的 [PrintStream]，
 * 保证标准输出中的中文/日文/韩文字符正常显示。
 *
 * **注意**：此函数在 JVM 进程启动阶段调用，依赖 java.desktop 模块的
 * `--add-opens` JVM 参数（在 build.gradle.kts 中配置）。
 */
private fun configureDesktopEnvironment() {
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")
    System.setOut(PrintStream(System.out, true, "UTF-8"))
    System.setErr(PrintStream(System.err, true, "UTF-8"))
}

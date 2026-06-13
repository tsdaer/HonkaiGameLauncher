/**
 * 应用生命周期协调器 —— 管理窗口可见性、系统托盘行为和游戏通信服务启停。
 *
 * 本文件定义了桌面应用运行时三大组件：
 * - [DesktopGameService]：游戏通信服务的桌面层接口
 * - [RuntimeDesktopGameService]：委托到 [core.GameService] 的默认实现
 * - [AppLifecycleCoordinator]：窗口 / 托盘 / 退出的统一协调
 *
 * ## 生命周期流程
 *
 * ```
 * 应用启动
 *    │
 *    ├─► AppLifecycleCoordinator.start()
 *    │      └─► gameService.start()  ──► Ktor 服务监听 127.0.0.1 随机端口
 *    │
 *    ├─► 窗口操作
 *    │      ├─► showWindow()    ──► isVisible = true
 *    │      ├─► hideWindow()    ──► isVisible = false  （关闭按钮 → 最小化到托盘）
 *    │      └─► toggleWindowVisibility() ──► 双击托盘图标切换
 *    │
 *    └─► 退出流程
 *           └─► exit(onDispose, onExitProcess)
 *                  ├─► gameService.stop()     ──► 停止 Ktor 服务 + 清理端口文件
 *                  ├─► onDispose()             ──► Compose dispose
 *                  └─► onExitProcess()         ──► kotlin.system.exitProcess(0)
 * ```
 *
 * 设计要点：
 * - [DesktopGameService] 抽象允许测试时注入 Fake 实现，不依赖真实网络
 * - 窗口关闭默认为隐藏（最小化到托盘），真正退出需通过托盘右键菜单
 * - 退出流程通过 `exited` 标志位保证幂等，防止重复调用 `stop/dispose/exit`
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 游戏通信服务的桌面层接口。
 *
 * 将 [core.GameService] 的启动 / 停止操作抽象为平台无关的接口，
 * 便于在测试中用 Fake 实现替换真实网络服务。
 *
 * @see RuntimeDesktopGameService
 * @see core.GameService
 */
interface DesktopGameService {
    /** 启动游戏通信服务（Ktor/Netty HTTP 服务端，监听 127.0.0.1 随机端口） */
    fun start()

    /** 停止游戏通信服务，清理端口文件和其他资源 */
    fun stop()
}

/**
 * [DesktopGameService] 的运行时实现。
 *
 * 将 [core.GameService] 适配为 [DesktopGameService] 接口，
 * 通过委托方式直接调用 core 层的服务生命周期方法。
 *
 * @param gameService core 层游戏通信服务实例，默认取自 [core.RuntimeServices.gameService]
 */
class RuntimeDesktopGameService(
    private val gameService: core.GameService = core.RuntimeServices.gameService,
) : DesktopGameService {
    override fun start() {
        gameService.start()
    }

    override fun stop() {
        gameService.stop()
    }
}

/**
 * 应用生命周期协调器。
 *
 * 统一管理：
 * - **游戏服务启停**：在应用启动时开启 Ktor/Netty 服务，退出时安全关闭
 * - **窗口可见性**：通过 Compose `mutableStateOf` 响应式控制窗口显示 / 隐藏
 * - **退出流程**：保证 `stop → dispose → exit` 的执行顺序和幂等性
 *
 * 使用方式：
 * ```kotlin
 * val coordinator = remember { AppLifecycleCoordinator().also { it.start() } }
 *
 * // 窗口
 * Window(
 *     onCloseRequest = { coordinator.hideWindow() },
 *     visible = coordinator.isVisible,
 *     // ...
 * )
 *
 * // 托盘退出
 * coordinator.exit(
 *     onDispose = { dispose() },
 *     onExitProcess = { exitProcess(0) },
 * )
 * ```
 *
 * @param gameService 游戏通信服务，默认使用 [RuntimeDesktopGameService]
 */
class AppLifecycleCoordinator(
    private val gameService: DesktopGameService = RuntimeDesktopGameService(),
) {
    /**
     * 窗口是否可见。
     *
     * 使用 Compose `mutableStateOf` 实现响应式状态：
     * - `true`：窗口正常显示
     * - `false`：窗口隐藏（最小化到托盘）
     *
     * 修改此值会触发 Compose 重组。
     */
    var isVisible by mutableStateOf(true)
        private set

    /** 游戏服务是否已启动，防止重复调用 [start] */
    private var serviceStarted = false

    /** 是否已执行退出流程，防止重复调用 [exit] */
    private var exited = false

    /**
     * 启动游戏通信服务。
     *
     * 幂等操作：仅在首次调用时启动 Ktor/Netty 服务。
     * 服务监听 127.0.0.1 的随机空闲端口，端口信息写入
     * 系统临时目录下的 `honkai_rts_launcher_port.json`。
     */
    fun start() {
        if (serviceStarted) return

        gameService.start()
        serviceStarted = true
    }

    /**
     * 显示主窗口（从托盘恢复）。
     *
     * 将 [isVisible] 设为 `true`，触发 Compose 重组使窗口重新渲染。
     */
    fun showWindow() {
        isVisible = true
    }

    /**
     * 切换窗口可见性。
     *
     * 主要用于托盘图标双击操作：
     * - 窗口隐藏 → 显示
     * - 窗口显示 → 隐藏
     */
    fun toggleWindowVisibility() {
        isVisible = !isVisible
    }

    /**
     * 隐藏主窗口（最小化到托盘）。
     *
     * 将 [isVisible] 设为 `false`，窗口内容暂停渲染但进程继续运行。
     * 对应窗口关闭按钮的行为（X 按钮不退出，而是隐藏到托盘）。
     */
    fun hideWindow() {
        isVisible = false
    }

    /**
     * 安全退出应用。
     *
     * 按以下顺序执行退出流程（仅首次调用生效）：
     * 1. 停止游戏通信服务 ([DesktopGameService.stop])，释放端口并清理端口文件
     * 2. 释放 Compose 资源 ([onDispose])
     * 3. 终止 JVM 进程 ([onExitProcess])
     *
     * 通过 `exited` 标志位保证幂等：多次调用不会重复执行。
     *
     * @param onDispose Compose 资源释放回调，通常传入 `{ dispose() }`
     * @param onExitProcess JVM 进程终止回调，通常传入 `{ exitProcess(0) }`
     */
    fun exit(onDispose: () -> Unit, onExitProcess: () -> Unit) {
        if (exited) return

        exited = true
        gameService.stop()
        onDispose()
        onExitProcess()
    }
}

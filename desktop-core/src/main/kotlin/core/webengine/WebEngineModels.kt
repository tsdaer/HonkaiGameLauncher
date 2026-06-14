package core.webengine

/**
 * WebEngine 初始化阶段枚举。
 *
 * 反映嵌入式 Chromium 引擎从检测到就绪的完整初始化流程。
 */
enum class WebEnginePhase {
    /** 检测已安装的引擎 */
    Checking,
    /** 下载引擎包 */
    Downloading,
    /** 下载完成，即将解压 */
    DownloadFinishing,
    /** 解压引擎包 */
    Extracting,
    /** 安装引擎 */
    Installing,
    /** 初始化 CEF 运行时 */
    Initializing,
    /** 引擎就绪，可加载网页 */
    Ready
}

/**
 * WebEngine 初始化状态快照。
 *
 * 由 [WebEngineController] 通过 StateFlow 暴露，供 UI 层订阅。
 *
 * @property ready           引擎是否已就绪（可加载网页）
 * @property phase           当前初始化阶段
 * @property progress        初始化进度（0.0-1.0），仅在 Downloading/Ready 阶段有值
 * @property error           初始化失败时的错误信息
 * @property restartRequired 是否需要重启应用以完成引擎安装
 */
data class WebEngineState(
    val ready: Boolean = false,
    val phase: WebEnginePhase = WebEnginePhase.Initializing,
    val progress: Float? = null,
    val error: String? = null,
    val restartRequired: Boolean = false,
)

/** WebEngine 初始化进度回调接口（函数式） */
fun interface WebEngineProgressSink {
    fun update(phase: WebEnginePhase, progress: Float?)
}

/**
 * WebEngine 运行时抽象。
 *
 * 定义引擎初始化的平台相关操作，便于测试时 mock 替换。
 * 具体实现（如基于 KCEF）由 desktop-ui 提供。
 */
interface WebEngineRuntime {
    suspend fun initialize(progressSink: WebEngineProgressSink): WebEngineInitializationResult
}

/** WebEngine 初始化结果（密封接口） */
sealed interface WebEngineInitializationResult {
    /** 初始化成功，引擎就绪 */
    data object Ready : WebEngineInitializationResult
    /** 需要重启应用以完成安装 */
    data object RestartRequired : WebEngineInitializationResult
    /** 初始化失败 */
    data class Failed(val message: String) : WebEngineInitializationResult
}

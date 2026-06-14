package screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.webengine.WebEngineState
import kotlinx.coroutines.launch
import ui.webengine.WebEngineService

/**
 * 内置网页工具 ScreenModel。
 *
 * 管理 WebView 地址栏的状态、URL 归一化和 WebEngine 初始化状态委托。
 * WebEngine 的初始化逻辑由 [WebEngineService] 负责，此处订阅其状态流并
 * 转为 Compose 可观察状态供 UI 渲染。
 */
class WebScreenModel : ScreenModel {

    /** WebEngine 初始化状态（订阅 [WebEngineService] 的状态流） */
    var webEngineState by mutableStateOf(WebEngineService.state.value)
        private set

    /** WebEngine 是否就绪 */
    val webEngineReady get() = webEngineState.ready
    /** WebEngine 当前初始化阶段 */
    val webEnginePhase get() = webEngineState.phase
    /** WebEngine 初始化进度 */
    val webEngineProgress get() = webEngineState.progress
    /** WebEngine 初始化错误信息 */
    val webEngineError get() = webEngineState.error
    /** WebEngine 是否需要重启应用 */
    val webEngineRestartRequired get() = webEngineState.restartRequired

    /** 地址栏当前 URL */
    var address by mutableStateOf(HOME_URL)
        private set

    init {
        screenModelScope.launch {
            WebEngineService.state.collect { webEngineState = it }
        }
        WebEngineService.ensureInitialized()
    }

    /** 重试 WebEngine 初始化 */
    fun retryWebEngineInit() {
        WebEngineService.retry()
    }

    /** 更新地址栏文本 */
    fun updateAddress(value: String) {
        address = value
    }

    /** 从 WebView 加载完成回调中更新地址栏 */
    fun updateAddressFromLoadedUrl(value: String?) {
        if (!value.isNullOrBlank()) {
            address = value
        }
    }

    /**
     * 准备加载 URL。
     * 对原始输入进行归一化处理后更新地址栏。
     *
     * @return 归一化后的目标 URL
     */
    fun prepareLoadUrl(rawUrl: String): String {
        val targetUrl = normalizeUrl(rawUrl)
        address = targetUrl
        return targetUrl
    }

    companion object {
        /** 首页 URL */
        const val HOME_URL = "https://www.honkai-rts.com"

        /**
         * URL 归一化：
         * - 空白 → 返回 HOME_URL
         * - 已有 http:// 或 https:// 前缀 → 原样返回
         * - 其他 → 添加 https:// 前缀
         */
        fun normalizeUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank()) {
                return HOME_URL
            }
            return if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }
        }
    }
}

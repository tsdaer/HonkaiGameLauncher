/**
 * UI 设置控制器 —— 管理主题、语言和导航风格的持久化与响应式状态。
 *
 * 本文件包含：
 * - [DesktopUiSettingsStorage]：UI 设置的持久化存储接口
 * - [RuntimeDesktopUiSettingsStorage]：基于 multiplatform-settings 的默认实现
 * - [AppUiSettingsController]：响应式状态 + 回调，通过 CompositionLocal 注入 UI 树
 *
 * ## 数据流
 *
 * ```
 * 用户操作（设置页）
 *    │
 *    ├─► AppUiSettings.callbacks
 *    │      ├─► onThemeChanged()          ──► toggle isDarkTheme → 写入 storage
 *    │      ├─► onLanguageChanged(code)    ──► 更新 languageCode  → 写入 storage + 触发 LaunchedEffect
 *    │      └─► onNavigationStyleChanged   ──► 更新导航样式       → 写入 storage
 *    │
 *    ▼
 * Compose mutableStateOf  ──► 触发重组 ──► UI 即时响应
 * ```
 *
 * 设计要点：
 * - [DesktopUiSettingsStorage] 抽象允许测试时注入 Fake 存储，不依赖真实文件系统
 * - 语言和导航样式变更采用"有变化才写入"策略，避免无意义的 I/O
 * - 通过 [asAppUiSettings] 生成 [AppUiSettings] 数据类，经 CompositionLocal 注入
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import localization.changeLanguage
import ui.settings.AppNavigationStyle
import ui.settings.AppUiSettings

/**
 * UI 设置的持久化存储接口。
 *
 * 抽象 `multiplatform-settings` 的 `Settings` 类，
 * 支持 boolean 和 string 两种类型的读写，满足当前 UI 设置需求。
 * 测试时可注入 Fake 实现（见 AppUiSettingsControllerTest）。
 */
interface DesktopUiSettingsStorage {
    /**
     * 读取 boolean 类型的设置值。
     * @param key 设置键名
     * @param defaultValue 未设置时的默认值
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    /**
     * 写入 boolean 类型的设置值。
     * @param key 设置键名
     * @param value 要持久化的值
     */
    fun putBoolean(key: String, value: Boolean)

    /**
     * 读取 string 类型的设置值。
     * @param key 设置键名
     * @param defaultValue 未设置时的默认值
     */
    fun getString(key: String, defaultValue: String): String

    /**
     * 写入 string 类型的设置值。
     * @param key 设置键名
     * @param value 要持久化的值
     */
    fun putString(key: String, value: String)
}

/**
 * [DesktopUiSettingsStorage] 的运行时实现。
 *
 * 基于 multiplatform-settings 的 `Settings()` 无参构造函数，
 * 自动选择当前平台的默认存储后端（Windows 上为 Preferences API / Registry）。
 *
 * @param settings multiplatform-settings 的 Settings 实例
 */
class RuntimeDesktopUiSettingsStorage(
    private val settings: Settings = Settings(),
) : DesktopUiSettingsStorage {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return settings.getBoolean(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        settings.putBoolean(key, value)
    }

    override fun getString(key: String, defaultValue: String): String {
        return settings.getString(key, defaultValue)
    }

    override fun putString(key: String, value: String) {
        settings.putString(key, value)
    }
}

/**
 * UI 设置控制器。
 *
 * 作为桌面应用 UI 设置的唯一事实来源，职责包括：
 * - **状态管理**：通过 Compose `mutableStateOf` 持有主题 (dark/light)、
 *   语言 (zh/en) 和导航风格 (LeftCompact/Left/Top) 的当前值
 * - **持久化**：状态变更时立即写入 [DesktopUiSettingsStorage]
 * - **注入**：通过 [asAppUiSettings] 生成 [AppUiSettings] 数据类，
 *   经 `CompositionLocalProvider` 注入整个 Compose 树
 *
 * 生命周期：
 * ```
 * 构造 → 从 storage 恢复状态
 *   └─► 用户操作 → callback → 更新 State + 写入 storage
 *         ├─► 主题变更 → Compose 重组（AppFluentTheme 切换亮/暗）
 *         ├─► 语言变更 → LaunchedEffect → changeLanguage() → Compose 重组
 *         └─► 导航变更 → Compose 重组（NavigationBar 切换布局）
 * ```
 *
 * @param storage 持久化存储，默认使用 [RuntimeDesktopUiSettingsStorage]
 * @param languageApplier 语言切换函数，默认调用 [changeLanguage]
 */
class AppUiSettingsController(
    private val storage: DesktopUiSettingsStorage = RuntimeDesktopUiSettingsStorage(),
    private val languageApplier: (String) -> Unit = ::changeLanguage,
) {
    /**
     * 是否启用深色主题。
     *
     * 从 storage 恢复初始值（默认 `false` 即浅色模式）。
     * 变更时触发 Compose 重组，[AppFluentTheme] 据此切换亮/暗色板。
     */
    var isDarkTheme by mutableStateOf(storage.getBoolean(KEY_DARK_THEME, false))
        private set

    /**
     * 当前语言代码。
     *
     * 从 storage 恢复初始值（默认 `"zh"` 即中文）。
     * 变更时：
     * 1. 立即更新 Compose 状态
     * 2. 等待 [LaunchedEffect] 调用 [applyCurrentLanguage] 切换 JVM Locale
     */
    var languageCode by mutableStateOf(storage.getString(KEY_LANGUAGE_CODE, DEFAULT_LANGUAGE_CODE))
        private set

    /**
     * 当前导航栏样式。
     *
     * 从 storage 恢复初始值（默认 [AppNavigationStyle.LeftCompact]）。
     * 支持的样式见 [AppNavigationStyle] 枚举。
     */
    var navigationStyle by mutableStateOf(
        AppNavigationStyle.fromKey(
            storage.getString(KEY_NAVIGATION_STYLE, AppNavigationStyle.LeftCompact.key)
        )
    )
        private set

    /**
     * 应用当前语言设置到 JVM 运行时。
     *
     * 调用 [languageApplier] 函数（默认为 [changeLanguage]），
     * 切换 JVM 默认 Locale 并触发 Compose Multiplatform Resources 重新加载本地化文案。
     *
     * 通常在 [LaunchedEffect] 中调用，确保在 Compose 上下文中执行。
     */
    fun applyCurrentLanguage() {
        languageApplier(languageCode)
    }

    /**
     * 生成用于 CompositionLocal 注入的 [AppUiSettings] 快照。
     *
     * 将控制器持有的响应式状态和变更回调封装为不可变数据类，
     * 通过 `LocalAppUiSettings` 注入整个 Compose 组件树。
     *
     * @return 包含当前主题、语言、导航样式及其变更回调的 [AppUiSettings] 快照
     */
    fun asAppUiSettings(): AppUiSettings {
        return AppUiSettings(
            isDarkTheme = isDarkTheme,
            languageCode = languageCode,
            navigationStyle = navigationStyle,
            onThemeChanged = ::toggleTheme,
            onLanguageChanged = ::changeLanguageTo,
            onNavigationStyleChanged = ::changeNavigationStyle,
        )
    }

    /**
     * 切换深色/浅色主题。
     *
     * 取反 [isDarkTheme] 并立即写入 storage。
     */
    private fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        storage.putBoolean(KEY_DARK_THEME, isDarkTheme)
    }

    /**
     * 切换界面语言。
     *
     * 仅当新语言代码与当前不同时才更新状态并写入 storage，
     * 避免无意义的持久化操作和重组触发。
     *
     * @param language 目标语言代码（如 "zh"、"en"）
     */
    private fun changeLanguageTo(language: String) {
        if (languageCode == language) return

        languageCode = language
        storage.putString(KEY_LANGUAGE_CODE, language)
    }

    /**
     * 切换导航栏样式。
     *
     * 仅当新样式与当前不同时才更新状态并写入 storage。
     *
     * @param style 目标导航样式
     */
    private fun changeNavigationStyle(style: AppNavigationStyle) {
        if (navigationStyle == style) return

        navigationStyle = style
        storage.putString(KEY_NAVIGATION_STYLE, style.key)
    }

    private companion object {
        /** 深色主题设置的 storage key */
        const val KEY_DARK_THEME = "isDarkTheme"

        /** 语言代码设置的 storage key */
        const val KEY_LANGUAGE_CODE = "languageCode"

        /** 导航样式设置的 storage key */
        const val KEY_NAVIGATION_STYLE = "navigationStyle"

        /** 默认语言代码（中文） */
        const val DEFAULT_LANGUAGE_CODE = "zh"
    }
}

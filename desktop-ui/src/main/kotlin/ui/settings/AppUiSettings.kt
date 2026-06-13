package ui.settings

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 应用 UI 设置数据类。
 *
 * 由 Compose CompositionLocal 提供，在 [LocalAppUiSettings] 中注入。
 * 包含主题、语言、导航样式等 UI 偏好和变更回调。
 *
 * @property isDarkTheme             是否为深色主题
 * @property languageCode            当前语言代码（"zh" / "en"）
 * @property navigationStyle         导航栏布局样式
 * @property onThemeChanged          主题切换回调
 * @property onLanguageChanged       语言切换回调
 * @property onNavigationStyleChanged 导航样式切换回调
 */
data class AppUiSettings(
    val isDarkTheme: Boolean,
    val languageCode: String,
    val navigationStyle: AppNavigationStyle,
    val onThemeChanged: () -> Unit,
    val onLanguageChanged: (String) -> Unit,
    val onNavigationStyleChanged: (AppNavigationStyle) -> Unit
)

/**
 * 导航栏布局样式枚举。
 */
enum class AppNavigationStyle(val key: String) {
    /** 顶部导航栏 */
    Top("Top"),
    /** 左侧展开导航栏 */
    Left("Left"),
    /** 左侧紧凑导航栏 */
    LeftCompact("LeftCompact"),
    /** 左侧可折叠导航栏 */
    LeftCollapsed("LeftCollapsed");

    companion object {
        /** 从 key 字符串恢复枚举值，未知值默认返回 [LeftCompact] */
        fun fromKey(key: String): AppNavigationStyle {
            return entries.firstOrNull { it.key == key } ?: LeftCompact
        }
    }
}

/**
 * 应用 UI 设置的 CompositionLocal。
 * 使用 staticCompositionLocalOf 因为该设置在应用生命周期内不太可能频繁变更作用域。
 */
val LocalAppUiSettings = staticCompositionLocalOf<AppUiSettings> {
    error("LocalAppUiSettings is not provided")
}

/** 导航栏展开/折叠状态的 CompositionLocal */
val LocalNavExpanded = compositionLocalOf { false }

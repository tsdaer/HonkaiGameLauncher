package ui.settings

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

data class AppUiSettings(
    val isDarkTheme: Boolean,
    val languageCode: String,
    val navigationStyle: AppNavigationStyle,
    val onThemeChanged: () -> Unit,
    val onLanguageChanged: (String) -> Unit,
    val onNavigationStyleChanged: (AppNavigationStyle) -> Unit
)

enum class AppNavigationStyle(val key: String) {
    Top("Top"),
    Left("Left"),
    LeftCompact("LeftCompact"),
    LeftCollapsed("LeftCollapsed");

    companion object {
        fun fromKey(key: String): AppNavigationStyle {
            return entries.firstOrNull { it.key == key } ?: LeftCompact
        }
    }
}

val LocalAppUiSettings = staticCompositionLocalOf<AppUiSettings> {
    error("LocalAppUiSettings is not provided")
}

val LocalNavExpanded = compositionLocalOf { false }

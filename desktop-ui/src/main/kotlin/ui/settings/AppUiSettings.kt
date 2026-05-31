package ui.settings

import androidx.compose.runtime.staticCompositionLocalOf

data class AppUiSettings(
    val isDarkTheme: Boolean,
    val languageCode: String,
    val onThemeChanged: () -> Unit,
    val onLanguageChanged: (String) -> Unit
)

val LocalAppUiSettings = staticCompositionLocalOf<AppUiSettings> {
    error("LocalAppUiSettings is not provided")
}

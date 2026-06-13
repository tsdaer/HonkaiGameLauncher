import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import localization.changeLanguage
import ui.settings.AppNavigationStyle
import ui.settings.AppUiSettings

interface DesktopUiSettingsStorage {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
}

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

class AppUiSettingsController(
    private val storage: DesktopUiSettingsStorage = RuntimeDesktopUiSettingsStorage(),
    private val languageApplier: (String) -> Unit = ::changeLanguage,
) {
    var isDarkTheme by mutableStateOf(storage.getBoolean(KEY_DARK_THEME, false))
        private set

    var languageCode by mutableStateOf(storage.getString(KEY_LANGUAGE_CODE, DEFAULT_LANGUAGE_CODE))
        private set

    var navigationStyle by mutableStateOf(
        AppNavigationStyle.fromKey(
            storage.getString(KEY_NAVIGATION_STYLE, AppNavigationStyle.LeftCompact.key)
        )
    )
        private set

    fun applyCurrentLanguage() {
        languageApplier(languageCode)
    }

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

    private fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        storage.putBoolean(KEY_DARK_THEME, isDarkTheme)
    }

    private fun changeLanguageTo(language: String) {
        if (languageCode == language) return

        languageCode = language
        storage.putString(KEY_LANGUAGE_CODE, language)
    }

    private fun changeNavigationStyle(style: AppNavigationStyle) {
        if (navigationStyle == style) return

        navigationStyle = style
        storage.putString(KEY_NAVIGATION_STYLE, style.key)
    }

    private companion object {
        const val KEY_DARK_THEME = "isDarkTheme"
        const val KEY_LANGUAGE_CODE = "languageCode"
        const val KEY_NAVIGATION_STYLE = "navigationStyle"
        const val DEFAULT_LANGUAGE_CODE = "zh"
    }
}

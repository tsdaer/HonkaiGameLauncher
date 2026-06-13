import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ui.settings.AppNavigationStyle

class AppUiSettingsControllerTest {

    @Test
    fun `initial state is restored from storage`() {
        val storage = FakeDesktopUiSettingsStorage(
            strings = mutableMapOf(
                "languageCode" to "en",
                "navigationStyle" to AppNavigationStyle.Top.key,
            ),
            booleans = mutableMapOf("isDarkTheme" to true),
        )
        val controller = AppUiSettingsController(storage)

        assertTrue(controller.isDarkTheme)
        assertEquals("en", controller.languageCode)
        assertEquals(AppNavigationStyle.Top, controller.navigationStyle)
    }

    @Test
    fun `app ui settings callbacks persist changed values`() {
        val storage = FakeDesktopUiSettingsStorage()
        val controller = AppUiSettingsController(storage)
        val appUiSettings = controller.asAppUiSettings()

        appUiSettings.onThemeChanged()
        appUiSettings.onLanguageChanged("en")
        appUiSettings.onNavigationStyleChanged(AppNavigationStyle.Left)

        assertTrue(controller.isDarkTheme)
        assertEquals("en", controller.languageCode)
        assertEquals(AppNavigationStyle.Left, controller.navigationStyle)
        assertEquals(true, storage.booleans["isDarkTheme"])
        assertEquals("en", storage.strings["languageCode"])
        assertEquals(AppNavigationStyle.Left.key, storage.strings["navigationStyle"])
    }

    @Test
    fun `unchanged language and navigation style are not persisted again`() {
        val storage = FakeDesktopUiSettingsStorage(
            strings = mutableMapOf(
                "languageCode" to "zh",
                "navigationStyle" to AppNavigationStyle.LeftCompact.key,
            ),
        )
        val controller = AppUiSettingsController(storage)
        val appUiSettings = controller.asAppUiSettings()

        appUiSettings.onLanguageChanged("zh")
        appUiSettings.onNavigationStyleChanged(AppNavigationStyle.LeftCompact)

        assertFalse(storage.putStringCalls.contains("languageCode"))
        assertFalse(storage.putStringCalls.contains("navigationStyle"))
    }

    @Test
    fun `current language is applied on demand`() {
        val appliedLanguages = mutableListOf<String>()
        val controller = AppUiSettingsController(
            storage = FakeDesktopUiSettingsStorage(strings = mutableMapOf("languageCode" to "en")),
            languageApplier = { appliedLanguages.add(it) },
        )

        controller.applyCurrentLanguage()

        assertEquals(listOf("en"), appliedLanguages)
    }

    private class FakeDesktopUiSettingsStorage(
        val strings: MutableMap<String, String> = mutableMapOf(),
        val booleans: MutableMap<String, Boolean> = mutableMapOf(),
    ) : DesktopUiSettingsStorage {
        val putStringCalls = mutableListOf<String>()

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            return booleans[key] ?: defaultValue
        }

        override fun putBoolean(key: String, value: Boolean) {
            booleans[key] = value
        }

        override fun getString(key: String, defaultValue: String): String {
            return strings[key] ?: defaultValue
        }

        override fun putString(key: String, value: String) {
            putStringCalls.add(key)
            strings[key] = value
        }
    }
}

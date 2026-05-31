package screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.languageEn
import honkaigamelauncher.desktop_ui.generated.resources.languageSetting
import honkaigamelauncher.desktop_ui.generated.resources.languageZh
import honkaigamelauncher.desktop_ui.generated.resources.setGamePath
import honkaigamelauncher.desktop_ui.generated.resources.setting
import honkaigamelauncher.desktop_ui.generated.resources.themeDark
import honkaigamelauncher.desktop_ui.generated.resources.themeLight
import honkaigamelauncher.desktop_ui.generated.resources.themeSetting
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Folder
import compose.icons.evaicons.fill.Globe2
import compose.icons.evaicons.fill.Moon
import compose.icons.feathericons.Settings
import org.jetbrains.compose.resources.stringResource
import io.github.composefluent.component.Text as FluentText
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentDropdown
import ui.fluent.components.FluentSection
import ui.settings.LocalAppUiSettings
import viewModel.SettingScreenModel

class SettingScreen: Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return "setting"
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.setting)
    }

    override fun getIcon(): ImageVector {
        return compose.icons.FeatherIcons.Settings
    }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel{ SettingScreenModel() }
        val appUi = LocalAppUiSettings.current
        val state = rememberScrollState(0)
        val themeItems = remember {
            listOf(
                "LIGHT_KEY",
                "DARK_KEY"
            )
        }
        val themeSelectedIndex = if (appUi.isDarkTheme) 1 else 0
        val languageItems = remember {
            listOf(
                "zh",
                "en"
            )
        }
        val languageSelectedIndex = if (appUi.languageCode == "en") 1 else 0
        Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
            FluentSection(
                title = stringResource(Res.string.setGamePath),
                icon = EvaIcons.Fill.Folder
            ) {
                FluentButton(onClick = { screenModel.setGamePath() }) {
                    FluentText(screenModel.gamePath)
                }
            }

            FluentSection(
                title = stringResource(Res.string.themeSetting),
                icon = EvaIcons.Fill.Moon
            ) {
                FluentDropdown(
                    items = listOf(
                        stringResource(Res.string.themeLight),
                        stringResource(Res.string.themeDark)
                    ),
                    selectedIndex = themeSelectedIndex,
                    onSelectionChange = { index, _ ->
                        val targetDarkTheme = themeItems.getOrNull(index) == "DARK_KEY"
                        if (targetDarkTheme != appUi.isDarkTheme) {
                            appUi.onThemeChanged()
                        }
                    }
                )
            }

            FluentSection(
                title = stringResource(Res.string.languageSetting),
                icon = EvaIcons.Fill.Globe2
            ) {
                FluentDropdown(
                    items = listOf(
                        stringResource(Res.string.languageZh),
                        stringResource(Res.string.languageEn)
                    ),
                    selectedIndex = languageSelectedIndex,
                    onSelectionChange = { index, _ ->
                        val targetLanguage = languageItems.getOrNull(index) ?: "zh"
                        if (targetLanguage != appUi.languageCode) {
                            appUi.onLanguageChanged(targetLanguage)
                        }
                    }
                )
            }
        }
    }
}

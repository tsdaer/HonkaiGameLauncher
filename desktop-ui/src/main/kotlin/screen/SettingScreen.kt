package screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Folder
import compose.icons.evaicons.fill.Globe2
import compose.icons.evaicons.fill.Moon
import compose.icons.feathericons.Settings
import honkaigamelauncher.desktop_ui.generated.resources.*
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Switcher
import org.jetbrains.compose.resources.stringResource
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.components.FluentDropdown
import ui.settings.LocalAppUiSettings
import viewModel.SettingScreenModel
import io.github.composefluent.component.Text as FluentText

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
        val screenModel = rememberScreenModel { SettingScreenModel() }
        val appUi = LocalAppUiSettings.current
        val state = rememberScrollState(0)
        val languageItems = remember {
            listOf(
                "zh",
                "en"
            )
        }
        val languageSelectedIndex = if (appUi.languageCode == "en") 1 else 0
        val gamePathValue = if (screenModel.gamePath == "null" || screenModel.gamePath.isBlank()) {
            stringResource(Res.string.settingsNotSet)
        } else {
            screenModel.gamePath
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state)
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .widthIn(max = 1000.dp)
        ) {
            SettingsGroupHeader(stringResource(Res.string.settingsGroupAppearance))
            SettingItem(
                title = stringResource(Res.string.themeSetting),
                description = stringResource(Res.string.themeSettingDesc),
                icon = EvaIcons.Fill.Moon,
                trailing = {
                    Switcher(
                        checked = appUi.isDarkTheme,
                        text = if (appUi.isDarkTheme) {
                            stringResource(Res.string.themeDark)
                        } else {
                            stringResource(Res.string.themeLight)
                        },
                        textBefore = true,
                        onCheckStateChange = { checked ->
                            if (checked != appUi.isDarkTheme) {
                                appUi.onThemeChanged()
                            }
                        }
                    )
                }
            )
            SettingItem(
                title = stringResource(Res.string.languageSetting),
                description = stringResource(Res.string.languageSettingDesc),
                icon = EvaIcons.Fill.Globe2,
                trailing = {
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
                        },
                        // modifier = Modifier.width(160.dp)
                    )
                }
            )

            SettingsGroupHeader(stringResource(Res.string.settingsGroupLauncher))
            SettingItem(
                title = stringResource(Res.string.setGamePath),
                description = stringResource(Res.string.settingsDescGamePath),
                icon = EvaIcons.Fill.Folder,
                trailing = {
                    FluentButton(onClick = { screenModel.setGamePath() }) {
                        FluentText(stringResource(Res.string.settingsActionBrowse))
                    }
                }
            )
            FluentText(
                text = gamePathValue,
                style = FluentTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    FluentText(
        text = title,
        style = FluentTheme.typography.bodyStrong,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingItem(
    title: String,
    description: String,
    icon: ImageVector,
    trailing: @Composable () -> Unit
) {
    FluentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    FluentText(
                        text = title,
                        style = FluentTheme.typography.bodyStrong
                    )
                    FluentText(
                        text = description,
                        style = FluentTheme.typography.caption
                    )
                }
            }
            trailing()
        }
    }
}

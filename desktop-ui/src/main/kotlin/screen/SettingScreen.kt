package screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.ArrowIosDownward
import compose.icons.evaicons.fill.Folder
import compose.icons.evaicons.fill.Globe2
import compose.icons.evaicons.fill.Moon
import compose.icons.evaicons.fill.Navigation2
import compose.icons.feathericons.Settings
import honkaigamelauncher.desktop_ui.generated.resources.*
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Switcher
import navigation.SharedScreen
import navigation.screenRoute
import org.jetbrains.compose.resources.stringResource
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.components.FluentDropdown
import ui.fluent.theme.FluentTokens
import ui.settings.AppNavigationStyle
import ui.settings.LocalAppUiSettings
import viewModel.SettingScreenModel
import io.github.composefluent.component.Text as FluentText

class SettingScreen: Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return screenRoute(SharedScreen.Setting)
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
        val navigationStyles = remember {
            listOf(
                AppNavigationStyle.Top,
                AppNavigationStyle.Left,
                AppNavigationStyle.LeftCompact,
                AppNavigationStyle.LeftCollapsed
            )
        }
        val languageSelectedIndex = if (appUi.languageCode == "en") 1 else 0
        val navigationStyleSelectedIndex = navigationStyles.indexOf(appUi.navigationStyle).takeIf { it >= 0 } ?: 2
        val gamePathValue = if (screenModel.gamePath == "null" || screenModel.gamePath.isBlank()) {
            stringResource(Res.string.settingsNotSet)
        } else {
            screenModel.gamePath
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state)
                .padding(horizontal = 28.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SettingsHero()

            SettingsGroupCard(title = stringResource(Res.string.settingsGroupAppearance)) {
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
                            modifier = Modifier.width(180.dp)
                        )
                    }
                )
                SettingItem(
                    title = stringResource(Res.string.navigationStyleSetting),
                    description = stringResource(Res.string.navigationStyleSettingDesc),
                    icon = EvaIcons.Fill.Navigation2,
                    trailing = {
                        FluentDropdown(
                            items = navigationStyles.map { it.label() },
                            selectedIndex = navigationStyleSelectedIndex,
                            onSelectionChange = { index, _ ->
                                navigationStyles.getOrNull(index)?.let(appUi.onNavigationStyleChanged)
                            },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                )
            }

            SettingsGroupCard(title = stringResource(Res.string.settingsGroupLauncher)) {
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
                SettingPathPreview(gamePathValue)
            }
        }
    }
}

@Composable
private fun SettingsHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 980.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(FluentTokens.ColorToken.accent.copy(alpha = 0.08f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FluentText(
            text = stringResource(Res.string.settingsHeroTitle),
            style = FluentTheme.typography.title
        )
        FluentText(
            text = stringResource(Res.string.settingsHeroDesc),
            style = FluentTheme.typography.body
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 980.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FluentText(
            text = title,
            style = FluentTheme.typography.subtitle
        )
        FluentCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String,
    icon: ImageVector,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FluentTokens.ColorToken.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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
        Box(
            modifier = Modifier.padding(start = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            trailing()
        }
    }
}

@Composable
private fun SettingPathPreview(path: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = EvaIcons.Fill.ArrowIosDownward,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
        )
        FluentText(
            text = path,
            style = FluentTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AppNavigationStyle.label(): String {
    return when (this) {
        AppNavigationStyle.Top -> stringResource(Res.string.navigationStyleTop)
        AppNavigationStyle.Left -> stringResource(Res.string.navigationStyleLeft)
        AppNavigationStyle.LeftCompact -> stringResource(Res.string.navigationStyleLeftCompact)
        AppNavigationStyle.LeftCollapsed -> stringResource(Res.string.navigationStyleLeftCollapsed)
    }
}

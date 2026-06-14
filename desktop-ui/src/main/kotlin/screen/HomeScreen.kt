package screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.ScreenRegistry
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import compose.icons.EvaIcons
import compose.icons.FeatherIcons
import compose.icons.LineAwesomeIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Home
import compose.icons.feathericons.Activity
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.Globe
import compose.icons.feathericons.Settings
import compose.icons.lineawesomeicons.PuzzlePieceSolid
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickDocsDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickLogDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickPluginDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickSettingsDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickWebsiteDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeSectionQuickAccess
import honkaigamelauncher.desktop_ui.generated.resources.homeSectionStatus
import honkaigamelauncher.desktop_ui.generated.resources.screen_doc
import honkaigamelauncher.desktop_ui.generated.resources.screen_home
import honkaigamelauncher.desktop_ui.generated.resources.screen_log
import honkaigamelauncher.desktop_ui.generated.resources.screen_plugin
import honkaigamelauncher.desktop_ui.generated.resources.screen_website
import honkaigamelauncher.desktop_ui.generated.resources.setting
import navigation.SharedScreen
import navigation.screenRoute
import org.jetbrains.compose.resources.stringResource
import screen.home.HomeHero
import screen.home.HomePathCard
import screen.home.HomeStatusCard
import screen.home.QuickAccessCard
import screen.home.SectionHeader
import screen.home.homeBackgroundBrush
import ui.fluent.theme.FluentTokens
import ui.settings.LocalAppUiSettings
import screenmodel.HomeScreenModel

/**
 * 首页。
 *
 * 展示游戏状态概览（路径、连接状态、插件统计）和快捷导航入口。
 * 提供游戏 exe 选择、启动/停止、打开目录等操作入口。
 * 数据与交互逻辑由 [HomeScreenModel] 管理。
 */
class HomeScreen : Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return screenRoute(SharedScreen.Home)
    }

    override fun getIcon(): ImageVector {
        return EvaIcons.Fill.Home
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_home)
    }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { HomeScreenModel() }
        val uiState = screenModel.uiState
        val navigator = LocalNavigator.current
        val scrollState = rememberScrollState()
        val isDarkTheme = LocalAppUiSettings.current.isDarkTheme

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(homeBackgroundBrush(isDarkTheme))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 28.dp, vertical = 22.dp)
                    .widthIn(max = 1180.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HomeHero(
                    uiState = uiState,
                    onLaunch = { screenModel.launchGame() },
                    onSelectGame = { screenModel.selectGamePath() },
                    onRefresh = { screenModel.refresh() },
                    isDarkTheme = isDarkTheme,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HomeStatusCard(
                        title = stringResource(Res.string.homeSectionStatus),
                        uiState = uiState,
                        modifier = Modifier.weight(1.35f),
                        isDarkTheme = isDarkTheme
                    )
                    HomePathCard(
                        uiState = uiState,
                        onOpenFolder = { screenModel.openGameDirectory() },
                        modifier = Modifier.weight(1f),
                        isDarkTheme = isDarkTheme
                    )
                }

                SectionHeader(
                    title = stringResource(Res.string.homeSectionQuickAccess),
                    color = FluentTokens.ColorToken.accent
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessCard(
                        title = stringResource(Res.string.screen_plugin),
                        description = stringResource(Res.string.homeQuickPluginDesc),
                        icon = LineAwesomeIcons.PuzzlePieceSolid,
                        color = FluentTokens.ColorToken.accent,
                        onClick = { navigator?.push(ScreenRegistry.get(SharedScreen.Plugin)) }
                    )
                    QuickAccessCard(
                        title = stringResource(Res.string.screen_log),
                        description = stringResource(Res.string.homeQuickLogDesc),
                        icon = FeatherIcons.Activity,
                        color = Color(0xFF00A3A3),
                        onClick = { navigator?.push(ScreenRegistry.get(SharedScreen.Log)) }
                    )
                    QuickAccessCard(
                        title = stringResource(Res.string.screen_website),
                        description = stringResource(Res.string.homeQuickWebsiteDesc),
                        icon = FeatherIcons.Globe,
                        color = Color(0xFFE08B2F),
                        onClick = { navigator?.push(ScreenRegistry.get(SharedScreen.Web)) }
                    )
                    QuickAccessCard(
                        title = stringResource(Res.string.screen_doc),
                        description = stringResource(Res.string.homeQuickDocsDesc),
                        icon = FeatherIcons.BookOpen,
                        color = Color(0xFF6D6FE8),
                        onClick = { navigator?.push(ScreenRegistry.get(SharedScreen.Docs)) }
                    )
                    QuickAccessCard(
                        title = stringResource(Res.string.setting),
                        description = stringResource(Res.string.homeQuickSettingsDesc),
                        icon = FeatherIcons.Settings,
                        color = FluentTokens.ColorToken.LogLevel.warning,
                        onClick = { navigator?.push(ScreenRegistry.get(SharedScreen.Setting)) }
                    )
                }
            }
        }
    }
}

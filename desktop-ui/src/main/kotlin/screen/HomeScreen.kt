package screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.ScreenRegistry
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import compose.icons.EvaIcons
import compose.icons.FeatherIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Home
import compose.icons.feathericons.Activity
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Globe
import compose.icons.feathericons.Play
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Settings
import compose.icons.lineawesomeicons.PuzzlePieceSolid
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.homeActionOpenFolder
import honkaigamelauncher.desktop_ui.generated.resources.homeActionRefresh
import honkaigamelauncher.desktop_ui.generated.resources.homeActionSelectGame
import honkaigamelauncher.desktop_ui.generated.resources.homeActionStartGame
import honkaigamelauncher.desktop_ui.generated.resources.homeDirectoryMissing
import honkaigamelauncher.desktop_ui.generated.resources.homeGamePathLabel
import honkaigamelauncher.desktop_ui.generated.resources.homeHeroSubtitle
import honkaigamelauncher.desktop_ui.generated.resources.homeHeroTitle
import honkaigamelauncher.desktop_ui.generated.resources.homeLastLaunch
import honkaigamelauncher.desktop_ui.generated.resources.homeNeverLaunched
import honkaigamelauncher.desktop_ui.generated.resources.homePathNotSet
import honkaigamelauncher.desktop_ui.generated.resources.homePluginConfigMissing
import honkaigamelauncher.desktop_ui.generated.resources.homePluginCount
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickDocsDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickLogDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickPluginDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickSettingsDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeQuickWebsiteDesc
import honkaigamelauncher.desktop_ui.generated.resources.homeSectionQuickAccess
import honkaigamelauncher.desktop_ui.generated.resources.homeSectionStatus
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusError
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusLaunching
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusMissingExecutable
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusMissingGamePath
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusReady
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusRunning
import honkaigamelauncher.desktop_ui.generated.resources.pluginConfigLabel
import honkaigamelauncher.desktop_ui.generated.resources.screen_doc
import honkaigamelauncher.desktop_ui.generated.resources.screen_home
import honkaigamelauncher.desktop_ui.generated.resources.screen_log
import honkaigamelauncher.desktop_ui.generated.resources.screen_plugin
import honkaigamelauncher.desktop_ui.generated.resources.screen_website
import honkaigamelauncher.desktop_ui.generated.resources.setting
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import navigation.SharedScreen
import org.jetbrains.compose.resources.stringResource
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.theme.FluentTokens
import viewModel.HomeLaunchStatus
import viewModel.HomeScreenModel
import io.github.composefluent.component.Text as FluentText

class HomeScreen : Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return "home"
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
        val navigator = LocalNavigator.current
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .widthIn(max = 1180.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HomeHero(
                screenModel = screenModel,
                onLaunch = { screenModel.launchGame() },
                onSelectGame = { screenModel.selectGamePath() },
                onRefresh = { screenModel.refresh() },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeStatusCard(
                    title = stringResource(Res.string.homeSectionStatus),
                    screenModel = screenModel,
                    modifier = Modifier.weight(1.25f)
                )
                HomePathCard(
                    screenModel = screenModel,
                    onOpenFolder = { screenModel.openGameDirectory() },
                    modifier = Modifier.weight(1f)
                )
            }

            FluentText(
                text = stringResource(Res.string.homeSectionQuickAccess),
                style = FluentTheme.typography.bodyStrong,
                modifier = Modifier.padding(top = 4.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAccessCard(
                    title = stringResource(Res.string.screen_plugin),
                    description = stringResource(Res.string.homeQuickPluginDesc),
                    icon = compose.icons.LineAwesomeIcons.PuzzlePieceSolid,
                    color = FluentTokens.ColorToken.accent,
                    onClick = { navigator?.push(ScreenRegistry.get(SharedScreen.Plugin)) }
                )
                QuickAccessCard(
                    title = stringResource(Res.string.screen_log),
                    description = stringResource(Res.string.homeQuickLogDesc),
                    icon = FeatherIcons.Activity,
                    color = FluentTokens.ColorToken.LogLevel.display,
                    onClick = { navigator?.push(ScreenRegistry.get(SharedScreen.Log)) }
                )
                QuickAccessCard(
                    title = stringResource(Res.string.screen_website),
                    description = stringResource(Res.string.homeQuickWebsiteDesc),
                    icon = FeatherIcons.Globe,
                    color = FluentTokens.ColorToken.LogLevel.verbose,
                    onClick = { navigator?.push(ScreenRegistry.get(SharedScreen.Web)) }
                )
                QuickAccessCard(
                    title = stringResource(Res.string.screen_doc),
                    description = stringResource(Res.string.homeQuickDocsDesc),
                    icon = FeatherIcons.BookOpen,
                    color = FluentTokens.ColorToken.LogLevel.veryVerbose,
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

    @Composable
    private fun HomeHero(
        screenModel: HomeScreenModel,
        onLaunch: () -> Unit,
        onSelectGame: () -> Unit,
        onRefresh: () -> Unit,
    ) {
        val statusColor = statusColor(screenModel.launchStatus)
        val canLaunch = screenModel.launchStatus == HomeLaunchStatus.Ready ||
                screenModel.launchStatus == HomeLaunchStatus.Running

        FluentCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (screenModel.launchStatus == HomeLaunchStatus.Ready) {
                            FeatherIcons.Play
                        } else {
                            getIcon()
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    FluentText(
                        text = stringResource(Res.string.homeHeroTitle),
                        style = FluentTheme.typography.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FluentText(
                        text = stringResource(Res.string.homeHeroSubtitle),
                        style = FluentTheme.typography.body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor)
                        )
                        FluentText(
                            text = launchStatusText(screenModel),
                            style = FluentTheme.typography.caption,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FluentButton(
                        onClick = onLaunch,
                        disabled = !canLaunch || screenModel.launchStatus == HomeLaunchStatus.Launching
                    ) {
                        Icon(FeatherIcons.Play, contentDescription = null)
                        FluentText(stringResource(Res.string.homeActionStartGame))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FluentButton(onClick = onSelectGame, iconOnly = true) {
                            Icon(
                                FeatherIcons.Folder,
                                contentDescription = stringResource(Res.string.homeActionSelectGame)
                            )
                        }
                        FluentButton(onClick = onRefresh, iconOnly = true) {
                            Icon(
                                FeatherIcons.RefreshCw,
                                contentDescription = stringResource(Res.string.homeActionRefresh)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HomeStatusCard(
        title: String,
        screenModel: HomeScreenModel,
        modifier: Modifier = Modifier,
    ) {
        FluentCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FluentText(text = title, style = FluentTheme.typography.subtitle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusTile(
                        value = screenModel.pluginCount.toString(),
                        label = stringResource(Res.string.homePluginCount),
                        icon = compose.icons.LineAwesomeIcons.PuzzlePieceSolid,
                        color = FluentTokens.ColorToken.accent,
                        modifier = Modifier.weight(1f)
                    )
                    StatusTile(
                        value = if (screenModel.lastLaunchTime.isBlank()) {
                            stringResource(Res.string.homeNeverLaunched)
                        } else {
                            screenModel.lastLaunchTime
                        },
                        label = stringResource(Res.string.homeLastLaunch),
                        icon = FeatherIcons.CheckCircle,
                        color = FluentTokens.ColorToken.LogLevel.display,
                        modifier = Modifier.weight(1f)
                    )
                }
                PathLine(
                    icon = FeatherIcons.Folder,
                    label = stringResource(Res.string.homeGamePathLabel),
                    value = screenModel.gamePath.takeIf { screenModel.hasGamePath }
                        ?: stringResource(Res.string.homePathNotSet),
                    color = statusColor(screenModel.launchStatus)
                )
                PathLine(
                    icon = compose.icons.LineAwesomeIcons.PuzzlePieceSolid,
                    label = stringResource(Res.string.pluginConfigLabel),
                    value = screenModel.pluginConfigPath.ifBlank {
                        stringResource(Res.string.homePluginConfigMissing)
                    },
                    color = if (screenModel.hasPluginConfig) {
                        FluentTokens.ColorToken.accent
                    } else {
                        FluentTokens.ColorToken.LogLevel.warning
                    }
                )
            }
        }
    }

    @Composable
    private fun HomePathCard(
        screenModel: HomeScreenModel,
        onOpenFolder: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        FluentCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FluentText(
                    text = screenModel.gameFileName.ifBlank { stringResource(Res.string.homePathNotSet) },
                    style = FluentTheme.typography.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FluentText(
                    text = screenModel.gameDirectory.ifBlank { stringResource(Res.string.homeDirectoryMissing) },
                    style = FluentTheme.typography.caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                FluentButton(
                    onClick = onOpenFolder,
                    disabled = screenModel.gameDirectory.isBlank()
                ) {
                    Icon(FeatherIcons.Folder, contentDescription = null)
                    FluentText(stringResource(Res.string.homeActionOpenFolder))
                }
            }
        }
    }

    @Composable
    private fun StatusTile(
        value: String,
        label: String,
        icon: ImageVector,
        color: Color,
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.08f))
                .border(1.dp, color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                FluentText(
                    text = value,
                    style = FluentTheme.typography.bodyStrong,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FluentText(
                    text = label,
                    style = FluentTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    private fun PathLine(
        icon: ImageVector,
        label: String,
        value: String,
        color: Color,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.06f))
                .border(1.dp, color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            FluentText(
                text = "$label:",
                style = FluentTheme.typography.caption,
                color = color
            )
            FluentText(
                text = value,
                style = FluentTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    private fun QuickAccessCard(
        title: String,
        description: String,
        icon: ImageVector,
        color: Color,
        onClick: () -> Unit,
    ) {
        FluentCard(
            modifier = Modifier
                .width(218.dp)
                .height(112.dp)
        ) {
            FluentButton(
                onClick = onClick,
                modifier = Modifier.fillMaxSize(),
                contentArrangement = Arrangement.spacedBy(10.dp, Alignment.Start)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    FluentText(
                        text = title,
                        style = FluentTheme.typography.bodyStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FluentText(
                        text = description,
                        style = FluentTheme.typography.caption,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Composable
    private fun launchStatusText(screenModel: HomeScreenModel): String {
        return when (screenModel.launchStatus) {
            HomeLaunchStatus.MissingGamePath -> stringResource(Res.string.homeStatusMissingGamePath)
            HomeLaunchStatus.MissingExecutable -> stringResource(Res.string.homeStatusMissingExecutable)
            HomeLaunchStatus.Ready -> stringResource(Res.string.homeStatusReady)
            HomeLaunchStatus.Launching -> stringResource(Res.string.homeStatusLaunching)
            HomeLaunchStatus.Running -> stringResource(Res.string.homeStatusRunning)
            HomeLaunchStatus.Error -> screenModel.statusMessage.ifBlank {
                stringResource(Res.string.homeStatusError)
            }
        }
    }

    private fun statusColor(status: HomeLaunchStatus): Color {
        return when (status) {
            HomeLaunchStatus.Ready,
            HomeLaunchStatus.Running -> FluentTokens.ColorToken.accent
            HomeLaunchStatus.Launching -> FluentTokens.ColorToken.LogLevel.display
            HomeLaunchStatus.MissingGamePath,
            HomeLaunchStatus.MissingExecutable -> FluentTokens.ColorToken.LogLevel.warning
            HomeLaunchStatus.Error -> FluentTokens.ColorToken.LogLevel.error
        }
    }
}

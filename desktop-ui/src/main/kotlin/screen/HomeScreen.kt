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
import androidx.compose.ui.graphics.Brush
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
import core.GameConnectionStatus
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.homeActionOpenFolder
import honkaigamelauncher.desktop_ui.generated.resources.homeActionRefresh
import honkaigamelauncher.desktop_ui.generated.resources.homeActionSelectGame
import honkaigamelauncher.desktop_ui.generated.resources.homeActionStartGame
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionConnected
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionConnectedHint
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionLabel
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionStopped
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionStoppedHint
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionWaiting
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionWaitingHint
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
import ui.settings.LocalAppUiSettings
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
                    screenModel = screenModel,
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
                        screenModel = screenModel,
                        modifier = Modifier.weight(1.35f),
                        isDarkTheme = isDarkTheme
                    )
                    HomePathCard(
                        screenModel = screenModel,
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
                        icon = compose.icons.LineAwesomeIcons.PuzzlePieceSolid,
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

    @Composable
    private fun HomeHero(
        screenModel: HomeScreenModel,
        onLaunch: () -> Unit,
        onSelectGame: () -> Unit,
        onRefresh: () -> Unit,
        isDarkTheme: Boolean,
    ) {
        val statusColor = statusColor(screenModel.launchStatus)
        val canLaunch = screenModel.launchStatus == HomeLaunchStatus.Ready ||
                screenModel.launchStatus == HomeLaunchStatus.Running

        FluentCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(heroBrush(statusColor, isDarkTheme))
                    .border(1.dp, statusColor.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(statusColor.copy(alpha = 0.16f))
                            .border(1.dp, statusColor.copy(alpha = 0.32f), RoundedCornerShape(24.dp)),
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
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatusPill(
                            text = launchStatusText(screenModel),
                            color = statusColor,
                        )
                        FluentText(
                            text = stringResource(Res.string.homeHeroTitle),
                            style = FluentTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FluentText(
                            text = stringResource(Res.string.homeHeroSubtitle),
                            style = FluentTheme.typography.body,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FluentButton(
                                onClick = onLaunch,
                                disabled = !canLaunch || screenModel.launchStatus == HomeLaunchStatus.Launching
                            ) {
                                Icon(FeatherIcons.Play, contentDescription = null)
                                FluentText(stringResource(Res.string.homeActionStartGame))
                            }
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

                    ConnectionPulseCard(
                        status = screenModel.gameConnectionStatus,
                        statusColor = statusColor,
                        isDarkTheme = isDarkTheme,
                    )
                }
            }
        }
    }

    @Composable
    private fun HomeStatusCard(
        title: String,
        screenModel: HomeScreenModel,
        modifier: Modifier = Modifier,
        isDarkTheme: Boolean,
    ) {
        FluentCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBrush(isDarkTheme))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionHeader(title = title, color = statusColor(screenModel.launchStatus))
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
                        value = connectionStatusText(screenModel.gameConnectionStatus),
                        label = stringResource(Res.string.homeConnectionLabel),
                        icon = FeatherIcons.Activity,
                        color = connectionColor(screenModel.gameConnectionStatus),
                        modifier = Modifier.weight(1f)
                    )
                    StatusTile(
                        value = screenModel.lastLaunchTime.ifBlank {
                            stringResource(Res.string.homeNeverLaunched)
                        },
                        label = stringResource(Res.string.homeLastLaunch),
                        icon = FeatherIcons.CheckCircle,
                        color = Color(0xFF00A3A3),
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
        isDarkTheme: Boolean,
    ) {
        FluentCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBrush(isDarkTheme))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = stringResource(Res.string.homeGamePathLabel),
                    color = statusColor(screenModel.launchStatus)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusColor(screenModel.launchStatus).copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            statusColor(screenModel.launchStatus).copy(alpha = 0.14f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    }
                }
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
    private fun ConnectionPulseCard(
        status: GameConnectionStatus,
        statusColor: Color,
        isDarkTheme: Boolean,
    ) {
        val color = connectionColor(status)

        Box(
            modifier = Modifier
                .width(216.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(cardBrush(isDarkTheme))
                .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FluentText(
                    text = stringResource(Res.string.homeConnectionLabel),
                    style = FluentTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(color.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(color)
                                .border(4.dp, color.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FluentText(
                            text = connectionStatusText(status),
                            style = FluentTheme.typography.bodyStrong,
                            color = color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FluentText(
                            text = launchConnectionHint(status),
                            style = FluentTheme.typography.caption,
                            color = statusColor.copy(alpha = 0.86f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.09f))
                .border(1.dp, color.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
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
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.06f))
                .border(1.dp, color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
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
                .height(118.dp)
        ) {
            FluentButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.06f))
                    .border(1.dp, color.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                contentArrangement = Arrangement.spacedBy(11.dp, Alignment.Start)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
    private fun SectionHeader(
        title: String,
        color: Color,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            FluentText(
                text = title,
                style = FluentTheme.typography.bodyStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    private fun StatusPill(
        text: String,
        color: Color,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(color.copy(alpha = 0.11f))
                .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )
            FluentText(
                text = text,
                style = FluentTheme.typography.caption,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

    @Composable
    private fun connectionStatusText(status: GameConnectionStatus): String {
        return when (status) {
            GameConnectionStatus.Stopped -> stringResource(Res.string.homeConnectionStopped)
            GameConnectionStatus.Waiting -> stringResource(Res.string.homeConnectionWaiting)
            GameConnectionStatus.Connected -> stringResource(Res.string.homeConnectionConnected)
        }
    }

    @Composable
    private fun launchConnectionHint(status: GameConnectionStatus): String {
        return when (status) {
            GameConnectionStatus.Stopped -> stringResource(Res.string.homeConnectionStoppedHint)
            GameConnectionStatus.Waiting -> stringResource(Res.string.homeConnectionWaitingHint)
            GameConnectionStatus.Connected -> stringResource(Res.string.homeConnectionConnectedHint)
        }
    }

    private fun statusColor(status: HomeLaunchStatus): Color {
        return when (status) {
            HomeLaunchStatus.Ready -> Color(0xFF0E9F6E)
            HomeLaunchStatus.Running -> FluentTokens.ColorToken.accent
            HomeLaunchStatus.Launching -> Color(0xFFE08B2F)
            HomeLaunchStatus.MissingGamePath,
            HomeLaunchStatus.MissingExecutable -> FluentTokens.ColorToken.LogLevel.warning
            HomeLaunchStatus.Error -> FluentTokens.ColorToken.LogLevel.error
        }
    }

    private fun connectionColor(status: GameConnectionStatus): Color {
        return when (status) {
            GameConnectionStatus.Stopped -> FluentTokens.ColorToken.LogLevel.error
            GameConnectionStatus.Waiting -> Color(0xFFE08B2F)
            GameConnectionStatus.Connected -> FluentTokens.ColorToken.accent
        }
    }

    private fun homeBackgroundBrush(isDarkTheme: Boolean): Brush {
        return if (isDarkTheme) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF101820),
                    Color(0xFF17242E),
                    Color(0xFF202020),
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF7FBFF),
                    Color(0xFFFFFCF4),
                    Color(0xFFFAF9F8),
                )
            )
        }
    }

    private fun heroBrush(color: Color, isDarkTheme: Boolean): Brush {
        val base = if (isDarkTheme) Color(0xFF252525) else Color.White
        return Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = if (isDarkTheme) 0.22f else 0.15f),
                base,
                Color(0xFFE08B2F).copy(alpha = if (isDarkTheme) 0.14f else 0.09f),
            )
        )
    }

    private fun cardBrush(isDarkTheme: Boolean): Brush {
        return Brush.linearGradient(
            colors = if (isDarkTheme) {
                listOf(Color(0xFF303030), Color(0xFF282828))
            } else {
                listOf(Color.White, Color(0xFFFBFAF7))
            }
        )
    }
}

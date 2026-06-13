package screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.EvaIcons
import compose.icons.FeatherIcons
import compose.icons.LineAwesomeIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Home
import compose.icons.feathericons.Activity
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Play
import compose.icons.feathericons.RefreshCw
import compose.icons.lineawesomeicons.PuzzlePieceSolid
import core.GameConnectionStatus
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.homeActionOpenFolder
import honkaigamelauncher.desktop_ui.generated.resources.homeActionRefresh
import honkaigamelauncher.desktop_ui.generated.resources.homeActionSelectGame
import honkaigamelauncher.desktop_ui.generated.resources.homeActionStartGame
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionLabel
import honkaigamelauncher.desktop_ui.generated.resources.homeDirectoryMissing
import honkaigamelauncher.desktop_ui.generated.resources.homeGamePathLabel
import honkaigamelauncher.desktop_ui.generated.resources.homeHeroSubtitle
import honkaigamelauncher.desktop_ui.generated.resources.homeHeroTitle
import honkaigamelauncher.desktop_ui.generated.resources.homeLastLaunch
import honkaigamelauncher.desktop_ui.generated.resources.homeNeverLaunched
import honkaigamelauncher.desktop_ui.generated.resources.homePathNotSet
import honkaigamelauncher.desktop_ui.generated.resources.homePluginConfigMissing
import honkaigamelauncher.desktop_ui.generated.resources.homePluginCount
import honkaigamelauncher.desktop_ui.generated.resources.pluginConfigLabel
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import org.jetbrains.compose.resources.stringResource
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.theme.FluentTokens
import viewModel.HomeLaunchStatus
import viewModel.HomeUiState
import io.github.composefluent.component.Text as FluentText

@Composable
internal fun HomeHero(
    uiState: HomeUiState,
    onLaunch: () -> Unit,
    onSelectGame: () -> Unit,
    onRefresh: () -> Unit,
    isDarkTheme: Boolean,
) {
    val statusColor = statusColor(uiState.launchStatus)
    val canLaunch = uiState.launchStatus == HomeLaunchStatus.Ready ||
            uiState.launchStatus == HomeLaunchStatus.Running

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
                        imageVector = if (uiState.launchStatus == HomeLaunchStatus.Ready) {
                            FeatherIcons.Play
                        } else {
                            EvaIcons.Fill.Home
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
                        text = launchStatusText(uiState),
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
                            disabled = !canLaunch || uiState.launchStatus == HomeLaunchStatus.Launching
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
                    status = uiState.gameConnectionStatus,
                    statusColor = statusColor,
                    isDarkTheme = isDarkTheme,
                )
            }
        }
    }
}

@Composable
internal fun HomeStatusCard(
    title: String,
    uiState: HomeUiState,
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
            SectionHeader(title = title, color = statusColor(uiState.launchStatus))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusTile(
                    value = uiState.pluginCount.toString(),
                    label = stringResource(Res.string.homePluginCount),
                    icon = LineAwesomeIcons.PuzzlePieceSolid,
                    color = FluentTokens.ColorToken.accent,
                    modifier = Modifier.weight(1f)
                )
                StatusTile(
                    value = connectionStatusText(uiState.gameConnectionStatus),
                    label = stringResource(Res.string.homeConnectionLabel),
                    icon = FeatherIcons.Activity,
                    color = connectionColor(uiState.gameConnectionStatus),
                    modifier = Modifier.weight(1f)
                )
                StatusTile(
                    value = uiState.lastLaunchTime.ifBlank {
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
                value = uiState.gamePath?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.homePathNotSet),
                color = statusColor(uiState.launchStatus)
            )
            PathLine(
                icon = LineAwesomeIcons.PuzzlePieceSolid,
                label = stringResource(Res.string.pluginConfigLabel),
                value = uiState.pluginConfigPath.ifBlank {
                    stringResource(Res.string.homePluginConfigMissing)
                },
                color = if (uiState.pluginConfigPath.isNotBlank()) {
                    FluentTokens.ColorToken.accent
                } else {
                    FluentTokens.ColorToken.LogLevel.warning
                }
            )
        }
    }
}

@Composable
internal fun HomePathCard(
    uiState: HomeUiState,
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
                color = statusColor(uiState.launchStatus)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(statusColor(uiState.launchStatus).copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        statusColor(uiState.launchStatus).copy(alpha = 0.14f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FluentText(
                        text = uiState.gameFileName.ifBlank { stringResource(Res.string.homePathNotSet) },
                        style = FluentTheme.typography.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FluentText(
                        text = uiState.gameDirectory.ifBlank { stringResource(Res.string.homeDirectoryMissing) },
                        style = FluentTheme.typography.caption,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            FluentButton(
                onClick = onOpenFolder,
                disabled = uiState.gameDirectory.isBlank()
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
internal fun QuickAccessCard(
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
internal fun SectionHeader(
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

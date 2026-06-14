package screen.plugin

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.Box
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Database
import compose.icons.feathericons.ExternalLink
import compose.icons.feathericons.File
import compose.icons.feathericons.Folder
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Settings
import core.plugin.GamePluginConfig
import core.plugin.PluginLoadStatus
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.pluginCategoryDefault
import honkaigamelauncher.desktop_ui.generated.resources.pluginConfigLabel
import honkaigamelauncher.desktop_ui.generated.resources.pluginConfigPathPending
import honkaigamelauncher.desktop_ui.generated.resources.pluginDefaultDisabled
import honkaigamelauncher.desktop_ui.generated.resources.pluginDefaultEnabled
import honkaigamelauncher.desktop_ui.generated.resources.pluginDescriptionDefault
import honkaigamelauncher.desktop_ui.generated.resources.pluginEmptyError
import honkaigamelauncher.desktop_ui.generated.resources.pluginEmptyMissingConfig
import honkaigamelauncher.desktop_ui.generated.resources.pluginEmptyMissingGamePath
import honkaigamelauncher.desktop_ui.generated.resources.pluginEmptyReady
import honkaigamelauncher.desktop_ui.generated.resources.pluginGameLabel
import honkaigamelauncher.desktop_ui.generated.resources.pluginGamePathNotSet
import honkaigamelauncher.desktop_ui.generated.resources.pluginMetaGameFeature
import honkaigamelauncher.desktop_ui.generated.resources.pluginMetaName
import honkaigamelauncher.desktop_ui.generated.resources.pluginMetaPakPath
import honkaigamelauncher.desktop_ui.generated.resources.pluginMetaResolved
import honkaigamelauncher.desktop_ui.generated.resources.pluginMountOrder
import honkaigamelauncher.desktop_ui.generated.resources.pluginNotBound
import honkaigamelauncher.desktop_ui.generated.resources.pluginRefresh
import honkaigamelauncher.desktop_ui.generated.resources.pluginStatusError
import honkaigamelauncher.desktop_ui.generated.resources.pluginStatusLoaded
import honkaigamelauncher.desktop_ui.generated.resources.pluginStatusMissingConfig
import honkaigamelauncher.desktop_ui.generated.resources.pluginStatusMissingGamePath
import honkaigamelauncher.desktop_ui.generated.resources.pluginTypeBuiltIn
import honkaigamelauncher.desktop_ui.generated.resources.pluginTypePak
import honkaigamelauncher.desktop_ui.generated.resources.screen_plugin
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text as FluentText
import org.jetbrains.compose.resources.stringResource
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.theme.FluentTokens
import screenmodel.PluginUiState

@Composable
fun PluginOverview(
    uiState: PluginUiState,
    icon: ImageVector,
    onRefresh: () -> Unit,
) {
    val totalPlugins = uiState.plugins.size
    val pakPlugins = uiState.plugins.count { !it.isBuiltIn }
    val builtInPlugins = uiState.plugins.count { it.isBuiltIn }
    val enabledPlugins = uiState.plugins.count { it.defaultEnable }
    val statusColor = pluginStatusColor(uiState.loadStatus)

    FluentCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.24f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    FluentText(
                        text = stringResource(Res.string.screen_plugin),
                        style = FluentTheme.typography.title
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(statusColor)
                        )
                        FluentText(
                            text = pluginStatusText(uiState),
                            style = FluentTheme.typography.caption,
                            color = statusColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FluentButton(
                    onClick = onRefresh,
                    disabled = uiState.isLoading,
                    iconOnly = true
                ) {
                    Icon(
                        imageVector = FeatherIcons.RefreshCw,
                        contentDescription = stringResource(Res.string.pluginRefresh)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PluginStatTile(
                    value = totalPlugins.toString(),
                    label = stringResource(Res.string.screen_plugin),
                    icon = FeatherIcons.Database,
                    color = FluentTokens.ColorToken.accent,
                    modifier = Modifier.weight(1f)
                )
                PluginStatTile(
                    value = enabledPlugins.toString(),
                    label = stringResource(Res.string.pluginDefaultEnabled),
                    icon = FeatherIcons.CheckCircle,
                    color = FluentTokens.ColorToken.LogLevel.display,
                    modifier = Modifier.weight(1f)
                )
                PluginStatTile(
                    value = pakPlugins.toString(),
                    label = stringResource(Res.string.pluginTypePak),
                    icon = FeatherIcons.File,
                    color = FluentTokens.ColorToken.LogLevel.verbose,
                    modifier = Modifier.weight(1f)
                )
                PluginStatTile(
                    value = builtInPlugins.toString(),
                    label = stringResource(Res.string.pluginTypeBuiltIn),
                    icon = FeatherIcons.Box,
                    color = FluentTokens.ColorToken.LogLevel.veryVerbose,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(FluentTokens.ColorToken.LogLevel.unknown.copy(alpha = 0.06f))
                    .border(
                        1.dp,
                        FluentTokens.ColorToken.LogLevel.unknown.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                PluginPathLine(
                    icon = FeatherIcons.Folder,
                    label = stringResource(Res.string.pluginGameLabel),
                    value = uiState.gamePath
                        ?: stringResource(Res.string.pluginGamePathNotSet)
                )
                PluginPathLine(
                    icon = FeatherIcons.File,
                    label = stringResource(Res.string.pluginConfigLabel),
                    value = uiState.configPath.ifBlank { stringResource(Res.string.pluginConfigPathPending) }
                )
            }
        }
    }
}

@Composable
fun PluginListPanel(
    uiState: PluginUiState,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()

    Box(modifier = modifier) {
        FluentCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp)
        ) {
            if (uiState.plugins.isEmpty()) {
                EmptyPluginState(uiState.loadStatus, uiState.errorMessage)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    state = state,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.plugins) { plugin ->
                        PluginItem(plugin)
                    }
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            adapter = rememberScrollbarAdapter(scrollState = state)
        )
    }
}

@Composable
private fun PluginStatTile(
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
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
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
private fun pluginStatusText(uiState: PluginUiState): String {
    return when (uiState.loadStatus) {
        PluginLoadStatus.Ready -> stringResource(Res.string.pluginStatusLoaded, uiState.plugins.size)
        PluginLoadStatus.MissingGamePath -> stringResource(Res.string.pluginStatusMissingGamePath)
        PluginLoadStatus.MissingConfig -> stringResource(Res.string.pluginStatusMissingConfig)
        PluginLoadStatus.Error -> stringResource(Res.string.pluginStatusError)
    }
}

private fun pluginStatusColor(status: PluginLoadStatus): Color {
    return when (status) {
        PluginLoadStatus.Ready -> FluentTokens.ColorToken.accent
        PluginLoadStatus.MissingGamePath,
        PluginLoadStatus.MissingConfig -> FluentTokens.ColorToken.LogLevel.warning
        PluginLoadStatus.Error -> FluentTokens.ColorToken.LogLevel.error
    }
}

@Composable
private fun PluginPathLine(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FluentTokens.ColorToken.LogLevel.veryVerbose,
            modifier = Modifier.size(15.dp)
        )
        FluentText(
            text = "$label:",
            style = FluentTheme.typography.caption,
            color = FluentTokens.ColorToken.LogLevel.veryVerbose,
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
private fun EmptyPluginState(status: PluginLoadStatus, errorMessage: String) {
    val message = when (status) {
        PluginLoadStatus.Ready -> stringResource(Res.string.pluginEmptyReady)
        PluginLoadStatus.MissingGamePath -> stringResource(Res.string.pluginEmptyMissingGamePath)
        PluginLoadStatus.MissingConfig -> stringResource(Res.string.pluginEmptyMissingConfig)
        PluginLoadStatus.Error -> errorMessage.ifBlank { stringResource(Res.string.pluginEmptyError) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val color = pluginStatusColor(status)
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.10f))
                .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (status == PluginLoadStatus.Error) FeatherIcons.AlertCircle else FeatherIcons.Database,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        FluentText(
            text = message,
            style = FluentTheme.typography.body,
            color = color,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

@Composable
private fun PluginItem(plugin: GamePluginConfig) {
    val accentColor = if (plugin.defaultEnable) {
        FluentTokens.ColorToken.accent
    } else {
        FluentTokens.ColorToken.LogLevel.unknown
    }

    FluentCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (plugin.isBuiltIn) FeatherIcons.Box else FeatherIcons.File,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        FluentText(
                            text = plugin.friendlyName.ifBlank { plugin.name.ifBlank { plugin.gameFeatureName } },
                            style = FluentTheme.typography.bodyStrong,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FluentText(
                            text = plugin.description.ifBlank { stringResource(Res.string.pluginDescriptionDefault) },
                            style = FluentTheme.typography.caption,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PluginChip(
                        text = if (plugin.isBuiltIn) stringResource(Res.string.pluginTypeBuiltIn) else stringResource(Res.string.pluginTypePak),
                        icon = if (plugin.isBuiltIn) FeatherIcons.Box else FeatherIcons.File,
                        color = if (plugin.isBuiltIn) FluentTokens.ColorToken.LogLevel.verbose else FluentTokens.ColorToken.accent
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PluginChip(
                        text = plugin.category.ifBlank { stringResource(Res.string.pluginCategoryDefault) },
                        icon = FeatherIcons.Database,
                        color = FluentTokens.ColorToken.LogLevel.display
                    )
                    PluginChip(
                        text = if (plugin.defaultEnable) stringResource(Res.string.pluginDefaultEnabled) else stringResource(Res.string.pluginDefaultDisabled),
                        icon = FeatherIcons.CheckCircle,
                        color = accentColor
                    )
                    PluginChip(
                        text = stringResource(Res.string.pluginMountOrder, plugin.mountOrder?.toString() ?: "-"),
                        icon = FeatherIcons.Settings,
                        color = FluentTokens.ColorToken.LogLevel.log
                    )
                    if (plugin.createdBy.isNotBlank()) {
                        PluginChip(
                            text = plugin.createdBy,
                            icon = if (plugin.createdByUrl.isBlank()) FeatherIcons.Settings else FeatherIcons.ExternalLink,
                            color = FluentTokens.ColorToken.LogLevel.veryVerbose
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FluentTokens.ColorToken.LogLevel.unknown.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            FluentTokens.ColorToken.LogLevel.unknown.copy(alpha = 0.10f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PluginMetaLine(stringResource(Res.string.pluginMetaName), plugin.name)
                    PluginMetaLine(stringResource(Res.string.pluginMetaGameFeature), plugin.gameFeatureName)
                    PluginMetaLine(stringResource(Res.string.pluginMetaPakPath), plugin.pakPath.ifBlank { stringResource(Res.string.pluginNotBound) })
                    if (!plugin.isBuiltIn) {
                        PluginMetaLine(stringResource(Res.string.pluginMetaResolved), plugin.resolvedPakPath.orEmpty())
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginMetaLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FluentText(
            text = "$label:",
            style = FluentTheme.typography.caption,
            color = FluentTokens.ColorToken.LogLevel.veryVerbose
        )
        Spacer(modifier = Modifier.width(2.dp))
        FluentText(
            text = value.ifBlank { "-" },
            style = FluentTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PluginChip(
    text: String,
    icon: ImageVector,
    color: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
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

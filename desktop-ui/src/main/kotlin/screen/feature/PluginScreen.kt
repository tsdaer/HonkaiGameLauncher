package screen.feature

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.Box
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Database
import compose.icons.feathericons.ExternalLink
import compose.icons.feathericons.File
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Settings
import compose.icons.lineawesomeicons.PuzzlePieceSolid
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
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.theme.FluentTokens
import viewModel.GamePluginConfig
import viewModel.PluginLoadStatus
import viewModel.PluginScreenModel
import io.github.composefluent.component.Text as FluentText

class PluginScreen: Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return "plugin"
    }

    override fun getIcon(): ImageVector {
        return compose.icons.LineAwesomeIcons.PuzzlePieceSolid
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_plugin)
    }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { PluginScreenModel() }
        val state = rememberLazyListState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FluentCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = getIcon(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            FluentText(
                                text = stringResource(Res.string.screen_plugin),
                                style = FluentTheme.typography.subtitle
                            )
                            FluentText(
                                text = pluginStatusText(screenModel),
                                style = FluentTheme.typography.caption,
                                color = statusColor(screenModel.loadStatus),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FluentButton(
                            onClick = { screenModel.refresh() },
                            disabled = screenModel.isLoading,
                            iconOnly = true
                        ) {
                            Icon(
                                imageVector = FeatherIcons.RefreshCw,
                                contentDescription = stringResource(Res.string.pluginRefresh)
                            )
                        }
                    }

                    PluginPathLine(
                        icon = FeatherIcons.Settings,
                        label = stringResource(Res.string.pluginGameLabel),
                        value = screenModel.gamePath.takeUnless { it == "null" || it.isBlank() }
                            ?: stringResource(Res.string.pluginGamePathNotSet)
                    )
                    PluginPathLine(
                        icon = FeatherIcons.File,
                        label = stringResource(Res.string.pluginConfigLabel),
                        value = screenModel.configPath.ifBlank { stringResource(Res.string.pluginConfigPathPending) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                FluentCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp)
                ) {
                    if (screenModel.plugins.isEmpty()) {
                        EmptyPluginState(screenModel.loadStatus, screenModel.errorMessage)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            state = state,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(screenModel.plugins) { plugin ->
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
    }

    @Composable
    private fun pluginStatusText(screenModel: PluginScreenModel): String {
        return when (screenModel.loadStatus) {
            PluginLoadStatus.Ready -> stringResource(Res.string.pluginStatusLoaded, screenModel.plugins.size)
            PluginLoadStatus.MissingGamePath -> stringResource(Res.string.pluginStatusMissingGamePath)
            PluginLoadStatus.MissingConfig -> stringResource(Res.string.pluginStatusMissingConfig)
            PluginLoadStatus.Error -> stringResource(Res.string.pluginStatusError)
        }
    }

    private fun statusColor(status: PluginLoadStatus): Color {
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
                modifier = Modifier.size(15.dp)
            )
            FluentText(
                text = "$label:",
                style = FluentTheme.typography.caption,
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
            Icon(
                imageVector = if (status == PluginLoadStatus.Error) FeatherIcons.AlertCircle else FeatherIcons.Database,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            FluentText(
                text = message,
                style = FluentTheme.typography.body,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }

    @Composable
    private fun PluginItem(plugin: GamePluginConfig) {
        FluentCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (plugin.defaultEnable) FluentTokens.ColorToken.accent else FluentTokens.ColorToken.LogLevel.unknown)
                    )
                    Column(modifier = Modifier.weight(1f)) {
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PluginChip(
                        text = plugin.category.ifBlank { stringResource(Res.string.pluginCategoryDefault) },
                        icon = FeatherIcons.Database,
                        color = FluentTokens.ColorToken.LogLevel.display
                    )
                    PluginChip(
                        text = if (plugin.defaultEnable) stringResource(Res.string.pluginDefaultEnabled) else stringResource(Res.string.pluginDefaultDisabled),
                        icon = FeatherIcons.CheckCircle,
                        color = if (plugin.defaultEnable) FluentTokens.ColorToken.accent else FluentTokens.ColorToken.LogLevel.unknown
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

                PluginMetaLine(stringResource(Res.string.pluginMetaName), plugin.name)
                PluginMetaLine(stringResource(Res.string.pluginMetaGameFeature), plugin.gameFeatureName)
                PluginMetaLine(stringResource(Res.string.pluginMetaPakPath), plugin.pakPath.ifBlank { stringResource(Res.string.pluginNotBound) })
                if (!plugin.isBuiltIn) {
                    PluginMetaLine(stringResource(Res.string.pluginMetaResolved), plugin.resolvedPakPath.orEmpty())
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
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 3.dp),
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
}

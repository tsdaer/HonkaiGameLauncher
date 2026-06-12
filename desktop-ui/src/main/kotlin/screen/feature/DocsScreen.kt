package screen.feature

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.File
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.File
import compose.icons.feathericons.Folder
import compose.icons.feathericons.RefreshCw
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.docsDirectoryLabel
import honkaigamelauncher.desktop_ui.generated.resources.docsEmptyDirectory
import honkaigamelauncher.desktop_ui.generated.resources.docsErrorRead
import honkaigamelauncher.desktop_ui.generated.resources.docsErrorUnresolvedLink
import honkaigamelauncher.desktop_ui.generated.resources.docsGamePluginsSection
import honkaigamelauncher.desktop_ui.generated.resources.docsGeneralSection
import honkaigamelauncher.desktop_ui.generated.resources.docsMissingDirectory
import honkaigamelauncher.desktop_ui.generated.resources.docsMissingGamePath
import honkaigamelauncher.desktop_ui.generated.resources.docsOverviewTitle
import honkaigamelauncher.desktop_ui.generated.resources.docsRefresh
import honkaigamelauncher.desktop_ui.generated.resources.screen_doc
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.theme.FluentTokens
import viewModel.DocEntry
import viewModel.DocSection
import viewModel.DocsLoadStatus
import viewModel.DocsScreenModel
import io.github.composefluent.component.Text as FluentText

class DocsScreen : Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return "docs"
    }

    override fun getIcon(): ImageVector {
        return EvaIcons.Fill.File
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_doc)
    }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { DocsScreenModel() }
        val listState = rememberLazyListState()
        val systemUriHandler = LocalUriHandler.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DocsOverview(screenModel = screenModel, icon = getIcon())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.92f)
                        .fillMaxHeight()
                ) {
                    FluentCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 10.dp)
                    ) {
                        if (screenModel.documents.isEmpty()) {
                            DocsListEmptyState(screenModel = screenModel)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val generalDocs = screenModel.documents.filter { it.section == DocSection.General }
                                val pluginDocs = screenModel.documents.filter { it.section == DocSection.GamePlugins }

                                if (generalDocs.isNotEmpty()) {
                                    item {
                                        DocsSectionHeader(stringResource(Res.string.docsGeneralSection))
                                    }
                                    items(generalDocs, key = { it.absolutePath }) { entry ->
                                        DocListItem(
                                            entry = entry,
                                            selected = screenModel.selectedDocument?.absolutePath == entry.absolutePath,
                                            onClick = { screenModel.selectDocument(entry.absolutePath) },
                                        )
                                    }
                                }

                                if (pluginDocs.isNotEmpty()) {
                                    item {
                                        DocsSectionHeader(stringResource(Res.string.docsGamePluginsSection))
                                    }
                                    items(pluginDocs, key = { it.absolutePath }) { entry ->
                                        DocListItem(
                                            entry = entry,
                                            selected = screenModel.selectedDocument?.absolutePath == entry.absolutePath,
                                            onClick = { screenModel.selectDocument(entry.absolutePath) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )
                }

                FluentCard(
                    modifier = Modifier
                        .weight(1.7f)
                        .fillMaxHeight()
                ) {
                    when {
                        screenModel.selectedDocument == null || screenModel.loadStatus != DocsLoadStatus.Ready -> {
                            DocsReaderPlaceholder(screenModel = screenModel)
                        }

                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ReaderHeader(screenModel = screenModel)

                                if (screenModel.linkErrorMessage.isNotBlank()) {
                                    InlineWarning(
                                        text = stringResource(
                                            Res.string.docsErrorUnresolvedLink,
                                            screenModel.linkErrorMessage
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    val scrollState = rememberScrollState()

                                    Markdown(
                                        content = screenModel.markdownContent,
                                        colors = DefaultMarkdownColors(
                                            text = FluentTheme.colors.text.text.primary,
                                            codeBackground = FluentTokens.ColorToken.LogLevel.unknown.copy(alpha = 0.06f),
                                            inlineCodeBackground = FluentTokens.ColorToken.LogLevel.unknown.copy(alpha = 0.06f),
                                            dividerColor = FluentTokens.ColorToken.LogLevel.unknown.copy(alpha = 0.12f),
                                            tableBackground = FluentTokens.ColorToken.LogLevel.unknown.copy(alpha = 0.03f),
                                        ),
                                        typography = DefaultMarkdownTypography(
                                            h1 = TextStyle(fontSize = 24.sp),
                                            h2 = TextStyle(fontSize = 20.sp),
                                            h3 = TextStyle(fontSize = 17.sp),
                                            h4 = TextStyle(fontSize = 15.sp),
                                            h5 = TextStyle(fontSize = 14.sp),
                                            h6 = TextStyle(fontSize = 13.sp),
                                            text = TextStyle(fontSize = 14.sp),
                                            code = TextStyle(fontSize = 13.sp),
                                            inlineCode = TextStyle(fontSize = 13.sp),
                                            quote = TextStyle(fontSize = 14.sp),
                                            paragraph = TextStyle(fontSize = 14.sp),
                                            ordered = TextStyle(fontSize = 14.sp),
                                            bullet = TextStyle(fontSize = 14.sp),
                                            list = TextStyle(fontSize = 14.sp),
                                            textLink = TextLinkStyles(),
                                            table = TextStyle(fontSize = 13.sp),
                                        ),
                                        annotator = markdownAnnotator(
                                            config = markdownAnnotatorConfig(eolAsNewLine = true)
                                        ),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                    )

                                    VerticalScrollbar(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .padding(vertical = 4.dp),
                                        adapter = rememberScrollbarAdapter(scrollState)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocsOverview(
    screenModel: DocsScreenModel,
    icon: ImageVector,
) {
    val statusColor = docsStatusColor(screenModel.loadStatus)
    val docsPath = screenModel.docsDirectory.ifBlank { "-" }

    FluentCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.24f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FluentText(
                        text = stringResource(Res.string.screen_doc),
                        style = FluentTheme.typography.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FluentText(
                        text = "(${screenModel.documents.size})",
                        style = FluentTheme.typography.caption,
                        color = FluentTokens.ColorToken.LogLevel.veryVerbose,
                        maxLines = 1
                    )
                    FluentText(
                        text = docsStatusText(screenModel),
                        style = FluentTheme.typography.caption,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FluentText(
                    text = "${stringResource(Res.string.docsDirectoryLabel)}: $docsPath",
                    style = FluentTheme.typography.caption,
                    color = FluentTokens.ColorToken.LogLevel.veryVerbose,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FluentButton(
                onClick = { screenModel.refresh() },
                disabled = screenModel.isLoading,
                iconOnly = true
            ) {
                Icon(
                    imageVector = compose.icons.FeatherIcons.RefreshCw,
                    contentDescription = stringResource(Res.string.docsRefresh)
                )
            }
        }
    }
}

@Composable
private fun DocsSectionHeader(title: String) {
    FluentText(
        text = title,
        style = FluentTheme.typography.bodyStrong,
        color = FluentTokens.ColorToken.LogLevel.veryVerbose,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun DocListItem(
    entry: DocEntry,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = if (selected) FluentTokens.ColorToken.accent else FluentTokens.ColorToken.LogLevel.unknown
    val title = if (entry.isDefault) {
        stringResource(Res.string.docsOverviewTitle)
    } else {
        entry.title
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                accentColor.copy(alpha = if (selected) 0.10f else 0.04f),
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                accentColor.copy(alpha = if (selected) 0.26f else 0.10f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (entry.section == DocSection.GamePlugins) {
                    compose.icons.FeatherIcons.Folder
                } else {
                    compose.icons.FeatherIcons.File
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            FluentText(
                text = title,
                style = FluentTheme.typography.bodyStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FluentText(
                text = entry.relativePath,
                style = FluentTheme.typography.caption,
                color = FluentTokens.ColorToken.LogLevel.veryVerbose,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DocsListEmptyState(screenModel: DocsScreenModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FluentText(
            text = docsStatusText(screenModel),
            style = FluentTheme.typography.body,
            color = docsStatusColor(screenModel.loadStatus)
        )
    }
}

@Composable
private fun ReaderHeader(screenModel: DocsScreenModel) {
    val entry = screenModel.selectedDocument ?: return
    val title = if (entry.isDefault) {
        stringResource(Res.string.docsOverviewTitle)
    } else {
        entry.title
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FluentText(
            text = title,
            style = FluentTheme.typography.title
        )
        FluentText(
            text = entry.relativePath,
            style = FluentTheme.typography.caption,
            color = FluentTokens.ColorToken.LogLevel.veryVerbose
        )
    }
}

@Composable
private fun DocsReaderPlaceholder(screenModel: DocsScreenModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        docsStatusColor(screenModel.loadStatus).copy(alpha = 0.10f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        docsStatusColor(screenModel.loadStatus).copy(alpha = 0.18f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = compose.icons.FeatherIcons.AlertCircle,
                    contentDescription = null,
                    tint = docsStatusColor(screenModel.loadStatus),
                    modifier = Modifier.size(26.dp)
                )
            }

            FluentText(
                text = docsStatusText(screenModel),
                style = FluentTheme.typography.body,
                color = docsStatusColor(screenModel.loadStatus),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (screenModel.loadStatus == DocsLoadStatus.Error && screenModel.errorMessage.isNotBlank()) {
                FluentText(
                    text = stringResource(Res.string.docsErrorRead, screenModel.errorMessage),
                    style = FluentTheme.typography.caption,
                    color = FluentTokens.ColorToken.LogLevel.error
                )
            }
        }
    }
}

@Composable
private fun InlineWarning(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                FluentTokens.ColorToken.LogLevel.warning.copy(alpha = 0.10f),
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                FluentTokens.ColorToken.LogLevel.warning.copy(alpha = 0.18f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = compose.icons.FeatherIcons.AlertCircle,
            contentDescription = null,
            tint = FluentTokens.ColorToken.LogLevel.warning,
            modifier = Modifier.size(16.dp)
        )
        FluentText(
            text = text,
            style = FluentTheme.typography.caption,
            color = FluentTokens.ColorToken.LogLevel.warning
        )
    }
}

@Composable
private fun docsStatusText(screenModel: DocsScreenModel): String {
    return when (screenModel.loadStatus) {
        DocsLoadStatus.MissingGamePath -> stringResource(Res.string.docsMissingGamePath)
        DocsLoadStatus.MissingDocsDirectory -> stringResource(Res.string.docsMissingDirectory)
        DocsLoadStatus.Empty -> stringResource(Res.string.docsEmptyDirectory)
        DocsLoadStatus.Ready -> screenModel.selectedDocument?.relativePath
            ?: stringResource(Res.string.screen_doc)
        DocsLoadStatus.Error -> stringResource(Res.string.docsErrorRead, screenModel.errorMessage.ifBlank { "-" })
    }
}

private fun docsStatusColor(status: DocsLoadStatus): Color {
    return when (status) {
        DocsLoadStatus.Ready -> FluentTokens.ColorToken.accent
        DocsLoadStatus.MissingGamePath,
        DocsLoadStatus.MissingDocsDirectory,
        DocsLoadStatus.Empty -> FluentTokens.ColorToken.LogLevel.warning
        DocsLoadStatus.Error -> FluentTokens.ColorToken.LogLevel.error
    }
}

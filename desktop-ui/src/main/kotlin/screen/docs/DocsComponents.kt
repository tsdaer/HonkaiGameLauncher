package screen.docs

import androidx.compose.foundation.ScrollState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.File
import compose.icons.feathericons.Folder
import compose.icons.feathericons.RefreshCw
import core.docs.DocEntry
import core.docs.DocSection
import core.docs.DocsLoadStatus
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
import io.github.composefluent.component.Text as FluentText
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import screen.feature.DocTocItem
import screen.feature.DocsReaderController
import screen.feature.MarkdownPreview
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.theme.FluentTokens
import viewModel.DocsScreenModel
import viewModel.DocsUiState

@Composable
internal fun DocsOverview(
    uiState: DocsUiState,
    icon: ImageVector,
    onRefresh: () -> Unit,
) {
    val statusColor = docsStatusColor(uiState.loadStatus)
    val docsPath = uiState.docsDirectory.ifBlank { "-" }

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
                        text = "(${uiState.documents.size})",
                        style = FluentTheme.typography.caption,
                        color = FluentTokens.ColorToken.LogLevel.veryVerbose,
                        maxLines = 1
                    )
                    FluentText(
                        text = docsStatusText(uiState),
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
                onClick = onRefresh,
                disabled = uiState.isLoading,
                iconOnly = true
            ) {
                Icon(
                    imageVector = FeatherIcons.RefreshCw,
                    contentDescription = stringResource(Res.string.docsRefresh)
                )
            }
        }
    }
}

@Composable
internal fun DocsListPanel(
    uiState: DocsUiState,
    onSelectDocument: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    Box(modifier = modifier) {
        FluentCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 10.dp)
        ) {
            if (uiState.documents.isEmpty()) {
                DocsListEmptyState(uiState = uiState)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val generalDocs = uiState.documents.filter { it.section == DocSection.General }
                    val pluginDocs = uiState.documents.filter { it.section == DocSection.GamePlugins }

                    if (generalDocs.isNotEmpty()) {
                        item {
                            DocsSectionHeader(stringResource(Res.string.docsGeneralSection))
                        }
                        items(generalDocs, key = { it.absolutePath }) { entry ->
                            DocListItem(
                                entry = entry,
                                selected = uiState.selectedDocument?.absolutePath == entry.absolutePath,
                                onClick = { onSelectDocument(entry.absolutePath) },
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
                                selected = uiState.selectedDocument?.absolutePath == entry.absolutePath,
                                onClick = { onSelectDocument(entry.absolutePath) },
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
}

@Composable
internal fun DocsReaderPanel(
    uiState: DocsUiState,
    screenModel: DocsScreenModel,
    renderedMarkdown: String,
    headingSlugs: Map<Int, String>,
    readerScrollState: ScrollState,
    readerController: DocsReaderController,
    modifier: Modifier = Modifier,
) {
    FluentCard(modifier = modifier) {
        when {
            uiState.selectedDocument == null || uiState.loadStatus != DocsLoadStatus.Ready -> {
                DocsReaderPlaceholder(uiState = uiState)
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReaderHeader(uiState = uiState)

                    if (uiState.linkErrorMessage.isNotBlank()) {
                        InlineWarning(
                            text = stringResource(
                                Res.string.docsErrorUnresolvedLink,
                                uiState.linkErrorMessage
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .onGloballyPositioned {
                                readerController.viewportTopInWindow = it.positionInWindow().y
                            }
                    ) {
                        MarkdownPreview(
                            screenModel = screenModel,
                            markdown = renderedMarkdown,
                            controller = readerController,
                            headingSlugs = headingSlugs,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(readerScrollState)
                                .padding(end = 12.dp)
                        )

                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            adapter = rememberScrollbarAdapter(readerScrollState)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DocsTableOfContents(
    items: List<DocTocItem>,
    controller: DocsReaderController,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tocScrollState = rememberScrollState()

    FluentCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FluentText(
                text = "目录",
                style = FluentTheme.typography.bodyStrong,
                color = FluentTokens.ColorToken.LogLevel.veryVerbose,
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(tocScrollState)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items.forEach { item ->
                        DocsTocRow(
                            item = item,
                            onClick = { scope.launch { controller.scrollToAnchor(item.slug) } }
                        )
                    }
                }

                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp),
                    adapter = rememberScrollbarAdapter(tocScrollState)
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
                    FeatherIcons.Folder
                } else {
                    FeatherIcons.File
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
private fun DocsListEmptyState(uiState: DocsUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FluentText(
            text = docsStatusText(uiState),
            style = FluentTheme.typography.body,
            color = docsStatusColor(uiState.loadStatus)
        )
    }
}

@Composable
private fun ReaderHeader(uiState: DocsUiState) {
    val entry = uiState.selectedDocument ?: return
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
private fun DocsReaderPlaceholder(uiState: DocsUiState) {
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
                        docsStatusColor(uiState.loadStatus).copy(alpha = 0.10f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        docsStatusColor(uiState.loadStatus).copy(alpha = 0.18f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FeatherIcons.AlertCircle,
                    contentDescription = null,
                    tint = docsStatusColor(uiState.loadStatus),
                    modifier = Modifier.size(26.dp)
                )
            }

            FluentText(
                text = docsStatusText(uiState),
                style = FluentTheme.typography.body,
                color = docsStatusColor(uiState.loadStatus),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (uiState.loadStatus == DocsLoadStatus.Error && uiState.errorMessage.isNotBlank()) {
                FluentText(
                    text = stringResource(Res.string.docsErrorRead, uiState.errorMessage),
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
            imageVector = FeatherIcons.AlertCircle,
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
private fun DocsTocRow(
    item: DocTocItem,
    onClick: () -> Unit,
) {
    val indent = ((item.level - 1).coerceAtLeast(0) * 12).dp
    FluentText(
        text = item.title,
        style = FluentTheme.typography.caption.copy(
            fontWeight = if (item.level <= 1) FontWeight.SemiBold else FontWeight.Normal,
        ),
        color = if (item.level <= 2) {
            FluentTheme.colors.text.text.primary
        } else {
            FluentTokens.ColorToken.LogLevel.veryVerbose
        },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = indent, top = 4.dp, bottom = 4.dp, end = 4.dp)
    )
}

@Composable
private fun docsStatusText(uiState: DocsUiState): String {
    return when (uiState.loadStatus) {
        DocsLoadStatus.MissingGamePath -> stringResource(Res.string.docsMissingGamePath)
        DocsLoadStatus.MissingDocsDirectory -> stringResource(Res.string.docsMissingDirectory)
        DocsLoadStatus.Empty -> stringResource(Res.string.docsEmptyDirectory)
        DocsLoadStatus.Ready -> uiState.selectedDocument?.relativePath
            ?: stringResource(Res.string.screen_doc)
        DocsLoadStatus.Error -> stringResource(Res.string.docsErrorRead, uiState.errorMessage.ifBlank { "-" })
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

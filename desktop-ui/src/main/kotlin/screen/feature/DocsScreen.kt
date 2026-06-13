package screen.feature

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.File
import core.docs.DocsLoadStatus
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_doc
import navigation.SharedScreen
import navigation.screenRoute
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import screen.docs.DocsListPanel
import screen.docs.DocsOverview
import screen.docs.DocsReaderPanel
import screen.docs.DocsTableOfContents
import viewModel.DocsScreenModel

/**
 * 文档中心页。
 *
 * 展示从 `honkai_rts/docs/` 目录扫描的 Markdown 文档，
 * 支持文档列表浏览、文档内链接导航、Markdown 渲染和锚点跳转。
 * 数据与导航逻辑由 [DocsScreenModel] 管理。
 */
class DocsScreen : Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return screenRoute(SharedScreen.Docs)
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
        val uiState = screenModel.uiState

        val documentPath = uiState.selectedDocument?.absolutePath
        val renderedMarkdown = remember(uiState.markdownContent, documentPath) {
            normalizeMarkdownLineEndings(
                rewriteMarkdownResourceLinks(
                    markdown = uiState.markdownContent,
                    currentDocumentPath = documentPath,
                )
            )
        }
        val (tocItems, headingSlugs) = remember(renderedMarkdown) {
            extractDocHeadings(renderedMarkdown)
        }
        val readerScrollState = remember(documentPath) { ScrollState(0) }
        val readerController = remember(documentPath, readerScrollState) {
            DocsReaderController(readerScrollState)
        }

        LaunchedEffect(documentPath, uiState.pendingAnchor, readerController.registeredCount) {
            val anchor = uiState.pendingAnchor ?: return@LaunchedEffect
            if (readerController.resolveAnchor(anchor) != null) {
                readerController.scrollToAnchor(anchor)
                screenModel.consumePendingAnchor()
            }
        }

        val showToc = uiState.selectedDocument != null &&
            uiState.loadStatus == DocsLoadStatus.Ready &&
            tocItems.isNotEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DocsOverview(
                uiState = uiState,
                icon = getIcon(),
                onRefresh = { screenModel.refresh() },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DocsListPanel(
                    uiState = uiState,
                    onSelectDocument = { screenModel.selectDocument(it) },
                    modifier = Modifier
                        .weight(0.92f)
                        .fillMaxHeight(),
                )

                DocsReaderPanel(
                    uiState = uiState,
                    screenModel = screenModel,
                    renderedMarkdown = renderedMarkdown,
                    headingSlugs = headingSlugs,
                    readerScrollState = readerScrollState,
                    readerController = readerController,
                    modifier = Modifier
                        .weight(1.7f)
                        .fillMaxHeight(),
                )

                if (showToc) {
                    DocsTableOfContents(
                        items = tocItems,
                        controller = readerController,
                        modifier = Modifier
                            .weight(0.7f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

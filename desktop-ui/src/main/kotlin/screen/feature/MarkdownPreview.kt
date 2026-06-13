package screen.feature

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import viewModel.DocsScreenModel

@Composable
internal fun MarkdownPreview(
    screenModel: DocsScreenModel,
    markdown: String,
    controller: DocsReaderController,
    headingSlugs: Map<Int, String>,
    modifier: Modifier = Modifier,
) {
    val astRoot = rememberMarkdownAst(markdown)

    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val linkListener = remember(screenModel, controller, uriHandler, scope) {
        LinkInteractionListener { annotation ->
            val href = (annotation as? LinkAnnotation.Clickable)?.tag ?: return@LinkInteractionListener
            when {
                href.startsWith("#") ->
                    scope.launch { controller.scrollToAnchor(href.removePrefix("#")) }

                href.hasKnownScheme() ->
                    runCatching { uriHandler.openUri(href) }

                else -> {
                    if (!screenModel.openLinkedDocument(href)) {
                        runCatching { uriHandler.openUri(href) }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalDocsReaderController provides controller,
        LocalDocsHeadingSlugs provides headingSlugs,
        LocalMarkdownLinkListener provides linkListener,
    ) {
        FluentMarkdownDocument(
            root = astRoot,
            source = markdown,
            style = rememberDocsMarkdownStyle(),
            modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}


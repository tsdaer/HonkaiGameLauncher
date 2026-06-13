package screen.feature

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Globe
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.ExternalLink
import compose.icons.feathericons.RefreshCw
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_website
import honkaigamelauncher.desktop_ui.generated.resources.websiteActionGo
import honkaigamelauncher.desktop_ui.generated.resources.websiteActionOpenExternal
import honkaigamelauncher.desktop_ui.generated.resources.websiteAddressPlaceholder
import honkaigamelauncher.desktop_ui.generated.resources.websiteErrorTitle
import honkaigamelauncher.desktop_ui.generated.resources.websiteLinkGithub
import honkaigamelauncher.desktop_ui.generated.resources.websiteLinkOfficial
import honkaigamelauncher.desktop_ui.generated.resources.websiteLoading
import honkaigamelauncher.desktop_ui.generated.resources.websiteReady
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarSeverity
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.TextField
import navigation.SharedScreen
import navigation.screenRoute
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import ui.components.WebEngineInitContent
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.settings.LocalNavExpanded
import viewModel.WebScreenModel
import viewModel.WebScreenModel.Companion.normalizeUrl
import io.github.composefluent.component.Text as FluentText

/**
 * 内置网页工具页。
 *
 * 基于 KCEF 嵌入式 Chromium WebView 提供浏览器能力。
 * 支持地址栏导航、前进/后退、外部链接打开和 WebEngine 初始化状态展示。
 * 数据由 [WebScreenModel] 管理，引擎初始化由 [WebEngineService] 负责。
 */
class WebScreen: Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return screenRoute(SharedScreen.Web)
    }

    override fun getIcon(): ImageVector {
        return EvaIcons.Fill.Globe
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_website)
    }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { WebScreenModel() }

        if (!screenModel.webEngineReady) {
            WebEngineInitContent(
                phase = screenModel.webEnginePhase,
                progress = screenModel.webEngineProgress,
                errorMessage = screenModel.webEngineError,
                restartRequired = screenModel.webEngineRestartRequired,
                onRetry = { screenModel.retryWebEngineInit() }
            )
            return
        }

        val webViewState = rememberWebViewState(WebScreenModel.HOME_URL)
        val navigator = rememberWebViewNavigator()
        val uriHandler = LocalUriHandler.current

        fun loadUrl(rawUrl: String) {
            navigator.loadUrl(screenModel.prepareLoadUrl(rawUrl))
        }

        LaunchedEffect(webViewState.lastLoadedUrl) {
            screenModel.updateAddressFromLoadedUrl(webViewState.lastLoadedUrl)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FluentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1200.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = getIcon(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        FluentText(
                            text = webViewState.pageTitle?.takeIf { it.isNotBlank() }
                                ?: stringResource(Res.string.screen_website),
                            style = FluentTheme.typography.subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        WebStatusText(webViewState.loadingState)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FluentButton(
                            onClick = { navigator.navigateBack() },
                            disabled = !navigator.canGoBack,
                            iconOnly = true
                        ) {
                            Icon(compose.icons.FeatherIcons.ArrowLeft, contentDescription = null)
                        }
                        FluentButton(
                            onClick = { navigator.navigateForward() },
                            disabled = !navigator.canGoForward,
                            iconOnly = true
                        ) {
                            Icon(compose.icons.FeatherIcons.ArrowRight, contentDescription = null)
                        }
                        FluentButton(
                            onClick = { navigator.reload() },
                            iconOnly = true
                        ) {
                            Icon(compose.icons.FeatherIcons.RefreshCw, contentDescription = null)
                        }
                        TextField(
                            value = screenModel.address,
                            onValueChange = { screenModel.updateAddress(it) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = {
                                FluentText(stringResource(Res.string.websiteAddressPlaceholder))
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Go,
                                keyboardType = KeyboardType.Uri
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { loadUrl(screenModel.address) }
                            )
                        )
                        FluentButton(onClick = { loadUrl(screenModel.address) }) {
                            FluentText(stringResource(Res.string.websiteActionGo))
                        }
                        FluentButton(
                            onClick = { uriHandler.openUri(normalizeUrl(screenModel.address)) },
                            iconOnly = true
                        ) {
                            Icon(compose.icons.FeatherIcons.ExternalLink, contentDescription = null)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickLinkButton(
                            text = stringResource(Res.string.websiteLinkOfficial),
                            url = WebScreenModel.HOME_URL,
                            onOpen = ::loadUrl
                        )
                        QuickLinkButton(
                            text = stringResource(Res.string.websiteLinkGithub),
                            url = "https://github.com/tsdaer",
                            onOpen = ::loadUrl
                        )
                        FluentButton(onClick = { uriHandler.openUri(normalizeUrl(screenModel.address)) }) {
                            FluentText(stringResource(Res.string.websiteActionOpenExternal))
                        }
                    }

                    LoadingProgress(webViewState.loadingState)
                }
            }

            webViewState.errorsForCurrentRequest.lastOrNull { it.isFromMainFrame }?.let { error ->
                InfoBar(
                    title = { FluentText(stringResource(Res.string.websiteErrorTitle)) },
                    message = { FluentText("${error.code}: ${error.description}") },
                    severity = InfoBarSeverity.Warning,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val navExpanded = LocalNavExpanded.current
            if (!navExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1200.dp)
                        .clipToBounds()
                ) {
                    WebView(
                        state = webViewState,
                        navigator = navigator,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun WebStatusText(loadingState: LoadingState) {
    val status = when (loadingState) {
        LoadingState.Initializing -> stringResource(Res.string.websiteLoading)
        is LoadingState.Loading -> "${stringResource(Res.string.websiteLoading)} ${(loadingState.progress * 100).toInt()}%"
        LoadingState.Finished -> stringResource(Res.string.websiteReady)
    }
    FluentText(
        text = status,
        style = FluentTheme.typography.caption
    )
}

@Composable
private fun LoadingProgress(loadingState: LoadingState) {
    when (loadingState) {
        LoadingState.Initializing -> ProgressBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        )
        is LoadingState.Loading -> ProgressBar(
            progress = loadingState.progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        )
        LoadingState.Finished -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        )
    }
}

@Composable
private fun QuickLinkButton(
    text: String,
    url: String,
    onOpen: (String) -> Unit
) {
    FluentButton(onClick = { onOpen(url) }) {
        Icon(EvaIcons.Fill.Globe, contentDescription = null)
        FluentText(text)
    }
}

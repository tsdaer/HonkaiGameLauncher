package screen.feature

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import dev.datlag.kcef.KCEF
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_website
import honkaigamelauncher.desktop_ui.generated.resources.websiteActionGo
import honkaigamelauncher.desktop_ui.generated.resources.websiteActionOpenExternal
import honkaigamelauncher.desktop_ui.generated.resources.websiteAddressPlaceholder
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineChecking
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineDownloading
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineDownloadFinishing
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineErrorTitle
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineExtracting
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineInitializing
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineInstalling
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineReady
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineRestartRequired
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineRetry
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
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.composefluent.component.Text as FluentText


class WebScreen: Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return "web"
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
        val homeUrl = "https://www.honkai-rts.com"
        var webEngineReady by remember { mutableStateOf(false) }
        var webEnginePhase by remember { mutableStateOf(WebEnginePhase.Initializing) }
        var webEngineProgress by remember { mutableStateOf<Float?>(null) }
        var webEngineError by remember { mutableStateOf<String?>(null) }
        var webEngineRestartRequired by remember { mutableStateOf(false) }
        var initAttempt by remember { mutableStateOf(0) }
        val initScope = rememberCoroutineScope()

        LaunchedEffect(initAttempt) {
            webEngineReady = false
            webEngineError = null
            webEngineRestartRequired = false
            webEnginePhase = WebEnginePhase.Initializing
            webEngineProgress = null

            var initError: String? = null
            var restartRequired = false

            try {
                withContext(Dispatchers.IO) {
                    KCEF.init(
                        builder = {
                            installDir(kcefDataDir("bundle"))
                            settings {
                                cachePath = kcefDataDir("cache").absolutePath
                            }
                            progress {
                                onLocating {
                                    initScope.launch {
                                        webEnginePhase = WebEnginePhase.Checking
                                        webEngineProgress = null
                                    }
                                }
                                onDownloading { progress ->
                                    initScope.launch {
                                        val normalizedProgress = progress.coerceIn(0f, 1f)
                                        webEnginePhase = if (normalizedProgress >= 0.999f) {
                                            WebEnginePhase.DownloadFinishing
                                        } else {
                                            WebEnginePhase.Downloading
                                        }
                                        webEngineProgress = normalizedProgress
                                    }
                                }
                                onExtracting {
                                    initScope.launch {
                                        webEnginePhase = WebEnginePhase.Extracting
                                        webEngineProgress = null
                                    }
                                }
                                onInstall {
                                    initScope.launch {
                                        webEnginePhase = WebEnginePhase.Installing
                                        webEngineProgress = null
                                    }
                                }
                                onInitializing {
                                    initScope.launch {
                                        webEnginePhase = WebEnginePhase.Initializing
                                        webEngineProgress = null
                                    }
                                }
                                onInitialized {
                                    initScope.launch {
                                        webEnginePhase = WebEnginePhase.Ready
                                        webEngineProgress = 1f
                                    }
                                }
                            }
                        },
                        onError = { throwable ->
                            initError = throwable?.message
                                ?: throwable?.javaClass?.simpleName
                                ?: "Unknown error"
                        },
                        onRestartRequired = {
                            restartRequired = true
                        }
                    )
                }
                webEngineRestartRequired = restartRequired
                webEngineError = initError
                webEngineReady = !restartRequired && initError == null
            } catch (throwable: Throwable) {
                webEngineError = throwable.message ?: throwable.javaClass.simpleName
            }
        }

        if (!webEngineReady) {
            WebEngineInitContent(
                phase = webEnginePhase,
                progress = webEngineProgress,
                errorMessage = webEngineError,
                restartRequired = webEngineRestartRequired,
                onRetry = { initAttempt++ }
            )
            return
        }

        val webViewState = rememberWebViewState(homeUrl)
        val navigator = rememberWebViewNavigator()
        val uriHandler = LocalUriHandler.current
        var address by remember { mutableStateOf(homeUrl) }

        fun loadUrl(rawUrl: String) {
            val targetUrl = normalizeUrl(rawUrl)
            address = targetUrl
            navigator.loadUrl(targetUrl)
        }

        LaunchedEffect(webViewState.lastLoadedUrl) {
            webViewState.lastLoadedUrl?.let { address = it }
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
                            value = address,
                            onValueChange = { address = it },
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
                                onGo = { loadUrl(address) }
                            )
                        )
                        FluentButton(onClick = { loadUrl(address) }) {
                            FluentText(stringResource(Res.string.websiteActionGo))
                        }
                        FluentButton(
                            onClick = { uriHandler.openUri(normalizeUrl(address)) },
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
                            url = homeUrl,
                            onOpen = ::loadUrl
                        )
                        QuickLinkButton(
                            text = stringResource(Res.string.websiteLinkGithub),
                            url = "https://github.com/tsdaer",
                            onOpen = ::loadUrl
                        )
                        FluentButton(onClick = { uriHandler.openUri(normalizeUrl(address)) }) {
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1200.dp)
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

private enum class WebEnginePhase {
    Checking,
    Downloading,
    DownloadFinishing,
    Extracting,
    Installing,
    Initializing,
    Ready
}

@Composable
private fun WebEngineInitContent(
    phase: WebEnginePhase,
    progress: Float?,
    errorMessage: String?,
    restartRequired: Boolean,
    onRetry: () -> Unit
) {
    val message = when (phase) {
        WebEnginePhase.Checking -> stringResource(Res.string.websiteEngineChecking)
        WebEnginePhase.Downloading -> stringResource(Res.string.websiteEngineDownloading)
        WebEnginePhase.DownloadFinishing -> stringResource(Res.string.websiteEngineDownloadFinishing)
        WebEnginePhase.Extracting -> stringResource(Res.string.websiteEngineExtracting)
        WebEnginePhase.Installing -> stringResource(Res.string.websiteEngineInstalling)
        WebEnginePhase.Initializing -> stringResource(Res.string.websiteEngineInitializing)
        WebEnginePhase.Ready -> stringResource(Res.string.websiteEngineReady)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FluentCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FluentText(
                    text = message,
                    style = FluentTheme.typography.subtitle
                )
                if (errorMessage == null && !restartRequired) {
                    if (progress == null) {
                        ProgressBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    } else {
                        ProgressBar(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    }
                } else {
                    InfoBar(
                        title = { FluentText(stringResource(Res.string.websiteEngineErrorTitle)) },
                        message = {
                            FluentText(
                                if (restartRequired) {
                                    stringResource(Res.string.websiteEngineRestartRequired)
                                } else {
                                    errorMessage.orEmpty()
                                }
                            )
                        },
                        severity = InfoBarSeverity.Warning,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FluentButton(onClick = onRetry) {
                        FluentText(stringResource(Res.string.websiteEngineRetry))
                    }
                }
            }
        }
    }
}

private fun kcefDataDir(name: String): File {
    val baseDir = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let { localAppData ->
        File(localAppData, "HonkaiGameLauncher")
    } ?: File(System.getProperty("user.home"), ".HonkaiGameLauncher")

    return File(baseDir, "kcef-$name")
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

private fun normalizeUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) {
        return "https://www.honkai-rts.com"
    }
    return if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

package screen.feature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_website
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Globe
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface


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
        val webViewState = rememberWebViewState("https://google.com")

        Column(Modifier.fillMaxSize()) {
            val text = webViewState.let {
                "${it.pageTitle ?: ""} ${it.loadingState} ${it.lastLoadedUrl ?: ""}"
            }
            Text(text)
            WebView(
                state = webViewState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
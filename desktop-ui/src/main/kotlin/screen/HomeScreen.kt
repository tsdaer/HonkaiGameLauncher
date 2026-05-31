package screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_home
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Home
import org.jetbrains.compose.resources.stringResource

class HomeScreen: Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return "home"
    }

    override fun getIcon(): ImageVector {
        return EvaIcons.Fill.Home
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_home)
    }

    @Composable
    override fun Content() {

    }
}
package screen.feature

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_plugin
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.PlugSolid
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface

class PluginScreen: Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return "plugin"
    }

    override fun getIcon(): ImageVector {
        return LineAwesomeIcons.PlugSolid
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_plugin)
    }

    @Composable
    override fun Content() {
        Text("插件界面")
    }
}
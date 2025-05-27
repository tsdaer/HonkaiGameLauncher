package screen.feature

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import com.honkai_rts.honkaigamelauncher.generated.resources.Res
import com.honkai_rts.honkaigamelauncher.generated.resources.screen_plugin
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.PlugSolid
import org.jetbrains.compose.resources.stringResource
import util.IScreenInterface

class PluginScreen: Screen, IScreenInterface {

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
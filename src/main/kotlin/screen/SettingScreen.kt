package screen

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import com.honkai_rts.honkaigamelauncher.generated.resources.Res
import com.honkai_rts.honkaigamelauncher.generated.resources.setting
import compose.icons.feathericons.Settings
import org.jetbrains.compose.resources.stringResource
import util.IScreenInterface

class SettingScreen: Screen, IScreenInterface {

    override fun getUrl(): String {
        return "setting"
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.setting)
    }

    override fun getIcon(): ImageVector {
        return compose.icons.FeatherIcons.Settings
    }

    @Composable
    override fun Content() {
        Text("设置界面")
    }


}
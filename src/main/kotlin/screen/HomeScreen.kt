package screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.honkai_rts.honkaigamelauncher.generated.resources.Res
import com.honkai_rts.honkaigamelauncher.generated.resources.screen_home
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Folder
import compose.icons.evaicons.fill.Home
import org.jetbrains.compose.resources.stringResource
import ui.components.SettingsCard

class HomeScreen: Screen, IScreenInterface {
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
        val screenModel = rememberScreenModel{HomeScreenModel()}
        Column() {
            SettingsCard("测试", EvaIcons.Fill.Folder)
            {
                Button(onClick = {
                    screenModel.setGamePath()
                })
                {
                    Text(screenModel.gamePath)
                }
            }
        }
    }
}
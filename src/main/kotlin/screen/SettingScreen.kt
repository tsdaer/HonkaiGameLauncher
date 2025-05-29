package screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.honkai_rts.honkaigamelauncher.generated.resources.Res
import com.honkai_rts.honkaigamelauncher.generated.resources.setGamePath
import com.honkai_rts.honkaigamelauncher.generated.resources.setting
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Folder
import compose.icons.feathericons.Settings
import org.jetbrains.compose.resources.stringResource
import ui.components.SettingsCard

class SettingScreen: Screen, IScreenInterface {

    override val key = uniqueScreenKey

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
        val screenModel = rememberScreenModel{ SettingScreenModel()}
        val state = rememberScrollState(0)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
            SettingsCard(stringResource(Res.string.setGamePath), EvaIcons.Fill.Folder) {
                Button(onClick = { screenModel.setGamePath() }) {
                    Text(screenModel.gamePath)
                }
            }
        }
    }
}
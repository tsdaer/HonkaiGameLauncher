package screen.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import compose.icons.lineawesomeicons.PuzzlePieceSolid
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_plugin
import navigation.SharedScreen
import navigation.screenRoute
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import screen.plugin.PluginListPanel
import screen.plugin.PluginOverview
import viewModel.PluginScreenModel

/**
 * 插件配置页。
 *
 * 展示从 `GamePluginConfigs.toml` 加载的插件列表，包括插件名称、类型、路径、
 * 挂载顺序和默认启用状态。数据由 [PluginScreenModel] 管理。
 */
class PluginScreen : Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return screenRoute(SharedScreen.Plugin)
    }

    override fun getIcon(): ImageVector {
        return compose.icons.LineAwesomeIcons.PuzzlePieceSolid
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_plugin)
    }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { PluginScreenModel() }
        val uiState = screenModel.uiState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PluginOverview(
                uiState = uiState,
                icon = getIcon(),
                onRefresh = { screenModel.refresh() },
            )

            PluginListPanel(
                uiState = uiState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

package screen.feature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_doc
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.File
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import ui.fluent.components.FluentSection
import io.github.composefluent.component.Text as FluentText

class DocsScreen: Screen, IScreenInterface{

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return "docs"
    }

    override fun getIcon(): ImageVector {
        return EvaIcons.Fill.File
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_doc)
    }

    @Composable
    override fun Content() {
        Column(modifier = Modifier.fillMaxSize()) {
            FluentSection(
                title = stringResource(Res.string.screen_doc),
                icon = getIcon()
            ) {
                FluentText("文档界面")
            }
        }
    }

}

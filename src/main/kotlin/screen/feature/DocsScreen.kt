package screen.feature

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.honkai_rts.honkaigamelauncher.generated.resources.Res
import com.honkai_rts.honkaigamelauncher.generated.resources.screen_doc
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.File
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface

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
        Text("文档界面")
    }

}
package screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

interface IScreenInterface {

    fun getUrl(): String

    fun getIcon(): ImageVector

    @Composable
    fun getTitle(): String

}
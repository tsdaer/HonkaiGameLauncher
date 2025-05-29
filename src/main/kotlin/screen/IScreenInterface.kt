package screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

interface IScreenInterface {

    fun getUrl(): String

    fun getIcon(): ImageVector

    fun hideTitle(): Boolean {
        return false
    }

    fun diableAnimation(): Boolean {
        return false
    }

    @Composable
    fun getTitle(): String

}
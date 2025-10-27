package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch

class SettingScreenModel(
    val settings: Settings = Settings(),
) : ScreenModel {

    var gamePath by mutableStateOf(settings.getString("gamePath", "null"))

    fun setGamePath() {
        screenModelScope.launch {
            val file = FileKit.openFilePicker()
            if(file != null) {
                settings.putString("gamePath", file.path)
                gamePath = file.path // 更新状态值
            }
        }
    }
}
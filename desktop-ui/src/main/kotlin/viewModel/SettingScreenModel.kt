package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.platform.AppSettingsRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch

data class SettingUiState(
    val gamePath: String? = null,
)

class SettingScreenModel(
    private val settingsRepository: AppSettingsRepository = SettingsAppSettingsRepository(),
) : ScreenModel {

    var uiState by mutableStateOf(SettingUiState(gamePath = settingsRepository.getGamePath()))
        private set

    fun setGamePath() {
        screenModelScope.launch {
            val file = FileKit.openFilePicker()
            if(file != null) {
                settingsRepository.setGamePath(file.path)
                uiState = uiState.copy(gamePath = settingsRepository.getGamePath())
            }
        }
    }
}

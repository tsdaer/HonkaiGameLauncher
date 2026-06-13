package viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SettingUiState(
    val gamePath: String? = null,
)

class SettingScreenModel(
    private val settingsStore: AppSettingsStore = SharedAppSettingsStore.instance,
) : ScreenModel {

    var uiState by mutableStateOf(SettingUiState(gamePath = settingsStore.state.value.gamePath))
        private set

    init {
        screenModelScope.launch {
            settingsStore.state
                .map { it.gamePath }
                .distinctUntilChanged()
                .collect { gamePath ->
                    uiState = uiState.copy(gamePath = gamePath)
                }
        }
    }

    fun setGamePath() {
        screenModelScope.launch {
            val file = FileKit.openFilePicker()
            if(file != null) {
                settingsStore.setGamePath(file.path)
            }
        }
    }
}

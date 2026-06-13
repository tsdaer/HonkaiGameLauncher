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

/**
 * 设置页 UI 状态。
 *
 * @property gamePath 当前设置的游戏 exe 路径
 */
data class SettingUiState(
    val gamePath: String? = null,
)

/**
 * 设置页 ScreenModel。
 *
 * 管理游戏路径选择和持久化。监听 [AppSettingsStore] 的 gamePath 变化
 * 以保持 UI 与实际设置同步。
 */
class SettingScreenModel(
    private val settingsStore: AppSettingsStore = SharedAppSettingsStore.instance,
) : ScreenModel {

    var uiState by mutableStateOf(SettingUiState(gamePath = settingsStore.state.value.gamePath))
        private set

    init {
        // 监听 gamePath 变化，保持 UI 同步
        screenModelScope.launch {
            settingsStore.state
                .map { it.gamePath }
                .distinctUntilChanged()
                .collect { gamePath ->
                    uiState = uiState.copy(gamePath = gamePath)
                }
        }
    }

    /** 打开文件选择器设置游戏路径 */
    fun setGamePath() {
        screenModelScope.launch {
            val file = FileKit.openFilePicker()
            if(file != null) {
                settingsStore.setGamePath(file.path)
            }
        }
    }
}

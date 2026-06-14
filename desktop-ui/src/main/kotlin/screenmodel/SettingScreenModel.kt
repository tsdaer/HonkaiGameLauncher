package screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import core.config.GameColorSetting
import core.config.GameRgbaColor
import core.config.GameSettings
import core.config.GameSettingsService
import core.config.GameSettingsStatus
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.settings.AppSettingsStore
import ui.settings.SharedAppSettingsStore

/**
 * 设置页 UI 状态。
 *
 * @property gamePath 当前设置的游戏 exe 路径
 */
data class SettingUiState(
    val gamePath: String? = null,
    val gameSettingsForm: GameSettingsForm? = null,
    val gameSettingsStatus: SettingGameSettingsStatus = SettingGameSettingsStatus.Loading,
    val gameSettingsConfigDirectory: String = "",
    val gameSettingsMessage: String = "",
)

enum class SettingGameSettingsStatus {
    Loading,
    Ready,
    Saving,
    Saved,
    MissingGamePath,
    MissingConfig,
    Error,
}

data class GameSettingsForm(
    val language: String = "zh-Hans",
    val fullscreenMode: String = "Fullscreen",
    val dynamicResolution: Boolean = false,
    val useOverallScalabilityLevel: Boolean = true,
    val overallScalabilityLevel: String = "2",
    val audioQualityLevel: String = "0",
    val hdrDisplay: Boolean = false,
    val frameRateLimit: String = "0.0",
    val motionBlurAmount: String = "0.0",
    val motionBlurMax: String = "-1.0",
    val motionBlurScale: String = "1.0",
    val motionBlurTargetFps: String = "30.0",
    val resolutionWidth: String = "1280",
    val resolutionHeight: String = "720",
    val vSync: Boolean = false,
    val resolutionScale: String = "75.0",
    val upscalerName: String = "TSR",
    val antiAliasingQuality: String = "2",
    val fsr: String = "0",
    val frameGeneration: String = "0",
    val frameWarp: String = "0",
    val nis: String = "3",
    val nisSharpness: String = "0",
    val rayReconstruction: String = "0",
    val reflex: String = "0",
    val superResolution: String = "0",
    val textureQuality: String = "2",
    val shadowQuality: String = "2",
    val viewDistanceQuality: String = "2",
    val visualEffectQuality: String = "2",
    val globalIlluminationQuality: String = "2",
    val reflectionQuality: String = "2",
    val postProcessingQuality: String = "2",
    val foliageQuality: String = "2",
    val shadingQuality: String = "2",
    val mainVolume: String = "100.0",
    val musicVolume: String = "100.0",
    val voiceVolume: String = "100.0",
    val envVolume: String = "100.0",
    val widgetVolume: String = "100.0",
    val defaultAudioInputDeviceName: String = "",
    val defaultAudioOutputDeviceName: String = "",
    val defaultMusicPlayerFolder: String = "",
    val animBudgetAllocator: String = "1.0",
    val cameraArmScale: String = "1.0",
    val cameraMoveLag: String = "0.1",
    val cameraMoveSpeedScale: String = "1.0",
    val cameraRotationScale: String = "1.0",
    val sceneElementUpdate: String = "1.0",
    val serverMovePathFindPipeNum: String = "12",
    val unitFindPipeNum: String = "12",
    val colors: List<GameColorForm> = emptyList(),
) {
    fun toGameSettingsOrNull(): GameSettings? {
        return GameSettings(
            language = language,
            fullscreenMode = fullscreenMode,
            dynamicResolution = dynamicResolution,
            useOverallScalabilityLevel = useOverallScalabilityLevel,
            overallScalabilityLevel = overallScalabilityLevel.toIntOrNull() ?: return null,
            audioQualityLevel = audioQualityLevel.toIntOrNull() ?: return null,
            hdrDisplay = hdrDisplay,
            frameRateLimit = frameRateLimit.toFiniteDoubleOrNull() ?: return null,
            motionBlurAmount = motionBlurAmount.toFiniteDoubleOrNull() ?: return null,
            motionBlurMax = motionBlurMax.toFiniteDoubleOrNull() ?: return null,
            motionBlurScale = motionBlurScale.toFiniteDoubleOrNull() ?: return null,
            motionBlurTargetFps = motionBlurTargetFps.toFiniteDoubleOrNull() ?: return null,
            resolutionWidth = resolutionWidth.toIntOrNull()?.takeIf { it > 0 } ?: return null,
            resolutionHeight = resolutionHeight.toIntOrNull()?.takeIf { it > 0 } ?: return null,
            vSync = vSync,
            resolutionScale = resolutionScale.toFiniteDoubleOrNull() ?: return null,
            upscalerName = upscalerName,
            antiAliasingQuality = antiAliasingQuality.toIntOrNull() ?: return null,
            fsr = fsr.toIntOrNull() ?: return null,
            frameGeneration = frameGeneration.toIntOrNull() ?: return null,
            frameWarp = frameWarp.toIntOrNull() ?: return null,
            nis = nis.toIntOrNull() ?: return null,
            nisSharpness = nisSharpness.toIntOrNull() ?: return null,
            rayReconstruction = rayReconstruction.toIntOrNull() ?: return null,
            reflex = reflex.toIntOrNull() ?: return null,
            superResolution = superResolution.toIntOrNull() ?: return null,
            textureQuality = textureQuality.toIntOrNull() ?: return null,
            shadowQuality = shadowQuality.toIntOrNull() ?: return null,
            viewDistanceQuality = viewDistanceQuality.toIntOrNull() ?: return null,
            visualEffectQuality = visualEffectQuality.toIntOrNull() ?: return null,
            globalIlluminationQuality = globalIlluminationQuality.toIntOrNull() ?: return null,
            reflectionQuality = reflectionQuality.toIntOrNull() ?: return null,
            postProcessingQuality = postProcessingQuality.toIntOrNull() ?: return null,
            foliageQuality = foliageQuality.toIntOrNull() ?: return null,
            shadingQuality = shadingQuality.toIntOrNull() ?: return null,
            mainVolume = mainVolume.toFiniteDoubleOrNull() ?: return null,
            musicVolume = musicVolume.toFiniteDoubleOrNull() ?: return null,
            voiceVolume = voiceVolume.toFiniteDoubleOrNull() ?: return null,
            envVolume = envVolume.toFiniteDoubleOrNull() ?: return null,
            widgetVolume = widgetVolume.toFiniteDoubleOrNull() ?: return null,
            defaultAudioInputDeviceName = defaultAudioInputDeviceName,
            defaultAudioOutputDeviceName = defaultAudioOutputDeviceName,
            defaultMusicPlayerFolder = defaultMusicPlayerFolder,
            animBudgetAllocator = animBudgetAllocator.toFiniteDoubleOrNull() ?: return null,
            cameraArmScale = cameraArmScale.toFiniteDoubleOrNull() ?: return null,
            cameraMoveLag = cameraMoveLag.toFiniteDoubleOrNull() ?: return null,
            cameraMoveSpeedScale = cameraMoveSpeedScale.toFiniteDoubleOrNull() ?: return null,
            cameraRotationScale = cameraRotationScale.toFiniteDoubleOrNull() ?: return null,
            sceneElementUpdate = sceneElementUpdate.toFiniteDoubleOrNull() ?: return null,
            serverMovePathFindPipeNum = serverMovePathFindPipeNum.toIntOrNull() ?: return null,
            unitFindPipeNum = unitFindPipeNum.toIntOrNull() ?: return null,
            colors = colors.map { color ->
                GameColorSetting(
                    name = color.name,
                    value = GameRgbaColor(
                        red = color.red,
                        green = color.green,
                        blue = color.blue,
                        alpha = color.alpha,
                    ),
                )
            },
        )
    }
}

data class GameColorForm(
    val name: String,
    val red: Double,
    val green: Double,
    val blue: Double,
    val alpha: Double,
)

private fun String.toFiniteDoubleOrNull(): Double? {
    return toDoubleOrNull()?.takeIf { it.isFinite() }
}

/**
 * 设置页 ScreenModel。
 *
 * 管理游戏路径选择和持久化。监听 [AppSettingsStore] 的 gamePath 变化
 * 以保持 UI 与实际设置同步。
 */
class SettingScreenModel(
    private val settingsStore: AppSettingsStore = SharedAppSettingsStore.instance,
    private val gameSettingsService: GameSettingsService = GameSettingsService(),
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
                    loadGameSettings(gamePath)
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

    fun refreshGameSettings() {
        loadGameSettings(settingsStore.state.value.gamePath)
    }

    /** 打开目录选择器，选中后把路径写入音乐目录字段 */
    fun pickMusicFolder(current: GameSettingsForm) {
        screenModelScope.launch {
            val dir = FileKit.openDirectoryPicker()
            if (dir != null) {
                updateGameSettingsForm(current.copy(defaultMusicPlayerFolder = dir.path))
            }
        }
    }

    fun updateGameSettingsForm(form: GameSettingsForm) {
        uiState = uiState.copy(
            gameSettingsForm = form,
            gameSettingsStatus = if (uiState.gameSettingsStatus == SettingGameSettingsStatus.Saved) {
                SettingGameSettingsStatus.Ready
            } else {
                uiState.gameSettingsStatus
            },
            gameSettingsMessage = "",
        )
    }

    fun saveGameSettings() {
        val form = uiState.gameSettingsForm ?: return
        val settings = form.toGameSettingsOrNull()
        if (settings == null) {
            uiState = uiState.copy(
                gameSettingsStatus = SettingGameSettingsStatus.Error,
                gameSettingsMessage = "invalid-number",
            )
            return
        }

        screenModelScope.launch {
            uiState = uiState.copy(gameSettingsStatus = SettingGameSettingsStatus.Saving, gameSettingsMessage = "")
            val result = withContext(Dispatchers.IO) {
                gameSettingsService.save(settingsStore.state.value.gamePath, settings)
            }
            uiState = uiState.copy(
                gameSettingsForm = result.settings?.toForm() ?: uiState.gameSettingsForm,
                gameSettingsStatus = if (result.status == GameSettingsStatus.Ready) {
                    SettingGameSettingsStatus.Saved
                } else {
                    result.status.toUiStatus()
                },
                gameSettingsConfigDirectory = result.configDirectory,
                gameSettingsMessage = result.message,
            )
        }
    }

    private fun loadGameSettings(path: String?) {
        screenModelScope.launch {
            uiState = uiState.copy(gameSettingsStatus = SettingGameSettingsStatus.Loading, gameSettingsMessage = "")
            val result = withContext(Dispatchers.IO) {
                gameSettingsService.load(path)
            }
            uiState = uiState.copy(
                gameSettingsForm = result.settings?.toForm(),
                gameSettingsStatus = result.status.toUiStatus(),
                gameSettingsConfigDirectory = result.configDirectory,
                gameSettingsMessage = result.message,
            )
        }
    }

    private fun GameSettingsStatus.toUiStatus(): SettingGameSettingsStatus {
        return when (this) {
            GameSettingsStatus.Ready -> SettingGameSettingsStatus.Ready
            GameSettingsStatus.MissingGamePath -> SettingGameSettingsStatus.MissingGamePath
            GameSettingsStatus.MissingConfig -> SettingGameSettingsStatus.MissingConfig
            GameSettingsStatus.Error -> SettingGameSettingsStatus.Error
        }
    }

    private fun GameSettings.toForm(): GameSettingsForm {
        return GameSettingsForm(
            language = language,
            fullscreenMode = fullscreenMode,
            dynamicResolution = dynamicResolution,
            useOverallScalabilityLevel = useOverallScalabilityLevel,
            overallScalabilityLevel = overallScalabilityLevel.toString(),
            audioQualityLevel = audioQualityLevel.toString(),
            hdrDisplay = hdrDisplay,
            frameRateLimit = frameRateLimit.toString(),
            motionBlurAmount = motionBlurAmount.toString(),
            motionBlurMax = motionBlurMax.toString(),
            motionBlurScale = motionBlurScale.toString(),
            motionBlurTargetFps = motionBlurTargetFps.toString(),
            resolutionWidth = resolutionWidth.toString(),
            resolutionHeight = resolutionHeight.toString(),
            vSync = vSync,
            resolutionScale = resolutionScale.toString(),
            upscalerName = upscalerName,
            antiAliasingQuality = antiAliasingQuality.toString(),
            fsr = fsr.toString(),
            frameGeneration = frameGeneration.toString(),
            frameWarp = frameWarp.toString(),
            nis = nis.toString(),
            nisSharpness = nisSharpness.toString(),
            rayReconstruction = rayReconstruction.toString(),
            reflex = reflex.toString(),
            superResolution = superResolution.toString(),
            textureQuality = textureQuality.toString(),
            shadowQuality = shadowQuality.toString(),
            viewDistanceQuality = viewDistanceQuality.toString(),
            visualEffectQuality = visualEffectQuality.toString(),
            globalIlluminationQuality = globalIlluminationQuality.toString(),
            reflectionQuality = reflectionQuality.toString(),
            postProcessingQuality = postProcessingQuality.toString(),
            foliageQuality = foliageQuality.toString(),
            shadingQuality = shadingQuality.toString(),
            mainVolume = mainVolume.toString(),
            musicVolume = musicVolume.toString(),
            voiceVolume = voiceVolume.toString(),
            envVolume = envVolume.toString(),
            widgetVolume = widgetVolume.toString(),
            defaultAudioInputDeviceName = defaultAudioInputDeviceName,
            defaultAudioOutputDeviceName = defaultAudioOutputDeviceName,
            defaultMusicPlayerFolder = defaultMusicPlayerFolder,
            animBudgetAllocator = animBudgetAllocator.toString(),
            cameraArmScale = cameraArmScale.toString(),
            cameraMoveLag = cameraMoveLag.toString(),
            cameraMoveSpeedScale = cameraMoveSpeedScale.toString(),
            cameraRotationScale = cameraRotationScale.toString(),
            sceneElementUpdate = sceneElementUpdate.toString(),
            serverMovePathFindPipeNum = serverMovePathFindPipeNum.toString(),
            unitFindPipeNum = unitFindPipeNum.toString(),
            colors = colors.map { color ->
                GameColorForm(
                    name = color.name,
                    red = color.value.red,
                    green = color.value.green,
                    blue = color.value.blue,
                    alpha = color.value.alpha,
                )
            },
        )
    }
}

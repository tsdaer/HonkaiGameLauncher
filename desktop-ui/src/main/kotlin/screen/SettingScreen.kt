package screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.ArrowIosDownward
import compose.icons.evaicons.fill.Folder
import compose.icons.evaicons.fill.Globe2
import compose.icons.evaicons.fill.Moon
import compose.icons.evaicons.fill.Navigation2
import compose.icons.feathericons.Settings
import honkaigamelauncher.desktop_ui.generated.resources.*
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.ColorPicker
import io.github.composefluent.component.ColorSpectrum
import io.github.composefluent.component.FlyoutContainer
import io.github.composefluent.component.FlyoutPlacement
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.TextField
import navigation.SharedScreen
import navigation.screenRoute
import org.jetbrains.compose.resources.stringResource
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.components.FluentDropdown
import ui.fluent.theme.FluentTokens
import ui.settings.AppNavigationStyle
import ui.settings.LocalAppUiSettings
import viewModel.GameColorForm
import viewModel.GameSettingsForm
import viewModel.SettingGameSettingsStatus
import viewModel.SettingScreenModel
import io.github.composefluent.component.Text as FluentText

class SettingScreen: Screen, IScreenInterface {

    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return screenRoute(SharedScreen.Setting)
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.setting)
    }

    override fun getIcon(): ImageVector {
        return compose.icons.FeatherIcons.Settings
    }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { SettingScreenModel() }
        val uiState = screenModel.uiState
        val appUi = LocalAppUiSettings.current
        val state = rememberScrollState(0)
        val languageItems = remember {
            listOf(
                "zh",
                "en"
            )
        }
        val navigationStyles = remember {
            listOf(
                AppNavigationStyle.Top,
                AppNavigationStyle.Left,
                AppNavigationStyle.LeftCompact,
                AppNavigationStyle.LeftCollapsed
            )
        }
        val languageSelectedIndex = if (appUi.languageCode == "en") 1 else 0
        val navigationStyleSelectedIndex = navigationStyles.indexOf(appUi.navigationStyle).takeIf { it >= 0 } ?: 2
        val gamePathValue = uiState.gamePath ?: stringResource(Res.string.settingsNotSet)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            FluentTokens.ColorToken.accent.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Gray.copy(alpha = 0.04f),
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .widthIn(max = 1080.dp)
                    .verticalScroll(state)
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SettingsHero()

                SettingsGroupCard(title = stringResource(Res.string.settingsGroupAppearance)) {
                    SettingItem(
                        title = stringResource(Res.string.themeSetting),
                        description = stringResource(Res.string.themeSettingDesc),
                        icon = EvaIcons.Fill.Moon,
                        trailing = {
                            Switcher(
                                checked = appUi.isDarkTheme,
                                text = if (appUi.isDarkTheme) {
                                    stringResource(Res.string.themeDark)
                                } else {
                                    stringResource(Res.string.themeLight)
                                },
                                textBefore = true,
                                onCheckStateChange = { checked ->
                                    if (checked != appUi.isDarkTheme) {
                                        appUi.onThemeChanged()
                                    }
                                }
                            )
                        }
                    )
                    SettingItem(
                        title = stringResource(Res.string.languageSetting),
                        description = stringResource(Res.string.languageSettingDesc),
                        icon = EvaIcons.Fill.Globe2,
                        trailing = {
                            FluentDropdown(
                                items = listOf(
                                    stringResource(Res.string.languageZh),
                                    stringResource(Res.string.languageEn)
                                ),
                                selectedIndex = languageSelectedIndex,
                                onSelectionChange = { index, _ ->
                                    val targetLanguage = languageItems.getOrNull(index) ?: "zh"
                                    if (targetLanguage != appUi.languageCode) {
                                        appUi.onLanguageChanged(targetLanguage)
                                    }
                                },
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    )
                    SettingItem(
                        title = stringResource(Res.string.navigationStyleSetting),
                        description = stringResource(Res.string.navigationStyleSettingDesc),
                        icon = EvaIcons.Fill.Navigation2,
                        trailing = {
                            FluentDropdown(
                                items = navigationStyles.map { it.label() },
                                selectedIndex = navigationStyleSelectedIndex,
                                onSelectionChange = { index, _ ->
                                    navigationStyles.getOrNull(index)?.let(appUi.onNavigationStyleChanged)
                                },
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    )
                }

                SettingsGroupCard(title = stringResource(Res.string.settingsGroupLauncher)) {
                    SettingItem(
                        title = stringResource(Res.string.setGamePath),
                        description = stringResource(Res.string.settingsDescGamePath),
                        icon = EvaIcons.Fill.Folder,
                        trailing = {
                            FluentButton(onClick = { screenModel.setGamePath() }) {
                                FluentText(stringResource(Res.string.settingsActionBrowse))
                            }
                        }
                    )
                    SettingPathPreview(gamePathValue)
                }

                SettingsGroupCard(title = stringResource(Res.string.settingsGroupGame)) {
                    GameSettingsEditor(
                        uiState = uiState,
                        onRefresh = screenModel::refreshGameSettings,
                        onSave = screenModel::saveGameSettings,
                        onChange = screenModel::updateGameSettingsForm,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameSettingsEditor(
    uiState: viewModel.SettingUiState,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onChange: (GameSettingsForm) -> Unit,
) {
    val form = uiState.gameSettingsForm
    val busy = uiState.gameSettingsStatus == SettingGameSettingsStatus.Loading ||
        uiState.gameSettingsStatus == SettingGameSettingsStatus.Saving

    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            FluentTokens.ColorToken.accent.copy(alpha = 0.16f),
                            FluentTokens.ColorToken.accent.copy(alpha = 0.05f),
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FluentText(
                    text = gameSettingsStatusText(uiState),
                    style = FluentTheme.typography.bodyStrong
                )
                if (uiState.gameSettingsConfigDirectory.isNotBlank()) {
                    FluentText(
                        text = uiState.gameSettingsConfigDirectory,
                        style = FluentTheme.typography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            FluentButton(onClick = onRefresh, disabled = busy) {
                FluentText(stringResource(Res.string.settingsGameActionRefresh))
            }
            FluentButton(
                onClick = onSave,
                disabled = busy || form == null
            ) {
                FluentText(stringResource(Res.string.settingsGameActionSave))
            }
        }

        if (form == null) {
            FluentText(
                text = stringResource(Res.string.settingsGameUnavailableHint),
                style = FluentTheme.typography.caption
            )
            return@Column
        }

        val languageValues = withCurrent(listOf("zh-Hans", "en"), form.language)
        val languageLabels = labelsWithCurrent(
            listOf("zh-Hans", "en"),
            listOf(stringResource(Res.string.languageZh), stringResource(Res.string.languageEn)),
            form.language,
        )
        val fullscreenModeValues = listOf("Fullscreen", "WindowedFullscreen", "Windowed")
        val fullscreenModeLabels = listOf(
            stringResource(Res.string.settingsGameFullscreen),
            stringResource(Res.string.settingsGameWindowedFullscreen),
            stringResource(Res.string.settingsGameWindowed),
        )

        GameSettingsSectionTitle(stringResource(Res.string.settingsGameDisplaySection))
        GameSettingsTwoColumnRow {
            GameSettingsDropdown(
                label = stringResource(Res.string.settingsGameLanguage),
                values = languageValues,
                labels = languageLabels,
                selectedValue = form.language,
                onSelected = { onChange(form.copy(language = it)) },
            )
            GameSettingsDropdown(
                label = stringResource(Res.string.settingsGameFullscreenMode),
                values = withCurrent(fullscreenModeValues, form.fullscreenMode),
                labels = labelsWithCurrent(fullscreenModeValues, fullscreenModeLabels, form.fullscreenMode),
                selectedValue = form.fullscreenMode,
                onSelected = { onChange(form.copy(fullscreenMode = it)) },
            )
        }
        GameSettingsTwoColumnRow {
            GameSettingsResolutionField(form, onChange)
            GameSettingsTextField(
                label = stringResource(Res.string.settingsGameFrameRateLimit),
                value = form.frameRateLimit,
                onValueChange = { onChange(form.copy(frameRateLimit = it)) },
            )
        }
        GameSettingsTwoColumnRow {
            GameSettingsSwitch(
                label = stringResource(Res.string.settingsGameVSync),
                checked = form.vSync,
                onCheckedChange = { onChange(form.copy(vSync = it)) },
            )
            GameSettingsSwitch(
                label = stringResource(Res.string.settingsGameDynamicResolution),
                checked = form.dynamicResolution,
                onCheckedChange = { onChange(form.copy(dynamicResolution = it)) },
            )
        }
        GameSettingsTwoColumnRow {
            GameSettingsSwitch(
                label = stringResource(Res.string.settingsGameHdrDisplay),
                checked = form.hdrDisplay,
                onCheckedChange = { onChange(form.copy(hdrDisplay = it)) },
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(
                label = stringResource(Res.string.settingsGameResolutionScale),
                value = form.resolutionScale,
                onValueChange = { onChange(form.copy(resolutionScale = it)) },
            )
            GameSettingsDropdown(
                label = stringResource(Res.string.settingsGameUpscaler),
                values = withCurrent(listOf("TSR", "NIS", "FSR"), form.upscalerName),
                labels = withCurrent(listOf("TSR", "NIS", "FSR"), form.upscalerName),
                selectedValue = form.upscalerName,
                onSelected = { onChange(form.copy(upscalerName = it)) },
            )
        }
        GameSettingsSectionTitle(stringResource(Res.string.settingsGameMotionBlurSection))
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameMotionBlurAmount), form.motionBlurAmount) {
                onChange(form.copy(motionBlurAmount = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameMotionBlurMax), form.motionBlurMax) {
                onChange(form.copy(motionBlurMax = it))
            }
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameMotionBlurScale), form.motionBlurScale) {
                onChange(form.copy(motionBlurScale = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameMotionBlurTargetFps), form.motionBlurTargetFps) {
                onChange(form.copy(motionBlurTargetFps = it))
            }
        }

        GameSettingsSectionTitle(stringResource(Res.string.settingsGameUpscalerSection))
        GameSettingsTwoColumnRow {
            GameSettingsTextField("FSR", form.fsr) { onChange(form.copy(fsr = it)) }
            GameSettingsTextField(stringResource(Res.string.settingsGameSuperResolution), form.superResolution) {
                onChange(form.copy(superResolution = it))
            }
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField("NIS", form.nis) { onChange(form.copy(nis = it)) }
            GameSettingsTextField(stringResource(Res.string.settingsGameNisSharpness), form.nisSharpness) {
                onChange(form.copy(nisSharpness = it))
            }
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameFrameGeneration), form.frameGeneration) {
                onChange(form.copy(frameGeneration = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameFrameWarp), form.frameWarp) {
                onChange(form.copy(frameWarp = it))
            }
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameReflex), form.reflex) {
                onChange(form.copy(reflex = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameRayReconstruction), form.rayReconstruction) {
                onChange(form.copy(rayReconstruction = it))
            }
        }

        GameSettingsSectionTitle(stringResource(Res.string.settingsGameQualitySection))
        GameSettingsTwoColumnRow {
            GameSettingsSwitch(
                label = stringResource(Res.string.settingsGameUseOverallQuality),
                checked = form.useOverallScalabilityLevel,
                onCheckedChange = { onChange(form.copy(useOverallScalabilityLevel = it)) },
            )
            QualityDropdown(
                label = stringResource(Res.string.settingsGameOverallQuality),
                value = form.overallScalabilityLevel,
                onSelected = { onChange(form.copy(overallScalabilityLevel = it)) },
            )
        }
        GameSettingsTwoColumnRow {
            QualityDropdown(stringResource(Res.string.settingsGameAudioQuality), form.audioQualityLevel) {
                onChange(form.copy(audioQualityLevel = it))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        GameSettingsTwoColumnRow {
            QualityDropdown(stringResource(Res.string.settingsGameTextureQuality), form.textureQuality) {
                onChange(form.copy(textureQuality = it))
            }
            QualityDropdown(stringResource(Res.string.settingsGameShadowQuality), form.shadowQuality) {
                onChange(form.copy(shadowQuality = it))
            }
        }
        GameSettingsTwoColumnRow {
            QualityDropdown(stringResource(Res.string.settingsGameViewDistanceQuality), form.viewDistanceQuality) {
                onChange(form.copy(viewDistanceQuality = it))
            }
            QualityDropdown(stringResource(Res.string.settingsGameVisualEffectQuality), form.visualEffectQuality) {
                onChange(form.copy(visualEffectQuality = it))
            }
        }
        GameSettingsTwoColumnRow {
            QualityDropdown(stringResource(Res.string.settingsGameGlobalIlluminationQuality), form.globalIlluminationQuality) {
                onChange(form.copy(globalIlluminationQuality = it))
            }
            QualityDropdown(stringResource(Res.string.settingsGameReflectionQuality), form.reflectionQuality) {
                onChange(form.copy(reflectionQuality = it))
            }
        }
        GameSettingsTwoColumnRow {
            QualityDropdown(stringResource(Res.string.settingsGamePostProcessingQuality), form.postProcessingQuality) {
                onChange(form.copy(postProcessingQuality = it))
            }
            QualityDropdown(stringResource(Res.string.settingsGameFoliageQuality), form.foliageQuality) {
                onChange(form.copy(foliageQuality = it))
            }
        }
        GameSettingsTwoColumnRow {
            QualityDropdown(stringResource(Res.string.settingsGameShadingQuality), form.shadingQuality) {
                onChange(form.copy(shadingQuality = it))
            }
            QualityDropdown(stringResource(Res.string.settingsGameAntiAliasingQuality), form.antiAliasingQuality) {
                onChange(form.copy(antiAliasingQuality = it))
            }
        }

        GameSettingsSectionTitle(stringResource(Res.string.settingsGameAudioCameraSection))
        GameSettingsTwoColumnRow {
            GameSettingsTextField(
                label = stringResource(Res.string.settingsGameAudioInputDevice),
                value = form.defaultAudioInputDeviceName,
                keyboardType = KeyboardType.Text,
                onValueChange = { onChange(form.copy(defaultAudioInputDeviceName = it)) },
            )
            GameSettingsTextField(
                label = stringResource(Res.string.settingsGameAudioOutputDevice),
                value = form.defaultAudioOutputDeviceName,
                keyboardType = KeyboardType.Text,
                onValueChange = { onChange(form.copy(defaultAudioOutputDeviceName = it)) },
            )
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(
                label = stringResource(Res.string.settingsGameMusicFolder),
                value = form.defaultMusicPlayerFolder,
                keyboardType = KeyboardType.Text,
                onValueChange = { onChange(form.copy(defaultMusicPlayerFolder = it)) },
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameMainVolume), form.mainVolume) {
                onChange(form.copy(mainVolume = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameMusicVolume), form.musicVolume) {
                onChange(form.copy(musicVolume = it))
            }
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameVoiceVolume), form.voiceVolume) {
                onChange(form.copy(voiceVolume = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameEnvVolume), form.envVolume) {
                onChange(form.copy(envVolume = it))
            }
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameWidgetVolume), form.widgetVolume) {
                onChange(form.copy(widgetVolume = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameCameraMoveSpeed), form.cameraMoveSpeedScale) {
                onChange(form.copy(cameraMoveSpeedScale = it))
            }
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameCameraArmScale), form.cameraArmScale) {
                onChange(form.copy(cameraArmScale = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameCameraMoveLag), form.cameraMoveLag) {
                onChange(form.copy(cameraMoveLag = it))
            }
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(
                label = stringResource(Res.string.settingsGameCameraRotationSpeed),
                value = form.cameraRotationScale,
                onValueChange = { onChange(form.copy(cameraRotationScale = it)) },
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        GameSettingsSectionTitle(stringResource(Res.string.settingsGameAdvancedSection))
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameAnimBudgetAllocator), form.animBudgetAllocator) {
                onChange(form.copy(animBudgetAllocator = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameSceneElementUpdate), form.sceneElementUpdate) {
                onChange(form.copy(sceneElementUpdate = it))
            }
        }
        GameSettingsTwoColumnRow {
            GameSettingsTextField(stringResource(Res.string.settingsGameServerMovePathFindPipeNum), form.serverMovePathFindPipeNum) {
                onChange(form.copy(serverMovePathFindPipeNum = it))
            }
            GameSettingsTextField(stringResource(Res.string.settingsGameUnitFindPipeNum), form.unitFindPipeNum) {
                onChange(form.copy(unitFindPipeNum = it))
            }
        }

        if (form.colors.isNotEmpty()) {
            GameSettingsSectionTitle(stringResource(Res.string.settingsGameColorSection))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                form.colors.forEach { colorForm ->
                    GameColorPickerRow(
                        colorForm = colorForm,
                        onColorChanged = { updatedColor ->
                            onChange(
                                form.copy(
                                    colors = form.colors.map { color ->
                                        if (color.name == colorForm.name) updatedColor else color
                                    }
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun gameSettingsStatusText(uiState: viewModel.SettingUiState): String {
    return when (uiState.gameSettingsStatus) {
        SettingGameSettingsStatus.Loading -> stringResource(Res.string.settingsGameStatusLoading)
        SettingGameSettingsStatus.Ready -> stringResource(Res.string.settingsGameStatusReady)
        SettingGameSettingsStatus.Saving -> stringResource(Res.string.settingsGameStatusSaving)
        SettingGameSettingsStatus.Saved -> stringResource(Res.string.settingsGameStatusSaved)
        SettingGameSettingsStatus.MissingGamePath -> stringResource(Res.string.settingsGameStatusMissingGamePath)
        SettingGameSettingsStatus.MissingConfig -> stringResource(Res.string.settingsGameStatusMissingConfig)
        SettingGameSettingsStatus.Error -> if (uiState.gameSettingsMessage == "invalid-number") {
            stringResource(Res.string.settingsGameStatusInvalidNumber)
        } else {
            stringResource(Res.string.settingsGameStatusError, uiState.gameSettingsMessage.ifBlank { "-" })
        }
    }
}

@Composable
private fun GameSettingsSectionTitle(title: String) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(FluentTokens.ColorToken.accent)
        )
        FluentText(
            text = title,
            style = FluentTheme.typography.bodyStrong
        )
    }
}

@Composable
private fun GameSettingsTwoColumnRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun RowScope.GameSettingsTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.weight(1f),
    keyboardType: KeyboardType = KeyboardType.Number,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FluentText(text = label, style = FluentTheme.typography.caption)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}

@Composable
private fun RowScope.GameSettingsResolutionField(
    form: GameSettingsForm,
    onChange: (GameSettingsForm) -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FluentText(text = stringResource(Res.string.settingsGameResolution), style = FluentTheme.typography.caption)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = form.resolutionWidth,
                onValueChange = { onChange(form.copy(resolutionWidth = it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = form.resolutionHeight,
                onValueChange = { onChange(form.copy(resolutionHeight = it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
private fun RowScope.GameSettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FluentText(text = label, style = FluentTheme.typography.caption)
        Switcher(
            checked = checked,
            text = if (checked) stringResource(Res.string.settingsGameEnabled) else stringResource(Res.string.settingsGameDisabled),
            textBefore = true,
            onCheckStateChange = onCheckedChange,
        )
    }
}

@Composable
private fun RowScope.QualityDropdown(
    label: String,
    value: String,
    onSelected: (String) -> Unit,
) {
    val qualityValues = listOf("0", "1", "2", "3")
    val qualityLabels = listOf(
        stringResource(Res.string.settingsGameQualityLow),
        stringResource(Res.string.settingsGameQualityMedium),
        stringResource(Res.string.settingsGameQualityHigh),
        stringResource(Res.string.settingsGameQualityEpic),
    )

    GameSettingsDropdown(
        label = label,
        values = withCurrent(qualityValues, value),
        labels = labelsWithCurrent(qualityValues, qualityLabels, value),
        selectedValue = value,
        onSelected = onSelected,
    )
}

@Composable
private fun RowScope.GameSettingsDropdown(
    label: String,
    values: List<String>,
    labels: List<String>,
    selectedValue: String,
    onSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FluentText(text = label, style = FluentTheme.typography.caption)
        FluentDropdown(
            items = labels,
            selectedIndex = values.indexOf(selectedValue).takeIf { it >= 0 },
            onSelectionChange = { index, _ ->
                values.getOrNull(index)?.let(onSelected)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GameColorPickerRow(
    colorForm: GameColorForm,
    onColorChanged: (GameColorForm) -> Unit,
) {
    val color = colorForm.toComposeColor()

    FlyoutContainer(
        placement = FlyoutPlacement.BottomAlignedStart,
        flyout = {
            Column(
                modifier = Modifier.width(330.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ColorPreviewDot(color = color, size = 32.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        FluentText(
                            text = colorForm.name,
                            style = FluentTheme.typography.bodyStrong
                        )
                        FluentText(
                            text = colorForm.channelText(),
                            style = FluentTheme.typography.caption
                        )
                    }
                }
                ColorPicker(
                    colorSpectrum = ColorSpectrum.Round,
                    color = color,
                    onSelectedColorChanged = { selectedColor ->
                        onColorChanged(selectedColor.toGameColorForm(colorForm.name))
                    },
                    alphaEnabled = true,
                    moreButtonVisible = false,
                )
                FluentButton(
                    onClick = { isFlyoutVisible = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    FluentText(stringResource(Res.string.settingsGameColorClose))
                }
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.16f),
                                Color.Gray.copy(alpha = 0.04f),
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = color.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { isFlyoutVisible = !isFlyoutVisible }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ColorPreviewDot(color = color, size = 32.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FluentText(
                        text = colorForm.name,
                        style = FluentTheme.typography.bodyStrong
                    )
                    FluentText(
                        text = colorForm.channelText(),
                        style = FluentTheme.typography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FluentText(
                    text = stringResource(Res.string.settingsGameColorEdit),
                    style = FluentTheme.typography.caption
                )
            }
        }
    )
}

@Composable
private fun ColorPreviewDot(
    color: Color,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.72f), CircleShape)
            .background(color)
    )
}

private fun withCurrent(values: List<String>, current: String): List<String> {
    return if (current in values) values else values + current
}

private fun labelsWithCurrent(values: List<String>, labels: List<String>, current: String): List<String> {
    return if (current in values) labels else labels + current
}

private fun GameColorForm.toComposeColor(): Color {
    return Color(
        red = red.toFloat().coerceIn(0f, 1f),
        green = green.toFloat().coerceIn(0f, 1f),
        blue = blue.toFloat().coerceIn(0f, 1f),
        alpha = alpha.toFloat().coerceIn(0f, 1f),
    )
}

private fun Color.toGameColorForm(name: String): GameColorForm {
    return GameColorForm(
        name = name,
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}

private fun GameColorForm.channelText(): String {
    return "R ${red.toDisplayChannel()}  G ${green.toDisplayChannel()}  B ${blue.toDisplayChannel()}  A ${alpha.toDisplayChannel()}"
}

private fun Double.toDisplayChannel(): String {
    return "%.2f".format(this)
}

@Composable
private fun SettingsHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        FluentTokens.ColorToken.accent.copy(alpha = 0.22f),
                        FluentTokens.ColorToken.accent.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.05f),
                    )
                )
            )
            .border(
                width = 1.dp,
                color = FluentTokens.ColorToken.accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FluentText(
            text = stringResource(Res.string.settingsHeroTitle),
            style = FluentTheme.typography.title
        )
        FluentText(
            text = stringResource(Res.string.settingsHeroDesc),
            style = FluentTheme.typography.body
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1080.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(FluentTokens.ColorToken.accent)
            )
            FluentText(
                text = title,
                style = FluentTheme.typography.subtitle
            )
        }
        FluentCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String,
    icon: ImageVector,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Gray.copy(alpha = 0.025f))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FluentTokens.ColorToken.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FluentText(
                    text = title,
                    style = FluentTheme.typography.bodyStrong
                )
                FluentText(
                    text = description,
                    style = FluentTheme.typography.caption
                )
            }
        }
        Box(
            modifier = Modifier.padding(start = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            trailing()
        }
    }
}

@Composable
private fun SettingPathPreview(path: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = EvaIcons.Fill.ArrowIosDownward,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
        )
        FluentText(
            text = path,
            style = FluentTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AppNavigationStyle.label(): String {
    return when (this) {
        AppNavigationStyle.Top -> stringResource(Res.string.navigationStyleTop)
        AppNavigationStyle.Left -> stringResource(Res.string.navigationStyleLeft)
        AppNavigationStyle.LeftCompact -> stringResource(Res.string.navigationStyleLeftCompact)
        AppNavigationStyle.LeftCollapsed -> stringResource(Res.string.navigationStyleLeftCollapsed)
    }
}

package core.config

import core.platform.AppSettingsRepository
import java.io.File

/**
 * 游戏设置数据模型。
 *
 * 持有从 `GenericSetting.toml` 和 `GameSetting.toml` 两个配置文件中
 * 读取的所有可编辑游戏参数。各属性直接映射 TOML 文件中的键路径。
 *
 * 分为以下几组：
 * - **常规设置**：语言、全屏模式、分辨率、HDR、帧率限制等（来自 GenericSetting.toml）
 * - **画质设置**：纹理、阴影、视距、后处理等级等（来自 GenericSetting.toml）
 * - **升采样/抗锯齿**：FSR、DLSS、TSR 等 NVIDIA/AMD 技术参数（来自 GenericSetting.toml）
 * - **音频设置**：主音量、音乐、语音、环境音量、输入/输出设备（来自 GameSetting.toml）
 * - **相机/摇杆**：镜头臂缩放、移动延迟、移动/旋转速度等（来自 GameSetting.toml）
 * - **颜色设置**：动态 RGBA 颜色配置，表名可扩展（来自 GameSetting.toml）
 *
 * @property language                   界面语言代码，如 `"zh-Hans"`、`"en"`
 * @property fullscreenMode            全屏模式：`"Fullscreen"` / `"WindowedFullscreen"` / `"Windowed"`
 * @property dynamicResolution         是否启用动态分辨率
 * @property useOverallScalabilityLevel 是否使用全局可扩展性等级
 * @property overallScalabilityLevel    全局可扩展性等级（0-4）
 * @property audioQualityLevel          音频质量等级
 * @property hdrDisplay                 是否启用 HDR 显示
 * @property frameRateLimit            帧率上限（0.0 表示无限制）
 * @property motionBlurAmount          运动模糊强度
 * @property motionBlurMax             运动模糊最大值（-1.0 表示自动）
 * @property motionBlurScale           运动模糊缩放
 * @property motionBlurTargetFps       运动模糊目标 FPS
 * @property resolutionWidth           水平分辨率（像素）
 * @property resolutionHeight          垂直分辨率（像素）
 * @property vSync                     是否启用垂直同步
 * @property resolutionScale           分辨率缩放百分比（如 75.0 表示 75%）
 * @property upscalerName              升采样器名称（如 `"TSR"`、`"FSR"`、`"DLSS"`）
 * @property antiAliasingQuality       抗锯齿质量等级
 * @property fsr                       FSR 开关（0 = 关闭, 1 = FSR 1.0, 2 = FSR 2.0）
 * @property frameGeneration           帧生成开关
 * @property frameWarp                 帧扭曲开关
 * @property nis                       NVIDIA Image Scaling 等级
 * @property nisSharpness              NIS 锐化强度
 * @property rayReconstruction         光线重建开关
 * @property reflex                    NVIDIA Reflex 低延迟开关
 * @property superResolution           超分辨率开关
 * @property textureQuality            纹理质量等级
 * @property shadowQuality             阴影质量等级
 * @property viewDistanceQuality       视距质量等级
 * @property visualEffectQuality       视觉效果质量等级
 * @property globalIlluminationQuality 全局光照质量等级
 * @property reflectionQuality         反射质量等级
 * @property postProcessingQuality     后处理质量等级
 * @property foliageQuality            植被质量等级
 * @property shadingQuality            着色质量等级
 * @property mainVolume                主音量（0.0-100.0）
 * @property musicVolume               音乐音量（0.0-100.0）
 * @property voiceVolume               语音音量（0.0-100.0）
 * @property envVolume                 环境音量（0.0-100.0）
 * @property widgetVolume              UI 控件音量（0.0-100.0）
 * @property defaultAudioInputDeviceName  默认音频输入设备名称
 * @property defaultAudioOutputDeviceName 默认音频输出设备名称
 * @property defaultMusicPlayerFolder     默认音乐播放器文件夹路径
 * @property animBudgetAllocator       动画预算分配系数
 * @property cameraArmScale            相机臂缩放
 * @property cameraMoveLag             相机移动延迟
 * @property cameraMoveSpeedScale      相机移动速度缩放
 * @property cameraRotationScale       相机旋转速度缩放
 * @property sceneElementUpdate        场景元素更新频率系数
 * @property serverMovePathFindPipeNum 服务端移动寻路管道数
 * @property unitFindPipeNum           单位查找管道数
 * @property colors                    动态 RGBA 颜色配置列表
 */
data class GameSettings(
    val language: String = "zh-Hans",
    val fullscreenMode: String = "Fullscreen",
    val dynamicResolution: Boolean = false,
    val useOverallScalabilityLevel: Boolean = true,
    val overallScalabilityLevel: Int = 2,
    val audioQualityLevel: Int = 0,
    val hdrDisplay: Boolean = false,
    val frameRateLimit: Double = 0.0,
    val motionBlurAmount: Double = 0.0,
    val motionBlurMax: Double = -1.0,
    val motionBlurScale: Double = 1.0,
    val motionBlurTargetFps: Double = 30.0,
    val resolutionWidth: Int = 1280,
    val resolutionHeight: Int = 720,
    val vSync: Boolean = false,
    val resolutionScale: Double = 75.0,
    val upscalerName: String = "TSR",
    val antiAliasingQuality: Int = 2,
    val fsr: Int = 0,
    val frameGeneration: Int = 0,
    val frameWarp: Int = 0,
    val nis: Int = 3,
    val nisSharpness: Int = 0,
    val rayReconstruction: Int = 0,
    val reflex: Int = 0,
    val superResolution: Int = 0,
    val textureQuality: Int = 2,
    val shadowQuality: Int = 2,
    val viewDistanceQuality: Int = 2,
    val visualEffectQuality: Int = 2,
    val globalIlluminationQuality: Int = 2,
    val reflectionQuality: Int = 2,
    val postProcessingQuality: Int = 2,
    val foliageQuality: Int = 2,
    val shadingQuality: Int = 2,
    val mainVolume: Double = 100.0,
    val musicVolume: Double = 100.0,
    val voiceVolume: Double = 100.0,
    val envVolume: Double = 100.0,
    val widgetVolume: Double = 100.0,
    val defaultAudioInputDeviceName: String = "",
    val defaultAudioOutputDeviceName: String = "",
    val defaultMusicPlayerFolder: String = "",
    val animBudgetAllocator: Double = 1.0,
    val cameraArmScale: Double = 1.0,
    val cameraMoveLag: Double = 0.1,
    val cameraMoveSpeedScale: Double = 1.0,
    val cameraRotationScale: Double = 1.0,
    val sceneElementUpdate: Double = 1.0,
    val serverMovePathFindPipeNum: Int = 12,
    val unitFindPipeNum: Int = 12,
    val colors: List<GameColorSetting> = emptyList(),
)

/**
 * RGBA 颜色设置条目。
 *
 * 代表一个命名的颜色配置，如 `AttackLineColor`、`HealthBarColor` 等。
 * 对应的 TOML 结构为 `[SectionName]` 表，内含 `R`/`G`/`B`/`A` 四个通道值。
 *
 * @property name  颜色配置名称（即 TOML 表名）
 * @property value RGBA 通道值
 */
data class GameColorSetting(
    val name: String,
    val value: GameRgbaColor,
)

/**
 * RGBA 颜色通道值。
 *
 * 四个通道的取值范围为 0.0-1.0（归一化浮点）。
 *
 * @property red   红色通道（0.0-1.0）
 * @property green 绿色通道（0.0-1.0）
 * @property blue  蓝色通道（0.0-1.0）
 * @property alpha 不透明度通道（0.0-1.0，默认 1.0）
 */
data class GameRgbaColor(
    val red: Double,
    val green: Double,
    val blue: Double,
    val alpha: Double,
)

/**
 * 游戏设置加载/保存状态枚举。
 *
 * @property Ready           成功加载或保存
 * @property MissingGamePath 未设置游戏路径或路径无效
 * @property MissingConfig   配置文件 `GenericSetting.toml` 或 `GameSetting.toml` 不存在
 * @property Error           加载或保存过程中发生异常
 */
enum class GameSettingsStatus {
    Ready,
    MissingGamePath,
    MissingConfig,
    Error,
}

/**
 * 游戏设置操作结果。
 *
 * [GameSettingsService.load] 和 [GameSettingsService.save] 的返回值类型，
 * 包含操作状态和可选的设置数据。
 *
 * @property status          操作状态
 * @property settings        加载或保存后的游戏设置（仅在 status=Ready 时有值）
 * @property configDirectory 配置文件所在目录的绝对路径（`{游戏目录}/honkai_rts/Config/`）
 * @property message         错误描述（仅在 status=Error 时有值）
 */
data class GameSettingsResult(
    val status: GameSettingsStatus,
    val settings: GameSettings? = null,
    val configDirectory: String = "",
    val message: String = "",
)

/**
 * 游戏设置服务。
 *
 * 负责从游戏配置目录读取和写入 `GenericSetting.toml` 和 `GameSetting.toml` 两个文件。
 *
 * ## 配置文件位置
 * 基于游戏路径解析出 `{游戏目录}/honkai_rts/Config/` 作为配置根目录：
 * - `GenericSetting.toml`：画面、性能、升采样等通用设置（游戏内"画面设置"面板）
 * - `GameSetting.toml`：音频、相机、颜色、管道数等游戏内设置
 *
 * ## 读写策略分离
 * - **读取**使用 [TomlReader]（基于 ktoml AST 解析，健壮性强）
 * - **写入**使用 [SimpleTomlDocument]（逐行编辑保留未知键、注释和原有格式）
 *   这是因为保存时必须原样保留 TOML 文件中未被识别的键、注释和空白行
 *
 * ## 动态颜色表
 * `GameSetting.toml` 中任何同时包含 `R`/`G`/`B`/`A` 四个通道的 `[SectionName]` 表
 * 会被识别为颜色设置，以 [GameColorSetting] 形式读入 [GameSettings.colors] 列表。
 * 保存时颜色表会按原有结构写回，不会破坏未知表。
 */
class GameSettingsService {
    /**
     * 加载游戏设置。
     *
     * 流程：
     * 1. 解析配置目录路径
     * 2. 检查两个配置文件是否存在
     * 3. 使用 [TomlReader] 解析 TOML 内容
     * 4. 通过 [readSettings] 映射为 [GameSettings] 数据模型
     *
     * @param path 游戏可执行文件路径或目录路径；null 或无效值将返回 MissingGamePath
     * @return 包含加载结果和设置数据的 [GameSettingsResult]
     */
    fun load(path: String?): GameSettingsResult {
        val configDirectory = resolveConfigDirectory(path)
            ?: return GameSettingsResult(GameSettingsStatus.MissingGamePath)

        val genericFile = File(configDirectory, GENERIC_SETTING_FILE)
        val gameFile = File(configDirectory, GAME_SETTING_FILE)
        if (!genericFile.isFile || !gameFile.isFile) {
            return GameSettingsResult(
                status = GameSettingsStatus.MissingConfig,
                configDirectory = configDirectory.absolutePath,
            )
        }

        return runCatching {
            val genericDocument = TomlReader.parse(genericFile.readText())
            val gameDocument = TomlReader.parse(gameFile.readText())
            GameSettingsResult(
                status = GameSettingsStatus.Ready,
                settings = readSettings(genericDocument, gameDocument),
                configDirectory = configDirectory.absolutePath,
            )
        }.getOrElse { error ->
            GameSettingsResult(
                status = GameSettingsStatus.Error,
                configDirectory = configDirectory.absolutePath,
                message = error.message.orEmpty(),
            )
        }
    }

    /**
     * 保存游戏设置到配置文件。
     *
     * 写入策略：使用 [SimpleTomlDocument] 逐行编辑，保留未知键、注释和原有格式。
     * 流程：
     * 1. 解析配置目录路径
     * 2. 使用 [SimpleTomlDocument.parse] 分别加载两个文件为可编辑文档
     * 3. 调用 [writeGenericSettings] 和 [writeGameSettings] 修改文档中的已知键
     * 4. 将修改后的文档渲染写回原文件
     *
     * @param path     游戏可执行文件路径或目录路径
     * @param settings 要保存的游戏设置
     * @return 包含保存结果的 [GameSettingsResult]
     */
    fun save(path: String?, settings: GameSettings): GameSettingsResult {
        val configDirectory = resolveConfigDirectory(path)
            ?: return GameSettingsResult(GameSettingsStatus.MissingGamePath)

        val genericFile = File(configDirectory, GENERIC_SETTING_FILE)
        val gameFile = File(configDirectory, GAME_SETTING_FILE)
        if (!genericFile.isFile || !gameFile.isFile) {
            return GameSettingsResult(
                status = GameSettingsStatus.MissingConfig,
                configDirectory = configDirectory.absolutePath,
            )
        }

        return runCatching {
            val genericDocument = SimpleTomlDocument.parse(genericFile.readText())
            val gameDocument = SimpleTomlDocument.parse(gameFile.readText())

            writeGenericSettings(genericDocument, settings)
            writeGameSettings(gameDocument, settings)

            genericFile.writeText(genericDocument.render())
            gameFile.writeText(gameDocument.render())

            GameSettingsResult(
                status = GameSettingsStatus.Ready,
                settings = settings,
                configDirectory = configDirectory.absolutePath,
            )
        }.getOrElse { error ->
            GameSettingsResult(
                status = GameSettingsStatus.Error,
                configDirectory = configDirectory.absolutePath,
                message = error.message.orEmpty(),
            )
        }
    }

    /**
     * 根据游戏路径解析配置目录。
     *
     * 层级路径：`{游戏目录}/honkai_rts/Config/`
     * 如果输入的 path 指向 .exe 文件，则取其父目录作为游戏目录；
     * 如果指向目录则直接使用。
     *
     * @param path 游戏路径（可能为 null）
     * @return 配置目录的 [File] 对象，或 null（路径无效时）
     */
    private fun resolveConfigDirectory(path: String?): File? {
        val normalizedPath = AppSettingsRepository.normalizeGamePath(path) ?: return null
        val gameFile = File(normalizedPath)
        val gameDirectory = if (gameFile.isDirectory) gameFile else gameFile.parentFile ?: return null
        return File(File(gameDirectory, "honkai_rts"), "Config")
    }

    /**
     * 将两个 TOML 文档映射为 [GameSettings] 数据模型。
     *
     * 通用设置（GenericSetting.toml）负责画面、性能、升采样相关的键；
     * 游戏设置（GameSetting.toml）负责音频、相机、颜色、管道数相关的键。
     * 每个键都有合理的默认值，查询不到时使用默认值。
     *
     * 颜色表由 [TomlReader.readRgbaColorSections] 动态发现并提取。
     *
     * @param genericDocument GenericSetting.toml 的解析结果
     * @param gameDocument    GameSetting.toml 的解析结果
     * @return 合并后的 [GameSettings]
     */
    private fun readSettings(
        genericDocument: TomlReader,
        gameDocument: TomlReader,
    ): GameSettings {
        return GameSettings(
            language = genericDocument.string("Language") ?: "zh-Hans",
            fullscreenMode = genericDocument.string("FullscreenMode") ?: "Fullscreen",
            dynamicResolution = genericDocument.boolean("DynamicResolution") ?: false,
            useOverallScalabilityLevel = genericDocument.boolean("UseOverallScalabilityLevel") ?: true,
            overallScalabilityLevel = genericDocument.int("OverallScalabilityLevel") ?: 2,
            audioQualityLevel = genericDocument.int("AudioQualityLevel") ?: 0,
            hdrDisplay = genericDocument.boolean("HdrDisplay") ?: false,
            frameRateLimit = genericDocument.double("FrameRateLimit") ?: 0.0,
            motionBlurAmount = genericDocument.double("MotionBlurAmount") ?: 0.0,
            motionBlurMax = genericDocument.double("MotionBlurMax") ?: -1.0,
            motionBlurScale = genericDocument.double("MotionBlurScale") ?: 1.0,
            motionBlurTargetFps = genericDocument.double("MotionBlurTargetFPS") ?: 30.0,
            resolutionWidth = genericDocument.int("Resolution.X") ?: 1280,
            resolutionHeight = genericDocument.int("Resolution.Y") ?: 720,
            vSync = genericDocument.boolean("HonkaiUpscalerInfo.VSync") ?: false,
            resolutionScale = genericDocument.double("HonkaiUpscalerInfo.ResolutionScale") ?: 75.0,
            upscalerName = genericDocument.string("HonkaiUpscalerInfo.UpscalerName") ?: "TSR",
            antiAliasingQuality = genericDocument.int("HonkaiUpscalerInfo.AntiAliasingQuality") ?: 2,
            fsr = genericDocument.int("HonkaiUpscalerInfo.FSR") ?: 0,
            frameGeneration = genericDocument.int("HonkaiUpscalerInfo.FrameGeneration") ?: 0,
            frameWarp = genericDocument.int("HonkaiUpscalerInfo.FrameWarp") ?: 0,
            nis = genericDocument.int("HonkaiUpscalerInfo.NIS") ?: 3,
            nisSharpness = genericDocument.int("HonkaiUpscalerInfo.NIS_Sharpness") ?: 0,
            rayReconstruction = genericDocument.int("HonkaiUpscalerInfo.RayReconstruction") ?: 0,
            reflex = genericDocument.int("HonkaiUpscalerInfo.Reflex") ?: 0,
            superResolution = genericDocument.int("HonkaiUpscalerInfo.SuperResolution") ?: 0,
            textureQuality = genericDocument.int("TextureQuality") ?: 2,
            shadowQuality = genericDocument.int("ShadowQuality") ?: 2,
            viewDistanceQuality = genericDocument.int("ViewDistanceQuality") ?: 2,
            visualEffectQuality = genericDocument.int("VisualEffectQuality") ?: 2,
            globalIlluminationQuality = genericDocument.int("GlobalIlluminationQuality") ?: 2,
            reflectionQuality = genericDocument.int("ReflectionQuality") ?: 2,
            postProcessingQuality = genericDocument.int("PostProcessingQuality") ?: 2,
            foliageQuality = genericDocument.int("FoliageQuality") ?: 2,
            shadingQuality = genericDocument.int("ShadingQuality") ?: 2,
            mainVolume = gameDocument.double("MainVolume") ?: 100.0,
            musicVolume = gameDocument.double("MusicVolume") ?: 100.0,
            voiceVolume = gameDocument.double("VoiceVolume") ?: 100.0,
            envVolume = gameDocument.double("EnvVolume") ?: 100.0,
            widgetVolume = gameDocument.double("WidgetVolume") ?: 100.0,
            defaultAudioInputDeviceName = gameDocument.string("DefaultAudioInputDeviceName") ?: "",
            defaultAudioOutputDeviceName = gameDocument.string("DefaultAudioOutputDeviceName") ?: "",
            defaultMusicPlayerFolder = gameDocument.string("DefaultMusicPlayerFolder") ?: "",
            animBudgetAllocator = gameDocument.double("AnimBudgetAllocator") ?: 1.0,
            cameraArmScale = gameDocument.double("CameraArmScale") ?: 1.0,
            cameraMoveLag = gameDocument.double("CameraMoveLag") ?: 0.1,
            cameraMoveSpeedScale = gameDocument.double("CameraMoveSpeedScale") ?: 1.0,
            cameraRotationScale = gameDocument.double("CameraRotationScale") ?: 1.0,
            sceneElementUpdate = gameDocument.double("SceneElementUpdate") ?: 1.0,
            serverMovePathFindPipeNum = gameDocument.int("ServerMovePathFindPipeNum") ?: 12,
            unitFindPipeNum = gameDocument.int("UnitFindPipeNum") ?: 12,
            colors = gameDocument.readRgbaColorSections(),
        )
    }

    /**
     * 将 [GameSettings] 中的通用设置写入 [SimpleTomlDocument]。
     *
     * 修改的键对应 GenericSetting.toml 中的画面、性能、升采样等设置。
     * 文档中原有的未知键、注释和空白行原样保留。
     */
    private fun writeGenericSettings(document: SimpleTomlDocument, settings: GameSettings) {
        document.setString("Language", settings.language)
        document.setString("FullscreenMode", settings.fullscreenMode)
        document.setBoolean("DynamicResolution", settings.dynamicResolution)
        document.setBoolean("UseOverallScalabilityLevel", settings.useOverallScalabilityLevel)
        document.setInt("OverallScalabilityLevel", settings.overallScalabilityLevel)
        document.setInt("AudioQualityLevel", settings.audioQualityLevel)
        document.setBoolean("HdrDisplay", settings.hdrDisplay)
        document.setDouble("FrameRateLimit", settings.frameRateLimit)
        document.setDouble("MotionBlurAmount", settings.motionBlurAmount)
        document.setDouble("MotionBlurMax", settings.motionBlurMax)
        document.setDouble("MotionBlurScale", settings.motionBlurScale)
        document.setDouble("MotionBlurTargetFPS", settings.motionBlurTargetFps)
        document.setInt("Resolution.X", settings.resolutionWidth)
        document.setInt("Resolution.Y", settings.resolutionHeight)
        document.setBoolean("HonkaiUpscalerInfo.VSync", settings.vSync)
        document.setDouble("HonkaiUpscalerInfo.ResolutionScale", settings.resolutionScale)
        document.setString("HonkaiUpscalerInfo.UpscalerName", settings.upscalerName)
        document.setInt("HonkaiUpscalerInfo.AntiAliasingQuality", settings.antiAliasingQuality)
        document.setInt("HonkaiUpscalerInfo.FSR", settings.fsr)
        document.setInt("HonkaiUpscalerInfo.FrameGeneration", settings.frameGeneration)
        document.setInt("HonkaiUpscalerInfo.FrameWarp", settings.frameWarp)
        document.setInt("HonkaiUpscalerInfo.NIS", settings.nis)
        document.setInt("HonkaiUpscalerInfo.NIS_Sharpness", settings.nisSharpness)
        document.setInt("HonkaiUpscalerInfo.RayReconstruction", settings.rayReconstruction)
        document.setInt("HonkaiUpscalerInfo.Reflex", settings.reflex)
        document.setInt("HonkaiUpscalerInfo.SuperResolution", settings.superResolution)
        document.setInt("TextureQuality", settings.textureQuality)
        document.setInt("ShadowQuality", settings.shadowQuality)
        document.setInt("ViewDistanceQuality", settings.viewDistanceQuality)
        document.setInt("VisualEffectQuality", settings.visualEffectQuality)
        document.setInt("GlobalIlluminationQuality", settings.globalIlluminationQuality)
        document.setInt("ReflectionQuality", settings.reflectionQuality)
        document.setInt("PostProcessingQuality", settings.postProcessingQuality)
        document.setInt("FoliageQuality", settings.foliageQuality)
        document.setInt("ShadingQuality", settings.shadingQuality)
    }

    /**
     * 将 [GameSettings] 中的游戏内设置写入 [SimpleTomlDocument]。
     *
     * 修改的键对应 GameSetting.toml 中的音频、相机、颜色、管道数等设置。
     * 动态颜色表按 `[SectionName]` 结构写回，不会破坏未知表。
     */
    private fun writeGameSettings(document: SimpleTomlDocument, settings: GameSettings) {
        document.setDouble("MainVolume", settings.mainVolume)
        document.setDouble("MusicVolume", settings.musicVolume)
        document.setDouble("VoiceVolume", settings.voiceVolume)
        document.setDouble("EnvVolume", settings.envVolume)
        document.setDouble("WidgetVolume", settings.widgetVolume)
        document.setString("DefaultAudioInputDeviceName", settings.defaultAudioInputDeviceName)
        document.setString("DefaultAudioOutputDeviceName", settings.defaultAudioOutputDeviceName)
        document.setString("DefaultMusicPlayerFolder", settings.defaultMusicPlayerFolder)
        document.setDouble("AnimBudgetAllocator", settings.animBudgetAllocator)
        document.setDouble("CameraArmScale", settings.cameraArmScale)
        document.setDouble("CameraMoveLag", settings.cameraMoveLag)
        document.setDouble("CameraMoveSpeedScale", settings.cameraMoveSpeedScale)
        document.setDouble("CameraRotationScale", settings.cameraRotationScale)
        document.setDouble("SceneElementUpdate", settings.sceneElementUpdate)
        document.setInt("ServerMovePathFindPipeNum", settings.serverMovePathFindPipeNum)
        document.setInt("UnitFindPipeNum", settings.unitFindPipeNum)
        settings.colors.forEach { color ->
            document.setDouble("${color.name}.R", color.value.red)
            document.setDouble("${color.name}.G", color.value.green)
            document.setDouble("${color.name}.B", color.value.blue)
            document.setDouble("${color.name}.A", color.value.alpha)
        }
    }

    private companion object {
        /** GenericSetting.toml 文件名：通用画面与性能设置 */
        const val GENERIC_SETTING_FILE = "GenericSetting.toml"
        /** GameSetting.toml 文件名：音频、相机、颜色等游戏内设置 */
        const val GAME_SETTING_FILE = "GameSetting.toml"
    }
}

/**
 * 轻量级可编辑 TOML 文档。
 *
 * 在保持行序、注释和空白行的前提下，提供按「点分路径」读写的能力。
 * 与只读的 [TomlReader] 不同，此类用于**保存设置**场景，需要将修改写回原文件。
 *
 * ## 数据结构
 * - [lines]：按序保存每一行的原始文本（可修改）
 * - [values]：按「section.key」路径索引每行的 [TomlEntry]，包含行号、键、值、缩进
 *
 * ## 读操作
 * 通过 [string] / [int] / [double] / [boolean] 按路径查询；
 * 通过 [sectionNames] 枚举所有 `[SectionName]` 表头；
 * 通过 [readRgbaColorSections] 动态发现 RGBA 颜色表。
 *
 * ## 写操作
 * `set*` 系列方法会：
 * - 如果路径已存在：原地替换该行的值（保留缩进、注释和未知键）
 * - 如果路径不存在：在所属 section 末尾插入新行，若无 section 则新建表头
 *
 * ## 限制
 * - 不支持数组表（`[[...]]`）
 * - 不支持多行字符串
 * - 路径以 section.key 形式表达，如 `"Resolution.X"`
 */
private class SimpleTomlDocument private constructor(
    private val lines: MutableList<String>,
    private val values: MutableMap<String, TomlEntry>,
) {
    /** 查询字符串值（自动去除 TOML 引号） */
    fun string(path: String): String? = values[path]?.value?.trimTomlString()

    /** 查询整数值 */
    fun int(path: String): Int? = values[path]?.value?.toIntOrNull()

    /** 查询浮点值 */
    fun double(path: String): Double? = values[path]?.value?.toDoubleOrNull()

    /** 查询布尔值 */
    fun boolean(path: String): Boolean? = values[path]?.value?.toBooleanStrictOrNull()

    /**
     * 动态枚举所有 RGBA 颜色表。
     *
     * 遍历所有 `[SectionName]` 表头，若某个表同时包含 `R`/`G`/`B`/`A` 通道值，
     * 则将其识别为颜色设置。缺失通道用默认值填充（A 默认 1.0，其余 0.0）。
     */
    fun readRgbaColorSections(): List<GameColorSetting> {
        return sectionNames()
            .filter { section ->
                listOf("R", "G", "B", "A").all { channel -> double("$section.$channel") != null }
            }
            .map { section ->
                GameColorSetting(
                    name = section,
                    value = GameRgbaColor(
                        red = double("$section.R") ?: 0.0,
                        green = double("$section.G") ?: 0.0,
                        blue = double("$section.B") ?: 0.0,
                        alpha = double("$section.A") ?: 1.0,
                    ),
                )
            }
    }

    /** 设置字符串值（自动处理 TOML 引号转义） */
    fun setString(path: String, value: String) = set(path, "\"${value.escapeBasicTomlString()}\"")

    /** 设置整数值 */
    fun setInt(path: String, value: Int) = set(path, value.toString())

    /** 设置浮点值 */
    fun setDouble(path: String, value: Double) = set(path, value.toString())

    /** 设置布尔值 */
    fun setBoolean(path: String, value: Boolean) = set(path, value.toString())

    /**
     * 将文档渲染为完整 TOML 文本。
     * 使用系统换行符连接所有行，包括修改后的行和保留的原行。
     */
    fun render(): String = lines.joinToString(System.lineSeparator())

    /**
     * 设置指定路径的值。
     *
     * - 若路径已存在：原地替换该行的值，保留缩进格式
     * - 若路径不存在：在对应 section 末尾插入新行；如果 section 不存在则新建 `[SectionName]` 表头
     *
     * @param path            点分路径，如 `"Resolution.X"`
     * @param serializedValue 已序列化为字符串的值
     */
    private fun set(path: String, serializedValue: String) {
        val existing = values[path]
        if (existing != null) {
            lines[existing.lineIndex] = "${existing.indent}${existing.key} = $serializedValue"
            values[path] = existing.copy(value = serializedValue)
            return
        }

        val section = path.substringBeforeLast('.', missingDelimiterValue = "")
        val key = path.substringAfterLast('.')
        val insertIndex = findInsertIndex(section)
        val newLine = "$key = $serializedValue"
        lines.add(insertIndex, newLine)
        rebuildValues()
    }

    /**
     * 为新键值对找到合适的插入行号。
     *
     * - 顶层键（section 为空）：插在第一个 `[Table]` 行之前（或文件末尾）
     * - 表内键：插在对应 `[SectionName]` 表的下一个表头之前（或文件末尾）
     * - 如果表头不存在：先新建空行和表头，插入在文件末尾
     *
     * @param section 所属表名，空字符串表示顶层
     * @return 新行应插入的行号索引
     */
    private fun findInsertIndex(section: String): Int {
        if (section.isBlank()) {
            return lines.indexOfFirst { it.trim().startsWith("[") }.takeIf { it >= 0 } ?: lines.size
        }

        val headerIndex = lines.indexOfFirst { it.trim() == "[$section]" }
        if (headerIndex < 0) {
            if (lines.isNotEmpty() && lines.last().isNotBlank()) {
                lines.add("")
            }
            lines.add("[$section]")
            return lines.size
        }

        val nextSectionIndex = lines
            .drop(headerIndex + 1)
            .indexOfFirst { it.trim().startsWith("[") }
        return if (nextSectionIndex < 0) lines.size else headerIndex + 1 + nextSectionIndex
    }

    /** 重建内部索引。在修改 [lines] 后调用以保持一致性。 */
    private fun rebuildValues() {
        values.clear()
        values.putAll(indexLines(lines))
    }

    /**
     * 枚举所有原始表（非数组表）的表名。
     *
     * 遍历所有行，筛选出格式为 `[SectionName]`（不以 `[[` 开头）的行，
     * 提取方括号内的内容作为表名。
     */
    private fun sectionNames(): List<String> {
        return lines.mapNotNull { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.startsWith("[[")) {
                trimmed.removePrefix("[").removeSuffix("]").trim()
            } else {
                null
            }
        }
    }

    companion object {
        /**
         * 解析 TOML 文本为可编辑的 [SimpleTomlDocument]。
         *
         * 统一换行符为 `\n`，移除末尾空行。
         *
         * @param content TOML 原始文本
         * @return 可编辑的文档实例
         */
        fun parse(content: String): SimpleTomlDocument {
            val lines = content.replace("\r\n", "\n").replace('\r', '\n').split('\n').toMutableList()
            if (lines.lastOrNull() == "") {
                lines.removeAt(lines.lastIndex)
            }
            return SimpleTomlDocument(lines, indexLines(lines).toMutableMap())
        }

        /**
         * 按行索引 TOML 内容，构建「点分路径 → [TomlEntry]」映射。
         *
         * 遍历每一行：
         * - 空白行和注释行跳过
         * - `[SectionName]` 行更新当前 section 上下文
         * - `key = value` 行生成 `section.key` 路径索引
         *
         * @param lines 文档的每行文本
         * @return 路径到条目的映射
         */
        private fun indexLines(lines: List<String>): Map<String, TomlEntry> {
            val values = mutableMapOf<String, TomlEntry>()
            var section = ""

            lines.forEachIndexed { index, rawLine ->
                val trimmed = rawLine.trim()
                if (trimmed.isBlank() || trimmed.startsWith("#")) return@forEachIndexed
                if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.startsWith("[[")) {
                    section = trimmed.removePrefix("[").removeSuffix("]").trim()
                    return@forEachIndexed
                }

                val separatorIndex = rawLine.indexOf('=')
                if (separatorIndex <= 0) return@forEachIndexed

                val key = rawLine.substring(0, separatorIndex).trim()
                val value = stripInlineComment(rawLine.substring(separatorIndex + 1)).trim()
                val indent = rawLine.takeWhile { it.isWhitespace() }
                val path = if (section.isBlank()) key else "$section.$key"
                values[path] = TomlEntry(index, key, value, indent)
            }

            return values
        }

        /**
         * 去除行内注释。
         *
         * 从行首扫描，遇到首个不在引号字符串内的 `#` 时截断该行。
         * 这确保引号内的 `#`（如 `"#ff0000"`）不会被误判为注释。
         *
         * @param value 等号右侧的原始值文本
         * @return 去除注释后的值文本
         */
        private fun stripInlineComment(value: String): String {
            var inSingleQuotedString = false
            var inDoubleQuotedString = false

            value.forEachIndexed { index, char ->
                when (char) {
                    '\'' -> if (!inDoubleQuotedString) inSingleQuotedString = !inSingleQuotedString
                    '"' -> if (!inSingleQuotedString) inDoubleQuotedString = !inDoubleQuotedString
                    '#' -> if (!inSingleQuotedString && !inDoubleQuotedString) return value.substring(0, index)
                }
            }

            return value
        }
    }
}

/**
 * TOML 键值对的行级索引条目。
 *
 * 记录某一行在文档中的完整信息，用于原地编辑时的定位和格式保留。
 *
 * @property lineIndex 在 [SimpleTomlDocument.lines] 中的行号
 * @property key       键名（不含 section 前缀）
 * @property value     值文本（去除注释后，可能仍含 TOML 引号）
 * @property indent    该行的缩进字符串（空白前缀），用于写回时保持格式
 */
private data class TomlEntry(
    val lineIndex: Int,
    val key: String,
    val value: String,
    val indent: String,
)

/**
 * 去除 TOML 字符串值的外层引号。
 *
 * 如果字符串被单引号或双引号包裹（首尾字符相同），则剥离外层引号返回内部内容；
 * 否则原样返回。
 *
 * @receiver 可能被 TOML 引号包裹的字符串值
 * @return 去引号后的字符串内容
 */
private fun String.trimTomlString(): String {
    val trimmed = trim()
    return if (trimmed.length >= 2 &&
        ((trimmed.first() == '\'' && trimmed.last() == '\'') || (trimmed.first() == '"' && trimmed.last() == '"'))
    ) {
        trimmed.substring(1, trimmed.lastIndex)
    } else {
        trimmed
    }
}

/**
 * 对字符串进行基本 TOML 字符串转义。
 *
 * 将特殊字符转义为 TOML 双引号字符串中的合法序列：
 * - `\` → `\\`
 * - `"` → `\"`
 * - 换行 → `\n`
 * - 回车 → `\r`
 * - 制表符 → `\t`
 *
 * @receiver 原始字符串值
 * @return 转义后的字符串，可直接放入 TOML 双引号字符串内
 */
private fun String.escapeBasicTomlString(): String {
    return buildString {
        this@escapeBasicTomlString.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}

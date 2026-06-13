package core.config

import core.platform.AppSettingsRepository
import java.io.File

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

data class GameColorSetting(
    val name: String,
    val value: GameRgbaColor,
)

data class GameRgbaColor(
    val red: Double,
    val green: Double,
    val blue: Double,
    val alpha: Double,
)

enum class GameSettingsStatus {
    Ready,
    MissingGamePath,
    MissingConfig,
    Error,
}

data class GameSettingsResult(
    val status: GameSettingsStatus,
    val settings: GameSettings? = null,
    val configDirectory: String = "",
    val message: String = "",
)

class GameSettingsService {
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
            val genericDocument = SimpleTomlDocument.parse(genericFile.readText())
            val gameDocument = SimpleTomlDocument.parse(gameFile.readText())
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

    private fun resolveConfigDirectory(path: String?): File? {
        val normalizedPath = AppSettingsRepository.normalizeGamePath(path) ?: return null
        val gameFile = File(normalizedPath)
        val gameDirectory = if (gameFile.isDirectory) gameFile else gameFile.parentFile ?: return null
        return File(File(gameDirectory, "honkai_rts"), "Config")
    }

    private fun readSettings(
        genericDocument: SimpleTomlDocument,
        gameDocument: SimpleTomlDocument,
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
        const val GENERIC_SETTING_FILE = "GenericSetting.toml"
        const val GAME_SETTING_FILE = "GameSetting.toml"
    }
}

private class SimpleTomlDocument private constructor(
    private val lines: MutableList<String>,
    private val values: MutableMap<String, TomlEntry>,
) {
    fun string(path: String): String? = values[path]?.value?.trimTomlString()

    fun int(path: String): Int? = values[path]?.value?.toIntOrNull()

    fun double(path: String): Double? = values[path]?.value?.toDoubleOrNull()

    fun boolean(path: String): Boolean? = values[path]?.value?.toBooleanStrictOrNull()

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

    fun setString(path: String, value: String) = set(path, "\"${value.escapeBasicTomlString()}\"")

    fun setInt(path: String, value: Int) = set(path, value.toString())

    fun setDouble(path: String, value: Double) = set(path, value.toString())

    fun setBoolean(path: String, value: Boolean) = set(path, value.toString())

    fun render(): String = lines.joinToString(System.lineSeparator())

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

    private fun rebuildValues() {
        values.clear()
        values.putAll(indexLines(lines))
    }

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
        fun parse(content: String): SimpleTomlDocument {
            val lines = content.replace("\r\n", "\n").replace('\r', '\n').split('\n').toMutableList()
            if (lines.lastOrNull() == "") {
                lines.removeAt(lines.lastIndex)
            }
            return SimpleTomlDocument(lines, indexLines(lines).toMutableMap())
        }

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

private data class TomlEntry(
    val lineIndex: Int,
    val key: String,
    val value: String,
    val indent: String,
)

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

package core.config

import core.withTempGameFixture
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameSettingsServiceTest {
    private val service = GameSettingsService()

    @Test
    fun `load reads game settings from config directory next to executable`() = withTempGameFixture { fixture ->
        val executable = fixture.root.resolve("HonkaiRTS.exe").createFile()
        val config = fixture.root.resolve("honkai_rts").resolve("Config").createDirectories()
        config.resolve("GenericSetting.toml").writeText(GENERIC_SETTINGS)
        config.resolve("GameSetting.toml").writeText(GAME_SETTINGS)

        val result = service.load(executable.toString())

        assertEquals(GameSettingsStatus.Ready, result.status)
        val settings = assertNotNull(result.settings)
        assertEquals("zh-Hans", settings.language)
        assertEquals("WindowedFullscreen", settings.fullscreenMode)
        assertEquals(1920, settings.resolutionWidth)
        assertEquals(1080, settings.resolutionHeight)
        assertEquals(true, settings.vSync)
        assertEquals(true, settings.hdrDisplay)
        assertEquals(0.5, settings.motionBlurAmount)
        assertEquals(1, settings.fsr)
        assertEquals(2, settings.nisSharpness)
        assertEquals(80.0, settings.mainVolume)
        assertEquals("Microphone-1", settings.defaultAudioInputDeviceName)
        assertEquals("D:/Music", settings.defaultMusicPlayerFolder)
        assertEquals(1.5, settings.cameraMoveSpeedScale)
        assertEquals(8, settings.unitFindPipeNum)
        assertEquals(
            GameColorSetting(
                name = "AttackLineColor",
                value = GameRgbaColor(red = 1.0, green = 0.25, blue = 0.5, alpha = 0.75),
            ),
            settings.colors.first(),
        )
    }

    @Test
    fun `save updates known settings and preserves unknown values`() = withTempGameFixture { fixture ->
        val executable = fixture.root.resolve("HonkaiRTS.exe").createFile()
        val config = fixture.root.resolve("honkai_rts").resolve("Config").createDirectories()
        val genericFile = config.resolve("GenericSetting.toml")
        val gameFile = config.resolve("GameSetting.toml")
        genericFile.writeText(GENERIC_SETTINGS)
        gameFile.writeText(GAME_SETTINGS)

        val result = service.save(
            executable.toString(),
            GameSettings(
                language = "en",
                fullscreenMode = "Windowed",
                resolutionWidth = 2560,
                resolutionHeight = 1440,
                vSync = false,
                hdrDisplay = false,
                fsr = 2,
                frameGeneration = 1,
                mainVolume = 55.0,
                musicVolume = 44.0,
                defaultMusicPlayerFolder = "E:/Music",
                unitFindPipeNum = 16,
                colors = listOf(
                    GameColorSetting(
                        name = "AttackLineColor",
                        value = GameRgbaColor(red = 0.1, green = 0.2, blue = 0.3, alpha = 0.4),
                    ),
                ),
            ),
        )

        assertEquals(GameSettingsStatus.Ready, result.status)
        val genericContent = genericFile.readText()
        val gameContent = gameFile.readText()
        assertTrue(genericContent.contains("Language = \"en\""))
        assertTrue(genericContent.contains("FullscreenMode = \"Windowed\""))
        assertTrue(genericContent.contains("X = 2560"))
        assertTrue(genericContent.contains("VSync = false"))
        assertTrue(genericContent.contains("HdrDisplay = false"))
        assertTrue(genericContent.contains("FSR = 2"))
        assertTrue(genericContent.contains("FrameGeneration = 1"))
        assertTrue(genericContent.contains("UnknownGenericValue = 123"))
        assertTrue(gameContent.contains("MainVolume = 55.0"))
        assertTrue(gameContent.contains("MusicVolume = 44.0"))
        assertTrue(gameContent.contains("DefaultMusicPlayerFolder = \"E:/Music\""))
        assertTrue(gameContent.contains("UnitFindPipeNum = 16"))
        assertTrue(gameContent.contains("[AttackLineColor]"))
        assertTrue(gameContent.contains("R = 0.1"))
        assertTrue(gameContent.contains("G = 0.2"))
        assertTrue(gameContent.contains("B = 0.3"))
        assertTrue(gameContent.contains("A = 0.4"))
        assertTrue(gameContent.contains("UnknownGameValue = 'keep me'"))
    }

    @Test
    fun `load returns missing game path when path is not configured`() {
        val result = service.load(null)

        assertEquals(GameSettingsStatus.MissingGamePath, result.status)
        assertNull(result.settings)
    }

    @Test
    fun `load returns missing config when config files are absent`() = withTempGameFixture { fixture ->
        val executable = fixture.root.resolve("HonkaiRTS.exe").createFile()

        val result = service.load(executable.toString())

        assertEquals(GameSettingsStatus.MissingConfig, result.status)
        assertNull(result.settings)
    }

    private companion object {
        val GENERIC_SETTINGS = """
            Language = 'zh-Hans'
            AudioQualityLevel = 1
            FullscreenMode = 'WindowedFullscreen'
            DynamicResolution = true
            HdrDisplay = true
            UseOverallScalabilityLevel = true
            OverallScalabilityLevel = 3
            FrameRateLimit = 120.0
            MotionBlurAmount = 0.5
            MotionBlurMax = 2.0
            MotionBlurScale = 1.5
            MotionBlurTargetFPS = 45.0
            TextureQuality = 2
            ShadowQuality = 2
            ViewDistanceQuality = 2
            VisualEffectQuality = 2
            GlobalIlluminationQuality = 2
            ReflectionQuality = 2
            PostProcessingQuality = 2
            FoliageQuality = 2
            ShadingQuality = 2
            UnknownGenericValue = 123

            [HonkaiUpscalerInfo]
            VSync = true
            ResolutionScale = 90.0
            UpscalerName = 'TSR'
            AntiAliasingQuality = 2
            FSR = 1
            FrameGeneration = 0
            FrameWarp = 1
            NIS = 3
            NIS_Sharpness = 2
            RayReconstruction = 0
            Reflex = 1
            SuperResolution = 2

            [Resolution]
            X = 1920
            Y = 1080
        """.trimIndent()

        val GAME_SETTINGS = """
            MainVolume = 80.0
            MusicVolume = 70.0
            VoiceVolume = 60.0
            EnvVolume = 50.0
            WidgetVolume = 40.0
            DefaultAudioInputDeviceName = 'Microphone-1'
            DefaultAudioOutputDeviceName = 'Speaker-1'
            DefaultMusicPlayerFolder = 'D:/Music'
            AnimBudgetAllocator = 1.25
            CameraArmScale = 1.1
            CameraMoveLag = 0.2
            CameraMoveSpeedScale = 1.5
            CameraRotationScale = 1.25
            SceneElementUpdate = 0.75
            ServerMovePathFindPipeNum = 6
            UnitFindPipeNum = 8
            UnknownGameValue = 'keep me'

            [AttackLineColor]
            R = 1.0
            G = 0.25
            B = 0.5
            A = 0.75

            [NotAColor]
            R = 1.0
            G = 1.0
        """.trimIndent()
    }
}

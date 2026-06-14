package screenmodel

import core.GameConnectionStatus
import core.platform.AppSettingsRepository
import core.service.GamePathService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import screenmodel.HomeLaunchStatus
import screenmodel.HomeScreenModel
import ui.settings.AppSettingsStore
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModelTest {
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank game path maps to missing game path`() = runTest {
        val model = createModel(gamePath = null)
        advanceUntilIdle()

        assertEquals(HomeLaunchStatus.MissingGamePath, model.uiState.launchStatus)
        assertEquals("missing-game-path", model.uiState.statusMessage)
    }

    @Test
    fun `missing executable maps to missing executable`() = runTest {
        withTempGameFixture { fixture ->
            val missingExecutable = fixture.root.resolve("missing.exe").toFile().absolutePath

            val model = createModel(gamePath = missingExecutable)
            advanceUntilIdle()

            assertEquals(HomeLaunchStatus.MissingExecutable, model.uiState.launchStatus)
            assertEquals("missing-executable", model.uiState.statusMessage)
            assertEquals("missing.exe", model.uiState.gameFileName)
            assertEquals(fixture.root.toFile().absolutePath, model.uiState.gameDirectory)
        }
    }

    @Test
    fun `existing executable maps to ready with plugin metadata`() = runTest {
        withTempGameFixture { fixture ->
            val executable = fixture.root.resolve("Honkai.exe").createFile().toFile().absolutePath
            val config = fixture.plugins.resolve("GamePluginConfigs.toml").createFile()
            config.writeText(
                """
                [[PluginConfigs]]
                Name = "One"
                [[PluginConfigs]]
                Name = "Two"
                """.trimIndent()
            )

            val model = createModel(gamePath = executable)
            advanceUntilIdle()

            assertEquals(HomeLaunchStatus.Ready, model.uiState.launchStatus)
            assertEquals("Honkai.exe", model.uiState.gameFileName)
            assertEquals(config.toFile().absolutePath, model.uiState.pluginConfigPath)
            assertEquals(2, model.uiState.pluginCount)
        }
    }

    @Test
    fun `connected game status maps launch status to running`() = runTest {
        withTempGameFixture { fixture ->
            val executable = fixture.root.resolve("Honkai.exe").createFile().toFile().absolutePath
            val connectionStatus = MutableStateFlow(GameConnectionStatus.Waiting)

            val model = createModel(
                gamePath = executable,
                connectionStatus = connectionStatus,
            )
            advanceUntilIdle()

            connectionStatus.value = GameConnectionStatus.Connected
            advanceUntilIdle()

            assertEquals(GameConnectionStatus.Connected, model.uiState.gameConnectionStatus)
            assertEquals(HomeLaunchStatus.Running, model.uiState.launchStatus)
        }
    }

    private fun TestScope.createModel(
        gamePath: String?,
        connectionStatus: MutableStateFlow<GameConnectionStatus> = MutableStateFlow(GameConnectionStatus.Stopped),
    ): HomeScreenModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return HomeScreenModel(
            settingsStore = AppSettingsStore(FakeAppSettingsRepository(gamePath)),
            gamePathService = GamePathService(),
            gameConnectionStatus = connectionStatus,
        )
    }

    private class FakeAppSettingsRepository(gamePath: String?) : AppSettingsRepository {
        private var storedGamePath = AppSettingsRepository.normalizeGamePath(gamePath)

        override fun getGamePath(): String? {
            return storedGamePath
        }

        override fun setGamePath(path: String?) {
            storedGamePath = AppSettingsRepository.normalizeGamePath(path)
        }

        override fun getLogMaxEntries(defaultValue: Int): Int {
            return defaultValue
        }
    }

    private data class TempGameFixture(
        val root: Path,
        val plugins: Path,
    )

    private fun withTempGameFixture(block: (TempGameFixture) -> Unit) {
        val root = createTempDirectory("honkai-home-fixture")
        val plugins = root.resolve("honkai_rts").resolve("GamePlugins").createDirectories()
        block(TempGameFixture(root, plugins))
    }
}

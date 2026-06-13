package core.service

import core.withTempGameFixture
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GamePathServiceTest {
    private val service = GamePathService()

    @Test
    fun `blank and sentinel paths report missing game path`() {
        assertEquals("missing-game-path", service.inspect("").message)
        assertEquals("missing-game-path", service.inspect("null").message)
        assertEquals("missing-game-path", service.inspect(null).message)
    }

    @Test
    fun `missing executable reports inferred file and directory`() {
        withTempGameFixture { fixture ->
            val missingExecutable = fixture.root.resolve("missing.exe").toFile()

            val snapshot = service.inspect(missingExecutable.absolutePath)

            assertFalse(snapshot.executableExists)
            assertEquals("missing.exe", snapshot.gameFileName)
            assertEquals(fixture.root.toFile().absolutePath, snapshot.gameDirectory)
            assertEquals("missing-executable", snapshot.message)
        }
    }

    @Test
    fun `executable path resolves game directory and plugin config`() {
        withTempGameFixture { fixture ->
            val executable = fixture.root.resolve("Honkai.exe").createFile()
            val config = fixture.plugins.resolve("GamePluginConfigs.toml").createFile()
            config.writeText(
                """
                [[PluginConfigs]]
                Name = "One"
                [[PluginConfigs]]
                Name = "Two"
                """.trimIndent()
            )

            val snapshot = service.inspect(executable.toFile().absolutePath)

            assertTrue(snapshot.executableExists)
            assertEquals("Honkai.exe", snapshot.gameFileName)
            assertEquals(fixture.root.toFile().absolutePath, snapshot.gameDirectory)
            assertEquals(config.toFile().absolutePath, snapshot.pluginConfigPath)
            assertEquals(2, snapshot.pluginCount)
        }
    }

    @Test
    fun `directory path is accepted but is not an executable`() {
        withTempGameFixture { fixture ->
            val snapshot = service.inspect(fixture.root.toFile().absolutePath)

            assertFalse(snapshot.executableExists)
            assertEquals(fixture.root.fileName.toString(), snapshot.gameFileName)
            assertEquals(fixture.root.toFile().absolutePath, snapshot.gameDirectory)
            assertEquals("", snapshot.message)
        }
    }
}

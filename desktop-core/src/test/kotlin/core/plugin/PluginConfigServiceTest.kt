package core.plugin

import core.withTempGameFixture
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginConfigServiceTest {
    private val service = PluginConfigService()

    @Test
    fun `load reports missing game path and missing config`() {
        assertEquals(PluginLoadStatus.MissingGamePath, service.load("null").status)

        withTempGameFixture { fixture ->
            val executable = fixture.root.resolve("Honkai.exe").createFile()
            val result = service.load(executable.toFile().absolutePath)

            assertEquals(PluginLoadStatus.MissingConfig, result.status)
            assertEquals(fixture.plugins.resolve("GamePluginConfigs.toml").toFile().absolutePath, result.configPath)
        }
    }

    @Test
    fun `load parses plugin config from game directory`() {
        withTempGameFixture { fixture ->
            val executable = fixture.root.resolve("Honkai.exe").createFile()
            fixture.plugins.resolve("GamePluginConfigs.toml").createFile().writeText(
                """
                [[PluginConfigs]]
                Name = "PluginA"
                PakPath = "PluginA.pak"
                """.trimIndent()
            )

            val result = service.load(executable.toFile().absolutePath)

            assertEquals(PluginLoadStatus.Ready, result.status)
            assertEquals(1, result.plugins.size)
            assertEquals("PluginA", result.plugins.single().name)
        }
    }
}

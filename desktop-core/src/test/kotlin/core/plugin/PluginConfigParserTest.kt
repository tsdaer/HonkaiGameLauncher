package core.plugin

import core.withTempGameFixture
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluginConfigParserTest {
    private val parser = PluginConfigParser()

    @Test
    fun `parses basic fields comments empty values and pak paths`() {
        withTempGameFixture { fixture ->
            val plugins = parser.parse(
                content = """
                    # File comment
                    [[PluginConfigs]]
                    Category = "Combat" # inline comment
                    CreatedBy = "Alice # not comment"
                    CreatedByURL = ""
                    DefaultEnable = true
                    Description = 'Adds features'
                    FriendlyName = "Friendly"
                    GameFeatureName = "Feature"
                    MountOrder = 42
                    Name = "PluginA"
                    PakPath = "PluginA.pak"
                    
                    [[PluginConfigs]]
                    Name = "BuiltIn"
                    PakPath = "BuiltInPlugin"
                """.trimIndent(),
                pluginDirectory = fixture.plugins.toFile(),
            )

            assertEquals(2, plugins.size)
            assertEquals("Combat", plugins[0].category)
            assertEquals("Alice # not comment", plugins[0].createdBy)
            assertEquals("", plugins[0].createdByUrl)
            assertTrue(plugins[0].defaultEnable)
            assertEquals("Adds features", plugins[0].description)
            assertEquals("Friendly", plugins[0].friendlyName)
            assertEquals("Feature", plugins[0].gameFeatureName)
            assertEquals(42, plugins[0].mountOrder)
            assertEquals("PluginA", plugins[0].name)
            assertFalse(plugins[0].isBuiltIn)
            assertEquals(fixture.plugins.resolve("PluginA.pak").toFile().absolutePath, plugins[0].resolvedPakPath)
            assertTrue(plugins[1].isBuiltIn)
            assertNull(plugins[1].resolvedPakPath)
        }
    }

    @Test
    fun `missing boolean and mount order use safe defaults`() {
        val plugin = parser.parse(
            content = """
                [[PluginConfigs]]
                Name = "PluginA"
                DefaultEnable = maybe
                MountOrder = first
            """.trimIndent(),
            pluginDirectory = createTempDirectory().toFile(),
        ).single()

        assertFalse(plugin.defaultEnable)
        assertNull(plugin.mountOrder)
    }
}

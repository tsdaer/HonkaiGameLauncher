package core.docs

import core.plugin.GamePluginConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PluginDocsLinkerTest {
    private val linker = PluginDocsLinker()

    @Test
    fun `pak plugin links to GamePlugins document with same file name`() {
        val plugin = plugin(name = "PluginA", pakPath = "Paks/PluginA.pak")
        val document = doc(relativePath = "GamePlugins/PluginA.md")

        val result = linker.link(listOf(plugin), listOf(document))

        assertEquals(document, result.pluginDocumentByPluginName["PluginA"])
        assertEquals("PluginA", result.pluginNameByDocumentPath["GamePlugins/PluginA.md"])
    }

    @Test
    fun `plugin document matching ignores case`() {
        val plugin = plugin(name = "PluginA", pakPath = "plugina.PAK")
        val document = doc(relativePath = "GamePlugins/PluginA.md")

        val result = linker.link(listOf(plugin), listOf(document))

        assertEquals(document, result.pluginDocumentByPluginName["PluginA"])
    }

    @Test
    fun `built in plugin and general document are not linked`() {
        val builtInPlugin = plugin(name = "BuiltIn", pakPath = "BuiltInFeature")
        val pakPlugin = plugin(name = "PluginA", pakPath = "PluginA.pak")
        val generalDocument = doc(relativePath = "PluginA.md", section = DocSection.General)

        val result = linker.link(listOf(builtInPlugin, pakPlugin), listOf(generalDocument))

        assertNull(result.pluginDocumentByPluginName["BuiltIn"])
        assertNull(result.pluginDocumentByPluginName["PluginA"])
        assertEquals(emptyMap(), result.pluginNameByDocumentPath)
    }

    private fun plugin(name: String, pakPath: String): GamePluginConfig {
        return GamePluginConfig(
            category = "",
            createdBy = "",
            createdByUrl = "",
            defaultEnable = true,
            description = "",
            friendlyName = "",
            gameFeatureName = "",
            mountOrder = null,
            name = name,
            pakPath = pakPath,
            resolvedPakPath = null,
        )
    }

    private fun doc(
        relativePath: String,
        section: DocSection = DocSection.GamePlugins,
    ): DocEntry {
        return DocEntry(
            title = relativePath.substringAfterLast('/').substringBeforeLast('.'),
            absolutePath = "C:/$relativePath",
            relativePath = relativePath,
            section = section,
        )
    }
}

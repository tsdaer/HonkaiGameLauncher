package core.docs

import core.withTempGameFixture
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DocsIndexServiceTest {
    private val service = DocsIndexService()

    @Test
    fun `load reports missing game path and missing docs directory`() {
        assertEquals(DocsLoadStatus.MissingGamePath, service.load("null", null).status)

        withTempGameFixture { fixture ->
            val executable = fixture.root.resolve("Honkai.exe").createFile()
            val result = service.load(executable.toFile().absolutePath, null)

            assertEquals(DocsLoadStatus.MissingDocsDirectory, result.status)
            assertEquals(fixture.docs.toFile().absolutePath, result.docsDirectory)
        }
    }

    @Test
    fun `load reports empty docs directory`() {
        withTempGameFixture { fixture ->
            val executable = fixture.root.resolve("Honkai.exe").createFile()
            fixture.docs.createDirectories()

            val result = service.load(executable.toFile().absolutePath, null)

            assertEquals(DocsLoadStatus.Empty, result.status)
            assertNull(result.selectedDocument)
        }
    }

    @Test
    fun `default document is selected first and GamePlugins are grouped`() {
        withTempGameFixture { fixture ->
            val executable = fixture.root.resolve("Honkai.exe").createFile()
            fixture.docs.createDirectories()
            fixture.docs.resolve("Other.md").createFile().writeText("Other")
            fixture.docs.resolve("Default.md").createFile().writeText("Default")
            val pluginDocs = fixture.docs.resolve("GamePlugins").createDirectories()
            pluginDocs.resolve("Plugin.md").createFile().writeText("Plugin")

            val result = service.load(executable.toFile().absolutePath, null)

            assertEquals(DocsLoadStatus.Ready, result.status)
            assertEquals("Default.md", result.selectedDocument?.relativePath)
            assertEquals("Default", result.markdownContent)
            assertEquals(listOf("Default.md", "Other.md", "GamePlugins/Plugin.md"), result.documents.map { it.relativePath })
            assertEquals(DocSection.GamePlugins, result.documents.last().section)
        }
    }

    @Test
    fun `previous selection is preserved when available`() {
        withTempGameFixture { fixture ->
            val executable = fixture.root.resolve("Honkai.exe").createFile()
            fixture.docs.createDirectories()
            fixture.docs.resolve("Default.md").createFile().writeText("Default")
            fixture.docs.resolve("Other.md").createFile().writeText("Other")

            val result = service.load(executable.toFile().absolutePath, "Other.md")

            assertEquals("Other.md", result.selectedDocument?.relativePath)
            assertEquals("Other", result.markdownContent)
        }
    }
}

package core.docs

import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DocsLinkResolverTest {
    private val resolver = DocsLinkResolver()

    @Test
    fun `external and blank links are ignored`() {
        assertEquals(DocsLinkResolution.Ignored, resolver.resolve("", null, emptyList()))
        assertEquals(DocsLinkResolution.Ignored, resolver.resolve("https://example.com", null, emptyList()))
        assertEquals(DocsLinkResolution.Ignored, resolver.resolve("http://example.com", null, emptyList()))
    }

    @Test
    fun `relative markdown link resolves target document and anchor`() {
        val root = createTempDirectory("docs-link-resolver")
        val currentFile = root.resolve("guide").createDirectories().resolve("Intro.md").createFile()
        val targetFile = root.resolve("api").createDirectories().resolve("Usage.md").createFile()
        val current = docEntry(currentFile.toFile().absolutePath, "guide/Intro.md")
        val target = docEntry(targetFile.toFile().absolutePath, "api/Usage.md")

        val result = resolver.resolve("../api/Usage.md#install", current, listOf(current, target))

        val resolved = assertIs<DocsLinkResolution.Resolved>(result)
        assertEquals(target, resolved.target)
        assertEquals("install", resolved.anchor)
    }

    @Test
    fun `relative markdown link without fragment has no anchor`() {
        val root = createTempDirectory("docs-link-resolver")
        val currentFile = root.resolve("Intro.md").createFile()
        val targetFile = root.resolve("Usage.md").createFile()
        val current = docEntry(currentFile.toFile().absolutePath, "Intro.md")
        val target = docEntry(targetFile.toFile().absolutePath, "Usage.md")

        val result = resolver.resolve("Usage.md", current, listOf(current, target))

        val resolved = assertIs<DocsLinkResolution.Resolved>(result)
        assertEquals(target, resolved.target)
        assertNull(resolved.anchor)
    }

    @Test
    fun `missing markdown target is unresolved and handled`() {
        val root = createTempDirectory("docs-link-resolver")
        val currentFile = root.resolve("Intro.md").createFile()
        val current = docEntry(currentFile.toFile().absolutePath, "Intro.md")

        val result = resolver.resolve("Missing.md#title", current, listOf(current))

        assertEquals(DocsLinkResolution.Unresolved("Missing.md#title"), result)
    }

    @Test
    fun `non markdown relative links are ignored`() {
        val root = createTempDirectory("docs-link-resolver")
        val currentFile = root.resolve("Intro.md").createFile()
        val current = docEntry(currentFile.toFile().absolutePath, "Intro.md")

        assertEquals(DocsLinkResolution.Ignored, resolver.resolve("image.png", current, listOf(current)))
    }

    private fun docEntry(absolutePath: String, relativePath: String): DocEntry {
        return DocEntry(
            title = relativePath.substringAfterLast('/').substringBeforeLast('.'),
            absolutePath = absolutePath,
            relativePath = relativePath,
            section = DocSection.General,
        )
    }
}

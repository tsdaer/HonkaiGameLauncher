package screen.feature

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MarkdownScrollAnchorTest {

    @Test
    fun `extract headings continues after crlf fenced code block`() {
        val markdown = listOf(
            "# Markdown syntax guide",
            "",
            "## Blocks of code",
            "",
            "```kotlin",
            "@Composable",
            "fun MyApp() {",
            "    println(\"# Not a heading\")",
            "}",
            "```",
            "",
            "## Mermaid diagrams",
            "```mermaid",
            "classDiagram",
            "```",
            "",
            "## Inline code",
            "",
            "## Tex",
        ).joinToString("\r\n")

        val normalizedMarkdown = normalizeMarkdownLineEndings(markdown)
        val (items, headingSlugs) = extractDocHeadings(markdown)

        assertEquals(
            listOf(
                "Markdown syntax guide",
                "Blocks of code",
                "Mermaid diagrams",
                "Inline code",
                "Tex",
            ),
            items.map { it.title },
        )
        assertEquals("markdown-syntax-guide", headingSlugs[normalizedMarkdown.indexOf("# Markdown syntax guide")])
        assertEquals("blocks-of-code", headingSlugs[normalizedMarkdown.indexOf("## Blocks of code")])
        assertEquals("mermaid-diagrams", headingSlugs[normalizedMarkdown.indexOf("## Mermaid diagrams")])
    }

    @Test
    fun `extract headings ignores hashes inside fenced code`() {
        val markdown = """
            |# Visible heading
            |
            |```kotlin
            |# Hidden heading
            |```
            |
            |## Next heading
        """.trimMargin()

        val (items, _) = extractDocHeadings(markdown)

        assertEquals(listOf("Visible heading", "Next heading"), items.map { it.title })
        assertFalse(items.any { it.title == "Hidden heading" })
    }
}

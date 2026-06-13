package screen.feature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.CodeHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text as FluentText
import io.ratex.RaTeXEngine
import io.ratex.measure
import io.ratex.compose.RaTeX
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import ui.fluent.theme.FluentTokens
import ui.settings.LocalAppUiSettings
import viewModel.DocsScreenModel
import java.io.File
import java.net.URI
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun MarkdownCodeBlock(
    node: ASTNode,
    source: String,
    style: DocsMarkdownStyle,
) {
    val fencedCode = if (node.type == MarkdownElementTypes.CODE_FENCE) {
        parseFencedCodeBlock(extractMarkdownText(node, source))
    } else {
        null
    }
    val astCodeContent = node.children
        .filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
        .joinToString("") { extractMarkdownText(it, source) }
        .trimEnd()
    val code = fencedCode?.code
        ?: astCodeContent
            .ifBlank { extractMarkdownText(node, source).trimEnd() }
            .let(::trimFencedCodeContent)
    val language = fencedCode?.language ?: if (node.type == MarkdownElementTypes.CODE_FENCE) {
        extractFenceLanguage(node, source)
    } else {
        null
    }
    val trailingMarkdown = fencedCode?.trailingMarkdown.orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (language.equals("mermaid", ignoreCase = true)) {
            MarkdownMermaidBlock(code = code, style = style)
        } else {
            MarkdownCodeBlockContent(
                code = code,
                language = language,
                style = style
            )
        }

        if (trailingMarkdown.isNotBlank()) {
            MarkdownTrailingDocument(markdown = trailingMarkdown, style = style)
        }
    }
}

@Composable
internal fun MarkdownCodeBlockContent(
    code: String,
    language: String?,
    style: DocsMarkdownStyle,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.codeBackground, RoundedCornerShape(6.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!language.isNullOrBlank()) {
                MarkdownCodeLanguageBadge(language = language, style = style)
            }
            SyntaxHighlightedCodeText(
                code = code,
                language = language,
                style = style,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun MarkdownTrailingDocument(
    markdown: String,
    style: DocsMarkdownStyle,
) {
    val root = rememberMarkdownAst(markdown)

    FluentMarkdownDocument(
        root = root,
        source = markdown,
        style = style,
    )
}

@Composable
internal fun MarkdownCodeLanguageBadge(
    language: String,
    style: DocsMarkdownStyle,
) {
    FluentText(
        text = language.uppercase(),
        style = TextStyle(
            color = style.mutedColor,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = Modifier
            .background(style.subtleBackground, RoundedCornerShape(4.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable

internal fun SyntaxHighlightedCodeText(
    code: String,
    language: String?,
    style: DocsMarkdownStyle,
    modifier: Modifier = Modifier,
    fallbackStyle: TextStyle = style.codeBlockTextStyle,
) {
    val isDarkTheme = LocalAppUiSettings.current.isDarkTheme
    val syntaxLanguage = remember(language) { language?.toSyntaxLanguage() }
    val syntaxTheme = remember(isDarkTheme) { SyntaxThemes.themes(isDarkTheme).values.firstOrNull() }
    val highlights = remember(code, syntaxLanguage, syntaxTheme) {
        if (syntaxLanguage != null && syntaxTheme != null) {
            Highlights.Builder(code = code)
                .language(syntaxLanguage)
                .theme(syntaxTheme)
                .build()
        } else {
            null
        }
    }
    val highlightedCode = remember(code, highlights) {
        highlights
            ?.getHighlights()
            ?.toHighlightedCode(code)
            ?: AnnotatedString(code)
    }

    if (highlights == null) {
        FluentText(
            text = code,
            style = fallbackStyle,
            modifier = modifier
        )
    } else {
        FluentText(
            text = highlightedCode,
            style = fallbackStyle,
            modifier = modifier
        )
    }
}

internal data class FencedCodeBlock(
    val language: String?,
    val code: String,
    val trailingMarkdown: String,
)


internal fun parseFencedCodeBlock(raw: String): FencedCodeBlock? {
    val lines = raw.lines()
    val openingLine = lines.firstOrNull()?.trimStart() ?: return null
    val fenceMarker = openingLine
        .takeWhile { it == '`' || it == '~' }
        .takeIf { it.length >= 3 }
        ?: return null
    val language = openingLine
        .removePrefix(fenceMarker)
        .trim()
        .substringBefore(' ')
        .takeIf { it.isNotBlank() }
    val contentLines = lines.drop(1)
    val closingFenceIndex = contentLines.indexOfFirst { line ->
        line.trimStart().startsWith(fenceMarker)
    }
    val codeLines = if (closingFenceIndex >= 0) {
        contentLines.take(closingFenceIndex)
    } else {
        contentLines
    }
    val trailingLines = if (closingFenceIndex >= 0) {
        contentLines.drop(closingFenceIndex + 1)
    } else {
        emptyList()
    }

    return FencedCodeBlock(
        language = language,
        code = codeLines.joinToString("\n").trimEnd(),
        trailingMarkdown = trailingLines.joinToString("\n").trimStart()
    )
}

internal fun trimFencedCodeContent(raw: String): String {
    return raw.lines()
        .takeWhile { line ->
            val trimmed = line.trimStart()
            !trimmed.startsWith("```") && !trimmed.startsWith("~~~")
        }
        .joinToString("\n")
        .trimEnd()
}

internal fun extractFenceLanguage(node: ASTNode, source: String): String? {
    return node.children
        .firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }
        ?.let { extractMarkdownText(it, source).trim() }
        ?.takeIf { it.isNotBlank() }
        ?: extractMarkdownText(node, source)
            .lineSequence()
            .firstOrNull()
            ?.trim()
            ?.trimStart('`', '~')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.substringBefore(' ')
}

internal fun String.toSyntaxLanguage(): SyntaxLanguage? {
    val normalized = trim()
        .lowercase()
        .removePrefix("language-")
    if (normalized.isBlank() || normalized == "text" || normalized == "plain" || normalized == "plaintext") {
        return null
    }

    val aliases = mapOf(
        "c++" to "cpp",
        "cplusplus" to "cpp",
        "c#" to "csharp",
        "cs" to "csharp",
        "f#" to "fsharp",
        "fs" to "fsharp",
        "js" to "javascript",
        "mjs" to "javascript",
        "cjs" to "javascript",
        "jsx" to "javascript",
        "ts" to "typescript",
        "tsx" to "typescript",
        "kt" to "kotlin",
        "kts" to "kotlin",
        "py" to "python",
        "rb" to "ruby",
        "rs" to "rust",
        "sh" to "shell",
        "bash" to "shell",
        "zsh" to "shell",
        "ps1" to "powershell",
        "pwsh" to "powershell",
        "yml" to "yaml",
        "md" to "markdown",
        "html" to "xml",
        "xhtml" to "xml",
    )
    val candidates = listOfNotNull(normalized, aliases[normalized]).distinct()

    return candidates.firstNotNullOfOrNull { candidate ->
        SyntaxLanguage.getByName(candidate)
    }
}

internal fun List<CodeHighlight>.toHighlightedCode(code: String): AnnotatedString {
    return buildAnnotatedString {
        append(code)
        forEach { highlight ->
            val start = highlight.location.start.coerceIn(0, code.length)
            val end = highlight.location.end.coerceIn(start, code.length)
            if (start == end) {
                return@forEach
            }

            when (highlight) {
                is BoldHighlight -> addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    start = start,
                    end = end,
                )

                is ColorHighlight -> addStyle(
                    SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f)),
                    start = start,
                    end = end,
                )
            }
        }
    }
}


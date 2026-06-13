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
internal fun rememberDocsMarkdownStyle(): DocsMarkdownStyle {
    val isDarkTheme = LocalAppUiSettings.current.isDarkTheme
    val textColor = FluentTheme.colors.text.text.primary
    val mutedColor = if (isDarkTheme) {
        FluentTokens.ColorToken.LogLevel.unknown
    } else {
        FluentTokens.ColorToken.LogLevel.veryVerbose
    }
    val accentColor = FluentTokens.ColorToken.accent
    val unknownColor = FluentTokens.ColorToken.LogLevel.unknown

    return remember(isDarkTheme, textColor, mutedColor, accentColor, unknownColor) {
        DocsMarkdownStyle(
            textColor = textColor,
            mutedColor = mutedColor,
            accentColor = accentColor,
            codeBackground = unknownColor.copy(alpha = if (isDarkTheme) 0.18f else 0.045f),
            inlineCodeBackground = unknownColor.copy(alpha = if (isDarkTheme) 0.24f else 0.065f),
            borderColor = unknownColor.copy(alpha = if (isDarkTheme) 0.34f else 0.12f),
            dividerColor = unknownColor.copy(alpha = if (isDarkTheme) 0.28f else 0.16f),
            subtleBackground = unknownColor.copy(alpha = if (isDarkTheme) 0.12f else 0.035f),
        )
    }
}

internal data class DocsMarkdownStyle(
    val textColor: Color,
    val mutedColor: Color,
    val accentColor: Color,
    val codeBackground: Color,
    val inlineCodeBackground: Color,
    val borderColor: Color,
    val dividerColor: Color,
    val subtleBackground: Color,
) {
    val bodyTextStyle: TextStyle
        get() = TextStyle(
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )

    val codeBlockTextStyle: TextStyle
        get() = TextStyle(
            color = textColor,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = FontFamily.Monospace,
        )

    fun headingTextStyle(level: Int): TextStyle {
        val size = when (level) {
            1 -> 22.sp
            2 -> 18.sp
            3 -> 16.sp
            4 -> 14.sp
            else -> 14.sp
        }
        return TextStyle(
            color = if (level >= 5) mutedColor else textColor,
            fontSize = size,
            lineHeight = (size.value * 1.32f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}


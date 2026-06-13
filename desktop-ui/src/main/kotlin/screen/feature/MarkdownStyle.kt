package screen.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.composefluent.FluentTheme
import ui.fluent.theme.FluentTokens
import ui.settings.LocalAppUiSettings

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


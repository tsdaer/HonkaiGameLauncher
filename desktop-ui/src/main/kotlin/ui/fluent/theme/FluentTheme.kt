package ui.fluent.theme

import androidx.compose.runtime.Composable
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.darkColors
import io.github.composefluent.lightColors

@OptIn(ExperimentalFluentApi::class)
@Composable
fun AppFluentTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColors(FluentTokens.ColorToken.accent)
    } else {
        lightColors(FluentTokens.ColorToken.accent)
    }

    FluentTheme(
        colors = colors,
        typography = FluentTypography(),
        cornerRadius = FluentTokens.Radius.asCornerRadius(),
        content = content
    )
}

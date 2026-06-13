package ui.fluent.theme

import androidx.compose.runtime.Composable
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.darkColors
import io.github.composefluent.lightColors

/**
 * 应用级 Fluent Design 主题包装器。
 *
 * 根据 [darkTheme] 参数在深色/浅色主题间切换，
 * 集成自定义 [FluentTypography] 作为默认排版样式。
 *
 * @param darkTheme true 使用深色主题，false 使用浅色主题
 */
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

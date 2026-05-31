package ui.fluent.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.composefluent.CornerRadius
import ui.theme.light.backgroundDark
import ui.theme.light.backgroundLight
import ui.theme.light.errorDark
import ui.theme.light.errorLight
import ui.theme.light.surfaceContainerHighDark
import ui.theme.light.surfaceContainerLowLight

object FluentTokens {
    object ColorToken {
        // Keep Fluent's default accent to ensure stable generated shades in current library version.
        val accent: Color = Color(0xFF0078D4)

        val windowBackgroundLight: Color = backgroundLight
        val windowBackgroundDark: Color = backgroundDark

        val cardLight: Color = surfaceContainerLowLight
        val cardDark: Color = surfaceContainerHighDark

        val dangerLight: Color = errorLight
        val dangerDark: Color = errorDark
    }

    object Radius {
        val overlay: Dp = 8.dp
        val control: Dp = 4.dp
        val intersectionEdge: Dp = 0.dp

        fun asCornerRadius(): CornerRadius = CornerRadius(
            overlay = overlay,
            control = control,
            intersectionEdge = intersectionEdge
        )
    }

    object Spacing {
        val xSmall: Dp = 4.dp
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val xLarge: Dp = 24.dp
        val pageHorizontal: Dp = 48.dp
        val pageVertical: Dp = 40.dp
    }

    object Elevation {
        val floatingWindow: Dp = 3.dp
        val contentSurface: Dp = 8.dp
    }
}

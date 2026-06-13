package ui.fluent.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.composefluent.CornerRadius

/**
 * Fluent Design 设计令牌（单例）。
 *
 * 集中管理颜色、圆角半径、间距等设计系统常量。
 * 各组件的默认尺寸和颜色值统一由此定义，保证视觉一致性。
 */
object FluentTokens {
    object ColorToken {
        // Keep Fluent's default accent to ensure stable generated shades in current library version.
        val accent: Color = Color(0xFF0078D4)

        val windowBackgroundLight: Color = Color(0xFFFAF9F8)
        val windowBackgroundDark: Color = Color(0xFF202020)

        val cardLight: Color = Color(0xFFFFFFFF)
        val cardDark: Color = Color(0xFF2B2B2B)

        val dangerLight: Color = Color(0xFFD13438)
        val dangerDark: Color = Color(0xFFFF99A4)

        object WindowControl {
            val closeHover: Color = Color(0xFFE81123)
            val closePressed: Color = Color(0xFFC50F1F)
        }

        object LogLevel {
            val fatal: Color = Color(0xFFD13438)
            val error: Color = Color(0xFFE74856)
            val warning: Color = Color(0xFFF9A825)
            val display: Color = accent
            val log: Color = Color(0xFF8A8886)
            val verbose: Color = Color(0xFF8764B8)
            val veryVerbose: Color = Color(0xFF69797E)
            val unknown: Color = Color(0xFF8A8886)
        }
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

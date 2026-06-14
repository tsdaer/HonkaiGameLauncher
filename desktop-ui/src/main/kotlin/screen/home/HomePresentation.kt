package screen.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import core.GameConnectionStatus
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionConnected
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionConnectedHint
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionStopped
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionStoppedHint
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionWaiting
import honkaigamelauncher.desktop_ui.generated.resources.homeConnectionWaitingHint
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusError
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusLaunching
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusMissingExecutable
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusMissingGamePath
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusReady
import honkaigamelauncher.desktop_ui.generated.resources.homeStatusRunning
import org.jetbrains.compose.resources.stringResource
import ui.fluent.theme.FluentTokens
import screenmodel.HomeLaunchStatus
import screenmodel.HomeUiState

/** 根据启动状态返回本地化状态文本 */
@Composable
internal fun launchStatusText(uiState: HomeUiState): String {
    return when (uiState.launchStatus) {
        HomeLaunchStatus.MissingGamePath -> stringResource(Res.string.homeStatusMissingGamePath)
        HomeLaunchStatus.MissingExecutable -> stringResource(Res.string.homeStatusMissingExecutable)
        HomeLaunchStatus.Ready -> stringResource(Res.string.homeStatusReady)
        HomeLaunchStatus.Launching -> stringResource(Res.string.homeStatusLaunching)
        HomeLaunchStatus.Running -> stringResource(Res.string.homeStatusRunning)
        HomeLaunchStatus.Error -> uiState.statusMessage.ifBlank {
            stringResource(Res.string.homeStatusError)
        }
    }
}

/** 根据连接状态返回本地化文本 */
@Composable
internal fun connectionStatusText(status: GameConnectionStatus): String {
    return when (status) {
        GameConnectionStatus.Stopped -> stringResource(Res.string.homeConnectionStopped)
        GameConnectionStatus.Waiting -> stringResource(Res.string.homeConnectionWaiting)
        GameConnectionStatus.Connected -> stringResource(Res.string.homeConnectionConnected)
    }
}

/** 连接状态辅助提示文本 */
@Composable
internal fun launchConnectionHint(status: GameConnectionStatus): String {
    return when (status) {
        GameConnectionStatus.Stopped -> stringResource(Res.string.homeConnectionStoppedHint)
        GameConnectionStatus.Waiting -> stringResource(Res.string.homeConnectionWaitingHint)
        GameConnectionStatus.Connected -> stringResource(Res.string.homeConnectionConnectedHint)
    }
}

/** 启动状态对应的主题色 */
internal fun statusColor(status: HomeLaunchStatus): Color {
    return when (status) {
        HomeLaunchStatus.Ready -> Color(0xFF0E9F6E)
        HomeLaunchStatus.Running -> FluentTokens.ColorToken.accent
        HomeLaunchStatus.Launching -> Color(0xFFE08B2F)
        HomeLaunchStatus.MissingGamePath,
        HomeLaunchStatus.MissingExecutable -> FluentTokens.ColorToken.LogLevel.warning
        HomeLaunchStatus.Error -> FluentTokens.ColorToken.LogLevel.error
    }
}

/** 连接状态对应的主题色 */
internal fun connectionColor(status: GameConnectionStatus): Color {
    return when (status) {
        GameConnectionStatus.Stopped -> FluentTokens.ColorToken.LogLevel.error
        GameConnectionStatus.Waiting -> Color(0xFFE08B2F)
        GameConnectionStatus.Connected -> FluentTokens.ColorToken.accent
    }
}

/** 首页背景渐变（深色/浅色主题） */
internal fun homeBackgroundBrush(isDarkTheme: Boolean): Brush {
    return if (isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF101820),
                Color(0xFF17242E),
                Color(0xFF202020),
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF7FBFF),
                Color(0xFFFFFCF4),
                Color(0xFFFAF9F8),
            )
        )
    }
}

/** 首页 Hero 卡片背景渐变 */
internal fun heroBrush(color: Color, isDarkTheme: Boolean): Brush {
    val base = if (isDarkTheme) Color(0xFF252525) else Color.White
    return Brush.linearGradient(
        colors = listOf(
            color.copy(alpha = if (isDarkTheme) 0.22f else 0.15f),
            base,
            Color(0xFFE08B2F).copy(alpha = if (isDarkTheme) 0.14f else 0.09f),
        )
    )
}

/** 卡片背景渐变 */
internal fun cardBrush(isDarkTheme: Boolean): Brush {
    return Brush.linearGradient(
        colors = if (isDarkTheme) {
            listOf(Color(0xFF303030), Color(0xFF282828))
        } else {
            listOf(Color.White, Color(0xFFFBFAF7))
        }
    )
}

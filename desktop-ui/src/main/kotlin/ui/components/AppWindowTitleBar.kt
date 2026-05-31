package ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import cafe.adriel.voyager.navigator.LocalNavigator
import compose.icons.FeatherIcons
import compose.icons.LineAwesomeIcons
import compose.icons.feathericons.CornerUpLeft
import compose.icons.feathericons.Maximize
import compose.icons.feathericons.Minimize
import compose.icons.feathericons.Minus
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Sun
import compose.icons.feathericons.X
import compose.icons.lineawesomeicons.LanguageSolid
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.appTitle
import honkaigamelauncher.desktop_ui.generated.resources.logo
import io.github.composefluent.component.Button as FluentButton
import io.github.composefluent.component.Icon as FluentIcon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.fluent.theme.FluentTokens
import ui.fluent.theme.LegacyThemeAdapter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WindowScope.AppWindowTitleBar(
    state: WindowState,
    onCloseRequest: () -> Unit,
    onThemeChanged: () -> Unit,
    onLanguageChanged: () -> Unit,
    isDarkTheme: Boolean
) {
    val isFloating = state.placement == WindowPlacement.Floating
    val surfaceColor = if (isDarkTheme) {
        FluentTokens.ColorToken.cardDark.copy(alpha = 0.9f)
    } else {
        FluentTokens.ColorToken.cardLight.copy(alpha = 0.9f)
    }
    val borderColor = MaterialTheme.colors.primary.copy(alpha = 0.16f)

    val content: @Composable () -> Unit = {
        val navigator = LocalNavigator.current

        LegacyThemeAdapter(darkTheme = isDarkTheme) {
            Surface(
                color = surfaceColor,
                border = BorderStroke(1.dp, borderColor),
                elevation = if (isFloating) 2.dp else 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppTitleActionButton(
                        imageVector = FeatherIcons.CornerUpLeft,
                        contentDescription = "回退",
                        enabled = navigator?.canPop == true,
                        onClick = { navigator?.pop() }
                    )

                    androidx.compose.material.Icon(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = "App Icon",
                        modifier = Modifier.size(22.dp),
                        tint = Color.Unspecified
                    )

                    Text(
                        text = stringResource(Res.string.appTitle),
                        color = MaterialTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    AppTitleActionButton(
                        imageVector = remember(isDarkTheme) {
                            if (isDarkTheme) FeatherIcons.Sun else FeatherIcons.Moon
                        },
                        contentDescription = if (isDarkTheme) "切换至亮色主题" else "切换至暗色主题",
                        onClick = onThemeChanged
                    )
                    AppTitleActionButton(
                        imageVector = LineAwesomeIcons.LanguageSolid,
                        contentDescription = "切换语言",
                        onClick = onLanguageChanged
                    )
                    AppTitleActionButton(
                        imageVector = FeatherIcons.Minus,
                        contentDescription = "最小化",
                        onClick = { state.isMinimized = !state.isMinimized }
                    )
                    AppTitleActionButton(
                        imageVector = remember(state.placement) {
                            if (state.placement == WindowPlacement.Maximized) {
                                FeatherIcons.Minimize
                            } else {
                                FeatherIcons.Maximize
                            }
                        },
                        contentDescription = if (state.placement == WindowPlacement.Maximized) "恢复窗口" else "最大化",
                        onClick = {
                            state.placement = if (state.placement == WindowPlacement.Maximized) {
                                WindowPlacement.Floating
                            } else {
                                WindowPlacement.Maximized
                            }
                        }
                    )
                    AppTitleActionButton(
                        imageVector = FeatherIcons.X,
                        contentDescription = "退出",
                        onClick = onCloseRequest
                    )
                }
            }
        }
    }

    val modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                state.placement =
                    if (state.placement == WindowPlacement.Maximized)
                        WindowPlacement.Floating
                    else
                        WindowPlacement.Maximized
            }
        )
    }

    if (isFloating) {
        WindowDraggableArea(modifier = modifier) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
private fun AppTitleActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FluentButton(
        onClick = onClick,
        disabled = !enabled,
        iconOnly = true
    ) {
        FluentIcon(
            imageVector = imageVector,
            contentDescription = contentDescription
        )
    }
}

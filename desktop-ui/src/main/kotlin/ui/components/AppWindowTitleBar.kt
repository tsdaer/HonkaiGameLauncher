package ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import cafe.adriel.voyager.navigator.LocalNavigator
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.appTitle
import honkaigamelauncher.desktop_ui.generated.resources.logo
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Maximize
import io.github.composefluent.icons.regular.Open
import io.github.composefluent.component.FontIcon
import io.github.composefluent.component.FontIconPrimitive
import io.github.composefluent.component.ButtonDefaults
import io.github.composefluent.component.Icon
import io.github.composefluent.component.NavigationDefaults
import io.github.composefluent.component.ButtonColor
import io.github.composefluent.component.Text as FluentText
import io.github.composefluent.FluentTheme
import io.github.composefluent.icons.regular.FullScreenMaximize
import io.github.composefluent.icons.regular.FullScreenMinimize
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.fluent.theme.FluentTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WindowScope.AppWindowTitleBar(
    state: WindowState,
    onCloseRequest: () -> Unit
) {
    val content: @Composable () -> Unit = {
        val navigator = LocalNavigator.current
        val closeButtonColors = ButtonDefaults.subtleButtonColors(
            default = ButtonColor(
                fillColor = Color.Transparent,
                contentColor = FluentTheme.colors.text.text.secondary,
                borderBrush = SolidColor(Color.Transparent)
            ),
            hovered = ButtonColor(
                fillColor = FluentTokens.ColorToken.WindowControl.closeHover,
                contentColor = FluentTheme.colors.text.onAccent.primary,
                borderBrush = SolidColor(Color.Transparent)
            ),
            pressed = ButtonColor(
                fillColor = FluentTokens.ColorToken.WindowControl.closePressed,
                contentColor = FluentTheme.colors.text.onAccent.secondary,
                borderBrush = SolidColor(Color.Transparent)
            ),
            disabled = ButtonColor(
                fillColor = FluentTheme.colors.subtleFill.disabled,
                contentColor = FluentTheme.colors.text.text.disabled,
                borderBrush = SolidColor(Color.Transparent)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavigationDefaults.BackButton(
                disabled = navigator?.canPop != true,
                onClick = { navigator?.pop() }
            )

            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "App Icon",
                modifier = Modifier.size(32.dp)
            )

            FluentText(
                text = stringResource(Res.string.appTitle),
                color = FluentTheme.colors.text.text.primary,
                style = FluentTheme.typography.bodyStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            NavigationDefaults.Button(
                onClick = { state.isMinimized = !state.isMinimized },
                icon = {
                    FontIcon(
                        type = FontIconPrimitive.Dash,
                        contentDescription = "最小化"
                    )
                }
            )
            NavigationDefaults.Button(
                onClick = {
                    state.placement = if (state.placement == WindowPlacement.Maximized) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Maximized
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (state.placement == WindowPlacement.Maximized) {
                            Icons.Regular.FullScreenMinimize
                        } else {
                            Icons.Regular.FullScreenMaximize
                        },
                        contentDescription = if (state.placement == WindowPlacement.Maximized) "恢复窗口" else "最大化"
                    )
                }
            )
            NavigationDefaults.Button(
                onClick = onCloseRequest,
                buttonColors = closeButtonColors,
                icon = {
                    FontIcon(
                        type = FontIconPrimitive.Close,
                        contentDescription = "退出"
                    )
                }
            )
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

    if (state.placement == WindowPlacement.Floating) {
        WindowDraggableArea(modifier = modifier) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

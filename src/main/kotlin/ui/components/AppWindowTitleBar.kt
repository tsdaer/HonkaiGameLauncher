package ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import cafe.adriel.voyager.navigator.LocalNavigator
import com.honkai_rts.honkaigamelauncher.generated.resources.Res
import com.honkai_rts.honkaigamelauncher.generated.resources.appTitle
import com.honkai_rts.honkaigamelauncher.generated.resources.logo
import compose.icons.feathericons.CornerUpLeft
import compose.icons.feathericons.Maximize
import compose.icons.feathericons.Minimize
import compose.icons.feathericons.Minus
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Sun
import compose.icons.feathericons.X
import compose.icons.lineawesomeicons.LanguageSolid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WindowScope.AppWindowTitleBar(state: WindowState, onCloseRequest: () -> Unit,onThemeChanged:()->Unit,onLanguageChanged:()->Unit,isDarkTheme: Boolean)
{
    val isFloating = state.placement == WindowPlacement.Floating

    val content: @Composable () -> Unit = {
        val navigator = LocalNavigator.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CustomIconButton(
                    iconSize = 42.dp,
                    imageVector = compose.icons.FeatherIcons.CornerUpLeft,
                    contentDescription = "回退",
                    enabled = navigator?.canPop == true,
                    onClick = {
                        navigator?.pop()
                    })
                Icon(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(40.dp),
                    tint = Color.Unspecified
                )
                Text(text = stringResource(Res.string.appTitle),
                    modifier = Modifier.padding(start = 5.dp, end = 5.dp),
                    color = MaterialTheme.colors.primary)

                Box(modifier = Modifier.weight(1f))

                Row {
                    CustomIconButton(
                        iconSize = 32.dp,
                        imageVector = remember(isDarkTheme) {
                            if (isDarkTheme) compose.icons.FeatherIcons.Sun else compose.icons.FeatherIcons.Moon
                        },
                        contentDescription = if (isDarkTheme) "切换至亮色主题" else "切换至暗色主题",
                        onClick = onThemeChanged)
                    CustomIconButton(
                        iconSize = 32.dp,
                        imageVector = compose.icons.LineAwesomeIcons.LanguageSolid,
                        contentDescription = "切换语言",
                        onClick = onLanguageChanged
                    )
                    CustomIconButton(
                        iconSize = 32.dp,
                        imageVector = compose.icons.FeatherIcons.Minus,
                        contentDescription = "最小化",
                        onClick = { state.isMinimized = !state.isMinimized })
                    CustomIconButton(
                        iconSize = 32.dp,
                        imageVector = remember(state.placement) {
                            if (state.placement == WindowPlacement.Maximized) {
                                compose.icons.FeatherIcons.Minimize // 全屏时显示"恢复"图标
                            } else {
                                compose.icons.FeatherIcons.Maximize // 浮动时显示"最大化"图标
                            }
                        },
                        contentDescription = if (state.placement == WindowPlacement.Maximized) "恢复窗口" else "最大化",
                        onClick = {
                            state.placement = if (state.placement == WindowPlacement.Maximized) {
                                WindowPlacement.Floating
                            } else {
                                WindowPlacement.Maximized
                            }
                        })
                    CustomIconButton(
                        iconSize = 32.dp,
                        imageVector = compose.icons.FeatherIcons.X,
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


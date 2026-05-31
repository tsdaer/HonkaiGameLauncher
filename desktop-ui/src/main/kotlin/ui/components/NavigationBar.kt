package ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.component.Icon
import io.github.composefluent.component.MenuItem
import io.github.composefluent.component.NavigationDisplayMode
import io.github.composefluent.component.NavigationView
import io.github.composefluent.component.Text
import io.github.composefluent.component.rememberNavigationState
import navigation.SharedScreen
import navigation.featureScreens
import screen.IScreenInterface
import ui.fluent.theme.FluentTokens
import ui.fluent.theme.LegacyThemeAdapter

private data class NavEntry(
    val screenProvider: SharedScreen
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalVoyagerApi::class, ExperimentalFluentApi::class)
@Composable
fun NavigationBar(
    darkTheme: Boolean
) {
    val navigator = LocalNavigator.current
    val navigationState = rememberNavigationState(initialExpanded = false)

    val menuEntries = remember {
        buildList {
            add(NavEntry(screenProvider = SharedScreen.Home))
            addAll(featureScreens.map { NavEntry(screenProvider = it) })
        }
    }
    val footerEntries = remember {
        listOf(
            NavEntry(screenProvider = SharedScreen.Setting)
        )
    }

    LegacyThemeAdapter(darkTheme = darkTheme) {
        NavigationView(
            displayMode = NavigationDisplayMode.LeftCompact,
            state = navigationState,
            menuItems = {
                items(menuEntries.size) { index ->
                    val entry = menuEntries[index]
                    val screen = rememberScreen(entry.screenProvider)
                    val screenInfo = screen as IScreenInterface
                    val current = navigator?.lastItem as? IScreenInterface
                    val selected = current?.getUrl() == screenInfo.getUrl()
                    val titleText = screenInfo.getTitle()
                    val iconVector = screenInfo.getIcon()

                    MenuItem(
                        selected = selected,
                        onClick = {
                            if (current?.getUrl() != screenInfo.getUrl()) {
                                navigator?.push(screen)
                            }
                        },
                        text = { Text(text = titleText) },
                        icon = {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = titleText
                            )
                        }
                    )
                }
            },
            footerItems = {
                items(footerEntries.size) { index ->
                    val entry = footerEntries[index]
                    val screen = rememberScreen(entry.screenProvider)
                    val screenInfo = screen as IScreenInterface
                    val current = navigator?.lastItem as? IScreenInterface
                    val titleText = screenInfo.getTitle()
                    val iconVector = screenInfo.getIcon()
                    MenuItem(
                        selected = current?.getUrl() == screenInfo.getUrl(),
                        onClick = {
                            if (current?.getUrl() != screenInfo.getUrl()) {
                                navigator?.push(screen)
                            }
                        },
                        text = { Text(text = titleText) },
                        icon = {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = titleText
                            )
                        }
                    )
                }
            },
            pane = {
                val currentScreen = navigator?.lastItem as? IScreenInterface
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = FluentTokens.Spacing.large,
                            end = FluentTokens.Spacing.xLarge,
                            top = FluentTokens.Spacing.xLarge,
                            bottom = FluentTokens.Spacing.xLarge
                        )
                ) {
                    currentScreen?.let {
                        Text(
                            text = it.getTitle(),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    navigator?.let {
                        val slowAnimation = tween<IntOffset>(durationMillis = 300)
                        val slowAnimation2 = tween<Float>(durationMillis = 300)
                        AnimatedContent(
                            targetState = navigator.lastItem.key,
                            transitionSpec = {
                                (slideInVertically(animationSpec = slowAnimation) { fullHeight -> (0.1 * fullHeight).toInt() } + fadeIn(animationSpec = slowAnimation2)).togetherWith(
                                    slideOutVertically(animationSpec = slowAnimation) { fullHeight -> (-0.1 * fullHeight).toInt() } + fadeOut(slowAnimation2)
                                )
                            },
                            contentKey = { navigator.lastItem.key }
                        ) {
                            CurrentScreen()
                        }
                    }
                }
            }
        )
    }
}

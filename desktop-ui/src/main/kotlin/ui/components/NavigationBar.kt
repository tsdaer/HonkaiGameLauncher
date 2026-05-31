@file:Suppress("SameParameterValue")

package ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Menu
import io.github.composefluent.component.Button as FluentButton
import io.github.composefluent.component.Icon as FluentIcon
import io.github.composefluent.component.Text as FluentText
import io.github.composefluent.component.ToggleButton as FluentToggleButton
import navigation.SharedScreen
import navigation.featureScreens
import screen.IScreenInterface
import ui.fluent.theme.LegacyThemeAdapter

@Composable
private fun NavigationMenuToggleButton(
    showDetail: Boolean,
    textAlpha: Float,
    onClick: () -> Unit
) {
    FluentButton(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            FluentIcon(
                imageVector = EvaIcons.Fill.Menu,
                contentDescription = "menu"
            )
            if (showDetail) {
                FluentText(
                    text = "Menu",
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .alpha(textAlpha)
                )
            }
        }
    }
}

@Composable
private fun NavigationFeatureItem(
    featureScreen: Screen,
    showDetail: Boolean,
    textAlpha: Float
) {
    val navigator = LocalNavigator.current
    val data = featureScreen as? IScreenInterface

    val isSelected by remember(navigator, data) {
        derivedStateOf {
            val lastData = navigator?.lastItem as? IScreenInterface
            data?.getUrl() == lastData?.getUrl()
        }
    }

    FluentToggleButton(
        checked = isSelected,
        onCheckedChanged = { checked ->
            if (!checked || data == null) return@FluentToggleButton
            val lastData = navigator?.lastItem as? IScreenInterface
            if (lastData == null || data.getUrl() != lastData.getUrl()) {
                navigator?.push(featureScreen)
            }
        },
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        if (data != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                FluentIcon(
                    imageVector = data.getIcon(),
                    contentDescription = data.getTitle()
                )
                if (showDetail) {
                    FluentText(
                        text = data.getTitle(),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .alpha(textAlpha)
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationBar(
    darkTheme: Boolean
) {
    val state = rememberScrollState(0)
    var isExpanded by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }

    val size by animateDpAsState(
        targetValue = if (isExpanded) 300.dp else 50.dp,
        finishedListener = {
            if (isExpanded) {
                showDetail = true
            }
        }
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (showDetail) 1f else 0f
    )

    LegacyThemeAdapter(darkTheme = darkTheme) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(size)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 8.dp)
            ) {
                NavigationMenuToggleButton(
                    showDetail = showDetail,
                    textAlpha = textAlpha,
                    onClick = {
                        isExpanded = !isExpanded
                        showDetail = false
                    }
                )

                NavigationFeatureItem(
                    featureScreen = rememberScreen(SharedScreen.Home),
                    showDetail = showDetail,
                    textAlpha = textAlpha
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(state)
                ) {
                    featureScreens.forEach { screen ->
                        NavigationFeatureItem(
                            featureScreen = rememberScreen(screen),
                            showDetail = showDetail,
                            textAlpha = textAlpha
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                NavigationFeatureItem(
                    featureScreen = rememberScreen(SharedScreen.Setting),
                    showDetail = showDetail,
                    textAlpha = textAlpha
                )
            }
        }
    }
}

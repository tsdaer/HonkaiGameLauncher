@file:Suppress("SameParameterValue")

package ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Menu
import navigation.SharedScreen
import navigation.featureScreens
import screen.IScreenInterface


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationItem(icon: ImageVector, showDetail: Boolean, title: String, onClick:() ->Unit )
{
    Button(
        onClick = onClick,
        modifier = Modifier.height(50.dp).fillMaxWidth().padding(4.dp),
        contentPadding = PaddingValues(9.dp),
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.background,
            contentColor = MaterialTheme.colors.primary,
            disabledBackgroundColor = MaterialTheme.colors.error,
        ),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp,
            disabledElevation = 0.dp,
            hoveredElevation = 4.dp,
            focusedElevation = 0.dp
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.fillMaxHeight(),
                tint = MaterialTheme.colors.primary,
            )
            if(showDetail) Text(title, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun NavigationFeatureItem(featureScreen: Screen, showDetail: Boolean, textAlpha:Float)
{
    val navigator = LocalNavigator.current
    val data = featureScreen as? IScreenInterface

    // 使用 derivedStateOf 自动计算选中状态
    val isSelected by remember {
        derivedStateOf {
            val lastData = navigator?.lastItem as? IScreenInterface
            data?.getUrl() == lastData?.getUrl()
        }
    }

    // 悬浮状态
    var isHovered by remember { mutableStateOf(false) }

    // 组合动画值：优先显示选中状态，其次显示悬浮状态
    val elevation by animateDpAsState(
        targetValue = when {
            isSelected -> 8.dp
            isHovered -> 2.dp  // 悬浮时稍低的elevation
            else -> 0.dp
        },
        label = "elevationAnimation"
    )

    Surface(
        elevation = elevation,  // 使用动画值
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth()
            .padding(4.dp)
            .onPointerEvent(PointerEventType.Enter){ isHovered = true }
            .onPointerEvent(PointerEventType.Exit){ isHovered = false },
    ) {
        Button(
            onClick = {
                data?.takeIf {
                    val lastData = navigator?.lastItem as? IScreenInterface
                    lastData == null || it.getUrl() != lastData.getUrl()
                }?.let {
                    navigator?.push(featureScreen)
                }
            },
            modifier = Modifier.height(50.dp).fillMaxWidth(),
            contentPadding = PaddingValues(9.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = MaterialTheme.colors.primary,
            ),
            elevation = ButtonDefaults.elevation(0.dp,0.dp,0.dp,0.dp,0.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically, horizontalArrangement =  Arrangement.Start) {
                if (data != null) {
                    Icon(
                        imageVector = data.getIcon(),
                        contentDescription = data.getTitle(),
                        modifier = Modifier.fillMaxHeight(),
                        tint = MaterialTheme.colors.primary,
                    )
                    Text(data.getTitle(), modifier = Modifier.padding(start = 8.dp).alpha(textAlpha), fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun NavigationBar() {
    val state = rememberScrollState(0)

    var isExpanded by remember { mutableStateOf(false) }

    var showDetail by remember { mutableStateOf(false) }

    val size by animateDpAsState(
        targetValue = if (isExpanded) 300.dp else 50.dp,
        finishedListener = { if (isExpanded) showDetail = true }
    )

    // 可选：添加透明度动画让文本渐变出现
    val textAlpha by animateFloatAsState(
        targetValue = if (showDetail) 1f else 0f
    )

    Box(modifier = Modifier.fillMaxHeight().width(size))
    {
        Column(modifier = Modifier.fillMaxHeight().padding(bottom = 8.dp)) {

            NavigationItem(
                icon = EvaIcons.Fill.Menu,
                showDetail = showDetail,
                title = "",
                onClick = {
                    isExpanded = !isExpanded
                    showDetail = false
                })


            NavigationFeatureItem(rememberScreen(SharedScreen.Home), showDetail,textAlpha)

            Column(modifier = Modifier.weight(1f).verticalScroll(state)) {
                featureScreens.forEach { screen ->
                    NavigationFeatureItem(rememberScreen(screen), showDetail,textAlpha)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom
            ){
                NavigationFeatureItem(rememberScreen(SharedScreen.Setting), showDetail,textAlpha)
            }
        }
    }
}
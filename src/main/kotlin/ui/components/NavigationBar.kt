@file:Suppress("SameParameterValue")

package ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.Menu
import service.navigation.SharedScreen
import service.navigation.featureScreens
import util.IScreenInterface


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationItem(icon: ImageVector, showDetail: Boolean, title: String, onClick:() ->Unit )
{
    Button(
        onClick = onClick,
        modifier = Modifier.height(50.dp).fillMaxWidth().padding(4.dp),
        contentPadding = PaddingValues(4.dp),
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
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colors.primary,
            )
            if(showDetail) Text(title, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationFeatureItem(featureScreen: Screen, showDetail: Boolean)
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

    // 添加动画效果让变化更明显
    val elevation by animateDpAsState(
        targetValue = if(isSelected) 4.dp else 0.dp,
        label = "elevationAnimation"
    )

    Surface(
        elevation = elevation,  // 使用动画值
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier.height(50.dp).fillMaxWidth().padding(4.dp),
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
            contentPadding = PaddingValues(4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = MaterialTheme.colors.primary,
            ),
            elevation = ButtonDefaults.elevation(0.dp,4.dp,0.dp,4.dp,0.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                if (data != null) {
                    Icon(
                        imageVector = data.getIcon(),
                        contentDescription = data.getTitle(),
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colors.primary,
                    )
                    if(showDetail) Text(data.getTitle(), modifier = Modifier.padding(start = 8.dp), fontSize = 18.sp)
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


            NavigationFeatureItem(rememberScreen(SharedScreen.Home), showDetail)

            Column(modifier = Modifier.weight(1f).verticalScroll(state)) {
                featureScreens.forEach { screen ->
                    NavigationFeatureItem(rememberScreen(screen), showDetail)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom
            ){
                NavigationFeatureItem(rememberScreen(SharedScreen.Setting), showDetail)
            }
        }
    }
}
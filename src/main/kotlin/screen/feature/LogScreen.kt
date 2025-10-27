package screen.feature

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.honkai_rts.honkaigamelauncher.generated.resources.Res
import com.honkai_rts.honkaigamelauncher.generated.resources.screen_log
import compose.icons.feathericons.Activity
import compose.icons.feathericons.ChevronsDown
import compose.icons.lineawesomeicons.ArrowDownSolid
import compose.icons.lineawesomeicons.CodeSolid
import core.LauncherLogEntry
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import viewModel.LogScreenModel

class LogScreen: Screen, IScreenInterface {
    override val key = uniqueScreenKey


    override fun getUrl(): String {
        return "log"
    }

    override fun getIcon(): ImageVector {
        return compose.icons.FeatherIcons.Activity
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(Res.string.screen_log)
    }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel{ LogScreenModel() }
        val allLogs = screenModel.logs
        val selectedType = screenModel.selectedType
        val selectedCategory = screenModel.selectedCategory
        val autoScroll = screenModel.autoScroll

        val allTypes = screenModel.availableTypes()
        val allCategories = screenModel.availableCategories()

        val filteredLogs by remember(allLogs.size, selectedType, selectedCategory) {
            derivedStateOf { screenModel.filteredLogs() }
        }

        var expandedType by remember { mutableStateOf(false) }
        var expandedCategory by remember { mutableStateOf(false) }

        Column(
            Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Type 下拉框
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    TextButton(onClick = { expandedType = !expandedType }) {
                        Text(
                            text = logTypeLabel(screenModel.selectedType),
                            //color = Color.White
                        )
                    }
                    DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        DropdownMenuItem(onClick = {
                            screenModel.selectedType = null
                            expandedType = false
                        }) {
                            Text("全部类型")
                        }
                        allTypes.forEach { t ->
                            DropdownMenuItem(onClick = {
                                screenModel.selectedType = t
                                expandedType = false
                            }) {
                                Text(logTypeLabel(t))
                            }
                        }
                    }
                }

                // Category 下拉框
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    TextButton(onClick = { expandedCategory = !expandedCategory }) {
                        Text(
                            text = screenModel.selectedCategory ?: "全部分类",
                            //color = Color.White
                        )
                    }
                    DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        DropdownMenuItem(onClick = {
                            screenModel.selectedCategory = null
                            expandedCategory = false
                        }) {
                            Text("全部分类")
                        }
                        allCategories.forEach { c ->
                            DropdownMenuItem(onClick = {
                                screenModel.selectedCategory = c
                                expandedCategory = false
                            }) {
                                Text(c)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 自动滚动开关按钮
                TextButton(
                    onClick = { screenModel.toggleAutoScroll() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = compose.icons.FeatherIcons.ChevronsDown,
                        contentDescription = "自动滚动",
                        tint = if (autoScroll) Color(0xFF00FF00) else Color.Gray
                    )
                }

                // 清空按钮
                TextButton(onClick = { screenModel.clear() }) {
                    Text("清空")
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val state = rememberLazyListState()

                // 自动滚动到最新日志
                LaunchedEffect(filteredLogs.size, autoScroll) {
                    if (autoScroll && filteredLogs.isNotEmpty()) {
                        state.animateScrollToItem(filteredLogs.size - 1)
                    }
                }

                LazyColumn(Modifier.fillMaxSize().padding(end = 12.dp).background(Color(0xFF101010)), state) {
                    items(filteredLogs) { log ->
                        LogItem(log)
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = state
                    )
                )
            }
        }
    }

    val logColors = mapOf(
        // Fatal
        1 to Color(0xFFFF0000),
        // Error
        2 to Color(0xFFFF6347),
        // Warning
        3 to Color(0xFFFFFF00),
        // Display
        4 to Color(0xFF00FFFF),
        // Log
        5 to Color(0xFFCCCCCC),
        // Verbose
        6 to Color(0xFF8888FF),
        // VeryVerbose
        7 to Color(0xFF7777AA)
    )

    fun logTypeLabel(type: Int?): String = when (type) {
        null -> "全部类型"
        1 -> "Fatal"
        2 -> "Error"
        3 -> "Warning"
        4 -> "Display"
        5 -> "Log"
        6 -> "Verbose"
        7 -> "VeryVerbose"
        else -> "Unknown"
    }

    @Composable
    fun LogItem(log: LauncherLogEntry) {
        val color = logColors[log.type] ?: Color.White
        val typeLabel = logTypeLabel(log.type)
        val borderColor = Color(color.red, color.green, color.blue, 0.3f);
        Box(modifier = Modifier.padding(vertical = 2.dp).border(2.dp, borderColor, RoundedCornerShape(4.dp)).fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(8.dp,4.dp),
                text = "[${log.time}] [$typeLabel] [${log.category}] ${log.message}",
                color = color,
                fontSize = 14.sp,
            )
        }
    }
}
package screen.feature

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import compose.icons.feathericons.Activity
import compose.icons.feathericons.ChevronsDown
import core.LauncherLogEntry
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.screen_log
import io.github.composefluent.component.Icon as FluentIcon
import io.github.composefluent.component.Text as FluentText
import io.github.composefluent.component.ToggleButton as FluentToggleButton
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.components.FluentDropdown
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
        val screenModel = rememberScreenModel { LogScreenModel() }
        val allLogs = screenModel.logs
        val selectedType = screenModel.selectedType
        val selectedCategory = screenModel.selectedCategory
        val autoScroll = screenModel.autoScroll

        val allTypes = screenModel.availableTypes()
        val allCategories = screenModel.availableCategories()

        val typeOptions = remember(allTypes) {
            buildList {
                add("全部类型")
                addAll(allTypes.map { type -> logTypeLabel(type) })
            }
        }
        val typeSelectedIndex = remember(selectedType, allTypes) {
            if (selectedType == null) {
                0
            } else {
                val index = allTypes.indexOf(selectedType)
                if (index >= 0) index + 1 else 0
            }
        }

        val categoryOptions = remember(allCategories) {
            buildList {
                add("全部分类")
                addAll(allCategories)
            }
        }
        val categorySelectedIndex = remember(selectedCategory, allCategories) {
            if (selectedCategory == null) {
                0
            } else {
                val index = allCategories.indexOf(selectedCategory)
                if (index >= 0) index + 1 else 0
            }
        }

        val filteredLogs by remember(allLogs.size, selectedType, selectedCategory) {
            derivedStateOf { screenModel.filteredLogs() }
        }

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FluentDropdown(
                    items = typeOptions,
                    selectedIndex = typeSelectedIndex,
                    onSelectionChange = { index, _ ->
                        screenModel.selectedType = if (index == 0) {
                            null
                        } else {
                            allTypes.getOrNull(index - 1)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                FluentDropdown(
                    items = categoryOptions,
                    selectedIndex = categorySelectedIndex,
                    onSelectionChange = { index, _ ->
                        screenModel.selectedCategory = if (index == 0) {
                            null
                        } else {
                            allCategories.getOrNull(index - 1)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.weight(1f))

                FluentToggleButton(
                    checked = autoScroll,
                    onCheckedChanged = { checked ->
                        if (checked != screenModel.autoScroll) {
                            screenModel.toggleAutoScroll()
                        }
                    },
                    iconOnly = true
                ) {
                    FluentIcon(
                        imageVector = compose.icons.FeatherIcons.ChevronsDown,
                        contentDescription = "自动滚动"
                    )
                }

                FluentButton(onClick = { screenModel.clear() }) {
                    FluentText("清空")
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val state = rememberLazyListState()

                LaunchedEffect(filteredLogs.size, autoScroll) {
                    if (autoScroll && filteredLogs.isNotEmpty()) {
                        state.animateScrollToItem(filteredLogs.size - 1)
                    }
                }

                FluentCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            state = state
                        ) {
                            items(filteredLogs) { log ->
                                LogItem(log)
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp),
                            adapter = rememberScrollbarAdapter(
                                scrollState = state
                            )
                        )
                    }
                }
            }
        }
    }

    val logColors = mapOf(
        1 to Color(0xFFFF0000),
        2 to Color(0xFFFF6347),
        3 to Color(0xFFFFFF00),
        4 to Color(0xFF00FFFF),
        5 to Color(0xFFCCCCCC),
        6 to Color(0xFF8888FF),
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
        FluentCard(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .background(color)
                )
                FluentText(
                    text = "[${log.time}] [$typeLabel] [${log.category}] ${log.message}",
                    color = color,
                    fontSize = 14.sp
                )
            }
        }
    }
}

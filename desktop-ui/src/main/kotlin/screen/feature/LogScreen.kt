package screen.feature

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import compose.icons.feathericons.Activity
import compose.icons.feathericons.ChevronsDown
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.logActionAutoScroll
import honkaigamelauncher.desktop_ui.generated.resources.logActionClear
import honkaigamelauncher.desktop_ui.generated.resources.logAllCategories
import honkaigamelauncher.desktop_ui.generated.resources.logAllTypes
import honkaigamelauncher.desktop_ui.generated.resources.logTypeUnknown
import honkaigamelauncher.desktop_ui.generated.resources.screen_log
import navigation.SharedScreen
import navigation.screenRoute
import org.jetbrains.compose.resources.stringResource
import screen.IScreenInterface
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import ui.fluent.components.FluentDropdown
import ui.fluent.theme.FluentTokens
import core.LogScreenEntry
import screenmodel.LogScreenModel
import io.github.composefluent.component.Icon as FluentIcon
import io.github.composefluent.component.Text as FluentText
import io.github.composefluent.component.ToggleButton as FluentToggleButton

/**
 * 游戏日志页。
 *
 * 实时展示游戏通过 HTTP 回传的运行日志，支持按日志类型和分类筛选，
 * 以及自动滚动和缓冲区管理。数据由 [LogScreenModel] 管理。
 */
class LogScreen: Screen, IScreenInterface {
    override val key = uniqueScreenKey

    override fun getUrl(): String {
        return screenRoute(SharedScreen.Log)
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
        val uiState = screenModel.uiState
        val filteredLogs = screenModel.filteredLogs

        val allTypes = screenModel.availableTypes
        val allCategories = screenModel.availableCategories
        val allTypesLabel = stringResource(Res.string.logAllTypes)
        val allCategoriesLabel = stringResource(Res.string.logAllCategories)
        val autoScrollLabel = stringResource(Res.string.logActionAutoScroll)
        val clearLabel = stringResource(Res.string.logActionClear)
        val unknownTypeLabel = stringResource(Res.string.logTypeUnknown)

        val typeOptions = buildList {
            add(allTypesLabel)
            addAll(allTypes.map { type -> logTypeLabel(type, allTypesLabel, unknownTypeLabel) })
        }
        val typeSelectedIndex = if (uiState.selectedType == null) {
            0
        } else {
            val index = allTypes.indexOf(uiState.selectedType)
            if (index >= 0) index + 1 else 0
        }

        val categoryOptions = buildList {
            add(allCategoriesLabel)
            addAll(allCategories)
        }
        val categorySelectedIndex = if (uiState.selectedCategory == null) {
            0
        } else {
            val index = allCategories.indexOf(uiState.selectedCategory)
            if (index >= 0) index + 1 else 0
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
                        screenModel.selectType(if (index == 0) {
                            null
                        } else {
                            allTypes.getOrNull(index - 1)
                        })
                    },
                    modifier = Modifier.weight(1f)
                )

                FluentDropdown(
                    items = categoryOptions,
                    selectedIndex = categorySelectedIndex,
                    onSelectionChange = { index, _ ->
                        screenModel.selectCategory(if (index == 0) {
                            null
                        } else {
                            allCategories.getOrNull(index - 1)
                        })
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.weight(1f))

                FluentToggleButton(
                    checked = uiState.autoScroll,
                    onCheckedChanged = { checked ->
                        if (checked != uiState.autoScroll) {
                            screenModel.toggleAutoScroll()
                        }
                    },
                    iconOnly = true
                ) {
                    FluentIcon(
                        imageVector = compose.icons.FeatherIcons.ChevronsDown,
                        contentDescription = autoScrollLabel
                    )
                }

                FluentButton(onClick = { screenModel.clear() }) {
                    FluentText(clearLabel)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val state = rememberLazyListState()

                LaunchedEffect(filteredLogs.size, uiState.autoScroll) {
                    if (uiState.autoScroll && filteredLogs.isNotEmpty()) {
                        state.scrollToItem(filteredLogs.size - 1)
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
                            items(
                                items = filteredLogs,
                                key = { entry -> entry.id },
                            ) { entry ->
                                LogItem(entry)
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

    fun logTypeLabel(
        type: Int?,
        allTypesLabel: String,
        unknownTypeLabel: String,
    ): String = when (type) {
        null -> allTypesLabel
        1 -> "Fatal"
        2 -> "Error"
        3 -> "Warning"
        4 -> "Display"
        5 -> "Log"
        6 -> "Verbose"
        7 -> "VeryVerbose"
        else -> unknownTypeLabel
    }

    fun logTypeColor(type: Int?) = when (type) {
        1 -> FluentTokens.ColorToken.LogLevel.fatal
        2 -> FluentTokens.ColorToken.LogLevel.error
        3 -> FluentTokens.ColorToken.LogLevel.warning
        4 -> FluentTokens.ColorToken.LogLevel.display
        5 -> FluentTokens.ColorToken.LogLevel.log
        6 -> FluentTokens.ColorToken.LogLevel.verbose
        7 -> FluentTokens.ColorToken.LogLevel.veryVerbose
        else -> FluentTokens.ColorToken.LogLevel.unknown
    }

    @Composable
    fun LogItem(entry: LogScreenEntry) {
        val log = entry.log
        val color = logTypeColor(log.type)
        val typeLabel = logTypeLabel(
            type = log.type,
            allTypesLabel = stringResource(Res.string.logAllTypes),
            unknownTypeLabel = stringResource(Res.string.logTypeUnknown),
        )
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

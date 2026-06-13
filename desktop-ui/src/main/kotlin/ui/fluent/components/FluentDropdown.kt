package ui.fluent.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.composefluent.component.ComboBox

/** Fluent 风格下拉选择组件，封装 compose-fluent 的 [ComboBox] */
@Composable
fun FluentDropdown(
    items: List<String>,
    selectedIndex: Int?,
    onSelectionChange: (index: Int, item: String) -> Unit,
    modifier: Modifier = Modifier,
    header: String? = null,
    placeholder: String? = null,
    disabled: Boolean = false
) {
    ComboBox(
        modifier = modifier,
        header = header,
        placeholder = placeholder,
        disabled = disabled,
        items = items,
        selected = selectedIndex,
        onSelectionChange = onSelectionChange
    )
}

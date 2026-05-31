package ui.fluent.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.composefluent.surface.Card

@Composable
fun FluentCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        content = content
    )
}

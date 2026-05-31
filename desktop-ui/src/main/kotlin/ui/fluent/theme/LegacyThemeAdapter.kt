package ui.fluent.theme

import androidx.compose.runtime.Composable

/**
 * Compatibility adapter used during migration.
 * Existing entry points can call this to adopt Fluent theming incrementally.
 */
@Composable
fun LegacyThemeAdapter(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    AppFluentTheme(
        darkTheme = darkTheme,
        content = content
    )
}

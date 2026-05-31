package ui.fluent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text
import io.github.composefluent.component.TextField
import io.github.composefluent.darkColors
import io.github.composefluent.lightColors

@Composable
fun FluentSandbox(isDarkTheme: Boolean) {
    var inputText by remember { mutableStateOf("Honkai RTS Launcher") }
    var switchChecked by remember { mutableStateOf(false) }
    val colors = if (isDarkTheme) darkColors() else lightColors()

    FluentTheme(colors = colors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Fluent UI Sandbox",
                style = FluentTheme.typography.subtitle
            )
            Text(
                text = "M1 baseline validation: Button / Text / Switch / TextField",
                style = FluentTheme.typography.body
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                header = { Text("Input") },
                placeholder = { Text("Type something...") }
            )

            Switcher(
                checked = switchChecked,
                onCheckStateChange = { switchChecked = it },
                text = "Enable sandbox option"
            )

            Button(
                onClick = { switchChecked = !switchChecked }
            ) {
                Text("Toggle Switch")
            }

            AccentButton(
                onClick = { inputText = "Fluent demo is running" }
            ) {
                Text("Primary Action")
            }
        }
    }
}

package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineChecking
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineDownloading
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineDownloadFinishing
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineErrorTitle
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineExtracting
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineInitializing
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineInstalling
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineReady
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineRestartRequired
import honkaigamelauncher.desktop_ui.generated.resources.websiteEngineRetry
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarSeverity
import io.github.composefluent.component.ProgressBar
import org.jetbrains.compose.resources.stringResource
import ui.fluent.components.FluentButton
import ui.fluent.components.FluentCard
import viewModel.WebEnginePhase
import io.github.composefluent.component.Text as FluentText

@Composable
fun WebEngineInitContent(
    phase: WebEnginePhase,
    progress: Float?,
    errorMessage: String?,
    restartRequired: Boolean,
    onRetry: () -> Unit
) {
    val message = when (phase) {
        WebEnginePhase.Checking -> stringResource(Res.string.websiteEngineChecking)
        WebEnginePhase.Downloading -> stringResource(Res.string.websiteEngineDownloading)
        WebEnginePhase.DownloadFinishing -> stringResource(Res.string.websiteEngineDownloadFinishing)
        WebEnginePhase.Extracting -> stringResource(Res.string.websiteEngineExtracting)
        WebEnginePhase.Installing -> stringResource(Res.string.websiteEngineInstalling)
        WebEnginePhase.Initializing -> stringResource(Res.string.websiteEngineInitializing)
        WebEnginePhase.Ready -> stringResource(Res.string.websiteEngineReady)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FluentCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FluentText(
                    text = message,
                    style = FluentTheme.typography.subtitle
                )
                if (errorMessage == null && !restartRequired) {
                    if (progress == null) {
                        ProgressBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    } else {
                        ProgressBar(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    }
                } else {
                    InfoBar(
                        title = { FluentText(stringResource(Res.string.websiteEngineErrorTitle)) },
                        message = {
                            FluentText(
                                if (restartRequired) {
                                    stringResource(Res.string.websiteEngineRestartRequired)
                                } else {
                                    errorMessage.orEmpty()
                                }
                            )
                        },
                        severity = InfoBarSeverity.Warning,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FluentButton(onClick = onRetry) {
                        FluentText(stringResource(Res.string.websiteEngineRetry))
                    }
                }
            }
        }
    }
}

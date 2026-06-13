package core.platform

import java.awt.Desktop
import java.io.File

class FileSystemGateway(
    private val directoryOpener: (File) -> Unit = { Desktop.getDesktop().open(it) },
) {
    fun openDirectory(path: String): Result<Unit> = runCatching {
        val directory = File(path)
        require(directory.exists() && directory.isDirectory) { "directory-not-found" }
        directoryOpener(directory)
    }
}

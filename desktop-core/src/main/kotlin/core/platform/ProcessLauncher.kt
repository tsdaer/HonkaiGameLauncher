package core.platform

import java.io.File

class ProcessLauncher {
    fun launch(executablePath: String): Result<Process> = runCatching {
        val executable = File(executablePath)
        ProcessBuilder(executable.absolutePath)
            .directory(executable.parentFile)
            .start()
    }
}

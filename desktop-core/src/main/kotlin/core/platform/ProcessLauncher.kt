package core.platform

import java.io.File

/**
 * 进程启动器。
 *
 * 封装 [ProcessBuilder] 调用，负责启动外部可执行文件（如游戏 exe）。
 *
 * ## 设计说明
 * - 工作目录设置为可执行文件所在目录，确保游戏运行时能正确访问相对路径资源
 * - 返回 [Result<Process>] 而非裸 [Process]，使得调用方可以统一处理启动失败
 * - 启动后不等待进程退出，由调用方管理进程生命周期
 */
class ProcessLauncher {
    /**
     * 启动一个外部可执行文件。
     *
     * @param executablePath 可执行文件的绝对路径
     * @return 成功则返回代表已启动进程的 [Process]（在 [Result] 中），
     *         失败（如文件不存在、无执行权限）则返回 [Result.failure]
     */
    fun launch(executablePath: String): Result<Process> = runCatching {
        val executable = File(executablePath)
        ProcessBuilder(executable.absolutePath)
            .directory(executable.parentFile)
            .start()
    }
}

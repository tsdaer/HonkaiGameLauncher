package core.platform

import java.awt.Desktop
import java.io.File

/**
 * 文件系统网关。
 *
 * 封装平台相关的文件系统操作，以方便测试和跨平台适配。
 *
 * 当前仅提供打开目录功能。通过构造函数注入 [directoryOpener]，
 * 测试时可以传递 mock 实现来验证调用行为。
 *
 * @property directoryOpener 实际执行目录打开操作的函数，默认使用 [Desktop.getDesktop().open]
 */
class FileSystemGateway(
    private val directoryOpener: (File) -> Unit = { Desktop.getDesktop().open(it) },
) {
    /**
     * 在操作系统的文件管理器中打开指定目录。
     *
     * @param path 要打开的目录路径
     * @return 成功返回 [Result.success]，失败（如路径不存在/非目录）返回 [Result.failure]
     */
    fun openDirectory(path: String): Result<Unit> = runCatching {
        val directory = File(path)
        require(directory.exists() && directory.isDirectory) { "directory-not-found" }
        directoryOpener(directory)
    }
}

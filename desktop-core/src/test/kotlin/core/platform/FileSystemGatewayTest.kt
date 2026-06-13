package core.platform

import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileSystemGatewayTest {
    @Test
    fun `open directory delegates existing directories to opener`() {
        val directory = createTempDirectory("honkai-open-dir").toFile()
        var openedPath = ""
        val gateway = FileSystemGateway { openedPath = it.absolutePath }

        val result = gateway.openDirectory(directory.absolutePath)

        assertTrue(result.isSuccess)
        assertEquals(directory.absolutePath, openedPath)
    }

    @Test
    fun `open directory fails for missing paths`() {
        val gateway = FileSystemGateway { error("should not open missing path") }

        val result = gateway.openDirectory("Z:/definitely/missing/honkai")

        assertTrue(result.isFailure)
    }
}

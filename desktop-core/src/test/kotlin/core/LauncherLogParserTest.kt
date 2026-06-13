package core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LauncherLogParserTest {
    @Test
    fun `parses single log entry`() {
        val logs = LauncherLogParser.parse(
            """{"type":1,"category":"Game","time":"10:00","message":"Started"}"""
        ).getOrThrow()

        assertEquals(1, logs.size)
        assertEquals("Started", logs.single().message)
    }

    @Test
    fun `parses array log entries`() {
        val logs = LauncherLogParser.parse(
            """
            [
              {"type":1,"category":"Game","time":"10:00","message":"Started"},
              {"type":2,"category":"Game","time":"10:01","message":"Ready"}
            ]
            """.trimIndent()
        ).getOrThrow()

        assertEquals(listOf("Started", "Ready"), logs.map { it.message })
    }

    @Test
    fun `invalid json returns failure`() {
        assertTrue(LauncherLogParser.parse("not json").isFailure)
    }
}

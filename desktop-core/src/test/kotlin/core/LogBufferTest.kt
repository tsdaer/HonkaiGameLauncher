package core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LogBufferTest {
    @Test
    fun `append tracks logs filtered logs and sorted options`() {
        val buffer = LogBuffer(maxLogCount = 10)

        val snapshot = buffer.append(
            listOf(
                log(type = 3, category = "Gameplay", message = "warning"),
                log(type = 1, category = "Engine", message = "fatal"),
                log(type = 3, category = "Engine", message = "warning again"),
            )
        )

        assertEquals(listOf("warning", "fatal", "warning again"), snapshot.logs.map { it.log.message })
        assertEquals(snapshot.logs, snapshot.filteredLogs)
        assertEquals(listOf(1, 3), snapshot.availableTypes)
        assertEquals(listOf("Engine", "Gameplay"), snapshot.availableCategories)
    }

    @Test
    fun `select type and category filters matching logs`() {
        val buffer = LogBuffer(maxLogCount = 10)
        buffer.append(
            listOf(
                log(type = 2, category = "Engine", message = "engine error"),
                log(type = 2, category = "Gameplay", message = "gameplay error"),
                log(type = 5, category = "Engine", message = "engine info"),
            )
        )

        buffer.selectType(2)
        val snapshot = buffer.selectCategory("Engine")

        assertEquals(listOf("engine error"), snapshot.filteredLogs.map { it.log.message })
        assertEquals(2, snapshot.uiState.selectedType)
        assertEquals("Engine", snapshot.uiState.selectedCategory)
    }

    @Test
    fun `trim overflow removes oldest logs and resets missing filters`() {
        val buffer = LogBuffer(maxLogCount = 2)
        buffer.append(listOf(log(type = 1, category = "Old", message = "old")))
        buffer.selectType(1)
        buffer.selectCategory("Old")

        val snapshot = buffer.append(
            listOf(
                log(type = 2, category = "New", message = "new one"),
                log(type = 3, category = "New", message = "new two"),
            )
        )

        assertEquals(listOf("new one", "new two"), snapshot.logs.map { it.log.message })
        assertEquals(listOf(2, 3), snapshot.availableTypes)
        assertEquals(listOf("New"), snapshot.availableCategories)
        assertNull(snapshot.uiState.selectedType)
        assertNull(snapshot.uiState.selectedCategory)
        assertEquals(snapshot.logs, snapshot.filteredLogs)
    }

    @Test
    fun `clear removes logs options and filters but keeps auto scroll preference`() {
        val buffer = LogBuffer(maxLogCount = 10)
        buffer.append(listOf(log(type = 2, category = "Engine", message = "error")))
        buffer.selectType(2)
        buffer.toggleAutoScroll()

        val snapshot = buffer.clear()

        assertEquals(emptyList(), snapshot.logs)
        assertEquals(emptyList(), snapshot.filteredLogs)
        assertEquals(emptyList(), snapshot.availableTypes)
        assertNull(snapshot.uiState.selectedType)
        assertFalse(snapshot.uiState.autoScroll)
    }

    private fun log(
        type: Int,
        category: String,
        message: String,
    ): LauncherLogEntry {
        return LauncherLogEntry(
            type = type,
            category = category,
            time = "10:00",
            message = message,
        )
    }
}

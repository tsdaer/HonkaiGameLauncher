package core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppSettingsRepositoryTest {
    @Test
    fun `normalize game path converts blank and legacy sentinel to null`() {
        assertNull(AppSettingsRepository.normalizeGamePath(null))
        assertNull(AppSettingsRepository.normalizeGamePath(""))
        assertNull(AppSettingsRepository.normalizeGamePath("   "))
        assertNull(AppSettingsRepository.normalizeGamePath("null"))
    }

    @Test
    fun `normalize game path trims valid values`() {
        assertEquals("C:/Games/Honkai.exe", AppSettingsRepository.normalizeGamePath("  C:/Games/Honkai.exe  "))
    }
}

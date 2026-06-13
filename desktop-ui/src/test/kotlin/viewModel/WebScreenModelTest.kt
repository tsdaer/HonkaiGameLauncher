package viewModel

import kotlin.test.Test
import kotlin.test.assertEquals

class WebScreenModelTest {
    @Test
    fun `normalize url falls back to home for blank input`() {
        assertEquals(WebScreenModel.HOME_URL, WebScreenModel.normalizeUrl(""))
        assertEquals(WebScreenModel.HOME_URL, WebScreenModel.normalizeUrl("   "))
    }

    @Test
    fun `normalize url keeps existing http and https schemes`() {
        assertEquals("http://localhost:3000", WebScreenModel.normalizeUrl(" http://localhost:3000 "))
        assertEquals("HTTPS://example.com", WebScreenModel.normalizeUrl("HTTPS://example.com"))
    }

    @Test
    fun `normalize url adds https scheme when missing`() {
        assertEquals("https://example.com/docs", WebScreenModel.normalizeUrl("example.com/docs"))
    }
}

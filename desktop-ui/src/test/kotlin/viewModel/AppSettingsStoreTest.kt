package viewModel

import core.platform.AppSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsStoreTest {
    @Test
    fun `loads initial state from repository`() {
        val repository = FakeAppSettingsRepository(
            gamePath = "C:/Games/Honkai.exe",
            logMaxEntries = 250,
        )
        val store = AppSettingsStore(repository)

        assertEquals(
            AppSettingsState(
                gamePath = "C:/Games/Honkai.exe",
                logMaxEntries = 250,
            ),
            store.state.value,
        )
    }

    @Test
    fun `set game path persists normalized path and emits updated state`() = runTest {
        val repository = FakeAppSettingsRepository()
        val store = AppSettingsStore(repository)

        store.setGamePath("  D:/Games/Honkai.exe  ")

        assertEquals("D:/Games/Honkai.exe", repository.getGamePath())
        assertEquals("D:/Games/Honkai.exe", store.state.value.gamePath)
    }

    @Test
    fun `set blank game path clears state`() = runTest {
        val repository = FakeAppSettingsRepository(gamePath = "C:/Games/Honkai.exe")
        val store = AppSettingsStore(repository)

        store.setGamePath("   ")

        assertNull(repository.getGamePath())
        assertNull(store.state.value.gamePath)
    }

    private class FakeAppSettingsRepository(
        gamePath: String? = null,
        private val logMaxEntries: Int = AppSettingsStore.DEFAULT_LOG_MAX_ENTRIES,
    ) : AppSettingsRepository {
        private var storedGamePath = AppSettingsRepository.normalizeGamePath(gamePath)

        override fun getGamePath(): String? {
            return storedGamePath
        }

        override fun setGamePath(path: String?) {
            storedGamePath = AppSettingsRepository.normalizeGamePath(path)
        }

        override fun getLogMaxEntries(defaultValue: Int): Int {
            return logMaxEntries
        }
    }
}

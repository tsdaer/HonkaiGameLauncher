package viewModel

import com.russhwolf.settings.Settings
import core.platform.AppSettingsRepository

class SettingsAppSettingsRepository(
    private val settings: Settings = Settings(),
) : AppSettingsRepository {
    override fun getGamePath(): String? {
        return AppSettingsRepository.normalizeGamePath(
            settings.getString(
                key = AppSettingsRepository.GAME_PATH_KEY,
                defaultValue = AppSettingsRepository.NO_GAME_PATH_SENTINEL,
            )
        )
    }

    override fun setGamePath(path: String?) {
        settings.putString(
            key = AppSettingsRepository.GAME_PATH_KEY,
            value = AppSettingsRepository.normalizeGamePath(path)
                ?: AppSettingsRepository.NO_GAME_PATH_SENTINEL,
        )
    }

    override fun getLogMaxEntries(defaultValue: Int): Int {
        return settings.getInt(AppSettingsRepository.LOG_MAX_ENTRIES_KEY, defaultValue)
    }
}

package core.platform

interface AppSettingsRepository {
    fun getGamePath(): String?

    fun setGamePath(path: String?)

    fun getLogMaxEntries(defaultValue: Int): Int

    companion object {
        const val GAME_PATH_KEY = "gamePath"
        const val LOG_MAX_ENTRIES_KEY = "logMaxEntries"
        const val NO_GAME_PATH_SENTINEL = "null"

        fun normalizeGamePath(path: String?): String? {
            return path
                ?.trim()
                ?.takeUnless { it.isBlank() || it == NO_GAME_PATH_SENTINEL }
        }
    }
}

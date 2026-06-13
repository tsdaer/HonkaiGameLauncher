package core

import kotlinx.serialization.json.Json

object LauncherLogParser {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parse(text: String): Result<List<LauncherLogEntry>> = runCatching {
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            json.decodeFromString<List<LauncherLogEntry>>(trimmed)
        } else {
            listOf(json.decodeFromString<LauncherLogEntry>(trimmed))
        }
    }
}

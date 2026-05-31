package core

import kotlinx.serialization.Serializable

@Serializable
data class LauncherLogEntry(
    val type: Int,
    val category: String,
    val time: String,
    val message: String
)

package org.lerchenflo.schneaggchatv3mp.app.logging

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class LogType {
    INFO, WARNING, ERROR, DEBUG
}

@Entity
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: LogType,
    val message: String
)

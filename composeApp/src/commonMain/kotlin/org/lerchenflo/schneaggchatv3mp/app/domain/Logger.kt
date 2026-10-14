package org.lerchenflo.schneaggchatv3mp.app.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType

/** App log: prints each line and keeps the most recent entries for the in-app log viewer. */
interface Logger {
    suspend fun log(message: String, logType: LogType)

    suspend fun logWarning(message: String)
    suspend fun logError(message: String)
    suspend fun logInfo(message: String)
    suspend fun logDebug(message: String)

    fun getLogs(): Flow<List<LogEntry>>

    suspend fun clearLogs()
}

package org.lerchenflo.schneaggchatv3mp.app.logging

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import kotlin.time.Clock

private const val MAX_MESSAGE_LENGTH = 8000

class LoggingRepository(
    private val database: AppDatabase
){
    suspend fun log(message: String, logType: LogType){
        // Some throwables (e.g. a deep StackOverflowError) produce stack traces with thousands
        // of lines. Rendering a message that large as a single Text in the logs viewer is what
        // used to crash the dialog when trying to view such an entry, so cap it here.
        val truncatedMessage = if (message.length > MAX_MESSAGE_LENGTH) {
            message.take(MAX_MESSAGE_LENGTH) + "\n… truncated (${message.length} chars total)"
        } else {
            message
        }

        val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val formattedTimestamp = "${timestamp.year}-${timestamp.month.ordinal.toString().padStart(2, '0')}-${timestamp.day.toString().padStart(2, '0')} ${timestamp.hour.toString().padStart(2, '0')}:${timestamp.minute.toString().padStart(2, '0')}:${timestamp.second.toString().padStart(2, '0')}"
        val consoleMessage = "[$formattedTimestamp] [${logType.name}] $truncatedMessage"
        println(consoleMessage)
        database.logDao().upsertLog(LogEntry(
            type = logType,
            message = truncatedMessage
        ))
        // Keep only the 50 most recent log entries - cleanup only when count exceeds 50
        val logCount = database.logDao().getLogCount()
        if (logCount > 50) {
            database.logDao().deleteOldLogs()
        }
    }

    suspend fun logWarning(message: String) = log(message, LogType.WARNING)
    suspend fun logError(message: String) = log(message, LogType.ERROR)
    suspend fun logInfo(message: String) = log(message, LogType.INFO)
    suspend fun logDebug(message: String) = log(message, LogType.DEBUG)

    fun getLogs(): Flow<List<LogEntry>> {
        return database.logDao().getLogs()
    }

    suspend fun clearLogs() {
        database.logDao().clearLogs()
    }
}

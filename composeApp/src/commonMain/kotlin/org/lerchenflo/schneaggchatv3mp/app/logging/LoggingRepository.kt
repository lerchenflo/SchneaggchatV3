package org.lerchenflo.schneaggchatv3mp.app.logging

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import kotlin.time.Clock

class LoggingRepository(
    private val database: AppDatabase
){
    suspend fun log(message: String, logType: LogType){
        val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val formattedTimestamp = "${timestamp.year}-${timestamp.month.ordinal.toString().padStart(2, '0')}-${timestamp.day.toString().padStart(2, '0')} ${timestamp.hour.toString().padStart(2, '0')}:${timestamp.minute.toString().padStart(2, '0')}:${timestamp.second.toString().padStart(2, '0')}"
        val consoleMessage = "[$formattedTimestamp] [${logType.name}] $message"
        println(consoleMessage)
        database.logDao().upsertLog(LogEntry(
            type = logType,
            message = message
        ))
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

package org.lerchenflo.schneaggchatv3mp.app.logging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase

class LoggingRepository(
    private val database: AppDatabase
){
    suspend fun log(message: String, logType: LogType){
        println("ADDING TO LOGS: $message")
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

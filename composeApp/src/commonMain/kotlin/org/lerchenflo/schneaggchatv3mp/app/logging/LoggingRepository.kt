package org.lerchenflo.schneaggchatv3mp.app.logging

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
}

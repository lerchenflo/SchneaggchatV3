package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.app.logging.LogEntry
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType
import org.lerchenflo.schneaggchatv3mp.datasource.domain.Logger

/**
 * [Logger] that keeps every entry in memory, so tests can check what was logged.
 * Safe to call from several threads (Room and Ktor call back on IO threads).
 */
class FakeLogger : Logger {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())

    val logs: List<LogEntry> get() = _logs.value

    override suspend fun log(message: String, logType: LogType) {
        _logs.update { it + LogEntry(type = logType, message = message) }
    }

    override suspend fun logWarning(message: String) = log(message, LogType.WARNING)
    override suspend fun logError(message: String) = log(message, LogType.ERROR)
    override suspend fun logInfo(message: String) = log(message, LogType.INFO)
    override suspend fun logDebug(message: String) = log(message, LogType.DEBUG)

    override fun getLogs(): Flow<List<LogEntry>> = _logs.asStateFlow()

    override suspend fun clearLogs() {
        _logs.value = emptyList()
    }
}

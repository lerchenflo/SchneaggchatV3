package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository

/**
 * [AuthLog] on top of [LoggingRepository]. Persisting a line is three Room calls and the manager
 * reports from inside its lock, so the write is handed to [scope] instead of awaited, and a
 * failing write (database closed mid-logout, disk full) is printed rather than thrown: logging
 * must never block or break the session state machine.
 */
class LoggingAuthLog(
    private val loggingRepository: LoggingRepository,
    private val scope: CoroutineScope,
) : AuthLog {
    // Console only: these fire on every suppressed/skipped attempt and would push the real
    // outcomes out of the 50-entry persisted log.
    override suspend fun debug(message: String) = println("[DEBUG] $message")
    override suspend fun info(message: String) = persist(LogType.INFO, message)
    override suspend fun warn(message: String) = persist(LogType.WARNING, message)
    override suspend fun error(message: String) = persist(LogType.ERROR, message)

    private fun persist(type: LogType, message: String) {
        scope.launch {
            try {
                loggingRepository.log(message, type)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[${type.name}] $message (not persisted: ${e.message})")
            }
        }
    }
}

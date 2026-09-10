package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository

class LoggingAuthLog(
    private val loggingRepository: LoggingRepository,
) : AuthLog {
    // Console only: these fire on every suppressed/skipped attempt and would push the real
    // outcomes out of the 50-entry persisted log.
    override suspend fun debug(message: String) = println("[DEBUG] $message")
    override suspend fun info(message: String) = loggingRepository.logInfo(message)
    override suspend fun warn(message: String) = loggingRepository.logWarning(message)
    override suspend fun error(message: String) = loggingRepository.logError(message)
}

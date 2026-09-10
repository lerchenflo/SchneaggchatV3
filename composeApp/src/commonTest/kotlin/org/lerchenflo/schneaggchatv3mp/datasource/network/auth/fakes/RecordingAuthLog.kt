package org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes

import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthLog

class RecordingAuthLog : AuthLog {
    val lines = mutableListOf<String>()

    override suspend fun debug(message: String) { lines += "DEBUG $message" }
    override suspend fun info(message: String) { lines += "INFO $message" }
    override suspend fun warn(message: String) { lines += "WARN $message" }
    override suspend fun error(message: String) { lines += "ERROR $message" }
}

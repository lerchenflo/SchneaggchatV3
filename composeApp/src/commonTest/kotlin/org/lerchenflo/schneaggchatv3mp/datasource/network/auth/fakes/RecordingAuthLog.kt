package org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes

import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthLog

class RecordingAuthLog : AuthLog {
    val lines = mutableListOf<String>()

    /** When set, every call throws - models a log sink whose database is closed or full. */
    var failing = false

    override suspend fun debug(message: String) = record("DEBUG $message")
    override suspend fun info(message: String) = record("INFO $message")
    override suspend fun warn(message: String) = record("WARN $message")
    override suspend fun error(message: String) = record("ERROR $message")

    private fun record(line: String) {
        if (failing) throw IllegalStateException("fake log failure")
        lines += line
    }
}

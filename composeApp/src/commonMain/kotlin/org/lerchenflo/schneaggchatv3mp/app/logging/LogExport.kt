package org.lerchenflo.schneaggchatv3mp.app.logging

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString

@OptIn(FormatStringsInDatetimeFormats::class)
fun formatAllLogs(logs: List<LogEntry>): String {
    if (logs.isEmpty()) return "No logs available"

    val timestamp =
        kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LocalDateTime.Format { byUnicodePattern("dd-MM-yyyy HH:mm:ss") })

    val header = """
        ========================================
        Application Logs Export
        Generated: $timestamp
        Total Logs: ${logs.size}
        ========================================
        
    """.trimIndent()

    val logEntries = logs.joinToString("\n\n") { log ->
        """
        [${log.type}] ${log.id}
        Message: ${log.message}
        
        timestamp: ${millisToString(log.timeStamp, format = "dd.MM HH:mm:ss.SSS")}
        ---
        """.trimIndent()
    }

    val footer = """
        
        ========================================
        End of Logs
        ========================================
    """.trimIndent()

    return header + logEntries + footer
}

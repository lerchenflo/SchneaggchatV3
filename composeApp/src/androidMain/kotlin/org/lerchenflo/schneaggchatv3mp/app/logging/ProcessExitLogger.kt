package org.lerchenflo.schneaggchatv3mp.app.logging

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import java.time.Instant

private const val EXIT_LOG_PREFIX = "Previous app process ended abnormally"
private const val MAX_TRACE_LINES = 40
private const val MAX_TRACE_CHARS = 3_000

/**
 * Logs into the app log (Settings log viewer) how the previous app process ended, when that end
 * was not a normal one (crash, native crash, ANR, kill by the system). The uncaught exception
 * handler only sees Kotlin/Java crashes; native crashes, ANRs and kills leave no log behind
 * otherwise. The app log itself tells whether this exit was logged already. Android 11+.
 */
fun logAbnormalLastExit(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val koin = KoinPlatform.getKoin()
    koin.get<ApplicationScope>().launch {
        val logger = koin.get<Logger>()
        val exit = lastExit(context) ?: return@launch
        if (exit.isNormal()) return@launch

        val alreadyLogged = logger.getLogs().first().any { it.message.startsWith(exit.headline()) }
        if (!alreadyLogged) logger.log(exit.describe(), exit.logType())
    }
}

/** How the previous process of this app ended (newest entry of the system's exit history). */
@RequiresApi(Build.VERSION_CODES.R)
private fun lastExit(context: Context): ApplicationExitInfo? {
    val activityManager = context.getSystemService(ActivityManager::class.java) ?: return null
    return activityManager.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull()
}

@RequiresApi(Build.VERSION_CODES.R)
private fun ApplicationExitInfo.isNormal(): Boolean = when (reason) {
    ApplicationExitInfo.REASON_EXIT_SELF -> status == 0
    ApplicationExitInfo.REASON_USER_REQUESTED,
    ApplicationExitInfo.REASON_USER_STOPPED,
    ApplicationExitInfo.REASON_PACKAGE_UPDATED,
    ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE,
    ApplicationExitInfo.REASON_PERMISSION_CHANGE -> true
    else -> false
}

@RequiresApi(Build.VERSION_CODES.R)
private fun ApplicationExitInfo.logType(): LogType = when (reason) {
    ApplicationExitInfo.REASON_CRASH,
    ApplicationExitInfo.REASON_CRASH_NATIVE,
    ApplicationExitInfo.REASON_ANR,
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> LogType.ERROR
    else -> LogType.WARNING
}

/** First part of the log message; reason and time identify the exit in the app log. */
@RequiresApi(Build.VERSION_CODES.R)
private fun ApplicationExitInfo.headline(): String =
    "$EXIT_LOG_PREFIX: ${reasonName()} at ${Instant.ofEpochMilli(timestamp)}"

@RequiresApi(Build.VERSION_CODES.R)
private fun ApplicationExitInfo.describe(): String = buildString {
    append(headline())
    append(" (process $processName, importance $importance, status $status")
    append(", pss ${pss / 1024} MB, rss ${rss / 1024} MB)")
    description?.let { append(": $it") }
    anrTrace()?.let { append("\n\nANR trace:\n$it") }
}

/**
 * The main thread's stack from an ANR thread dump (that's where an ANR blocks), at most
 * [MAX_TRACE_LINES] lines and [MAX_TRACE_CHARS] chars so the log entry stays small. Falls back to
 * the dump's first lines when no main thread block is found. Native crash tombstones are binary
 * and skipped.
 */
@RequiresApi(Build.VERSION_CODES.R)
private fun ApplicationExitInfo.anrTrace(): String? {
    if (reason != ApplicationExitInfo.REASON_ANR) return null
    return runCatching {
        traceInputStream?.bufferedReader()?.use { reader ->
            val lines = reader.readLines()
            val mainStart = lines.indexOfFirst { it.startsWith("\"main\"") }
            val block = if (mainStart >= 0) {
                lines.drop(mainStart).takeWhile { it.isNotBlank() }
            } else {
                lines
            }
            block.take(MAX_TRACE_LINES).joinToString("\n").take(MAX_TRACE_CHARS).ifBlank { null }
        }
    }.getOrNull()
}

@RequiresApi(Build.VERSION_CODES.R)
private fun ApplicationExitInfo.reasonName(): String = when (reason) {
    ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
    ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
    ApplicationExitInfo.REASON_CRASH -> "CRASH"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
    ApplicationExitInfo.REASON_ANR -> "ANR"
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
    ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
    ApplicationExitInfo.REASON_FREEZER -> "FREEZER"
    ApplicationExitInfo.REASON_OTHER -> "OTHER"
    else -> "UNKNOWN($reason)"
}

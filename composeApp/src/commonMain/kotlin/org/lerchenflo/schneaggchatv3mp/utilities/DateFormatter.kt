@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.runtime.Composable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.yesterday
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(FormatStringsInDatetimeFormats::class)
fun getCurrentTimeMillisString(): String {
    return getCurrentTimeMillisLong().toString()
}

fun getCurrentTimeMillisLong(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

fun getStartOfYearMillis(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    // 1. Get the current instant in time
    val now = Clock.System.now()

    // 2. Convert to local date-time to extract the current year number
    val currentYear = now.toLocalDateTime(timeZone).year

    // 3. Create a LocalDate representing January 1st of this year
    val startOfYearDate = LocalDate(currentYear, 1, 1)

    // 4. Get the Instant when that day started in the specified timezone
    val startOfYearInstant = startOfYearDate.atStartOfDayIn(timeZone)

    // 5. Convert that instant to epoch milliseconds
    return startOfYearInstant.toEpochMilliseconds()
}

@OptIn(FormatStringsInDatetimeFormats::class)
fun millisToString(
    millis: Long,
    format: String = "dd.MM.yyyy HH:mm:ss"
): String {
    val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val formatter = LocalDateTime.Format { byUnicodePattern(format) }
    return localDateTime.format(formatter)
}

@OptIn(FormatStringsInDatetimeFormats::class)
fun iso8601DateFormatter(
    iso8601Format: String,
    format: String = "dd.MM.yyyy"
): String {
    if(iso8601Format.isEmpty()){
        return ""
    }
    return try {
        // Parse the ISO string (e.g., "2004-08-13")
        val date = LocalDate.parse(iso8601Format)

        // Define the output format
        val formatter = LocalDate.Format { byUnicodePattern(format) }

        date.format(formatter)
    } catch (e: Exception) {
        println("Error parsing date: ${e.message}")
        ""
    }

}

@OptIn(ExperimentalTime::class)
fun millisToDuration(
    millis: Long,
    showYears: Boolean = true,
    showMonths: Boolean = true,
    showWeeks: Boolean = true,
    showDays: Boolean = true,
    showHours: Boolean = true,
    showMinutes: Boolean = true,
    showSeconds: Boolean = true,
): String {
    val SECOND = 1L
    val MINUTE = 60 * SECOND
    val HOUR   = 60 * MINUTE
    val DAY    = 24 * HOUR
    val WEEK   =  7 * DAY
    val MONTH  = 30 * DAY
    val YEAR   = 365 * DAY

    var remaining = millis / 1000  // total seconds

    val years   = remaining / YEAR;   remaining %= YEAR
    val months  = remaining / MONTH;  remaining %= MONTH
    val weeks   = remaining / WEEK;   remaining %= WEEK
    val days    = remaining / DAY;    remaining %= DAY
    val hours   = remaining / HOUR;   remaining %= HOUR
    val minutes = remaining / MINUTE; remaining %= MINUTE
    val seconds = remaining / SECOND

    return listOfNotNull(
        if (showYears   && years   > 0) "${years}y"     else null,
        if (showMonths  && months  > 0) "${months}mo"   else null,
        if (showWeeks   && weeks   > 0) "${weeks}w"     else null,
        if (showDays    && days    > 0) "${days}d"      else null,
        if (showHours   && hours   > 0) "${hours}h"     else null,
        if (showMinutes && minutes > 0) "${minutes}min" else null,
        if (showSeconds && seconds > 0) "${seconds}s"   else null,
    ).joinToString(" ").ifEmpty { "0s" }
}


@OptIn(FormatStringsInDatetimeFormats::class, ExperimentalTime::class)
@Composable
fun millisToTimeDateOrYesterday(
    millis: Long,
    timeFormat: String = "HH:mm",
    dateFormatWithoutYear: String = "dd.MM",
    dateFormatWithYear: String = "dd.MM.yyyy"
): String {
    val tz = TimeZone.currentSystemDefault()
    val instant = Instant.fromEpochMilliseconds(millis)
    val now = Clock.System.now()

    val targetLdt = instant.toLocalDateTime(tz)
    val nowLdt = now.toLocalDateTime(tz)

    val targetDate = targetLdt.date
    val currentDate = nowLdt.date

    return when {
        // Today -> show time
        targetDate == currentDate -> {
            val timeFormatter = LocalDateTime.Format { byUnicodePattern(timeFormat) }
            targetLdt.format(timeFormatter)
        }

        // Yesterday -> localized "Yesterday" string
        targetDate == currentDate.minus(DatePeriod(days = 1)) -> {
            stringResource(Res.string.yesterday)
        }

        // Same year -> show date without year
        targetDate.year == currentDate.year -> {
            val dateFormatter = LocalDateTime.Format { byUnicodePattern(dateFormatWithoutYear) }
            "${targetLdt.format(dateFormatter)}."
        }

        // Older -> show full date with year
        else -> {
            val dateFormatter = LocalDateTime.Format { byUnicodePattern(dateFormatWithYear) }
            targetLdt.format(dateFormatter)
        }
    }
}

fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    // padStart ensures the seconds always have two digits (e.g., 0:05 instead of 0:5)
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

fun isBirthdayToday(birthDate: String?): Boolean {
    if (birthDate.isNullOrBlank()) return false
    val parsed = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return false
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return (parsed.month == today.month && parsed.day == today.day) ||  //Birthdate is really today
            (parsed.month == Month(2) && parsed.day == 29 &&            //29. Feb is shown on 28th
                    today.month == Month(2) && today.day ==28)
}

package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.runtime.Composable
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.just_now
import schneaggchatv3mp.composeapp.generated.resources.yesterday


@OptIn(kotlinx.datetime.format.FormatStringsInDatetimeFormats::class)
fun getCurrentTimeMillisString(
): String {
    return getCurrentTimeMillisLong().toString()
}

fun getCurrentTimeMillisLong(): Long {
    return Clock.System.now().toEpochMilliseconds()
}


@OptIn(kotlinx.datetime.format.FormatStringsInDatetimeFormats::class)
fun millisToString(
    millis: Long,
    format: String = "dd.MM.yyyy HH:mm:ss"
): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val formatter = LocalDateTime.Format { byUnicodePattern(format) }
    return formatter.format(localDateTime)
}

@Composable
fun millisToDuration(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val now = Clock.System.now()
    val duration = now - instant

    return when {
        duration.inWholeDays > 7 -> "${duration.inWholeDays / 7}w"
        duration.inWholeDays > 0 -> "${duration.inWholeDays}d"
        duration.inWholeHours > 0 -> "${duration.inWholeHours}h"
        duration.inWholeMinutes > 0 -> "${duration.inWholeMinutes}min"
        else -> stringResource(Res.string.just_now)
    }
}

@OptIn(kotlinx.datetime.format.FormatStringsInDatetimeFormats::class)
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
            timeFormatter.format(targetLdt)
        }

        // Yesterday -> localized "Yesterday" string
        targetDate == currentDate.minus(DatePeriod(days = 1)) -> {
            stringResource(Res.string.yesterday)
        }

        // Same year -> show date without year
        targetDate.year == currentDate.year -> {
            val dateFormatter = LocalDateTime.Format { byUnicodePattern(dateFormatWithoutYear) }
            dateFormatter.format(targetLdt) + "."
        }

        // Older -> show full date with year
        else -> {
            val dateFormatter = LocalDateTime.Format { byUnicodePattern(dateFormatWithYear) }
            dateFormatter.format(targetLdt)
        }
    }
}
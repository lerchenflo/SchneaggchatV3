package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.runtime.Composable
import kotlinx.datetime.Clock
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

@Composable
@OptIn(kotlinx.datetime.format.FormatStringsInDatetimeFormats::class)
fun millisToTimeDateOrYesterday(
    millis: Long,
    timeFormat: String = "HH:mm",
    dateFormatWithoutYear: String = "dd.MM",
    dateFormatWithYear: String = "dd.MM.yyyy"
): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val now = Clock.System.now()

    val currentDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val targetDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date

    return when {
        targetDate == currentDate -> {
            // Today - return time only
            val timeFormatter = LocalDateTime.Format { byUnicodePattern(timeFormat) }
            timeFormatter.format(instant.toLocalDateTime(TimeZone.currentSystemDefault()))
        }
        targetDate == currentDate.minus(1, DateTimeUnit.DAY) -> {
            // Yesterday
            stringResource(Res.string.yesterday)
        }
        targetDate.year == currentDate.year -> {
            // This year - return date without year
            val dateFormatter = LocalDateTime.Format { byUnicodePattern(dateFormatWithoutYear) }
            dateFormatter.format(instant.toLocalDateTime(TimeZone.currentSystemDefault())) + "."
        }
        else -> {
            // Previous years - return date with year
            val dateFormatter = LocalDateTime.Format { byUnicodePattern(dateFormatWithYear) }
            dateFormatter.format(instant.toLocalDateTime(TimeZone.currentSystemDefault()))
        }
    }
}
@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.month_april
import schneaggchatv3mp.composeapp.generated.resources.month_august
import schneaggchatv3mp.composeapp.generated.resources.month_december
import schneaggchatv3mp.composeapp.generated.resources.month_february
import schneaggchatv3mp.composeapp.generated.resources.month_january
import schneaggchatv3mp.composeapp.generated.resources.month_july
import schneaggchatv3mp.composeapp.generated.resources.month_june
import schneaggchatv3mp.composeapp.generated.resources.month_march
import schneaggchatv3mp.composeapp.generated.resources.month_may
import schneaggchatv3mp.composeapp.generated.resources.month_november
import schneaggchatv3mp.composeapp.generated.resources.month_october
import schneaggchatv3mp.composeapp.generated.resources.month_september
import schneaggchatv3mp.composeapp.generated.resources.weekday_friday_short
import schneaggchatv3mp.composeapp.generated.resources.weekday_monday_short
import schneaggchatv3mp.composeapp.generated.resources.weekday_saturday_short
import schneaggchatv3mp.composeapp.generated.resources.weekday_sunday_short
import schneaggchatv3mp.composeapp.generated.resources.weekday_thursday_short
import schneaggchatv3mp.composeapp.generated.resources.weekday_tuesday_short
import schneaggchatv3mp.composeapp.generated.resources.weekday_wednesday_short
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

/**
 * The next occurrence of [parsed]'s month/day on or after [today], or null if [parsed] is blank/
 * unparseable. 29. Feb is treated as 28. Feb in non-leap years, consistent with [isBirthdayToday].
 */
private fun nextBirthdayOccurrence(parsed: LocalDate, today: LocalDate): LocalDate {
    fun occurrenceInYear(year: Int): LocalDate {
        return if (parsed.month == Month(2) && parsed.day == 29 && !isLeapYear(year)) {
            LocalDate(year, 2, 28)
        } else {
            LocalDate(year, parsed.month, parsed.day)
        }
    }

    val thisYear = occurrenceInYear(today.year)
    return if (thisYear >= today) thisYear else occurrenceInYear(today.year + 1)
}

/**
 * Days from today until the next occurrence of this birthday. 0 means the birthday is today.
 * Null if [birthDate] is blank/unparseable.
 */
fun daysUntilNextBirthday(birthDate: String?): Int? {
    if (birthDate.isNullOrBlank()) return null
    val parsed = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return null
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    return today.daysUntil(nextBirthdayOccurrence(parsed, today))
}

/**
 * The calendar month (1-12) of the next occurrence of this birthday, or null if [birthDate] is
 * blank/unparseable.
 */
fun nextBirthdayMonth(birthDate: String?): Int? {
    if (birthDate.isNullOrBlank()) return null
    val parsed = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return null
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    return nextBirthdayOccurrence(parsed, today).month.number
}

/**
 * The age the person turns on their next birthday, or null if [birthDate] is blank/unparseable,
 * has no plausible birth year (before 1900), or would produce an implausible age (<= 0 or > 130).
 */
fun ageOnNextBirthday(birthDate: String?): Int? {
    if (birthDate.isNullOrBlank()) return null
    val parsed = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return null
    if (parsed.year < 1900) return null

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val nextOccurrence = nextBirthdayOccurrence(parsed, today)
    val age = nextOccurrence.year - parsed.year

    return age.takeIf { it in 1..130 }
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

/** [millis] as a [LocalDate] in the device's current time zone. */
fun millisToLocalDate(millis: Long): LocalDate {
    return kotlin.time.Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
}

/** Monday of the ISO week containing [date]. */
fun startOfWeek(date: LocalDate): LocalDate {
    return date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))
}

@OptIn(FormatStringsInDatetimeFormats::class)
fun LocalDate.toFormattedString(format: String = "dd.MM.yyyy"): String {
    val formatter = LocalDate.Format { byUnicodePattern(format) }
    return this.format(formatter)
}

/** ISO month number (1-12) to its localized full name resource. */
fun monthNameResource(month: Int): StringResource = when (month) {
    1 -> Res.string.month_january
    2 -> Res.string.month_february
    3 -> Res.string.month_march
    4 -> Res.string.month_april
    5 -> Res.string.month_may
    6 -> Res.string.month_june
    7 -> Res.string.month_july
    8 -> Res.string.month_august
    9 -> Res.string.month_september
    10 -> Res.string.month_october
    11 -> Res.string.month_november
    else -> Res.string.month_december
}

/** [kotlinx.datetime.DayOfWeek] to its localized 2-letter abbreviation resource. */
fun weekdayShortResource(dayOfWeek: kotlinx.datetime.DayOfWeek): StringResource = when (dayOfWeek.isoDayNumber) {
    1 -> Res.string.weekday_monday_short
    2 -> Res.string.weekday_tuesday_short
    3 -> Res.string.weekday_wednesday_short
    4 -> Res.string.weekday_thursday_short
    5 -> Res.string.weekday_friday_short
    6 -> Res.string.weekday_saturday_short
    else -> Res.string.weekday_sunday_short
}

/** Today's date, recomputed when the day rolls over past local midnight. */
@Composable
fun rememberToday(): LocalDate {
    val tz = TimeZone.currentSystemDefault()
    var today by remember { mutableStateOf(Clock.System.now().toLocalDateTime(tz).date) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Clock.System.now()
            today = now.toLocalDateTime(tz).date
            val nextMidnight = today.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)
            delay((nextMidnight - now).inWholeMilliseconds.coerceAtLeast(0L) + 1000L)
        }
    }

    return today
}

/** month * 100 + day - a yearly-recurring key for a birthday, independent of year. */
fun LocalDate.monthDayKey(): Int = month.number * 100 + day

/**
 * Birthday keys that fall on [date]: its own key, plus 229 when [date] is 28 Feb in a non-leap
 * year - same 29-Feb rule as [isBirthdayToday].
 */
fun birthdayKeysFor(date: LocalDate): List<Int> {
    val keys = mutableListOf(date.monthDayKey())
    if (date.month == Month(2) && date.day == 28 && !isLeapYear(date.year)) {
        keys.add(229)
    }
    return keys
}

/**
 * Age turned on [date] by someone born in [birthYear], or null when [birthYear] is missing or
 * implausible (before 1900, or yields an age outside 1..130). Mirrors the guard in
 * [ageOnNextBirthday] but for an arbitrary [date] instead of the next upcoming occurrence.
 */
fun ageOnDate(birthYear: Int?, date: LocalDate): Int? {
    if (birthYear == null || birthYear < 1900) return null
    val age = date.year - birthYear
    return age.takeIf { it in 1..130 }
}

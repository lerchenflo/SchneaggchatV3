package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate
import org.lerchenflo.schneaggchatv3mp.utilities.birthdayKeysFor

/** A friend's (or the own user's) recurring birthday, shown on the events calendar. */
@Stable
data class CalendarBirthday(
    val userId: String,
    val displayName: String,
    val profilePictureUrl: String?,
    val monthDayKey: Int, // month * 100 + day; 29 Feb stored as 229
    val birthYear: Int?,
    val isOwn: Boolean,
)

/** Birthdays landing on [date] (including the 29-Feb-on-28-Feb rule), deduplicated by user. */
fun birthdaysOn(byMonthDay: Map<Int, List<CalendarBirthday>>, date: LocalDate): List<CalendarBirthday> {
    return birthdayKeysFor(date)
        .flatMap { byMonthDay[it].orEmpty() }
        .distinctBy { it.userId }
}

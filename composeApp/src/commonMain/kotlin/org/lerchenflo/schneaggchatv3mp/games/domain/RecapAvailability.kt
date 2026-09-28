package org.lerchenflo.schneaggchatv3mp.games.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Hardcoded recap season: the chat selector button shows from mid December to mid January, then
 * the settings entry takes over for another month. Outside that, the recap is only reachable from
 * the developer settings.
 */
object RecapAvailability {

    private const val SEASON_SPLIT_DAY = 15

    fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    /** 15 Dec - 15 Jan */
    fun isChatSelectorButtonVisible(date: LocalDate = today()): Boolean =
        (date.month == Month.DECEMBER && date.day >= SEASON_SPLIT_DAY) ||
                (date.month == Month.JANUARY && date.day <= SEASON_SPLIT_DAY)

    /** 16 Jan - 15 Feb */
    fun isSettingsEntryVisible(date: LocalDate = today()): Boolean =
        (date.month == Month.JANUARY && date.day > SEASON_SPLIT_DAY) ||
                (date.month == Month.FEBRUARY && date.day <= SEASON_SPLIT_DAY)

    /**
     * The year the recap covers. In January and February that is still the past year - the server
     * would otherwise default to the new, almost empty year.
     */
    fun recapYear(date: LocalDate = today()): Int =
        if (date.month == Month.JANUARY || date.month == Month.FEBRUARY) date.year - 1 else date.year
}

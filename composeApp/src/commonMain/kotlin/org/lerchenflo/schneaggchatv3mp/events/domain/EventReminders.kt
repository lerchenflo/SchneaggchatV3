package org.lerchenflo.schneaggchatv3mp.events.domain

/** Rules for event reminders, the same limits the server checks. */
object EventReminders {
    const val MAX_COUNT = 5
    const val STEP_MINUTES = 5
    const val MIN_MINUTES = 5
    const val MAX_MINUTES = 14 * 24 * 60

    /** Preset lead times offered in the picker: 1 h, 6 h, 1 day, 2 days. */
    val PRESETS = listOf(60, 6 * 60, 24 * 60, 2 * 24 * 60)

    /** Preselected on a new event: 1 day and 1 hour before. */
    val DEFAULT = listOf(24 * 60, 60)

    fun isValid(minutes: Int): Boolean =
        minutes in MIN_MINUTES..MAX_MINUTES && minutes % STEP_MINUTES == 0

    /** Adds [minutes] if valid and not there yet; keeps the list longest first and at most [MAX_COUNT]. */
    fun add(current: List<Int>, minutes: Int): List<Int> =
        if (!isValid(minutes) || minutes in current || current.size >= MAX_COUNT) current
        else (current + minutes).sortedDescending()

    /** Drops the reminders whose time is already past at [nowMillis] - the server would never send them. */
    fun upcoming(reminderMinutes: List<Int>, startDate: Long, nowMillis: Long): List<Int> =
        reminderMinutes.filter { startDate - it * 60_000L > nowMillis }
}

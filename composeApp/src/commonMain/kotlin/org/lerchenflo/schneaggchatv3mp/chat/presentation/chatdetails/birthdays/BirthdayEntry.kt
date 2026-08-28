package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.birthdays

import org.lerchenflo.schneaggchatv3mp.chat.domain.User

/**
 * A single row on the birthdays screen - a friend (or the own user) whose next birthday is
 * [daysUntil] days away.
 */
data class BirthdayEntry(
    val user: User,
    val daysUntil: Int,
    val nextBirthdayMonth: Int, // 1-12, calendar month of the next occurrence
    val turningAge: Int?, // null when the birth year is missing/implausible
    val isToday: Boolean,
    val isOwn: Boolean,
)

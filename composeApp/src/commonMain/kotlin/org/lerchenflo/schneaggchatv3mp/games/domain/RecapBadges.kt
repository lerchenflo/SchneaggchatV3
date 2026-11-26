package org.lerchenflo.schneaggchatv3mp.games.domain

/**
 * Personality badges on the last recap page. The server sends none: they are earned here from the
 * recap numbers (thresholds below). Declared in show order, rarest and most telling first.
 */
enum class RecapBadge {
    BUG_HUNTER,
    IDEA_MACHINE,
    NIGHT_OWL,
    EARLY_BIRD,
    SPEED_READER,
    QUICK_REPLIER,
    GHOST,
    DOUBLE_TEXTER,
    ICEBREAKER,
    CHATTERBOX,
    STREAK_MASTER,
    ALARM_CLOCK,
    FORGETFUL,
}

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS

private const val BUG_HUNTER_MIN_BUGS = 3L
private const val IDEA_MACHINE_MIN_IDEAS = 3L
private val NIGHT_OWL_HOURS = setOf(22, 23, 0, 1, 2, 3, 4)
private val EARLY_BIRD_HOURS = (5..8).toSet()
private const val SPEED_READER_MAX_READ_MILLIS = 2 * MINUTE_MILLIS
private const val SPEED_READER_MIN_SAMPLES = 20L
private const val QUICK_REPLIER_MAX_REPLY_MILLIS = 5 * MINUTE_MILLIS
private const val GHOST_MIN_REPLY_MILLIS = 3 * HOUR_MILLIS
private const val REPLY_MIN_SAMPLES = 20L
private const val DOUBLE_TEXTER_MIN_MESSAGES = 15
private const val ICEBREAKER_MIN_CONVERSATIONS = 20L
private const val ICEBREAKER_MIN_SHARE_PERCENT = 60
private const val CHATTERBOX_MIN_MESSAGES = 5_000L
private const val STREAK_MASTER_MIN_DAYS = 100
private const val ALARM_CLOCK_MIN_WAKES = 25L
private const val FORGETFUL_MIN_RESETS = 2L

/**
 * The badges the requester earned this year. Failed logins never earn [RecapBadge.FORGETFUL]:
 * anyone who knows the username can cause one.
 */
fun RecapResponse.earnedBadges(): List<RecapBadge> = RecapBadge.entries.filter { badge ->
    when (badge) {
        RecapBadge.BUG_HUNTER -> feedback.bugsReportedThisYear >= BUG_HUNTER_MIN_BUGS
        RecapBadge.IDEA_MACHINE -> feedback.featuresRequestedThisYear >= IDEA_MACHINE_MIN_IDEAS
        RecapBadge.NIGHT_OWL -> messaging.peakHourIn(NIGHT_OWL_HOURS)
        RecapBadge.EARLY_BIRD -> messaging.peakHourIn(EARLY_BIRD_HOURS)
        RecapBadge.SPEED_READER -> chatHabits.readSampleCount >= SPEED_READER_MIN_SAMPLES &&
            (chatHabits.medianReadMillis ?: Long.MAX_VALUE) <= SPEED_READER_MAX_READ_MILLIS
        RecapBadge.QUICK_REPLIER -> chatHabits.replySampleCount >= REPLY_MIN_SAMPLES &&
            (chatHabits.medianReplyMillis ?: Long.MAX_VALUE) <= QUICK_REPLIER_MAX_REPLY_MILLIS
        RecapBadge.GHOST -> chatHabits.replySampleCount >= REPLY_MIN_SAMPLES &&
            (chatHabits.medianReplyMillis ?: 0L) >= GHOST_MIN_REPLY_MILLIS
        RecapBadge.DOUBLE_TEXTER -> (chatHabits.longestMonologue?.messageCount ?: 0) >= DOUBLE_TEXTER_MIN_MESSAGES
        RecapBadge.ICEBREAKER -> chatHabits.conversations >= ICEBREAKER_MIN_CONVERSATIONS &&
            chatHabits.conversationsStartedByMe * 100 >= chatHabits.conversations * ICEBREAKER_MIN_SHARE_PERCENT
        RecapBadge.CHATTERBOX -> messaging.messagesSentThisYear >= CHATTERBOX_MIN_MESSAGES
        RecapBadge.STREAK_MASTER -> messaging.longestStreakDays >= STREAK_MASTER_MIN_DAYS
        RecapBadge.ALARM_CLOCK -> wakes.wakesSentThisYear >= ALARM_CLOCK_MIN_WAKES
        RecapBadge.FORGETFUL -> passwordResets.passwordResetEmailsSentThisYear >= FORGETFUL_MIN_RESETS
    }
}

private fun MessagingRecapDto.peakHourIn(hours: Set<Int>): Boolean =
    messagesSentThisYear > 0 && busiestHourOfDay?.let { it in hours } == true

package org.lerchenflo.schneaggchatv3mp.games.presentation.cchallenge

import org.lerchenflo.schneaggchatv3mp.games.data.CChallengeStreak

const val C_CHALLENGE_MAX_TRIES = 3

/** Points for solving on the first, second and third try. */
val C_CHALLENGE_POINTS = listOf(100, 60, 30)

sealed interface CChallengeAction {
    data object Start : CChallengeAction
    /** Screen is leaving (back, rotation, tab switch): keep the progress for the next visit. */
    data object Leave : CChallengeAction
    /** The screen noticed a possible day change (midnight passed or app resumed). */
    data object CheckDayChanged : CChallengeAction
    /** FILL_BLANK / PREDICT_OUTPUT: an answer option was picked. */
    data class SelectOption(val index: Int) : CChallengeAction
    /** FIX_SYNTAX: a code line was tapped. */
    data class SelectLine(val index: Int) : CChallengeAction
    /** ORDER_LINES: a pool line was appended to the answer. */
    data class PickOrderLine(val poolIndex: Int) : CChallengeAction
    /** ORDER_LINES: an answer line was put back into the pool. */
    data class RemoveOrderLine(val position: Int) : CChallengeAction
    data object SubmitOrder : CChallengeAction
}

data class CChallengeState(
    val challenge: CChallenge,
    val isStarted: Boolean = false,
    val isFinished: Boolean = false,
    val solved: Boolean = false,
    val triesUsed: Int = 0,
    /** Wrong answers so far: option indices, or code line indices for FIX_SYNTAX. */
    val wrongPicks: Set<Int> = emptySet(),
    /** ORDER_LINES: pool indices in the order the player placed them. */
    val orderPicked: List<Int> = emptyList(),
    /** ORDER_LINES: how many lines of the last wrong submission were in the right place. */
    val orderCorrectCount: Int? = null,
    val elapsedMillis: Long = 0L,
    val score: Int = 0,
    val streak: CChallengeStreak = CChallengeStreak(),
) {
    val triesLeft: Int get() = C_CHALLENGE_MAX_TRIES - triesUsed
}

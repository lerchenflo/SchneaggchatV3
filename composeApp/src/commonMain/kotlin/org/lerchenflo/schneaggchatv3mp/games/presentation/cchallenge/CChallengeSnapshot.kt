package org.lerchenflo.schneaggchatv3mp.games.presentation.cchallenge

import kotlinx.serialization.Serializable

const val C_CHALLENGE_SNAPSHOT_VERSION = 1

/**
 * Persisted progress on today's challenge. The challenge itself is regenerated from the day,
 * so only the player's answers are stored. Finished runs are kept too, so coming back the same
 * day shows the result instead of handing out new tries; the envelope's day check drops it at midnight.
 */
@Serializable
data class CChallengeSnapshot(
    val isFinished: Boolean,
    val solved: Boolean,
    val triesUsed: Int,
    val wrongPicks: Set<Int>,
    val orderPicked: List<Int>,
    val orderCorrectCount: Int?,
    val elapsedMillis: Long,
    val score: Int,
)

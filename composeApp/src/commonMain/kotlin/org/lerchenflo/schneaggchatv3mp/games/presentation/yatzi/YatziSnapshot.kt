package org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi

import kotlinx.serialization.Serializable

const val YATZI_SNAPSHOT_VERSION = 1

@Serializable
data class YatziPlayerSnapshot(
    val name: String,
    val scores: Map<YatziCategory, Int>,
    val userId: String? = null,
)

@Serializable
data class YatziDieSnapshot(
    val value: Int,
    val isKept: Boolean,
)

/** Persisted mid-game Yatzi state; potential scores are recalculated from the dice on restore. */
@Serializable
data class YatziSnapshot(
    val players: List<YatziPlayerSnapshot>,
    val currentPlayerIndex: Int,
    val currentRollCount: Int,
    val dice: List<YatziDieSnapshot>,
)

package org.lerchenflo.schneaggchatv3mp.games.presentation.game2048

import kotlinx.serialization.Serializable

const val GAME_2048_SNAPSHOT_VERSION = 1

/** Persisted mid-run 2048 state; the timer is re-anchored from [elapsedMillis] on restore. */
@Serializable
data class Game2048Snapshot(
    val grid: List<Int>,
    val gridSize: Int,
    val score: Int,
    val bestTile: Int,
    val hasReached2048: Boolean,
    val elapsedMillis: Long,
)

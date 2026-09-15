package org.lerchenflo.schneaggchatv3mp.games.presentation.gridrush

import kotlinx.serialization.Serializable

const val GRIDRUSH_SNAPSHOT_VERSION = 1

/**
 * Persisted daily run. Finished runs are kept too, so coming back the same day
 * shows the result; the envelope's day check drops everything at midnight.
 */
@Serializable
data class GridRushSnapshot(
    val board: List<List<GridTileColor?>>,
    val rows: Int,
    val cols: Int,
    val parMoves: Int,
    val score: Int,
    val movesUsed: Int,
    val elapsedMillis: Long,
    /** False while still in the free planning phase before the first tile press. */
    val timerStarted: Boolean,
    val isGameOver: Boolean,
    val won: Boolean,
)

package org.lerchenflo.schneaggchatv3mp.games.presentation.oddoneout

import kotlinx.serialization.Serializable

const val ODDONEOUT_SNAPSHOT_VERSION = 1

/** Persisted mid-run state; the run and round clocks are re-anchored from the stored millis on restore. */
@Serializable
data class OddOneOutSnapshot(
    val score: Int,
    val lives: Int,
    val round: Int,
    val gridSize: Int,
    val tiles: List<OddOneOutTile>,
    val oddIndex: Int,
    val variant: OddTileVariant,
    val oddLighten: Boolean,
    val oddDelta: Float,
    val roundTimeMillis: Long,
    val roundTimeRemainingMillis: Long,
    val elapsedMillis: Long,
    /** Saved while a lost life was being shown — the next round starts fresh on restore. */
    val awaitingNextRound: Boolean,
)

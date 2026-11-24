package org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow

import kotlinx.serialization.Serializable

/** Bump whenever the layout of [FourInARowSnapshot] changes; older saves are then discarded. */
const val FOUR_IN_A_ROW_SNAPSHOT_VERSION = 1

/** A running match: the players, every side's wins so far, the series length and the round on the board. */
@Serializable
data class FourInARowSnapshot(
    val players: List<FourInARowSnapshotPlayer>,
    val wins: List<Int>,
    val roundNumber: Int,
    val bestOf: Int? = null,
    val round: FourInARowRound,
)

@Serializable
data class FourInARowSnapshotPlayer(
    val name: String,
    val userId: String? = null,
)

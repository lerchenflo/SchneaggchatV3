package org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe

import kotlinx.serialization.Serializable

/** Bump whenever the layout of [TicTacToeSnapshot] changes; older saves are then discarded. */
const val TIC_TAC_TOE_SNAPSHOT_VERSION = 1

/** A running match: the players, their wins so far and the round on the board. */
@Serializable
data class TicTacToeSnapshot(
    val players: List<TicTacToeSnapshotPlayer>,
    val wins: List<Int>,
    val roundNumber: Int,
    val round: TicTacToeRound,
)

@Serializable
data class TicTacToeSnapshotPlayer(
    val name: String,
    val userId: String? = null,
)

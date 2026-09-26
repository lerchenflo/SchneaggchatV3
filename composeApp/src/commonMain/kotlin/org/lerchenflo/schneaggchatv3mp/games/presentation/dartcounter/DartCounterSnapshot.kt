package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartCounterSnapshotData
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartPlayerSnapshotData
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartThrow
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartTurn

// 2: the turn counters were dropped, they are derived from the stored darts
const val DART_COUNTER_SNAPSHOT_VERSION = 2

@Serializable
data class DartPlayerSnapshot(
    val name: String,
    val score: Int,
    val totalDartsThrown: Int,
    val isFinished: Boolean,
    val userId: String? = null,
)

@Serializable
data class DartThrowSnapshot(
    val score: Int,
    val isDouble: Boolean,
    val isTriple: Boolean,
    val actualScore: Int,
)

@Serializable
data class DartTurnSnapshot(
    val playerIndex: Int,
    val playerName: String,
    val scoreAtStart: Int,
    val dartsThrown: List<DartThrowSnapshot>,
)

/**
 * Persisted mid-game Dart Counter state. Stored field by field instead of replaying
 * [allThrows], because busted darts are not part of the throw history.
 */
@Serializable
data class DartCounterSnapshot(
    val doubleOut: Boolean,
    val countdown: Int,
    val players: List<DartPlayerSnapshot>,
    val currentPlayerIndex: Int,
    val turnStartScore: Int,
    val turnHistory: List<DartTurnSnapshot>,
    val currentTurnDarts: List<DartThrowSnapshot>,
    val allThrows: List<DartThrowSnapshot>,
)

private fun DartThrow.toSnapshot() = DartThrowSnapshot(
    score = score,
    isDouble = isDouble,
    isTriple = isTriple,
    actualScore = actualScore,
)

private fun DartThrowSnapshot.toDartThrow() = DartThrow(
    score = score,
    isDouble = isDouble,
    isTriple = isTriple,
    actualScore = actualScore,
)

fun DartCounterSnapshotData.toSnapshot() = DartCounterSnapshot(
    doubleOut = doubleOut,
    countdown = countdown,
    players = players.map {
        DartPlayerSnapshot(
            name = it.name,
            score = it.score,
            totalDartsThrown = it.totalDartsThrown,
            isFinished = it.isFinished,
            userId = it.userId,
        )
    },
    currentPlayerIndex = currentPlayerIndex,
    turnStartScore = turnStartScore,
    turnHistory = turnHistory.map { turn ->
        DartTurnSnapshot(
            playerIndex = turn.playerIndex,
            playerName = turn.playerName,
            scoreAtStart = turn.scoreAtStart,
            dartsThrown = turn.dartsThrown.map { it.toSnapshot() },
        )
    },
    currentTurnDarts = currentTurnDarts.map { it.toSnapshot() },
    allThrows = allThrows.map { it.toSnapshot() },
)

fun DartCounterSnapshot.toSnapshotData() = DartCounterSnapshotData(
    doubleOut = doubleOut,
    countdown = countdown,
    players = players.map {
        DartPlayerSnapshotData(
            name = it.name,
            score = it.score,
            totalDartsThrown = it.totalDartsThrown,
            isFinished = it.isFinished,
            userId = it.userId,
        )
    },
    currentPlayerIndex = currentPlayerIndex,
    turnStartScore = turnStartScore,
    turnHistory = turnHistory.map { turn ->
        DartTurn(
            playerIndex = turn.playerIndex,
            playerName = turn.playerName,
            scoreAtStart = turn.scoreAtStart,
            dartsThrown = turn.dartsThrown.map { it.toDartThrow() },
        )
    },
    currentTurnDarts = currentTurnDarts.map { it.toDartThrow() },
    allThrows = allThrows.map { it.toDartThrow() },
)

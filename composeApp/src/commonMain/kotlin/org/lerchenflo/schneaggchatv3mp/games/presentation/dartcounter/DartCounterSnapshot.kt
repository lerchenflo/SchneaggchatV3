package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import kotlinx.serialization.Serializable

const val DART_COUNTER_SNAPSHOT_VERSION = 1

@Serializable
data class DartPlayerSnapshot(
    val name: String,
    val score: Int,
    val totalDartsThrown: Int,
    val isFinished: Boolean,
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
    val currentThrow: Int,
    val throwCount: Int,
    val totalThrowsCount: Int,
)

fun DartCounterViewModel.DartThrow.toSnapshot() = DartThrowSnapshot(
    score = score,
    isDouble = isDouble,
    isTriple = isTriple,
    actualScore = actualScore,
)

fun DartThrowSnapshot.toDartThrow() = DartCounterViewModel.DartThrow(
    score = score,
    isDouble = isDouble,
    isTriple = isTriple,
    actualScore = actualScore,
)

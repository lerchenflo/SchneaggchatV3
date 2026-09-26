package org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter

/** Whether the last dart of a leg has to be a double. */
enum class DartOutMode {
    SINGLE_OUT,
    DOUBLE_OUT
}

/** One dart as it was scored: [score] is the segment base, [actualScore] the value after the multiplier. */
data class DartThrow(
    val score: Int,
    val isDouble: Boolean,
    val isTriple: Boolean,
    val actualScore: Int,
)

/** The board segment this dart hit; the multiplier follows from the double/triple flags. */
fun DartThrow.toSegment(): DartSegment {
    val multiplier = if (isTriple) 3 else if (isDouble) 2 else 1
    return DartSegment(base = score, multiplier = multiplier, isDouble = isDouble, isTriple = isTriple)
}

/** One completed turn, kept so a leg can be reconstructed. */
data class DartTurn(
    val playerIndex: Int,
    val playerName: String,
    val scoreAtStart: Int,
    val dartsThrown: List<DartThrow>,
)

/** Immutable view of one player, handed to the presentation layer. */
data class DartPlayer(
    val name: String,
    val score: Int,
    val totalDartsThrown: Int,
    val isFinished: Boolean,
) {
    /** Three-dart average over the whole leg, 0.0 before the first dart. */
    fun threeDartAverage(countdown: Int): Double =
        if (totalDartsThrown > 0) (countdown - score).toDouble() / totalDartsThrown * 3 else 0.0
}

/**
 * A running 301/501 leg: scores, turn history and whose turn it is. Deliberately free of any
 * Compose or lifecycle dependency - the ViewModel mutates it and projects the result into its
 * immutable state, so the UI never touches the game internals.
 */
class DartGame(
    val outMode: DartOutMode,
    val countdown: Int,
    playerNames: List<String>,
) {
    private class MutablePlayer(
        val name: String,
        var score: Int,
        var totalDartsThrown: Int = 0,
        var isFinished: Boolean = false,
    )

    val doubleOut: Boolean get() = outMode == DartOutMode.DOUBLE_OUT

    private val playerList: MutableList<MutablePlayer> =
        playerNames.map { MutablePlayer(name = it, score = countdown) }.toMutableList()

    var currentPlayerIndex: Int = 0
        private set

    var gameOver: Boolean = false
        private set

    private var turnStartScore: Int = playerList.firstOrNull()?.score ?: countdown
    private val turnHistory: MutableList<DartTurn> = mutableListOf()
    private var currentTurnDarts: MutableList<DartThrow> = mutableListOf()

    /** Every dart that was thrown, in order and including busts, so [undoLastThrow] can replay it. */
    private val allThrows: MutableList<DartThrow> = mutableListOf()

    // ─── Read-only projections ────────────────────────────────────────────────

    val players: List<DartPlayer>
        get() = playerList.map {
            DartPlayer(
                name = it.name,
                score = it.score,
                totalDartsThrown = it.totalDartsThrown,
                isFinished = it.isFinished,
            )
        }

    val currentPlayer: DartPlayer get() = players[currentPlayerIndex]

    val winners: List<DartPlayer> get() = players.filter { it.isFinished }

    val dartsThisTurn: List<DartThrow> get() = currentTurnDarts.toList()

    val turnTotal: Int get() = currentTurnDarts.sumOf { it.actualScore }

    val dartsLeft: Int get() = 3 - currentTurnDarts.size

    val totalThrows: Int get() = allThrows.size

    val canUndo: Boolean get() = allThrows.isNotEmpty()

    // ─── Mutations ────────────────────────────────────────────────────────────

    /**
     * Scores one dart for the active player and advances the leg: a third dart or a bust completes
     * the turn and hands over to the next player. Darts are counted even when they bust.
     */
    fun throwDart(segment: DartSegment) {
        if (gameOver) return
        val player = playerList[currentPlayerIndex]
        if (player.isFinished) return

        val dart = DartThrow(
            score = segment.base,
            isDouble = segment.isDouble,
            isTriple = segment.isTriple,
            actualScore = segment.points,
        )
        // Recorded before it is scored: a bust is still a dart that was thrown, and leaving it out
        // of the history would make an undo replay a different leg (see rebuildFromHistory)
        allThrows.add(dart)
        player.totalDartsThrown++

        if (applyScore(segment)) {
            currentTurnDarts.add(dart)

            // A checkout ends the turn as surely as a third dart does: without this the leg stays
            // on a player who can no longer throw, and throwDart rejects every further dart
            if ((currentTurnDarts.size >= 3 || player.isFinished) && !gameOver) {
                completeTurn()
                nextPlayer()
            }
        } else {
            // Bust: the whole turn is worthless, the score goes back to what it was before it
            player.score = turnStartScore
            completeTurn()
            nextPlayer()
        }
    }

    /**
     * Takes back the last dart by replaying the remaining history from the start, because a turn's
     * result cannot be reversed field by field. The replay is exact: [allThrows] holds every dart
     * that was thrown, busts included, and the engine re-derives each bust from the same state.
     */
    fun undoLastThrow(): Boolean {
        if (allThrows.isEmpty()) return false
        allThrows.removeAt(allThrows.lastIndex)
        rebuildFromHistory()
        return true
    }

    /** Applies [segment] to the active player, returning false when it busts. */
    private fun applyScore(segment: DartSegment): Boolean {
        val player = playerList[currentPlayerIndex]
        val newScore = player.score - segment.points

        if (doubleOut) {
            return when {
                // A leg can only be closed on a double, and 1 cannot be finished from
                newScore == 0 && segment.isDouble -> {
                    player.score = 0
                    player.isFinished = true
                    updateGameOver()
                    true
                }
                newScore > 1 -> {
                    player.score = newScore
                    true
                }
                else -> false
            }
        }

        if (newScore < 0) return false
        player.score = newScore
        if (newScore == 0) {
            player.isFinished = true
            updateGameOver()
        }
        return true
    }

    private fun completeTurn() {
        if (currentTurnDarts.isEmpty()) return
        turnHistory.add(
            DartTurn(
                playerIndex = currentPlayerIndex,
                playerName = playerList[currentPlayerIndex].name,
                scoreAtStart = turnStartScore,
                dartsThrown = currentTurnDarts.toList(),
            )
        )
        currentTurnDarts.clear()
    }

    private fun nextPlayer() {
        if (gameOver) return
        // Skip players who already closed their leg
        var attempts = 0
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size
            attempts++
        } while (playerList[currentPlayerIndex].isFinished && attempts < playerList.size)
        turnStartScore = playerList[currentPlayerIndex].score
    }

    private fun updateGameOver() {
        gameOver = playerList.all { it.isFinished }
    }

    private fun rebuildFromHistory() {
        for (player in playerList) {
            player.score = countdown
            player.isFinished = false
            player.totalDartsThrown = 0
        }
        turnHistory.clear()
        currentTurnDarts.clear()
        currentPlayerIndex = 0
        turnStartScore = countdown
        gameOver = false

        val replay = allThrows.toList()
        allThrows.clear()
        for (dart in replay) {
            throwDart(dart.toSegment())
        }
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    fun toSnapshot(userIds: List<String?>) = DartCounterSnapshotData(
        doubleOut = doubleOut,
        countdown = countdown,
        players = playerList.mapIndexed { index, player ->
            DartPlayerSnapshotData(
                name = player.name,
                score = player.score,
                totalDartsThrown = player.totalDartsThrown,
                isFinished = player.isFinished,
                userId = userIds.getOrNull(index),
            )
        },
        currentPlayerIndex = currentPlayerIndex,
        turnStartScore = turnStartScore,
        turnHistory = turnHistory.toList(),
        currentTurnDarts = currentTurnDarts.toList(),
        allThrows = allThrows.toList(),
    )

    /** Overwrites this freshly created game (same players and settings) with a stored one. */
    fun restoreFrom(snapshot: DartCounterSnapshotData) {
        snapshot.players.forEachIndexed { index, saved ->
            playerList.getOrNull(index)?.let { player ->
                player.score = saved.score
                player.totalDartsThrown = saved.totalDartsThrown
                player.isFinished = saved.isFinished
            }
        }
        currentPlayerIndex = snapshot.currentPlayerIndex.coerceIn(0, playerList.lastIndex)
        turnStartScore = snapshot.turnStartScore
        turnHistory.clear()
        turnHistory.addAll(snapshot.turnHistory)
        currentTurnDarts = snapshot.currentTurnDarts.toMutableList()
        allThrows.clear()
        allThrows.addAll(snapshot.allThrows)
        updateGameOver()
        // Saves written before a checkout handed over can sit on a player who already finished,
        // which used to freeze the leg for good. Hand over on restore rather than discard the save.
        if (!gameOver && playerList[currentPlayerIndex].isFinished) {
            completeTurn()
            nextPlayer()
        }
    }
}

/** Plain carrier between [DartGame] and the serialized snapshot, so the engine stays serializer-free. */
data class DartCounterSnapshotData(
    val doubleOut: Boolean,
    val countdown: Int,
    val players: List<DartPlayerSnapshotData>,
    val currentPlayerIndex: Int,
    val turnStartScore: Int,
    val turnHistory: List<DartTurn>,
    val currentTurnDarts: List<DartThrow>,
    val allThrows: List<DartThrow>,
)

data class DartPlayerSnapshotData(
    val name: String,
    val score: Int,
    val totalDartsThrown: Int,
    val isFinished: Boolean,
    val userId: String?,
)

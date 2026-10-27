package org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe

import kotlinx.serialization.Serializable
import kotlin.random.Random

const val TIC_TAC_TOE_MIN_PLAYERS = 2
const val TIC_TAC_TOE_MAX_PLAYERS = 4

/**
 * Rules of one match. Two players play the classic 3x3 board; more players get a bigger board and
 * a longer line so there is room to block. The twists can be combined freely.
 */
@Serializable
data class TicTacToeConfig(
    val playerCount: Int,
    val vanishing: Boolean = false,
    val gravity: Boolean = false,
    val bombs: Boolean = false,
    val chaos: Boolean = false,
) {
    val boardSize: Int
        get() = when {
            playerCount <= 2 -> 3
            playerCount == 3 -> 5
            else -> 6
        }

    val winLength: Int get() = if (playerCount <= 2) 3 else 4

    /** Vanishing marks: how many marks a player keeps; placing one more fades out their oldest. */
    val marksKept: Int get() = if (playerCount <= 2) 3 else winLength + 1

    /** Chaos shuffle: a row or column shifts after every this many turns (three full rounds). */
    val turnsPerShift: Int get() = playerCount * 3
}

/** Row 0 is the top row, column 0 the left column. */
@Serializable
data class Cell(val row: Int, val column: Int)

/** [id] grows with every placed mark, so the smallest id of a player is their oldest mark. */
@Serializable
data class TicTacToeMark(
    val id: Long,
    val owner: Int,
    val cell: Cell,
)

/** Chaos shuffle: row or column [index] rotated by one cell, towards the end when [forward]. */
@Serializable
data class BoardShift(
    val isRow: Boolean,
    val index: Int,
    val forward: Boolean,
)

/** One game on the board, from the first mark to a win or a draw. Players are referenced by index. */
@Serializable
data class TicTacToeRound(
    val config: TicTacToeConfig,
    val startingPlayer: Int,
    val currentPlayer: Int = startingPlayer,
    val marks: List<TicTacToeMark> = emptyList(),
    val turnCount: Int = 0,
    val nextMarkId: Long = 0,
    /** Players who already used their bomb this round. */
    val bombsUsed: Set<Int> = emptySet(),
    val winner: Int? = null,
    val winLine: List<Cell> = emptyList(),
    val draw: Boolean = false,
    /** What the last turn did, so the board can animate it. */
    val lastPlacedId: Long? = null,
    val lastBombCell: Cell? = null,
    val lastShift: BoardShift? = null,
) {
    val isOver: Boolean get() = winner != null || draw

    fun markAt(cell: Cell): TicTacToeMark? = marks.firstOrNull { it.cell == cell }

    fun canBomb(player: Int): Boolean =
        config.bombs && !isOver && player !in bombsUsed && marks.any { it.owner != player }

    /** Vanishing marks: the mark of [player] that fades out with their next placement, if any. */
    fun nextToVanish(player: Int): TicTacToeMark? {
        if (!config.vanishing) return null
        val own = marks.filter { it.owner == player }
        return if (own.size >= config.marksKept) own.minBy { it.id } else null
    }

    /** Chaos shuffle: turns left until the board shifts, null without the twist. */
    val turnsUntilShift: Int?
        get() = if (config.chaos) config.turnsPerShift - turnCount % config.turnsPerShift else null
}

sealed interface TicTacToeMove {
    /** With gravity, [cell] only selects the column. */
    data class Place(val cell: Cell) : TicTacToeMove

    /** Blows up the enemy mark on [cell] instead of placing one. */
    data class Bomb(val cell: Cell) : TicTacToeMove
}

/**
 * Pure rules: no Compose, coroutines or clock. Randomness (chaos shuffle) comes in through [random].
 */
class TicTacToeEngine(private val random: Random = Random.Default) {

    fun newRound(config: TicTacToeConfig, startingPlayer: Int): TicTacToeRound =
        TicTacToeRound(config = config, startingPlayer = startingPlayer)

    /** The round after [move]; returns [round] unchanged when the move is not allowed. */
    fun apply(round: TicTacToeRound, move: TicTacToeMove): TicTacToeRound {
        if (round.isOver) return round
        val config = round.config
        val mover = round.currentPlayer

        var marks = round.marks
        var nextMarkId = round.nextMarkId
        var bombsUsed = round.bombsUsed
        var placedId: Long? = null
        var bombCell: Cell? = null

        when (move) {
            is TicTacToeMove.Place -> {
                val target = landingCell(round, move.cell) ?: return round
                // The oldest mark fades out as the new one comes in, so it can never block it
                round.nextToVanish(mover)?.let { oldest -> marks = marks - oldest }
                placedId = nextMarkId++
                marks = marks + TicTacToeMark(id = placedId, owner = mover, cell = target)
            }
            is TicTacToeMove.Bomb -> {
                if (!round.canBomb(mover)) return round
                val victim = round.markAt(move.cell)?.takeIf { it.owner != mover } ?: return round
                marks = marks - victim
                bombsUsed = bombsUsed + mover
                bombCell = victim.cell
            }
        }
        if (config.gravity) marks = settle(marks, config.boardSize)

        val turnCount = round.turnCount + 1
        var shift: BoardShift? = null
        if (config.chaos && turnCount % config.turnsPerShift == 0) {
            shift = BoardShift(
                isRow = random.nextBoolean(),
                index = random.nextInt(config.boardSize),
                forward = random.nextBoolean(),
            )
            marks = shifted(marks, shift, config.boardSize)
            if (config.gravity) marks = settle(marks, config.boardSize)
        }

        val (winner, winLine) = decideWinner(marks, config, mover)
        val draw = winner == null && marks.size == config.boardSize * config.boardSize

        return round.copy(
            currentPlayer = if (winner != null || draw) mover else (mover + 1) % config.playerCount,
            marks = marks,
            turnCount = turnCount,
            nextMarkId = nextMarkId,
            bombsUsed = bombsUsed,
            winner = winner,
            winLine = winLine,
            draw = draw,
            lastPlacedId = placedId,
            lastBombCell = bombCell,
            lastShift = shift,
        )
    }

    /** Where a mark tapped on [cell] ends up: the cell itself, or with gravity the lowest free cell of its column. */
    fun landingCell(round: TicTacToeRound, cell: Cell): Cell? {
        val size = round.config.boardSize
        if (cell.row !in 0 until size || cell.column !in 0 until size) return null
        if (!round.config.gravity) return cell.takeIf { round.markAt(it) == null }
        return (size - 1 downTo 0)
            .map { row -> Cell(row, cell.column) }
            .firstOrNull { round.markAt(it) == null }
    }

    /**
     * The mover wins with a line of their own. A line that only appeared through a fall or a shuffle
     * still counts: then the first player after the mover (in turn order) who has one wins.
     */
    private fun decideWinner(marks: List<TicTacToeMark>, config: TicTacToeConfig, mover: Int): Pair<Int?, List<Cell>> {
        val turnOrder = (0 until config.playerCount).map { (mover + it) % config.playerCount }
        for (player in turnOrder) {
            val line = findLine(marks, player, config)
            if (line != null) return player to line
        }
        return null to emptyList()
    }

    private fun findLine(marks: List<TicTacToeMark>, player: Int, config: TicTacToeConfig): List<Cell>? {
        val owned = marks.filter { it.owner == player }.map { it.cell }.toSet()
        if (owned.size < config.winLength) return null
        val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (start in owned) {
            for ((rowStep, columnStep) in directions) {
                val line = (0 until config.winLength).map { step ->
                    Cell(start.row + rowStep * step, start.column + columnStep * step)
                }
                if (line.all { it in owned }) return line
            }
        }
        return null
    }

    /** Gravity: every mark falls to the bottom of its column, keeping the order within the column. */
    private fun settle(marks: List<TicTacToeMark>, size: Int): List<TicTacToeMark> =
        marks.groupBy { it.cell.column }.flatMap { (column, columnMarks) ->
            columnMarks
                .sortedByDescending { it.cell.row }
                .mapIndexed { index, mark -> mark.copy(cell = Cell(size - 1 - index, column)) }
        }

    private fun shifted(marks: List<TicTacToeMark>, shift: BoardShift, size: Int): List<TicTacToeMark> {
        val step = if (shift.forward) 1 else size - 1
        return marks.map { mark ->
            val cell = mark.cell
            when {
                shift.isRow && cell.row == shift.index ->
                    mark.copy(cell = cell.copy(column = (cell.column + step) % size))
                !shift.isRow && cell.column == shift.index ->
                    mark.copy(cell = cell.copy(row = (cell.row + step) % size))
                else -> mark
            }
        }
    }
}

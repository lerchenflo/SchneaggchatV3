package org.lerchenflo.schneaggchatv3mp.games.presentation.gridrush

import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import kotlin.math.abs

const val FULL_CLEAR_BONUS = 500
const val EFFICIENCY_BONUS_PER_MOVE = 100

/** Points for clearing a chain of [n] tiles. */
fun chainScore(n: Int): Int = n * (n - 1) * 10

data class GeneratedGridRushBoard(
    val board: List<List<GridTileColor?>>,
    val parMoves: Int,
)

/**
 * splitmix64 — tiny explicit PRNG so the daily board is identical on every
 * platform and Kotlin version (kotlin.random.Random does not guarantee that).
 */
private class SplitMix64(private var state: Long) {
    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    fun nextInt(bound: Int): Int = ((nextLong() ushr 1) % bound).toInt()

    /** Random value in [from, until) */
    fun nextInt(from: Int, until: Int): Int = from + nextInt(until - from)
}

/**
 * Generates the daily board for [epochDay] (UTC) and [difficulty] — everyone
 * gets the same board on the same day, and restarting reproduces it exactly.
 *
 * Reverse construction: the grid is filled bottom-up by stacking chains onto
 * the current column surface. Clearing those chains in reverse placement order
 * is a valid full-clear solution (every chain lies on top of everything placed
 * before it, so gravity never disturbs it), which makes each board provably
 * solvable with [GeneratedGridRushBoard.parMoves] = number of placed chains.
 */
fun generateDailyBoard(epochDay: Long, difficulty: GameDifficulty): GeneratedGridRushBoard {
    val (rows, cols, colorCount) = when (difficulty) {
        GameDifficulty.LOW -> Triple(6, 6, 4)
        GameDifficulty.MEDIUM -> Triple(8, 8, 5)
        GameDifficulty.HIGH -> Triple(10, 10, 5)
    }

    // A construction walk can strand a single cell; retry deterministically
    // with the next sub-seed (simulation: ~2-3 attempts on average)
    repeat(500) { attempt ->
        val random = SplitMix64(epochDay * 1_000_003L + difficulty.ordinal * 101L + attempt)
        val chains = constructChains(rows, cols, random)
        if (chains != null) {
            return GeneratedGridRushBoard(
                board = chainsToBoard(chains, rows, cols, colorCount, random),
                parMoves = chains.size,
            )
        }
    }
    return stripeFallback(rows, cols, colorCount)
}

/**
 * Builds the board as a list of chains in "surface space": levels count from
 * the bottom and nextFree[col] is each column's next free level. A chain walks
 * either up within its column or sideways to an adjacent column whose next
 * free level equals the current level, so it always sits on the surface.
 * Returns null when a chain gets stuck at length 1 (attempt is retried).
 */
private fun constructChains(rows: Int, cols: Int, random: SplitMix64): List<List<Cell>>? {
    val nextFree = IntArray(cols)
    var remaining = rows * cols
    val chains = mutableListOf<List<Cell>>()

    while (remaining > 0) {
        // Start on one of the lowest columns; keeping the surface flat makes
        // sideways moves available and stuck walks rare
        val minLevel = nextFree.filter { it < rows }.min()
        val startColumns = (0 until cols).filter { nextFree[it] == minLevel }
        var col = startColumns[random.nextInt(startColumns.size)]
        var level = nextFree[col]
        nextFree[col]++

        val path = mutableListOf(Cell(row = rows - 1 - level, col = col))
        val targetLength = random.nextInt(2, 6)
        while (path.size < targetLength) {
            val options = mutableListOf<Pair<Int, Int>>() // col to level
            if (level + 1 < rows) options += col to level + 1
            for (nextCol in intArrayOf(col - 1, col + 1)) {
                if (nextCol in 0 until cols && nextFree[nextCol] == level) options += nextCol to level
            }
            if (options.isEmpty()) break
            val (chosenCol, chosenLevel) = options[random.nextInt(options.size)]
            col = chosenCol
            level = chosenLevel
            nextFree[col]++
            path += Cell(row = rows - 1 - level, col = col)
        }

        if (path.size < 2) return null
        chains += path
        remaining -= path.size
    }
    return chains
}

private fun chainsToBoard(
    chains: List<List<Cell>>,
    rows: Int,
    cols: Int,
    colorCount: Int,
    random: SplitMix64,
): List<List<GridTileColor?>> {
    val board = Array(rows) { arrayOfNulls<GridTileColor>(cols) }
    chains.forEach { chain ->
        val color = GridTileColor.entries[random.nextInt(colorCount)]
        chain.forEach { cell -> board[cell.row][cell.col] = color }
    }
    return board.map { it.toList() }
}

/** Horizontal one-color stripes — trivially clearable row by row from the top. */
private fun stripeFallback(rows: Int, cols: Int, colorCount: Int): GeneratedGridRushBoard {
    val board = List(rows) { row -> List<GridTileColor?>(cols) { GridTileColor.entries[row % colorCount] } }
    return GeneratedGridRushBoard(board = board, parMoves = rows)
}

/** Whether [next] may be appended to [path]: adjacent, same color, unused, non-empty. */
fun isValidExtension(board: List<List<GridTileColor?>>, path: List<Cell>, next: Cell): Boolean {
    if (path.isEmpty()) return false
    if (next.row !in board.indices || next.col !in board[0].indices) return false
    val color = board[path.first().row][path.first().col] ?: return false
    val last = path.last()
    return abs(next.row - last.row) + abs(next.col - last.col) == 1 &&
            board[next.row][next.col] == color &&
            next !in path
}

/** Removes the chain cells and lets the remaining tiles fall down per column. */
fun applyChain(board: List<List<GridTileColor?>>, path: List<Cell>): List<List<GridTileColor?>> {
    val rows = board.size
    val cols = board[0].size
    val result = Array(rows) { row -> Array(cols) { col -> board[row][col] } }
    path.forEach { cell -> result[cell.row][cell.col] = null }
    for (col in 0 until cols) {
        val stack = (0 until rows).mapNotNull { row -> result[row][col] }
        for (row in 0 until rows) {
            val fromBottom = rows - 1 - row
            result[row][col] = if (fromBottom < stack.size) stack[stack.size - 1 - fromBottom] else null
        }
    }
    return result.map { it.toList() }
}

fun isBoardEmpty(board: List<List<GridTileColor?>>): Boolean =
    board.all { row -> row.all { it == null } }

/** True while any two orthogonally adjacent tiles share a color (a 2-chain is always clearable). */
fun hasAnyValidChain(board: List<List<GridTileColor?>>): Boolean {
    for (row in board.indices) {
        for (col in board[row].indices) {
            val color = board[row][col] ?: continue
            if (row + 1 < board.size && board[row + 1][col] == color) return true
            if (col + 1 < board[row].size && board[row][col + 1] == color) return true
        }
    }
    return false
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.crossword

import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordClue
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordDirection
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordPuzzle
import org.lerchenflo.schneaggchatv3mp.games.domain.SplitMix64

private const val WORKSPACE_SIZE = 15
private const val TARGET_WORDS = 14
private const val CANDIDATE_POOL_SIZE = 90
private const val MAX_ATTEMPTS = 30

private const val FLAG_ACROSS = 1
private const val FLAG_DOWN = 2

private data class Placement(
    val entry: GermanCrosswordEntry,
    val row: Int,
    val col: Int,
    val direction: CrosswordDirection,
)

/** Letters plus a per-cell record of which direction(s) already run through it. */
private class Workspace {
    val letters = Array(WORKSPACE_SIZE) { CharArray(WORKSPACE_SIZE) { ' ' } }
    val flags = Array(WORKSPACE_SIZE) { IntArray(WORKSPACE_SIZE) }

    fun isLetter(row: Int, col: Int): Boolean =
        row in 0 until WORKSPACE_SIZE && col in 0 until WORKSPACE_SIZE && letters[row][col] != ' '
}

/**
 * Generates the German daily crossword for [epochDay] — deterministic, so
 * everyone gets the same puzzle on the same day and restarting reproduces it.
 * Criss-cross style: words interlock on shared letters, remaining cells are
 * unused (rendered like blocks), built from the local word/clue list.
 */
fun generateGermanDailyPuzzle(epochDay: Long): CrosswordPuzzle {
    var best: List<Placement> = emptyList()
    repeat(MAX_ATTEMPTS) { attempt ->
        val random = SplitMix64(epochDay * 1_000_003L + attempt * 7_919L)
        val placements = buildLayout(random)
        if (placements.size >= TARGET_WORDS) return placementsToPuzzle(placements)
        if (placements.size > best.size) best = placements
    }
    // Deterministic fallback: the densest layout found (always >= a lone first word)
    return placementsToPuzzle(best)
}

private fun buildLayout(random: SplitMix64): List<Placement> {
    val pool = germanCrosswordWords.toMutableList()
    random.shuffle(pool)
    val candidates = pool.take(CANDIDATE_POOL_SIZE)

    val workspace = Workspace()
    val placements = mutableListOf<Placement>()

    // First word: horizontal, centered
    val first = candidates.first()
    val firstPlacement = Placement(first, WORKSPACE_SIZE / 2, (WORKSPACE_SIZE - first.word.length) / 2, CrosswordDirection.ACROSS)
    place(workspace, firstPlacement)
    placements.add(firstPlacement)

    // Several passes: a word unplaceable early often fits once the grid grew
    repeat(3) {
        for (entry in candidates) {
            if (placements.size >= TARGET_WORDS) return placements
            if (placements.any { it.entry.word == entry.word }) continue
            val placement = findBestPlacement(workspace, entry) ?: continue
            place(workspace, placement)
            placements.add(placement)
        }
    }
    return placements
}

private fun findBestPlacement(workspace: Workspace, entry: GermanCrosswordEntry): Placement? {
    var best: Placement? = null
    var bestCrossings = 0
    for (r in 0 until WORKSPACE_SIZE) {
        for (c in 0 until WORKSPACE_SIZE) {
            val gridChar = workspace.letters[r][c]
            if (gridChar == ' ') continue
            for (k in entry.word.indices) {
                if (entry.word[k] != gridChar) continue
                val downCrossings = canPlace(workspace, entry.word, r - k, c, CrosswordDirection.DOWN)
                if (downCrossings > bestCrossings) {
                    bestCrossings = downCrossings
                    best = Placement(entry, r - k, c, CrosswordDirection.DOWN)
                }
                val acrossCrossings = canPlace(workspace, entry.word, r, c - k, CrosswordDirection.ACROSS)
                if (acrossCrossings > bestCrossings) {
                    bestCrossings = acrossCrossings
                    best = Placement(entry, r, c - k, CrosswordDirection.ACROSS)
                }
            }
        }
    }
    return best
}

/** Returns the number of crossings if the word fits at (row, col), 0 if it does not. */
private fun canPlace(workspace: Workspace, word: String, row: Int, col: Int, direction: CrosswordDirection): Int {
    val dr = if (direction == CrosswordDirection.DOWN) 1 else 0
    val dc = if (direction == CrosswordDirection.ACROSS) 1 else 0
    val endRow = row + dr * (word.length - 1)
    val endCol = col + dc * (word.length - 1)
    if (row < 0 || col < 0 || endRow >= WORKSPACE_SIZE || endCol >= WORKSPACE_SIZE) return 0

    // The cell before the start and after the end must be free
    if (workspace.isLetter(row - dr, col - dc)) return 0
    if (workspace.isLetter(endRow + dr, endCol + dc)) return 0

    val ownFlag = if (direction == CrosswordDirection.ACROSS) FLAG_ACROSS else FLAG_DOWN
    var crossings = 0
    for (k in word.indices) {
        val r = row + dr * k
        val c = col + dc * k
        val existing = workspace.letters[r][c]
        if (existing == word[k]) {
            // A shared cell is only valid when the existing word runs
            // perpendicular — same-direction overlap would merge two words
            if (workspace.flags[r][c] and ownFlag != 0) return 0
            crossings++
        } else if (existing != ' ') {
            return 0
        } else {
            // Empty cell: its neighbors perpendicular to the word must be empty,
            // otherwise two parallel words would touch and form garbage sequences
            if (workspace.isLetter(r + dc, c + dr)) return 0
            if (workspace.isLetter(r - dc, c - dr)) return 0
        }
    }
    return crossings
}

private fun place(workspace: Workspace, placement: Placement) {
    val dr = if (placement.direction == CrosswordDirection.DOWN) 1 else 0
    val dc = if (placement.direction == CrosswordDirection.ACROSS) 1 else 0
    val flag = if (placement.direction == CrosswordDirection.ACROSS) FLAG_ACROSS else FLAG_DOWN
    for (k in placement.entry.word.indices) {
        val r = placement.row + dr * k
        val c = placement.col + dc * k
        workspace.letters[r][c] = placement.entry.word[k]
        workspace.flags[r][c] = workspace.flags[r][c] or flag
    }
}

/** Trims the layout to its bounding box, numbers the cells and builds the clues. */
private fun placementsToPuzzle(placements: List<Placement>): CrosswordPuzzle {
    val minRow = placements.minOf { it.row }
    val minCol = placements.minOf { it.col }
    val maxRow = placements.maxOf { it.row + if (it.direction == CrosswordDirection.DOWN) it.entry.word.length - 1 else 0 }
    val maxCol = placements.maxOf { it.col + if (it.direction == CrosswordDirection.ACROSS) it.entry.word.length - 1 else 0 }
    val rows = maxRow - minRow + 1
    val cols = maxCol - minCol + 1

    val solution = MutableList<Char?>(rows * cols) { null }
    for (p in placements) {
        val dr = if (p.direction == CrosswordDirection.DOWN) 1 else 0
        val dc = if (p.direction == CrosswordDirection.ACROSS) 1 else 0
        for (k in p.entry.word.indices) {
            solution[(p.row - minRow + dr * k) * cols + (p.col - minCol + dc * k)] = p.entry.word[k]
        }
    }

    // Standard crossword numbering: scan row-major, number every word start
    val startCells = placements.map { (it.row - minRow) * cols + (it.col - minCol) }.toSet()
    val cellNumbers = MutableList(rows * cols) { 0 }
    val startNumbers = mutableMapOf<Int, Int>() // cell index -> number
    var nextNumber = 1
    for (index in solution.indices) {
        if (index in startCells) {
            cellNumbers[index] = nextNumber
            startNumbers[index] = nextNumber
            nextNumber++
        }
    }

    val clues = placements.map { p ->
        val startIndex = (p.row - minRow) * cols + (p.col - minCol)
        val dr = if (p.direction == CrosswordDirection.DOWN) 1 else 0
        val dc = if (p.direction == CrosswordDirection.ACROSS) 1 else 0
        CrosswordClue(
            number = startNumbers.getValue(startIndex),
            direction = p.direction,
            text = p.entry.clue,
            cells = p.entry.word.indices.map { k ->
                (p.row - minRow + dr * k) * cols + (p.col - minCol + dc * k)
            },
        )
    }.sortedWith(compareBy({ it.direction }, { it.number }))

    return CrosswordPuzzle(
        rows = rows,
        cols = cols,
        solution = solution,
        cellNumbers = cellNumbers,
        clues = clues,
    )
}

package org.lerchenflo.schneaggchatv3mp.games.domain

import kotlinx.serialization.Serializable

enum class CrosswordLanguage {
    ENGLISH,
    GERMAN
}

enum class CrosswordDirection {
    ACROSS,
    DOWN
}

@Serializable
data class CrosswordClue(
    val number: Int,
    val direction: CrosswordDirection,
    val text: String,
    /** Row-major cell indices of the answer, in word order. */
    val cells: List<Int>,
)

/**
 * Language-independent crossword puzzle, generated locally in criss-cross style
 * from the word list of its language (unused cells stay null like blocks).
 */
@Serializable
data class CrosswordPuzzle(
    val rows: Int,
    val cols: Int,
    /** Row-major solution letters (A-Z); null = block / unused cell. */
    val solution: List<Char?>,
    /** Row-major clue numbers; 0 = unnumbered cell. */
    val cellNumbers: List<Int>,
    val clues: List<CrosswordClue>,
    /** Attribution under the grid; only puzzles saved before the own English list have one. */
    val sourceInfo: String? = null,
)

/**
 * Leaderboard score: how many letters the grid takes to fill. Puzzles are drawn
 * at random and differ in size, so a denser grid is worth more; the solve time
 * breaks ties between equally sized ones.
 */
val CrosswordPuzzle.letterCellCount: Int
    get() = solution.count { it != null }

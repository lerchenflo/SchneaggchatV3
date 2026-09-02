package org.lerchenflo.schneaggchatv3mp.games.presentation.crossword

import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordClue
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordDirection
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordLanguage
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordPuzzle

sealed interface CrosswordAction {
    data class SelectLanguage(val language: CrosswordLanguage) : CrosswordAction
    data object RetryLoad : CrosswordAction
    data object StopGame : CrosswordAction
    data object RestartGame : CrosswordAction
    data class CellTapped(val index: Int) : CrosswordAction
    data class KeyPressed(val letter: Char) : CrosswordAction
    data object Backspace : CrosswordAction
    data object NextClue : CrosswordAction
    data object PreviousClue : CrosswordAction
    data object CheckPuzzle : CrosswordAction
}

data class CrosswordState(
    val language: CrosswordLanguage? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val puzzle: CrosswordPuzzle? = null,
    /** Row-major user letters; null = empty (and always null on block cells). */
    val entries: List<Char?> = emptyList(),
    val selectedCell: Int = -1,
    val direction: CrosswordDirection = CrosswordDirection.ACROSS,
    /** Cells marked wrong by the last "check" — cleared again on the next input. */
    val wrongCells: Set<Int> = emptySet(),
    val isSolved: Boolean = false,
    val elapsedMillis: Long = 0L,
) {
    /** The clue containing the selected cell in the active direction. */
    val currentClue: CrosswordClue?
        get() {
            val p = puzzle ?: return null
            if (selectedCell < 0) return null
            return p.clues.firstOrNull { it.direction == direction && selectedCell in it.cells }
                ?: p.clues.firstOrNull { selectedCell in it.cells }
        }
}

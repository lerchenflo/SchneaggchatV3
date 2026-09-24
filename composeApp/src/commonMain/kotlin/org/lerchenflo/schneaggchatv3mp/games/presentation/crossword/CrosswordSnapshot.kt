package org.lerchenflo.schneaggchatv3mp.games.presentation.crossword

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordDirection
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordLanguage
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordPuzzle

const val CROSSWORD_SNAPSHOT_VERSION = 1

/**
 * Persisted crossword progress. The puzzle itself is stored too, so the English
 * one (downloaded) is not fetched again after leaving the screen. Solved puzzles
 * are kept so the result stays visible until a new one is started.
 */
@Serializable
data class CrosswordSnapshot(
    val language: CrosswordLanguage,
    val puzzle: CrosswordPuzzle,
    val entries: List<Char?>,
    val selectedCell: Int,
    val direction: CrosswordDirection,
    val isSolved: Boolean,
    val elapsedMillis: Long,
)

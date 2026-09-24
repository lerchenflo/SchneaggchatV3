package org.lerchenflo.schneaggchatv3mp.games.presentation.wordle

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleGuess
import org.lerchenflo.schneaggchatv3mp.games.domain.WordlePuzzle

const val WORDLE_SNAPSHOT_VERSION = 1

/**
 * Persisted Wordle progress. The puzzle is stored too, so the English word
 * (downloaded) is not fetched again after leaving the screen. Finished games are
 * kept so the result stays visible — starting a new word is an explicit choice,
 * not something a lost game does on its own.
 */
@Serializable
data class WordleSnapshot(
    val puzzle: WordlePuzzle,
    val guesses: List<WordleGuess>,
    val isSolved: Boolean,
    val isFailed: Boolean,
    val elapsedMillis: Long,
)

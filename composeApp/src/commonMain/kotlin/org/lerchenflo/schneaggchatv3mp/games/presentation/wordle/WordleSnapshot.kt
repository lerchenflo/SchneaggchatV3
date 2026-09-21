package org.lerchenflo.schneaggchatv3mp.games.presentation.wordle

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleGuess
import org.lerchenflo.schneaggchatv3mp.games.domain.WordlePuzzle

const val WORDLE_SNAPSHOT_VERSION = 1

/**
 * Persisted daily Wordle progress. The puzzle is stored too, so the English word
 * (downloaded) is not fetched again; the envelope's day check drops everything at
 * midnight. Finished games are kept so today's result stays visible — and so a
 * failed day cannot be retried.
 */
@Serializable
data class WordleSnapshot(
    val puzzle: WordlePuzzle,
    val guesses: List<WordleGuess>,
    val isSolved: Boolean,
    val isFailed: Boolean,
    val elapsedMillis: Long,
)

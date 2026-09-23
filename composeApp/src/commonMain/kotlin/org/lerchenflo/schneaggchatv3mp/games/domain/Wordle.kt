package org.lerchenflo.schneaggchatv3mp.games.domain

import kotlinx.serialization.Serializable

/** Classic Wordle rules: five letters, six tries. */
const val WORDLE_WORD_LENGTH = 5
const val WORDLE_MAX_GUESSES = 6

enum class WordleLanguage {
    ENGLISH,
    GERMAN
}

/** Feedback for one letter; [UNUSED] only applies to keyboard keys not guessed yet. */
enum class WordleLetterState {
    CORRECT,
    PRESENT,
    ABSENT,
    UNUSED
}

@Serializable
data class WordleGuess(
    /** Uppercase A-Z, [WORDLE_WORD_LENGTH] letters. */
    val word: String,
    /** Per-letter feedback, same length and order as [word]. */
    val states: List<WordleLetterState>,
)

/**
 * Language-independent puzzle word. English words are real NYT solutions pulled
 * from the public Wordle endpoint for a random past date, German ones are picked
 * at random from a local list so the game also works offline.
 */
@Serializable
data class WordlePuzzle(
    val language: WordleLanguage,
    /** Uppercase A-Z solution, [WORDLE_WORD_LENGTH] letters. */
    val solution: String,
    /** e.g. "Wordle #1920 · Tracy Bennett" — shown as attribution under the board. */
    val sourceInfo: String? = null,
)

/**
 * Standard Wordle marking: exact hits are taken first, then the remaining
 * letters are matched against the solution's leftovers, so a repeated letter is
 * only marked PRESENT as often as it actually occurs in the solution.
 */
fun evaluateWordleGuess(guess: String, solution: String): List<WordleLetterState> {
    val states = MutableList(guess.length) { WordleLetterState.ABSENT }
    val leftovers = mutableMapOf<Char, Int>()

    for (i in guess.indices) {
        val solutionLetter = solution.getOrNull(i) ?: continue
        if (guess[i] == solutionLetter) {
            states[i] = WordleLetterState.CORRECT
        } else {
            leftovers[solutionLetter] = (leftovers[solutionLetter] ?: 0) + 1
        }
    }

    for (i in guess.indices) {
        if (states[i] == WordleLetterState.CORRECT) continue
        val available = leftovers[guess[i]] ?: 0
        if (available > 0) {
            states[i] = WordleLetterState.PRESENT
            leftovers[guess[i]] = available - 1
        }
    }

    return states
}

/**
 * Leaderboard score: the number of letters typed to get there, so solving on the
 * first try scores [WORDLE_WORD_LENGTH] and the sixth try scores six times that.
 * Fewer is better here (the server ranks WORDLE ascending), the elapsed time
 * breaks ties.
 */
fun wordleScoreFor(guessesUsed: Int): Long =
    (guessesUsed.coerceAtLeast(1) * WORDLE_WORD_LENGTH).toLong()

package org.lerchenflo.schneaggchatv3mp.games.presentation.wordle

import org.lerchenflo.schneaggchatv3mp.games.domain.WordleGuess
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleLanguage
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleLetterState
import org.lerchenflo.schneaggchatv3mp.games.domain.WordlePuzzle

sealed interface WordleAction {
    data class SelectLanguage(val language: WordleLanguage) : WordleAction
    data object RetryLoad : WordleAction
    /** Screen is leaving (back, rotation, tab switch): keep the progress for the next visit. */
    /** The screen became visible; the clock only runs while someone is looking at it. */
    data object EnterGame : WordleAction

    data object LeaveGame : WordleAction
    /** Throw the current word away and start a fresh random one. */
    data object NewWord : WordleAction
    data class KeyPressed(val letter: Char) : WordleAction
    data object Backspace : WordleAction
    data object SubmitGuess : WordleAction
}

/** Why a submitted guess was rejected; shown above the board and shakes the row. */
enum class WordleInputError {
    TOO_SHORT,
    UNKNOWN_WORD,
}

data class WordleState(
    val language: WordleLanguage? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val puzzle: WordlePuzzle? = null,
    /** Submitted guesses with their feedback, oldest first. */
    val guesses: List<WordleGuess> = emptyList(),
    /** Letters typed into the row below [guesses], not submitted yet. */
    val currentInput: String = "",
    val isSolved: Boolean = false,
    val isFailed: Boolean = false,
    val elapsedMillis: Long = 0L,
    val inputError: WordleInputError? = null,
    /** The word [inputError] refers to, for the "not in the word list" message. */
    val rejectedWord: String = "",
    /** Bumped on every rejection so the same error can re-trigger the shake. */
    val errorNonce: Int = 0,
) {
    val isFinished: Boolean
        get() = isSolved || isFailed

    /**
     * Best feedback seen per letter, for coloring the on-screen keyboard.
     * CORRECT outranks PRESENT outranks ABSENT, so a letter never downgrades.
     */
    fun keyStates(): Map<Char, WordleLetterState> {
        val states = mutableMapOf<Char, WordleLetterState>()
        for (guess in guesses) {
            guess.word.forEachIndexed { index, letter ->
                val next = guess.states.getOrNull(index) ?: return@forEachIndexed
                val current = states[letter]
                if (current == null || next.rank > current.rank) states[letter] = next
            }
        }
        return states
    }
}

private val WordleLetterState.rank: Int
    get() = when (this) {
        WordleLetterState.UNUSED -> 0
        WordleLetterState.ABSENT -> 1
        WordleLetterState.PRESENT -> 2
        WordleLetterState.CORRECT -> 3
    }

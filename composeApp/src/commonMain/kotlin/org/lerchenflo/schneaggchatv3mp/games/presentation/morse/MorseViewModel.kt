package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameDifficultySelection
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

data class MorseState(
    val currentCode: String = "",
    val currentChar: Char? = null,
    val invalid: Boolean = false,
    val history: List<Char> = emptyList(),
    val challenge: MorseChallengeState? = null
)

/**
 * Challenge mode: the target text has to be typed in morse in correct order.
 * Points are awarded per correct character; after 3 wrong characters the game is over.
 */
data class MorseChallengeState(
    val targetText: String,
    val currentIndex: Int = 0,
    val errors: Int = 0,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val elapsedMillis: Long = 0L
)

private const val HISTORY_LIMIT = 10
private const val INVALID_CLEAR_DELAY_MS = 800L
private const val MAX_CODE_DEPTH = 5

const val CHALLENGE_MAX_ERRORS = 3
private const val CHALLENGE_POINTS_PER_CHAR = 10

private val CHALLENGE_WORDS = listOf(
    "HELLO", "WORLD", "RADIO", "SIGNAL", "MORSE", "CODE", "SHIP", "OCEAN", "PILOT", "TOWER",
    "APPLE", "HOUSE", "LIGHT", "SOUND", "MUSIC", "PIZZA", "TIGER", "EAGLE", "RIVER", "STONE",
    "CLOUD", "STORM", "BEACH", "TRAIL", "NIGHT",
)

class MorseViewModel(
    private val gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MorseState())
    val state: StateFlow<MorseState> = _state.asStateFlow()

    private var autoCommitJob: Job? = null
    private var challengeTimerJob: Job? = null
    private var challengeStartTime = 0L
    private var challengeDifficulty = GameDifficulty.MEDIUM

    fun addDot() = addSymbol(".")
    fun addDash() = addSymbol("-")

    private fun addSymbol(symbol: String) {
        autoCommitJob?.cancel()
        val current = _state.value
        if (current.challenge?.isGameOver == true) return
        val newCode = current.currentCode + symbol

        if (newCode.length > MAX_CODE_DEPTH) {
            triggerInvalid()
            return
        }

        val resolved = charForCode(newCode)
        _state.update { it.copy(currentCode = newCode, currentChar = resolved, invalid = false) }

        if (resolved != null) {
            autoCommitJob = viewModelScope.launch {
                delay(autoCommitDelayMs().milliseconds)
                commit()
            }
        }
    }

    /** Time before an entered character is committed — easier modes give more time. */
    private fun autoCommitDelayMs(): Long {
        val difficulty = if (_state.value.challenge != null) {
            challengeDifficulty
        } else {
            GameDifficultySelection.selected
        }
        return when (difficulty) {
            GameDifficulty.LOW -> 2400L
            GameDifficulty.MEDIUM -> 1800L
            GameDifficulty.HIGH -> 1000L
        }
    }

    fun commit() {
        val char = _state.value.currentChar ?: return
        val challenge = _state.value.challenge

        if (challenge != null) {
            if (!challenge.isGameOver) {
                evaluateChallengeChar(char, challenge)
            }
            _state.update { it.copy(currentCode = "", currentChar = null, invalid = false) }
        } else {
            _state.update { state ->
                val newHistory = (state.history + char).takeLast(HISTORY_LIMIT)
                state.copy(currentCode = "", currentChar = null, history = newHistory, invalid = false)
            }
        }
    }

    fun clear() {
        autoCommitJob?.cancel()
        _state.update { it.copy(currentCode = "", currentChar = null, invalid = false) }
    }

    fun startChallenge() {
        autoCommitJob?.cancel()
        challengeDifficulty = GameDifficultySelection.selected
        val wordCount = when (challengeDifficulty) {
            GameDifficulty.LOW -> 3
            GameDifficulty.MEDIUM -> 4
            GameDifficulty.HIGH -> 6
        }
        val targetText = CHALLENGE_WORDS.shuffled().take(wordCount).joinToString(" ")
        challengeStartTime = Clock.System.now().toEpochMilliseconds()

        _state.update {
            it.copy(
                currentCode = "",
                currentChar = null,
                invalid = false,
                challenge = MorseChallengeState(targetText = targetText)
            )
        }

        startChallengeTimer()
    }

    private fun startChallengeTimer() {
        challengeTimerJob?.cancel()
        challengeTimerJob = viewModelScope.launch {
            while (true) {
                _state.update { state ->
                    val challenge = state.challenge
                    if (challenge == null || challenge.isGameOver) {
                        state
                    } else {
                        state.copy(
                            challenge = challenge.copy(
                                elapsedMillis = Clock.System.now().toEpochMilliseconds() - challengeStartTime
                            )
                        )
                    }
                }
                delay(1000.milliseconds)
            }
        }
    }

    fun exitChallenge() {
        autoCommitJob?.cancel()
        challengeTimerJob?.cancel()
        _state.update { it.copy(currentCode = "", currentChar = null, invalid = false, challenge = null) }
    }

    private fun evaluateChallengeChar(char: Char, challenge: MorseChallengeState) {
        val target = challenge.targetText

        var updated = if (char == target[challenge.currentIndex]) {
            // Spaces cannot be typed in morse, skip them
            var nextIndex = challenge.currentIndex + 1
            while (nextIndex < target.length && target[nextIndex] == ' ') nextIndex++
            challenge.copy(
                currentIndex = nextIndex,
                score = challenge.score + CHALLENGE_POINTS_PER_CHAR
            )
        } else {
            challenge.copy(errors = challenge.errors + 1)
        }

        val completed = updated.currentIndex >= target.length
        val failed = updated.errors >= CHALLENGE_MAX_ERRORS
        if (completed || failed) {
            updated = updated.copy(isGameOver = true)
            challengeTimerJob?.cancel()
            submitChallengeScore(updated.score)
        }

        _state.update { it.copy(challenge = updated) }
    }

    private fun submitChallengeScore(score: Int) {
        val elapsed = Clock.System.now().toEpochMilliseconds() - challengeStartTime
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.MORSE,
                difficulty = challengeDifficulty,
                score = score.toLong(),
                timeMillis = elapsed,
            )
        }
    }

    private fun triggerInvalid() {
        _state.update { it.copy(currentCode = "", currentChar = null, invalid = true) }
        viewModelScope.launch {
            delay(INVALID_CLEAR_DELAY_MS.milliseconds)
            _state.update { it.copy(invalid = false) }
        }
    }
}

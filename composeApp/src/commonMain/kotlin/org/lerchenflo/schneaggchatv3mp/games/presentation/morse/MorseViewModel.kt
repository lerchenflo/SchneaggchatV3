package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameDifficultySelection
import org.lerchenflo.schneaggchatv3mp.games.presentation.awaitResume
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
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
    val isPaused: Boolean = false,
    val elapsedMillis: Long = 0L,
    val charTimeLimitMs: Long = 5000L,
    val charTimeRemainingMs: Long = 5000L
)

private const val HISTORY_LIMIT = 10
private const val INVALID_CLEAR_DELAY_MS = 350L
private const val MAX_CODE_DEPTH = 5

const val CHALLENGE_MAX_ERRORS = 3
private const val CHALLENGE_POINTS_PER_CHAR = 10

class MorseViewModel(
    private val gameHighscoreRepository: GameHighscoreRepository,
    private val loggingRepository: LoggingRepository,
    private val languageService: LanguageService
) : ViewModel() {

    private val _state = MutableStateFlow(MorseState())
    val state: StateFlow<MorseState> = _state.asStateFlow()

    private var autoCommitJob: Job? = null
    private var challengeTimerJob: Job? = null
    private var challengeStartTime = 0L
    private var challengeDifficulty = GameDifficulty.MEDIUM

    var selectedLanguage by mutableStateOf(LanguageSetting.ENGLISH)
        private set

    init {
        viewModelScope.launch { // Language
            selectedLanguage = languageService.getSystemLanguageSetting()
        }
    }

    val wordsList: List<String>
        get() = when (selectedLanguage) {
            LanguageSetting.ENGLISH -> ENGLISH_WORDS
            LanguageSetting.GERMAN -> GERMAN_WORDS
            LanguageSetting.VORI -> VORI_WORDS
            else -> ENGLISH_WORDS
        }


    fun addDot() = addSymbol(".")
    fun addDash() = addSymbol("-")

    private fun addSymbol(symbol: String) {
        autoCommitJob?.cancel()
        val current = _state.value
        val challenge = current.challenge
        if (challenge?.isGameOver == true || challenge?.isPaused == true) return
        val newCode = current.currentCode + symbol

        if (newCode.length > MAX_CODE_DEPTH) {
            triggerInvalid()
            return
        }

        val resolved = charForCode(newCode)
        _state.update { it.copy(currentCode = newCode, currentChar = resolved, invalid = false) }

        if (resolved != null) {
            val expected = challenge?.targetText?.getOrNull(challenge.currentIndex)
            if (challenge != null && resolved == expected) {
                // Landed on the correct character: advance immediately instead of
                // waiting out the usual accept delay, so a correct run feels instant.
                commit()
            } else {
                autoCommitJob = viewModelScope.launch {
                    delay(autoCommitDelayMs().milliseconds)
                    commit()
                }
            }
        }
    }

    /** Initial character time limit by difficulty. */
    private fun getInitialCharTimeLimitMs(difficulty: GameDifficulty): Long = when (difficulty) {
        GameDifficulty.LOW -> 10000L
        GameDifficulty.MEDIUM -> 7000L
        GameDifficulty.HIGH -> 2500L
    }

    /** Calculate character time limit based on number of correctly typed characters.
     *  Ramps up gradually ("not that fast") and caps at WW2 military speed (300-650 ms/char).
     */
    private fun calculateCharTimeLimitMs(charsTyped: Int, difficulty: GameDifficulty): Long {
        val (initial, decay, minLimit) = when (difficulty) {
            GameDifficulty.LOW -> Triple(10000L, 25L, 650L)       // ~18 WPM cap
            GameDifficulty.MEDIUM -> Triple(7000L, 20L, 450L)     // ~27 WPM WW2 operator cap
            GameDifficulty.HIGH -> Triple(2500L, 10L, 300L)       // ~40 WPM WW2 elite military speed
        }
        return (initial - charsTyped * decay).coerceAtLeast(minLimit)
    }

    /** Time before an entered character is committed — lower in all modes and shrinks proportionally in challenge. */
    private fun autoCommitDelayMs(): Long {
        val challenge = _state.value.challenge
        if (challenge != null) {
            val ratio = when (challengeDifficulty) {
                GameDifficulty.LOW -> 1200.0 / 10000.0
                GameDifficulty.MEDIUM -> 800.0 / 7000.0
                GameDifficulty.HIGH -> 500.0 / 2500.0
            }
            return (challenge.charTimeLimitMs * ratio).toLong().coerceAtLeast(80L)
        } else {
            return when (GameDifficultySelection.selected) {
                GameDifficulty.LOW -> 1200L
                GameDifficulty.MEDIUM -> 800L
                GameDifficulty.HIGH -> 500L
            }
        }
    }

    fun commit() {
        val char = _state.value.currentChar ?: return
        val challenge = _state.value.challenge
        if (challenge?.isPaused == true) return

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

    private fun ensureInfiniteText(targetText: String, currentIndex: Int): String {
        var text = targetText
        if (text.length - currentIndex < 30) {
            val newWords = wordsList.shuffled().take(6).joinToString(" ")
            text = if (text.isEmpty()) newWords else "$text $newWords"
        }
        return text
    }

    fun startChallenge() {
        autoCommitJob?.cancel()
        challengeDifficulty = GameDifficultySelection.selected
        val initialText = ensureInfiniteText("", 0)
        val initialTimeLimit = getInitialCharTimeLimitMs(challengeDifficulty)
        challengeStartTime = Clock.System.now().toEpochMilliseconds()

        _state.update {
            it.copy(
                currentCode = "",
                currentChar = null,
                invalid = false,
                challenge = MorseChallengeState(
                    targetText = initialText,
                    charTimeLimitMs = initialTimeLimit,
                    charTimeRemainingMs = initialTimeLimit
                )
            )
        }

        startChallengeTimer()
    }

    fun togglePause() {
        val challenge = _state.value.challenge ?: return
        if (challenge.isGameOver) return

        if (!challenge.isPaused) {
            // Pausing cancels any half-entered code so resuming always starts clean.
            autoCommitJob?.cancel()
            _state.update {
                it.copy(
                    currentCode = "",
                    currentChar = null,
                    invalid = false,
                    challenge = it.challenge?.copy(isPaused = true)
                )
            }
        } else {
            _state.update { it.copy(challenge = it.challenge?.copy(isPaused = false)) }
        }
    }

    private fun startChallengeTimer() {
        challengeTimerJob?.cancel()
        challengeTimerJob = viewModelScope.launch {
            val tickMs = 50L
            while (true) {
                val drift = awaitResume { _state.value.challenge?.isPaused == true }
                if (drift > 0) challengeStartTime += drift

                delay(tickMs.milliseconds)
                var submitScoreNeeded = false
                var scoreToSubmit = 0

                _state.update { state ->
                    val challenge = state.challenge
                    if (challenge == null || challenge.isGameOver) {
                        state
                    } else {
                        val elapsed = Clock.System.now().toEpochMilliseconds() - challengeStartTime
                        val remaining = challenge.charTimeRemainingMs - tickMs

                        if (remaining <= 0) {
                            if (state.currentCode.isNotEmpty()) {
                                // User is actively inputting morse code symbols! Keep timer at 0ms without resetting input
                                state.copy(
                                    challenge = challenge.copy(
                                        elapsedMillis = elapsed,
                                        charTimeRemainingMs = 0L
                                    )
                                )
                            } else {
                                // User is idle and timer ran out: penalize error & reset timer for retry
                                autoCommitJob?.cancel()
                                val newErrors = challenge.errors + 1
                                val failed = newErrors >= CHALLENGE_MAX_ERRORS

                                if (failed) {
                                    submitScoreNeeded = true
                                    scoreToSubmit = challenge.score
                                }

                                val charsTyped = challenge.score / CHALLENGE_POINTS_PER_CHAR
                                val nextTimeLimit = calculateCharTimeLimitMs(charsTyped, challengeDifficulty)

                                state.copy(
                                    currentCode = "",
                                    currentChar = null,
                                    invalid = true,
                                    challenge = challenge.copy(
                                        errors = newErrors,
                                        isGameOver = failed,
                                        elapsedMillis = elapsed,
                                        charTimeLimitMs = nextTimeLimit,
                                        charTimeRemainingMs = nextTimeLimit
                                    )
                                )
                            }
                        } else {
                            state.copy(
                                challenge = challenge.copy(
                                    elapsedMillis = elapsed,
                                    charTimeRemainingMs = remaining
                                )
                            )
                        }
                    }
                }

                if (submitScoreNeeded) {
                    challengeTimerJob?.cancel()
                    submitChallengeScore(scoreToSubmit)
                    break
                }
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

        if (char == target[challenge.currentIndex]) {
            var nextIndex = challenge.currentIndex + 1
            while (nextIndex < target.length && target[nextIndex] == ' ') nextIndex++
            val updatedText = ensureInfiniteText(target, nextIndex)

            val newScore = challenge.score + CHALLENGE_POINTS_PER_CHAR
            val charsTyped = newScore / CHALLENGE_POINTS_PER_CHAR
            val nextTimeLimit = calculateCharTimeLimitMs(charsTyped, challengeDifficulty)

            val updated = challenge.copy(
                targetText = updatedText,
                currentIndex = nextIndex,
                score = newScore,
                charTimeLimitMs = nextTimeLimit,
                charTimeRemainingMs = nextTimeLimit
            )
            _state.update { it.copy(challenge = updated) }
        } else {
            val newErrors = challenge.errors + 1
            val failed = newErrors >= CHALLENGE_MAX_ERRORS

            val charsTyped = challenge.score / CHALLENGE_POINTS_PER_CHAR
            val nextTimeLimit = calculateCharTimeLimitMs(charsTyped, challengeDifficulty)

            val updated = challenge.copy(
                errors = newErrors,
                isGameOver = failed,
                charTimeLimitMs = nextTimeLimit,
                charTimeRemainingMs = nextTimeLimit
            )
            triggerInvalid()
            if (failed) {
                challengeTimerJob?.cancel()
                submitChallengeScore(updated.score)
            }
            _state.update { it.copy(challenge = updated) }
        }
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

package org.lerchenflo.schneaggchatv3mp.games.presentation.oddoneout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameDifficultySelection
import kotlin.random.Random
import kotlin.time.Clock

private const val WRONG_TAP_PAUSE_MILLIS = 400L

class OddOneOutViewmodel(
    private val gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OddOneOutState())
    val state = _state.asStateFlow()

    private var timerJob: Job? = null
    private var runStartTime = 0L
    private var roundStartTime = 0L
    private var roundDeadline = 0L
    private var config = difficultyConfig(GameDifficultySelection.selected)
    private var currentDifficulty = GameDifficultySelection.selected

    /** False while a life is being lost and the miss is briefly shown, so timeouts/taps don't double-count. */
    private var roundActive = false

    fun onAction(action: OddOneOutAction) {
        when (action) {
            OddOneOutAction.StartGame -> startGame()
            OddOneOutAction.StopGame -> stopGame()
            OddOneOutAction.RestartGame -> startGame()
            is OddOneOutAction.OnTileTapped -> onTileTapped(action.index)
        }
    }

    private fun startGame() {
        currentDifficulty = GameDifficultySelection.selected
        config = difficultyConfig(currentDifficulty)
        timerJob?.cancel()
        runStartTime = Clock.System.now().toEpochMilliseconds()
        _state.value = OddOneOutState(gridSize = config.gridSize, isPlaying = true)
        startRound(round = 0)
        startTimer()
    }

    /** Ends the current run without submitting a score and returns to the start screen. */
    private fun stopGame() {
        timerJob?.cancel()
        roundActive = false
        _state.value = OddOneOutState()
    }

    private fun startRound(round: Int) {
        val roundTime = roundTimeMillis(config, round)
        val generated = generateRound(config, round, Random)
        roundStartTime = Clock.System.now().toEpochMilliseconds()
        roundDeadline = roundStartTime + roundTime
        roundActive = true
        _state.update {
            it.copy(
                round = round,
                tiles = generated.tiles,
                oddIndex = generated.oddIndex,
                variant = generated.variant,
                oddLighten = generated.lighten,
                oddDelta = generated.delta,
                roundTimeMillis = roundTime,
                roundTimeRemainingMillis = roundTime,
                wrongIndex = null,
            )
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(100L)
                val now = Clock.System.now().toEpochMilliseconds()
                val remaining = (roundDeadline - now).coerceAtLeast(0L)
                _state.update { it.copy(elapsedMillis = now - runStartTime, roundTimeRemainingMillis = remaining) }
                if (remaining <= 0L) onTimeout()
            }
        }
    }

    private fun onTimeout() {
        if (!roundActive) return
        roundActive = false
        loseLife(wrongIndex = null, round = _state.value.round)
    }

    private fun onTileTapped(index: Int) {
        val current = _state.value
        if (!current.isPlaying || current.isGameOver || !roundActive) return
        roundActive = false
        if (index == current.oddIndex) {
            val reaction = Clock.System.now().toEpochMilliseconds() - roundStartTime
            val gained = roundScore(reaction, current.roundTimeMillis)
            _state.update { it.copy(score = it.score + gained) }
            startRound(current.round + 1)
        } else {
            loseLife(wrongIndex = index, round = current.round)
        }
    }

    private fun loseLife(wrongIndex: Int?, round: Int) {
        val current = _state.value
        val lives = current.lives - 1
        if (lives <= 0) {
            timerJob?.cancel()
            val elapsed = Clock.System.now().toEpochMilliseconds() - runStartTime
            _state.update {
                it.copy(
                    lives = 0,
                    isPlaying = false,
                    isGameOver = true,
                    wrongIndex = wrongIndex,
                    elapsedMillis = elapsed,
                )
            }
            submitScore(current.score, elapsed)
        } else {
            _state.update { it.copy(lives = lives, wrongIndex = wrongIndex) }
            // Briefly show the miss before the next round starts
            viewModelScope.launch {
                delay(WRONG_TAP_PAUSE_MILLIS)
                if (_state.value.isPlaying) startRound(round + 1)
            }
        }
    }

    private fun submitScore(score: Int, timeMillis: Long) {
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.ODDONEOUT,
                difficulty = currentDifficulty,
                score = score.toLong(),
                timeMillis = timeMillis,
            )
        }
    }
}

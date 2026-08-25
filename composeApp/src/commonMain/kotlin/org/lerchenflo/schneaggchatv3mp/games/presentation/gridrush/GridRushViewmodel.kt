package org.lerchenflo.schneaggchatv3mp.games.presentation.gridrush

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
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameDifficultySelection
import org.lerchenflo.schneaggchatv3mp.games.presentation.awaitResume
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class GridRushViewmodel(
    private val gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(baseState(GameDifficultySelection.selected))
    val state = _state.asStateFlow()

    private var timerJob: Job? = null
    private var runStartTime = 0L
    private var currentDifficulty = GameDifficulty.MEDIUM

    fun onAction(action: GridRushAction) {
        when (action) {
            GridRushAction.StartGame -> startGame()
            GridRushAction.StopGame -> stopGame()
            GridRushAction.RestartGame -> startGame()
            GridRushAction.TogglePause -> togglePause()
            is GridRushAction.OnDragStart -> onDragStart(action.cell)
            is GridRushAction.OnDragMove -> onDragMove(action.cell)
            GridRushAction.OnDragEnd -> onDragEnd()
        }
    }

    private fun togglePause() {
        val current = _state.value
        if (!current.isPlaying || current.isGameOver) return
        // Cancel any drag in progress so resuming never leaves a half-committed chain.
        _state.update { it.copy(isPaused = !it.isPaused, dragPath = emptyList()) }
    }

    /** Today's local daily board — restarting on the same day reproduces the same board. */
    private fun baseState(difficulty: GameDifficulty): GridRushState {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val epochDay = today.toEpochDays()
        val generated = generateDailyBoard(epochDay, difficulty)
        return GridRushState(
            rows = generated.board.size,
            cols = generated.board[0].size,
            board = generated.board,
            parMoves = generated.parMoves,
        )
    }

    private fun startGame() {
        currentDifficulty = GameDifficultySelection.selected
        timerJob?.cancel()
        runStartTime = 0L // the timer only starts on the first tile press, planning is free
        _state.value = baseState(currentDifficulty).copy(isPlaying = true)
    }

    /** Ends the current run without submitting a score and returns to the start screen. */
    private fun stopGame() {
        timerJob?.cancel()
        runStartTime = 0L
        _state.value = baseState(GameDifficultySelection.selected)
    }

    private fun onDragStart(cell: Cell) {
        val current = _state.value
        if (!current.isPlaying || current.isGameOver || current.isPaused) return
        if (current.board.getOrNull(cell.row)?.getOrNull(cell.col) == null) return
        if (runStartTime == 0L) startTimer()
        _state.update { it.copy(dragPath = listOf(cell)) }
    }

    private fun startTimer() {
        runStartTime = Clock.System.now().toEpochMilliseconds()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val drift = awaitResume { _state.value.isPaused }
                if (drift > 0) runStartTime += drift

                delay(100L.milliseconds)
                _state.update { it.copy(elapsedMillis = Clock.System.now().toEpochMilliseconds() - runStartTime) }
            }
        }
    }

    private fun onDragMove(cell: Cell) {
        if (_state.value.isPaused) return
        _state.update { current ->
            val path = current.dragPath
            when {
                path.isEmpty() || cell == path.last() -> current
                // Dragging back onto the previous cell removes the last one (undo by backtracking)
                path.size >= 2 && cell == path[path.size - 2] -> current.copy(dragPath = path.dropLast(1))
                isValidExtension(current.board, path, cell) -> current.copy(dragPath = path + cell)
                else -> current
            }
        }
    }

    private fun onDragEnd() {
        val current = _state.value
        if (!current.isPlaying || !current.dragValid) {
            _state.update { it.copy(dragPath = emptyList()) }
            return
        }

        val newBoard = applyChain(current.board, current.dragPath)
        var score = current.score + chainScore(current.dragPath.size)
        val movesUsed = current.movesUsed + 1
        val won = isBoardEmpty(newBoard)
        val deadEnd = !won && !hasAnyValidChain(newBoard)

        if (won) {
            score += FULL_CLEAR_BONUS + EFFICIENCY_BONUS_PER_MOVE * max(0, current.parMoves - movesUsed)
        }

        val gameOver = won || deadEnd
        val elapsedMillis = if (runStartTime != 0L) {
            Clock.System.now().toEpochMilliseconds() - runStartTime
        } else {
            current.elapsedMillis
        }

        if (gameOver) timerJob?.cancel()
        _state.update {
            it.copy(
                board = newBoard,
                dragPath = emptyList(),
                score = score,
                movesUsed = movesUsed,
                elapsedMillis = elapsedMillis,
                isPlaying = !gameOver,
                isGameOver = gameOver,
                won = won,
            )
        }
        // Only completed full clears count for the leaderboard; dead ends submit nothing
        if (won) submitScore(score, elapsedMillis)
    }

    private fun submitScore(score: Int, timeMillis: Long) {
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.GRIDRUSH,
                difficulty = currentDifficulty,
                score = score.toLong(),
                timeMillis = timeMillis,
            )
        }
    }
}

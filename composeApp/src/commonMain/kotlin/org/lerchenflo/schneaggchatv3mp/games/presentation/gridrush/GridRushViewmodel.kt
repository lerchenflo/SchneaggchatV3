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
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameDifficultySelection
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.games.presentation.awaitResume
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.today
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_daily_reset
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class GridRushViewmodel(
    private val gameHighscoreRepository: GameHighscoreRepository,
    gameSaveRepository: GameSaveRepository,
) : ViewModel() {

    /** Local day the current board was generated for (declared before the state, which sets it). */
    private var boardEpochDay = 0L

    private val _state = MutableStateFlow(baseState(GameDifficultySelection.selected))
    val state = _state.asStateFlow()

    private val saveSession = GameSaveSession(
        game = GameId.GRIDRUSH,
        serializer = GridRushSnapshot.serializer(),
        schemaVersion = GRIDRUSH_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )
    /** The screen waits for this before auto-starting a run, so a restored one is never overwritten. */
    val restoreChecked = saveSession.restoreChecked

    private var timerJob: Job? = null
    private var runStartTime = 0L
    private var currentDifficulty = GameDifficulty.MEDIUM

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = ::pauseAndPersist)
        viewModelScope.launch {
            // Coming back from the background may be on a new day
            AppLifecycleManager.appResumedEvent.collect { checkDayChanged() }
        }
    }

    fun onAction(action: GridRushAction) {
        when (action) {
            GridRushAction.StartGame -> startGame()
            GridRushAction.StopGame -> stopGame()
            GridRushAction.RestartGame -> startGame()
            GridRushAction.TogglePause -> togglePause()
            GridRushAction.LeaveGame -> pauseAndPersist()
            GridRushAction.CheckDayChanged -> checkDayChanged()
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
        boardEpochDay = today().toEpochDays()
        val generated = generateDailyBoard(boardEpochDay, difficulty)
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
        saveSession.clear()
        _state.value = baseState(GameDifficultySelection.selected)
    }

    /** Leaving the screen or backgrounding the app: freeze the run and keep it for the next visit. */
    private fun pauseAndPersist() {
        val current = _state.value
        // Only a running clock needs pausing; the free planning phase has nothing to freeze
        if (current.isPlaying && !current.isGameOver && runStartTime != 0L) {
            _state.update { it.copy(isPaused = true, dragPath = emptyList()) }
        }
        persist()
    }

    private fun persist() = saveSession.persist(currentDifficulty, snapshotOrNull())

    /** Null before the run started; finished runs are kept so today's result stays visible. */
    private fun snapshotOrNull(): GridRushSnapshot? {
        val current = _state.value
        if (!current.isPlaying && !current.isGameOver) return null
        return GridRushSnapshot(
            board = current.board,
            rows = current.rows,
            cols = current.cols,
            parMoves = current.parMoves,
            score = current.score,
            movesUsed = current.movesUsed,
            elapsedMillis = current.elapsedMillis,
            timerStarted = runStartTime != 0L,
            isGameOver = current.isGameOver,
            won = current.won,
        )
    }

    /** Brings today's saved run (or result) back; a running clock comes back paused. */
    private fun restore(save: GameSave<GridRushSnapshot>) {
        val data = save.data
        currentDifficulty = save.difficulty
        boardEpochDay = save.epochDay
        timerJob?.cancel()
        runStartTime = 0L
        val resumeClock = !data.isGameOver && data.timerStarted
        _state.value = GridRushState(
            isPlaying = !data.isGameOver,
            isGameOver = data.isGameOver,
            won = data.won,
            score = data.score,
            movesUsed = data.movesUsed,
            parMoves = data.parMoves,
            elapsedMillis = data.elapsedMillis,
            rows = data.rows,
            cols = data.cols,
            board = data.board,
            // The free planning phase has no clock, so there is nothing to pause there
            isPaused = resumeClock,
        )
        if (resumeClock) {
            startTimer(startTime = Clock.System.now().toEpochMilliseconds() - data.elapsedMillis)
        }
    }

    /** The daily board changed underneath a run that is still on screen: swap in today's board. */
    private fun checkDayChanged() {
        if (boardEpochDay == today().toEpochDays()) return
        val wasStarted = _state.value.isPlaying || _state.value.isGameOver
        timerJob?.cancel()
        runStartTime = 0L
        saveSession.clear()
        currentDifficulty = GameDifficultySelection.selected
        // Straight into a fresh planning phase: the screen's start overlay is already dismissed
        _state.value = baseState(currentDifficulty).copy(isPlaying = wasStarted)
        if (wasStarted) {
            viewModelScope.launch { SnackbarManager.showMessage(getString(Res.string.games_daily_reset)) }
        }
    }

    private fun onDragStart(cell: Cell) {
        val current = _state.value
        if (!current.isPlaying || current.isGameOver || current.isPaused) return
        if (current.board.getOrNull(cell.row)?.getOrNull(cell.col) == null) return
        if (runStartTime == 0L) startTimer()
        _state.update { it.copy(dragPath = listOf(cell)) }
    }

    private fun startTimer(startTime: Long = Clock.System.now().toEpochMilliseconds()) {
        timerJob?.cancel()
        runStartTime = startTime
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
        // Daily game: keep today's result so coming back shows it instead of a fresh board
        if (gameOver) persist()
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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        // viewModelScope is already cancelled here; the write runs on the application scope
        persist()
    }
}

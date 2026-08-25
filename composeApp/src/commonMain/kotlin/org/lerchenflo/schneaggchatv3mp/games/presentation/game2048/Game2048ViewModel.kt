package org.lerchenflo.schneaggchatv3mp.games.presentation.game2048

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
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

enum class MoveDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

const val DEFAULT_GRID_SIZE = 4

data class Game2048State(
    val grid: List<Int> = List(DEFAULT_GRID_SIZE * DEFAULT_GRID_SIZE) { 0 },
    val gridSize: Int = DEFAULT_GRID_SIZE,
    val score: Int = 0,
    val bestTile: Int = 0,
    val isGameOver: Boolean = false,
    val isGameStarted: Boolean = false,
    val isPaused: Boolean = false,
    val hasReached2048: Boolean = false,
    val elapsedMillis: Long = 0L,
)

sealed interface Game2048Action {
    data object StartGame : Game2048Action
    data object StopGame : Game2048Action
    data object RestartGame : Game2048Action
    data object TogglePause : Game2048Action
    data class Move(val direction: MoveDirection) : Game2048Action
}

/** Board size by difficulty: harder means a smaller, less forgiving board. */
private fun gridSizeFor(difficulty: GameDifficulty): Int = when (difficulty) {
    GameDifficulty.LOW -> 4
    GameDifficulty.MEDIUM -> 3
    GameDifficulty.HIGH -> 2
}

class Game2048ViewModel(
    private val gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(Game2048State())
    val state = _state.asStateFlow()

    private var timerJob: Job? = null
    private var gameStartTime = 0L
    private var currentDifficulty = GameDifficulty.MEDIUM

    fun onAction(action: Game2048Action) {
        when (action) {
            Game2048Action.StartGame -> startGame()
            Game2048Action.StopGame -> stopGame()
            Game2048Action.RestartGame -> startGame()
            Game2048Action.TogglePause -> togglePause()
            is Game2048Action.Move -> move(action.direction)
        }
    }

    private fun startGame() {
        currentDifficulty = GameDifficultySelection.selected
        val size = gridSizeFor(currentDifficulty)
        timerJob?.cancel()
        gameStartTime = Clock.System.now().toEpochMilliseconds()

        val emptyGrid = MutableList(size * size) { 0 }
        spawnRandomTile(emptyGrid)
        spawnRandomTile(emptyGrid)

        _state.value = Game2048State(
            grid = emptyGrid,
            gridSize = size,
            score = 0,
            bestTile = emptyGrid.maxOrNull() ?: 0,
            isGameOver = false,
            isGameStarted = true,
            isPaused = false,
            hasReached2048 = false,
            elapsedMillis = 0L,
        )

        startTimer()
    }

    private fun stopGame() {
        timerJob?.cancel()
        _state.value = Game2048State()
    }

    private fun togglePause() {
        val current = _state.value
        if (!current.isGameStarted || current.isGameOver) return
        _state.update { it.copy(isPaused = !it.isPaused) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val drift = awaitResume { _state.value.isPaused }
                if (drift > 0) gameStartTime += drift

                _state.update {
                    it.copy(
                        elapsedMillis = Clock.System.now().toEpochMilliseconds() - gameStartTime
                    )
                }
                delay(32.milliseconds)
            }
        }
    }

    private fun move(direction: MoveDirection) {
        val currentState = _state.value
        if (!currentState.isGameStarted || currentState.isGameOver || currentState.isPaused) return

        val currentGrid = currentState.grid
        val gridSize = currentState.gridSize
        val (newGrid, addedScore) = executeMove(currentGrid, direction, gridSize)

        if (newGrid == currentGrid) {
            // No move was possible in this direction
            return
        }

        val mutableGrid = newGrid.toMutableList()
        spawnRandomTile(mutableGrid)

        val newScore = currentState.score + addedScore
        val maxTile = mutableGrid.maxOrNull() ?: 0
        val reached2048 = currentState.hasReached2048 || maxTile >= 2048
        val gameOver = isGridGameOver(mutableGrid, gridSize)

        _state.update {
            it.copy(
                grid = mutableGrid,
                score = newScore,
                bestTile = maxTile,
                hasReached2048 = reached2048,
                isGameOver = gameOver,
            )
        }

        if (gameOver) {
            timerJob?.cancel()
            submitScore(newScore.toLong())
        }
    }

    private fun spawnRandomTile(grid: MutableList<Int>) {
        val emptyIndices = grid.indices.filter { grid[it] == 0 }
        if (emptyIndices.isEmpty()) return

        val randomIndex = emptyIndices.random(Random)
        val fourProbability = when (currentDifficulty) {
            GameDifficulty.LOW -> 0.05
            GameDifficulty.MEDIUM -> 0.10
            GameDifficulty.HIGH -> 0.25
        }
        val tileValue = if (Random.nextDouble() < fourProbability) 4 else 2
        grid[randomIndex] = tileValue
    }

    private fun executeMove(grid: List<Int>, direction: MoveDirection, size: Int): Pair<List<Int>, Int> {
        val result = MutableList(size * size) { 0 }
        var totalScore = 0

        when (direction) {
            MoveDirection.LEFT -> {
                for (row in 0 until size) {
                    val line = (0 until size).map { col -> grid[row * size + col] }
                    val (mergedLine, lineScore) = slideAndMergeLine(line, size)
                    totalScore += lineScore
                    for (col in 0 until size) {
                        result[row * size + col] = mergedLine[col]
                    }
                }
            }
            MoveDirection.RIGHT -> {
                for (row in 0 until size) {
                    val line = (0 until size).map { col -> grid[row * size + (size - 1 - col)] }
                    val (mergedLine, lineScore) = slideAndMergeLine(line, size)
                    totalScore += lineScore
                    for (col in 0 until size) {
                        result[row * size + (size - 1 - col)] = mergedLine[col]
                    }
                }
            }
            MoveDirection.UP -> {
                for (col in 0 until size) {
                    val line = (0 until size).map { row -> grid[row * size + col] }
                    val (mergedLine, lineScore) = slideAndMergeLine(line, size)
                    totalScore += lineScore
                    for (row in 0 until size) {
                        result[row * size + col] = mergedLine[row]
                    }
                }
            }
            MoveDirection.DOWN -> {
                for (col in 0 until size) {
                    val line = (0 until size).map { row -> grid[(size - 1 - row) * size + col] }
                    val (mergedLine, lineScore) = slideAndMergeLine(line, size)
                    totalScore += lineScore
                    for (row in 0 until size) {
                        result[(size - 1 - row) * size + col] = mergedLine[row]
                    }
                }
            }
        }

        return Pair(result, totalScore)
    }

    private fun slideAndMergeLine(line: List<Int>, size: Int): Pair<List<Int>, Int> {
        val nonZero = line.filter { it != 0 }
        val merged = mutableListOf<Int>()
        var score = 0
        var i = 0

        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val mergedVal = nonZero[i] * 2
                merged.add(mergedVal)
                score += mergedVal
                i += 2
            } else {
                merged.add(nonZero[i])
                i += 1
            }
        }

        while (merged.size < size) {
            merged.add(0)
        }

        return Pair(merged, score)
    }

    private fun isGridGameOver(grid: List<Int>, size: Int): Boolean {
        // Any empty cell?
        if (grid.any { it == 0 }) return false

        // Check horizontal merges
        for (row in 0 until size) {
            for (col in 0 until size - 1) {
                if (grid[row * size + col] == grid[row * size + col + 1]) return false
            }
        }

        // Check vertical merges
        for (col in 0 until size) {
            for (row in 0 until size - 1) {
                if (grid[row * size + col] == grid[(row + 1) * size + col]) return false
            }
        }

        return true
    }

    private fun submitScore(score: Long) {
        val finalTime = Clock.System.now().toEpochMilliseconds() - gameStartTime
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.GAME_2048,
                difficulty = currentDifficulty,
                score = score,
                timeMillis = finalTime,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

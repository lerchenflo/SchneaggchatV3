package org.lerchenflo.schneaggchatv3mp.games.presentation.flappybird

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameDifficultySelection
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

data class PipePair(
    val id: Long,
    val x: Float,
    val topHeight: Float,
    val gapHeight: Float,
    val passed: Boolean = false,
)

data class FlappyBirdState(
    val birdY: Float = WORLD_HEIGHT / 2f,
    val birdVelocity: Float = 0f,
    val pipes: List<PipePair> = emptyList(),
    val score: Int = 0,
    val isGameStarted: Boolean = false,
    val isGameOver: Boolean = false,
    val elapsedMillis: Long = 0L,
) {
    companion object {
        const val WORLD_WIDTH = 360f
        const val WORLD_HEIGHT = 640f
        const val GROUND_Y = 560f
        const val BIRD_X = 80f
        const val BIRD_SIZE = 36f
        const val PIPE_WIDTH = 56f
    }
}

sealed class FlappyBirdAction {
    data object StartGame : FlappyBirdAction()
    data object Flap : FlappyBirdAction()
    data object ResetGame : FlappyBirdAction()
}

class FlappyBirdViewModel(
    private val gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FlappyBirdState())
    val state: StateFlow<FlappyBirdState> = _state.asStateFlow()

    private var gameLoopJob: Job? = null
    private var gameStartTime = 0L
    private var currentDifficulty = GameDifficulty.MEDIUM
    private var pipeIdCounter = 0L

    init {
        resetGame()
    }

    fun onAction(action: FlappyBirdAction) {
        when (action) {
            is FlappyBirdAction.StartGame -> startGame()
            is FlappyBirdAction.Flap -> flap()
            is FlappyBirdAction.ResetGame -> resetGame()
        }
    }

    private fun startGame() {
        if (_state.value.isGameStarted) return

        currentDifficulty = GameDifficultySelection.selected
        gameStartTime = Clock.System.now().toEpochMilliseconds()
        pipeIdCounter = 0L

        val initialPipes = listOf(
            createPipePair(startX = FlappyBirdState.WORLD_WIDTH + 100f)
        )

        _state.value = FlappyBirdState(
            birdY = FlappyBirdState.WORLD_HEIGHT / 2f - 40f,
            birdVelocity = getJumpImpulse(),
            pipes = initialPipes,
            score = 0,
            isGameStarted = true,
            isGameOver = false,
            elapsedMillis = 0L,
        )

        startGameLoop()
    }

    private fun flap() {
        val currentState = _state.value
        if (!currentState.isGameStarted || currentState.isGameOver) return

        _state.value = currentState.copy(
            birdVelocity = getJumpImpulse()
        )
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (isActive) {
                updatePhysics()
                delay(16.milliseconds) // ~60 FPS
            }
        }
    }

    private fun updatePhysics() {
        val currentState = _state.value
        if (!currentState.isGameStarted || currentState.isGameOver) return

        val updatedTime = Clock.System.now().toEpochMilliseconds() - gameStartTime
        val elapsedSeconds = updatedTime / 1000f

        val gravity = getGravity()
        val startSpeed = getBaseSpeed()
        // Gradually increase speed continuously over time and with score
        val currentSpeed = (startSpeed + (elapsedSeconds * 0.04f) + (currentState.score * 0.08f)).coerceAtMost(7.5f)
        val gapHeight = getGapHeight()

        val maxFallSpeed = 14f
        var newVelocity = (currentState.birdVelocity + gravity).coerceAtMost(maxFallSpeed)
        var newBirdY = currentState.birdY + newVelocity

        // Move pipes & update passed state
        var newScore = currentState.score
        val updatedPipes = mutableListOf<PipePair>()

        for (pipe in currentState.pipes) {
            val updatedX = pipe.x - currentSpeed
            var isPassed = pipe.passed

            if (!isPassed && updatedX + FlappyBirdState.PIPE_WIDTH < FlappyBirdState.BIRD_X) {
                isPassed = true
                newScore += 1
            }

            // Only keep pipes that haven't scrolled off the left edge completely
            if (updatedX + FlappyBirdState.PIPE_WIDTH > -20f) {
                updatedPipes.add(pipe.copy(x = updatedX, passed = isPassed))
            }
        }

        // Spawn new pipe when last pipe moves far enough
        val lastPipeX = updatedPipes.lastOrNull()?.x ?: 0f
        val pipeDistance = when (currentDifficulty) {
            GameDifficulty.LOW -> 220f
            GameDifficulty.MEDIUM -> 200f
            GameDifficulty.HIGH -> 180f
        }

        if (lastPipeX <= FlappyBirdState.WORLD_WIDTH - pipeDistance) {
            updatedPipes.add(createPipePair(startX = FlappyBirdState.WORLD_WIDTH, gapHeightOverride = gapHeight))
        }

        // Check side crash (hitting vertical wall of tower from side)
        val sideCrash = checkSideCollision(newBirdY, updatedPipes, currentSpeed)

        // Process resting on top of bottom tower or bumping bottom of top tower
        val birdLeft = FlappyBirdState.BIRD_X + 4f
        val birdRight = FlappyBirdState.BIRD_X + FlappyBirdState.BIRD_SIZE - 4f

        for (pipe in updatedPipes) {
            val pipeLeft = pipe.x
            val pipeRight = pipe.x + FlappyBirdState.PIPE_WIDTH
            val topPipeBottom = pipe.topHeight
            val bottomPipeTop = pipe.topHeight + pipe.gapHeight

            // Horizontally over tower
            if (birdRight > pipeLeft + 2f && birdLeft < pipeRight - 2f) {
                val birdBottom = newBirdY + FlappyBirdState.BIRD_SIZE - 4f
                val birdTop = newBirdY + 4f

                // Landing on top surface of bottom tower
                if (birdBottom >= bottomPipeTop && currentState.birdY + FlappyBirdState.BIRD_SIZE - 4f <= bottomPipeTop + 14f) {
                    newBirdY = bottomPipeTop - FlappyBirdState.BIRD_SIZE + 4f
                    newVelocity = 0f
                }
                // Bumping underside surface of top tower
                else if (birdTop <= topPipeBottom && currentState.birdY + 4f >= topPipeBottom - 14f) {
                    newBirdY = topPipeBottom - 4f
                    newVelocity = maxOf(newVelocity, 1f)
                }
            }
        }

        // Rest on ground or ceiling
        if (newBirdY + FlappyBirdState.BIRD_SIZE >= FlappyBirdState.GROUND_Y) {
            newBirdY = FlappyBirdState.GROUND_Y - FlappyBirdState.BIRD_SIZE
            newVelocity = 0f
        }
        if (newBirdY <= 0f) {
            newBirdY = 0f
            newVelocity = maxOf(newVelocity, 0f)
        }

        if (sideCrash) {
            _state.value = currentState.copy(
                birdY = newBirdY,
                birdVelocity = newVelocity,
                pipes = updatedPipes,
                score = newScore,
                isGameOver = true,
                elapsedMillis = updatedTime,
            )
            gameOver()
        } else {
            _state.value = currentState.copy(
                birdY = newBirdY,
                birdVelocity = newVelocity,
                pipes = updatedPipes,
                score = newScore,
                elapsedMillis = updatedTime,
            )
        }
    }

    private fun checkSideCollision(birdY: Float, pipes: List<PipePair>, currentSpeed: Float): Boolean {
        val birdLeft = FlappyBirdState.BIRD_X + 4f
        val birdRight = FlappyBirdState.BIRD_X + FlappyBirdState.BIRD_SIZE - 4f
        val birdTop = birdY + 4f
        val birdBottom = birdY + FlappyBirdState.BIRD_SIZE - 4f

        for (pipe in pipes) {
            val pipeLeft = pipe.x
            val topPipeBottom = pipe.topHeight
            val bottomPipeTop = pipe.topHeight + pipe.gapHeight

            // Check if front edge of snail collides into the left side wall of top or bottom tower
            val isHittingLeftSide = (birdRight >= pipeLeft && birdLeft < pipeLeft + currentSpeed + 4f)
            if (isHittingLeftSide) {
                // If snail's vertical body extends into top tower wall (above topPipeBottom) or bottom tower wall (below bottomPipeTop)
                val hitsTopSide = (birdTop < topPipeBottom - 4f)
                val hitsBottomSide = (birdBottom > bottomPipeTop + 4f)

                if (hitsTopSide || hitsBottomSide) {
                    return true
                }
            }
        }
        return false
    }

    private fun createPipePair(startX: Float, gapHeightOverride: Float? = null): PipePair {
        pipeIdCounter++
        val gap = gapHeightOverride ?: getGapHeight()
        val minTop = 60f
        val maxTop = FlappyBirdState.GROUND_Y - gap - 60f
        val topHeight = Random.nextFloat() * (maxTop - minTop) + minTop

        return PipePair(
            id = pipeIdCounter,
            x = startX,
            topHeight = topHeight,
            gapHeight = gap,
            passed = false,
        )
    }

    private fun gameOver() {
        gameLoopJob?.cancel()
        submitScore()
    }

    private fun submitScore() {
        val finalScore = _state.value.score.toLong()
        val finalTime = _state.value.elapsedMillis

        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.FLAPPYBIRD,
                difficulty = currentDifficulty,
                score = finalScore,
                timeMillis = finalTime,
            )
        }
    }

    private fun getGravity(): Float = when (currentDifficulty) {
        GameDifficulty.LOW -> 0.38f
        GameDifficulty.MEDIUM -> 0.44f
        GameDifficulty.HIGH -> 0.50f
    }

    private fun getJumpImpulse(): Float = when (currentDifficulty) {
        GameDifficulty.LOW -> -7.5f
        GameDifficulty.MEDIUM -> -8.2f
        GameDifficulty.HIGH -> -9.0f
    }

    private fun getBaseSpeed(): Float = when (currentDifficulty) {
        GameDifficulty.LOW -> 2.0f
        GameDifficulty.MEDIUM -> 2.5f
        GameDifficulty.HIGH -> 3.0f
    }

    private fun getGapHeight(): Float = when (currentDifficulty) {
        GameDifficulty.LOW -> 170f
        GameDifficulty.MEDIUM -> 145f
        GameDifficulty.HIGH -> 125f
    }

    private fun resetGame() {
        gameLoopJob?.cancel()
        _state.value = FlappyBirdState()
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}

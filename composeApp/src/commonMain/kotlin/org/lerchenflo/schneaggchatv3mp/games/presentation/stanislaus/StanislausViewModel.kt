package org.lerchenflo.schneaggchatv3mp.games.presentation.stanislaus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameDifficultySelection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Clock

private data class DifficultyConfig(
    val durationMillis: Long,
    val fishSpeed: Float,
    val minDepth: Float,
    val maxDepth: Float,
    val liveHint: Boolean,
    /** 0f keeps the fish on a straight line, >0f gives it a gentle up/down bob. */
    val fishBobAmplitude: Float,
)

private fun difficultyConfig(difficulty: GameDifficulty): DifficultyConfig = when (difficulty) {
    GameDifficulty.LOW -> DifficultyConfig(durationMillis = 75_000L, fishSpeed = 14f, minDepth = 10f, maxDepth = 45f, liveHint = true, fishBobAmplitude = 6f)
    GameDifficulty.MEDIUM -> DifficultyConfig(durationMillis = 60_000L, fishSpeed = 20f, minDepth = 8f, maxDepth = 55f, liveHint = false, fishBobAmplitude = 0f)
    GameDifficulty.HIGH -> DifficultyConfig(durationMillis = 45_000L, fishSpeed = 28f, minDepth = 6f, maxDepth = 65f, liveHint = false, fishBobAmplitude = 6f)
}

class StanislausViewModel(
    private val gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StanislausState())
    val state = _state.asStateFlow()

    private var loopJob: Job? = null
    private var runStartTime = 0L
    private var lastTickTime = 0L
    private var revealDeadline = 0L
    private var config = difficultyConfig(GameDifficultySelection.selected)
    private var currentDifficulty = GameDifficultySelection.selected

    fun onAction(action: StanislausAction) {
        when (action) {
            StanislausAction.StartGame -> startGame()
            StanislausAction.StopGame -> stopGame()
            StanislausAction.RestartGame -> startGame()
            is StanislausAction.Throw -> throwSpear(action.directionX, action.directionY)
        }
    }

    private fun startGame() {
        currentDifficulty = GameDifficultySelection.selected
        config = difficultyConfig(currentDifficulty)
        loopJob?.cancel()
        runStartTime = Clock.System.now().toEpochMilliseconds()
        lastTickTime = runStartTime
        _state.value = StanislausState(
            isPlaying = true,
            timeRemainingMillis = config.durationMillis,
            fish = spawnFish(),
            liveHint = config.liveHint,
        )
        startLoop()
    }

    /** Ends the current run without submitting a score and returns to the start screen. */
    private fun stopGame() {
        loopJob?.cancel()
        _state.value = StanislausState()
    }

    private fun startLoop() {
        loopJob = viewModelScope.launch {
            while (isActive) {
                delay(16L)
                val now = Clock.System.now().toEpochMilliseconds()
                val dtMillis = now - lastTickTime
                lastTickTime = now
                tick(now, dtMillis)
            }
        }
    }

    private fun throwSpear(directionX: Float, directionY: Float) {
        val current = _state.value
        if (!current.canAim) return
        val length = sqrt(directionX * directionX + directionY * directionY)
        if (length < 0.0001f) return
        _state.value = current.copy(
            spear = Spear(
                x = HAND_X,
                y = HAND_Y,
                directionX = directionX / length,
                directionY = directionY / length,
            )
        )
    }

    private fun tick(now: Long, dtMillis: Long) {
        val current = _state.value
        if (!current.isPlaying) return
        val dt = dtMillis / 1000f

        var fish = current.fish?.let { advanceFish(it, dt) }
        var spear = current.spear
        var reveal = current.reveal
        var score = current.score
        var streak = current.streak

        if (spear != null && fish != null) {
            val moved = advanceSpear(spear, dt)
            val hit = pointToSegmentDistance(
                Vec2(fish.x, fish.depth),
                Vec2(spear.x, spear.y),
                Vec2(moved.x, moved.y),
            ) <= CATCH_RADIUS
            val outOfBounds = moved.x < -10f || moved.x > WORLD_WIDTH + 10f ||
                moved.y > WATER_DEPTH + 10f || moved.y < -(AIR_HEIGHT + 10f)

            when {
                hit -> {
                    val sighting = sight(fish.x, fish.depth, STANI_X, EYE_HEIGHT)
                    score += computeScore(fish.x, fish.depth, sighting, streak)
                    streak += 1
                    reveal = ThrowReveal(sighting, fish.x, fish.depth, ThrowResult.HIT)
                    revealDeadline = now + REVEAL_DURATION_MILLIS
                    spear = null
                    fish = spawnFish()
                }
                outOfBounds -> {
                    val sighting = sight(fish.x, fish.depth, STANI_X, EYE_HEIGHT)
                    reveal = ThrowReveal(sighting, fish.x, fish.depth, ThrowResult.MISS)
                    revealDeadline = now + REVEAL_DURATION_MILLIS
                    streak = 0
                    runStartTime -= MISS_TIME_PENALTY_MILLIS
                    spear = null
                }
                else -> spear = moved
            }
        }

        if (reveal != null && now >= revealDeadline) {
            reveal = null
        }

        val elapsed = (now - runStartTime).coerceAtLeast(0L)
        val remaining = (config.durationMillis - elapsed).coerceAtLeast(0L)
        val liveSighting = fish?.let { sight(it.x, it.depth, STANI_X, EYE_HEIGHT) }

        _state.value = current.copy(
            fish = fish,
            spear = spear,
            reveal = reveal,
            score = score,
            streak = streak,
            elapsedMillis = elapsed,
            timeRemainingMillis = remaining,
            currentSighting = liveSighting,
        )

        if (remaining <= 0L) {
            endGame(score, elapsed)
        }
    }

    private fun advanceFish(fish: FishState, dt: Float): FishState {
        var x = fish.x + fish.directionX * fish.speed * dt
        var direction = fish.directionX
        if (x < POND_LEFT) {
            x = POND_LEFT
            direction = 1f
        } else if (x > POND_RIGHT) {
            x = POND_RIGHT
            direction = -1f
        }
        return fish.copy(x = x, directionX = direction, bobPhase = fish.bobPhase + dt * 1.4f)
    }

    private fun advanceSpear(spear: Spear, dt: Float): Spear {
        val speed = if (spear.y < 0f) AIR_SPEED else WATER_SPEED
        return spear.copy(
            x = spear.x + spear.directionX * speed * dt,
            y = spear.y + spear.directionY * speed * dt,
        )
    }

    private fun spawnFish(): FishState {
        val x = Random.nextFloat() * (POND_RIGHT - POND_LEFT) + POND_LEFT
        val baseDepth = Random.nextFloat() * (config.maxDepth - config.minDepth) + config.minDepth
        val direction = if (Random.nextBoolean()) 1f else -1f
        val speed = config.fishSpeed * (0.85f + Random.nextFloat() * 0.3f)
        val bobPhase = Random.nextFloat() * (2f * PI.toFloat())
        return FishState(
            x = x,
            baseDepth = baseDepth,
            directionX = direction,
            speed = speed,
            bobPhase = bobPhase,
            bobAmplitude = config.fishBobAmplitude,
        )
    }

    private fun computeScore(fishX: Float, fishDepth: Float, sighting: Sighting, streak: Int): Int {
        val depthBonus = (fishDepth * 2f).toInt()
        val displacementBonus = (abs(fishX - sighting.apparentX) * 3f).toInt()
        val streakMultiplier = 1f + 0.1f * streak.coerceAtMost(10)
        return ((BASE_CATCH_SCORE + depthBonus + displacementBonus) * streakMultiplier).toInt()
    }

    private fun endGame(finalScore: Int, finalElapsed: Long) {
        loopJob?.cancel()
        _state.value = _state.value.copy(isPlaying = false, isGameOver = true)
        submitScore(finalScore.toLong(), finalElapsed)
    }

    private fun submitScore(score: Long, timeMillis: Long) {
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.STANISLAUS,
                difficulty = currentDifficulty,
                score = score,
                timeMillis = timeMillis,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
    }
}

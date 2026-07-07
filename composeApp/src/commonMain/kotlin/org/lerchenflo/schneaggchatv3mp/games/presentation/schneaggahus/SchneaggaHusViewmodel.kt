package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

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
import kotlin.time.Clock

private const val SCORE_PER_DELIVERY = 10

class SchneaggaHusViewmodel(
    private val gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SchneaggaHusState())
    val state = _state.asStateFlow()

    private var gameLoopJob: Job? = null
    private var gameStartTime = 0L
    private var currentDifficulty = GameDifficulty.MEDIUM
    private var tilesPerSecond = 1.7f
    private var spawnIntervalMs = 2600L
    private var nextSchneaggId = 0

    fun onAction(action: SchneaggaHusAction) {
        when (action) {
            SchneaggaHusAction.StartGame -> startGame()
            SchneaggaHusAction.StopGame -> stopGame()
            SchneaggaHusAction.RestartGame -> startGame()
            is SchneaggaHusAction.OnSwitchClick -> toggleSwitch(action.position)
        }
    }

    private fun startGame() {
        currentDifficulty = GameDifficultySelection.selected
        tilesPerSecond = when (currentDifficulty) {
            GameDifficulty.LOW -> 1.2f
            GameDifficulty.MEDIUM -> 1.7f
            GameDifficulty.HIGH -> 2.3f
        }
        spawnIntervalMs = when (currentDifficulty) {
            GameDifficulty.LOW -> 3500L
            GameDifficulty.MEDIUM -> 2600L
            GameDifficulty.HIGH -> 1900L
        }
        gameStartTime = Clock.System.now().toEpochMilliseconds()
        nextSchneaggId = 0
        _state.value = SchneaggaHusState(isPlaying = true)
        startGameLoop()
    }

    /** Ends the current run without submitting a score and returns to the start screen. */
    private fun stopGame() {
        gameLoopJob?.cancel()
        _state.value = SchneaggaHusState()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTickTime = Clock.System.now().toEpochMilliseconds()
            var nextSpawnAt = 0L // first schneagg spawns immediately
            while (isActive) {
                delay(16L) // ~60 FPS
                val now = Clock.System.now().toEpochMilliseconds()
                val deltaTiles = tilesPerSecond * (now - lastTickTime) / 1000f
                lastTickTime = now
                val elapsedMillis = now - gameStartTime

                if (elapsedMillis >= nextSpawnAt) {
                    spawnSchneagg()
                    nextSpawnAt = elapsedMillis + currentSpawnInterval()
                }

                if (!moveSchneaggs(deltaTiles, elapsedMillis)) break
            }
        }
    }

    /** Spawns get gradually faster the longer the run lasts, down to 40% of the base interval. */
    private fun currentSpawnInterval(): Long {
        val factor = (1f - 0.03f * nextSchneaggId).coerceAtLeast(0.4f)
        return (spawnIntervalMs * factor).toLong()
    }

    private fun spawnSchneagg() {
        val schneagg = Schneagg(
            id = nextSchneaggId++,
            color = SCHNEAGGHUS_HOUSES.random().color,
            fromTile = SCHNEAGGHUS_SPAWN,
            toTile = SCHNEAGGHUS_FIRST_TRACK,
            progress = 0f,
        )
        _state.update { it.copy(schneaggList = it.schneaggList + schneagg) }
    }

    /** Advances all schneaggs by [deltaTiles] tile lengths; returns false when the run ended. */
    private fun moveSchneaggs(deltaTiles: Float, elapsedMillis: Long): Boolean {
        val current = _state.value
        var score = current.score
        var lives = current.lives
        val moved = mutableListOf<Schneagg>()

        for (schneagg in current.schneaggList) {
            var s = schneagg.copy(progress = schneagg.progress + deltaTiles)
            var removed = false
            // Turning happens exactly at tile centers, so fast schneaggs never skip a switch
            while (!removed && s.progress >= 1f) {
                val arrivedAt = s.toTile
                val house = current.schneagghusList.firstOrNull { it.position == arrivedAt }
                if (house != null) {
                    if (house.color == s.color) score += SCORE_PER_DELIVERY else lives--
                    removed = true
                } else {
                    val tile = current.trackList.firstOrNull { it.position == arrivedAt }
                    if (tile == null) {
                        removed = true // fell off the track, should not happen with a valid level
                    } else {
                        s = s.copy(
                            fromTile = arrivedAt,
                            toTile = arrivedAt.step(tile.exit),
                            progress = s.progress - 1f,
                        )
                    }
                }
            }
            if (!removed) moved += s
        }

        val gameOver = lives <= 0
        _state.update {
            it.copy(
                schneaggList = if (gameOver) emptyList() else moved,
                score = score,
                lives = lives.coerceAtLeast(0),
                elapsedMillis = elapsedMillis,
                isPlaying = !gameOver,
                isGameOver = gameOver,
            )
        }
        if (gameOver) {
            gameLoopJob?.cancel()
            submitScore(score, elapsedMillis)
        }
        return !gameOver
    }

    private fun toggleSwitch(position: Position) {
        _state.update { state ->
            state.copy(
                trackList = state.trackList.map { tile ->
                    if (tile.position == position && tile.isSwitch) {
                        tile.copy(activeExit = (tile.activeExit + 1) % tile.exits.size)
                    } else {
                        tile
                    }
                }
            )
        }
    }

    private fun submitScore(score: Int, timeMillis: Long) {
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.SCHNEAGGAHUS,
                difficulty = currentDifficulty,
                score = score.toLong(),
                timeMillis = timeMillis,
            )
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameDifficultySelection
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.games.presentation.awaitResume
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.Clock

private const val SCORE_PER_DELIVERY = 10
private const val MAX_WAVE_SNAILS = 20
private const val WAVE_INTERMISSION_MS = 1500L
/** Time to read a new map before its first schneagg leaves the tunnel. */
private const val WAVE_READ_TIME_MS = 900L
/** Schneaggs never spawn closer together than this many tile lengths of travel, so they do not stack in the tunnel. */
private const val MIN_SPAWN_GAP_TILES = 1.15f
/**
 * Rubber-band pacing: the spawn gap is the difficulty's base gap times [pace].
 * Every correct delivery tightens the pace, every wrong one loosens it again, so
 * a player on a roll gets more schneaggs at once and a struggling one gets air.
 */
private const val PACE_START = 1f
private const val PACE_FASTEST = 0.3f
private const val PACE_SLOWEST = 1.6f
private const val PACE_CORRECT_FACTOR = 0.93f
private const val PACE_WRONG_FACTOR = 1.25f
/** Each gap is scattered by up to this fraction so spawns never come at a fixed beat. */
private const val SPAWN_JITTER = 0.2f
/** How long the "Wave N" banner stays after a wave started. */
internal const val WAVE_BANNER_MS = 1200L
/** How long a delivery pulse is shown on its house. */
internal const val FEEDBACK_DURATION_MS = 900L

private fun waveSnailCount(wave: Int): Int = (8 + 2 * (wave - 1)).coerceAtMost(MAX_WAVE_SNAILS)

/** One more house every second wave, up to what the grid can hold. */
private fun waveHouseCount(difficulty: GameDifficulty, wave: Int): Int =
    (baseHouseCount(difficulty) + (wave - 1) / 2).coerceAtMost(maxHouseCount(difficulty))

private fun waveTilesPerSecond(difficulty: GameDifficulty, wave: Int): Float {
    val base = when (difficulty) {
        GameDifficulty.LOW -> 1.0f
        GameDifficulty.MEDIUM -> 1.4f
        GameDifficulty.HIGH -> 1.8f
    }
    return base * (1f + 0.07f * (wave - 1)).coerceAtMost(2f)
}

private fun baseSpawnGapMs(difficulty: GameDifficulty): Float = when (difficulty) {
    GameDifficulty.LOW -> 3000f
    GameDifficulty.MEDIUM -> 2400f
    GameDifficulty.HIGH -> 1900f
}

private fun scorePerDelivery(wave: Int): Int = SCORE_PER_DELIVERY * wave

private fun now(): Long = Clock.System.now().toEpochMilliseconds()

class SchneaggaHusViewmodel(
    private val gameHighscoreRepository: GameHighscoreRepository,
    gameSaveRepository: GameSaveRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(idleState(GameDifficultySelection.selected))
    val state = _state.asStateFlow()

    private val saveSession = GameSaveSession(
        game = GameId.SCHNEAGGAHUS,
        serializer = SchneaggaHusSnapshot.serializer(),
        schemaVersion = SCHNEAGGAHUS_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )
    /** The screen waits for this before auto-starting a run, so a restored one is never overwritten. */
    val restoreChecked = saveSession.restoreChecked

    private var gameLoopJob: Job? = null
    private var gameStartTime = 0L
    private var currentDifficulty = GameDifficulty.MEDIUM
    private var tilesPerSecond = waveTilesPerSecond(GameDifficulty.MEDIUM, 1)
    /** Spawn gap multiplier driven by how well the player is doing; carried across waves and saves. */
    private var pace = PACE_START
    private var nextSchneaggId = 0
    /** Run time (ms) at which the next schneagg spawns; kept outside the loop so it survives a restore. */
    private var nextSpawnAtElapsed = 0L

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = ::pauseAndPersist)
    }

    fun onAction(action: SchneaggaHusAction) {
        when (action) {
            SchneaggaHusAction.StartGame -> startGame()
            SchneaggaHusAction.StopGame -> stopGame()
            SchneaggaHusAction.RestartGame -> startGame()
            SchneaggaHusAction.TogglePause -> togglePause()
            SchneaggaHusAction.LeaveGame -> pauseAndPersist()
            is SchneaggaHusAction.OnSwitchClick -> toggleSwitch(action.position)
        }
    }

    /** Leaving the screen or backgrounding the app: freeze the run and keep it for the next visit. */
    private fun pauseAndPersist() {
        val current = _state.value
        if (current.isPlaying && !current.isGameOver) {
            _state.update { it.copy(isPaused = true) }
        }
        persist()
    }

    private fun persist() = saveSession.persist(currentDifficulty, snapshotOrNull())

    /** Null when there is no run worth keeping (not started or already over). */
    private fun snapshotOrNull(): SchneaggaHusSnapshot? {
        val current = _state.value
        if (!current.isPlaying || current.isGameOver) return null
        return SchneaggaHusSnapshot(
            gridWidth = current.gridWidth,
            gridHeight = current.gridHeight,
            spawn = current.spawn,
            trackList = current.trackList,
            houses = current.schneagghusList.map { it.toSnapshot() },
            schneaggs = current.schneaggList.map { it.toSnapshot() },
            score = current.score,
            lives = current.lives,
            elapsedMillis = current.elapsedMillis,
            nextSchneaggId = nextSchneaggId,
            nextSpawnAtElapsed = nextSpawnAtElapsed,
            pace = pace,
            wave = current.wave,
            waveSnailTotal = current.waveSnailTotal,
            waveDelivered = current.waveDelivered,
            upcomingArgb = current.upcoming.map { it.toArgb() },
            waveStartedAtElapsed = current.waveStartedAtElapsed,
            intermissionUntilElapsed = current.intermissionUntilElapsed,
        )
    }

    /** Brings a saved run back paused; the game loop parks in awaitResume until the user resumes. */
    private fun restore(save: GameSave<SchneaggaHusSnapshot>) {
        val data = save.data
        currentDifficulty = save.difficulty
        applyWaveTuning(data.wave)
        gameLoopJob?.cancel()
        nextSchneaggId = data.nextSchneaggId
        nextSpawnAtElapsed = data.nextSpawnAtElapsed
        pace = data.pace.coerceIn(PACE_FASTEST, PACE_SLOWEST)
        gameStartTime = now() - data.elapsedMillis
        _state.value = SchneaggaHusState(
            isPlaying = true,
            isGameOver = false,
            isPaused = true,
            score = data.score,
            lives = data.lives,
            elapsedMillis = data.elapsedMillis,
            gridWidth = data.gridWidth,
            gridHeight = data.gridHeight,
            spawn = data.spawn,
            schneaggList = data.schneaggs.map { it.toSchneagg() },
            schneagghusList = data.houses.map { it.toHouse() },
            trackList = data.trackList,
            wave = data.wave,
            waveSnailTotal = data.waveSnailTotal,
            waveDelivered = data.waveDelivered,
            upcoming = data.upcomingArgb.map { Color(it) },
            waveStartedAtElapsed = data.waveStartedAtElapsed,
            intermissionUntilElapsed = data.intermissionUntilElapsed,
        )
        startGameLoop()
    }

    private fun applyWaveTuning(wave: Int) {
        tilesPerSecond = waveTilesPerSecond(currentDifficulty, wave)
    }

    /**
     * Gap until the next spawn: base gap scaled by the current [pace], scattered a
     * little, and never shorter than the travel time that keeps schneaggs apart.
     */
    private fun nextSpawnGapMs(): Long {
        val jitter = 1f + (Random.nextFloat() * 2f - 1f) * SPAWN_JITTER
        val wanted = baseSpawnGapMs(currentDifficulty) * pace * jitter
        val floor = MIN_SPAWN_GAP_TILES / tilesPerSecond * 1000f
        return max(wanted, floor).toLong()
    }

    private fun adjustPace(correct: Boolean) {
        pace = (pace * if (correct) PACE_CORRECT_FACTOR else PACE_WRONG_FACTOR)
            .coerceIn(PACE_FASTEST, PACE_SLOWEST)
    }

    private fun togglePause() {
        val current = _state.value
        if (!current.isPlaying || current.isGameOver) return
        _state.update { it.copy(isPaused = !it.isPaused) }
    }

    private fun startGame() {
        currentDifficulty = GameDifficultySelection.selected
        gameStartTime = now()
        nextSchneaggId = 0
        pace = PACE_START
        // Every run gets a freshly generated map sized for the selected difficulty
        val map = generateSchneaggaHusMap(currentDifficulty, waveHouseCount(currentDifficulty, 1))
        _state.value = stateForMap(map).copy(isPlaying = true)
        beginWave(wave = 1, elapsedMillis = 0L)
        startGameLoop()
    }

    /** Ends the current run without submitting a score and returns to the start screen. */
    private fun stopGame() {
        gameLoopJob?.cancel()
        saveSession.clear()
        _state.value = idleState(GameDifficultySelection.selected)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTickTime = now()
            while (isActive) {
                val drift = awaitResume { _state.value.isPaused }
                if (drift > 0) {
                    gameStartTime += drift
                    lastTickTime += drift
                }

                delay(16L) // ~60 FPS
                val now = now()
                val deltaTiles = tilesPerSecond * (now - lastTickTime) / 1000f
                lastTickTime = now
                val elapsedMillis = now - gameStartTime

                val intermissionUntil = _state.value.intermissionUntilElapsed
                if (intermissionUntil != null) {
                    if (elapsedMillis >= intermissionUntil) {
                        beginWave(_state.value.wave + 1, elapsedMillis)
                    } else {
                        _state.update { it.copy(elapsedMillis = elapsedMillis, feedback = it.feedback.prune(elapsedMillis)) }
                    }
                    continue
                }

                if (_state.value.upcoming.isNotEmpty() && elapsedMillis >= nextSpawnAtElapsed) {
                    spawnSchneagg()
                    nextSpawnAtElapsed = elapsedMillis + nextSpawnGapMs()
                }

                if (!moveSchneaggs(deltaTiles, elapsedMillis)) break

                val after = _state.value
                if (after.upcoming.isEmpty() && after.schneaggList.isEmpty()) endWave(elapsedMillis)
            }
        }
    }

    /**
     * Wave done: the next map shows up right away under the "Wave N" banner, so the
     * player can read the new layout before its first schneagg leaves the tunnel.
     */
    private fun endWave(elapsedMillis: Long) {
        val nextWave = _state.value.wave + 1
        val map = generateSchneaggaHusMap(currentDifficulty, waveHouseCount(currentDifficulty, nextWave))
        _state.update {
            it.copy(
                gridWidth = map.gridWidth,
                gridHeight = map.gridHeight,
                spawn = map.spawn,
                trackList = map.trackList,
                schneagghusList = map.houseList,
                schneaggList = emptyList(),
                intermissionUntilElapsed = elapsedMillis + WAVE_INTERMISSION_MS,
            )
        }
    }

    /** Starts [wave] on the map currently in state: rolls its color queue and resets the wave counters. */
    private fun beginWave(wave: Int, elapsedMillis: Long) {
        applyWaveTuning(wave)
        val colors = _state.value.schneagghusList.map { it.color }
        val count = waveSnailCount(wave)
        // Every house gets at least one schneagg, the rest is random, then everything is shuffled
        val queue = (colors.take(count) + List((count - colors.size).coerceAtLeast(0)) { colors.random() }).shuffled()
        nextSpawnAtElapsed = elapsedMillis + WAVE_READ_TIME_MS
        _state.update {
            it.copy(
                schneaggList = emptyList(),
                wave = wave,
                waveSnailTotal = count,
                waveDelivered = 0,
                upcoming = queue,
                waveStartedAtElapsed = elapsedMillis,
                intermissionUntilElapsed = null,
                feedback = emptyList(),
                elapsedMillis = elapsedMillis,
            )
        }
    }

    private fun spawnSchneagg() {
        val current = _state.value
        val color = current.upcoming.firstOrNull() ?: return
        val schneagg = Schneagg(
            id = nextSchneaggId++,
            color = color,
            tile = current.spawn,
            entry = DIRECTION.NORTH,
            exit = DIRECTION.SOUTH,
            progress = 0f,
        )
        _state.update { it.copy(schneaggList = it.schneaggList + schneagg, upcoming = it.upcoming.drop(1)) }
    }

    /** Advances all schneaggs by [deltaTiles] tile lengths; returns false when the run ended. */
    private fun moveSchneaggs(deltaTiles: Float, elapsedMillis: Long): Boolean {
        val current = _state.value
        // Only needed when a schneagg crosses into another tile, so built lazily
        val tracks by lazy { current.trackList.associateBy { it.position } }
        val houses by lazy { current.schneagghusList.associateBy { it.position } }
        var score = current.score
        var lives = current.lives
        var delivered = 0
        val moved = ArrayList<Schneagg>(current.schneaggList.size)
        val newFeedback = mutableListOf<DeliveryFeedback>()

        fun deliver(house: Schneaggahus, schneagg: Schneagg) {
            val correct = house.color == schneagg.color
            val points = if (correct) scorePerDelivery(current.wave) else 0
            if (correct) score += points else lives--
            adjustPace(correct)
            delivered++
            newFeedback += DeliveryFeedback(house.position, correct, points, elapsedMillis)
        }

        for (schneagg in current.schneaggList) {
            var s = schneagg
            var length = pathLength(s.entry, s.exit)
            var progress = s.progress + deltaTiles / length
            var removed = false

            // Inside a house once past the tile center: delivered and gone
            val houseHere = houses[s.tile]
            if (houseHere != null && progress >= 0.5f) {
                deliver(houseHere, s)
                removed = true
            }

            // Tile changes happen exactly at the side middle, so fast schneaggs never skip a switch
            while (!removed && progress >= 1f) {
                val leftoverTiles = (progress - 1f) * length
                val next = s.tile.step(s.exit)
                val house = houses[next]
                if (house != null) {
                    // Crawls straight into the house; counts once the center is reached
                    s = s.copy(tile = next, entry = s.exit.opposite(), exit = s.exit)
                    length = 1f
                    progress = leftoverTiles
                    if (progress >= 0.5f) {
                        deliver(house, s)
                        removed = true
                    }
                } else {
                    val tile = tracks[next]
                    if (tile == null) {
                        removed = true // fell off the track, cannot happen on a valid map
                    } else {
                        // The exit is locked now; toggling the switch later does not affect this schneagg
                        val entry = s.exit.opposite()
                        s = s.copy(tile = next, entry = entry, exit = tile.exit)
                        length = pathLength(entry, tile.exit)
                        progress = leftoverTiles / length
                    }
                }
            }
            if (!removed) moved += s.copy(progress = progress)
        }

        val gameOver = lives <= 0
        val feedback = (current.feedback + newFeedback).prune(elapsedMillis)
        _state.update {
            it.copy(
                schneaggList = if (gameOver) emptyList() else moved,
                score = score,
                lives = lives.coerceAtLeast(0),
                elapsedMillis = elapsedMillis,
                waveDelivered = it.waveDelivered + delivered,
                feedback = feedback,
                isPlaying = !gameOver,
                isGameOver = gameOver,
            )
        }
        if (gameOver) {
            gameLoopJob?.cancel()
            saveSession.clear()
            submitScore(score, elapsedMillis)
        }
        return !gameOver
    }

    private fun toggleSwitch(position: Position) {
        val current = _state.value
        if (current.isPaused) return
        // A schneagg on the tile already committed to its exit; flipping the rails under it would look wrong
        if (current.schneaggList.any { it.tile == position }) return
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

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        // viewModelScope is already cancelled here; the write runs on the application scope
        persist()
    }
}

private fun List<DeliveryFeedback>.prune(elapsedMillis: Long): List<DeliveryFeedback> =
    filter { elapsedMillis - it.atElapsedMillis < FEEDBACK_DURATION_MS }

private fun stateForMap(map: SchneaggaHusMap) = SchneaggaHusState(
    gridWidth = map.gridWidth,
    gridHeight = map.gridHeight,
    spawn = map.spawn,
    schneagghusList = map.houseList,
    trackList = map.trackList,
)

/** Map shown behind the start screen before a run begins. */
private fun idleState(difficulty: GameDifficulty) = stateForMap(generateSchneaggaHusMap(difficulty))

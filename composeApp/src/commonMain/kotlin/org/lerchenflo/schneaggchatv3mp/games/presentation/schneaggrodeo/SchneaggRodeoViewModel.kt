package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.domain.LeaderboardPeriod
import org.lerchenflo.schneaggchatv3mp.games.domain.leaderboard
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoEvent
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SchneaggRodeoEngine
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.WORLD_HEIGHT_UNITS

class SchneaggRodeoViewModel(
    private val gameHighscoreRepository: GameHighscoreRepository,
    gameSaveRepository: GameSaveRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SchneaggRodeoState())
    val state: StateFlow<SchneaggRodeoState> = _state.asStateFlow()

    /**
     * The world, rebuilt every frame. Kept apart from [state] so the screen can read it in the draw
     * phase only: a frame then redraws the canvas without recomposing the HUD and controls.
     */
    private val _frame = MutableStateFlow(SchneaggRodeoFrame())
    val frame: StateFlow<SchneaggRodeoFrame> = _frame.asStateFlow()

    /** Profile pictures of the friends shown on the track, by user id. */
    private val _people = MutableStateFlow(RodeoPeopleUi())
    val people: StateFlow<RodeoPeopleUi> = _people.asStateFlow()

    private val saveSession = GameSaveSession(
        game = GameId.SCHNEAGG_RODEO,
        serializer = SchneaggRodeoSnapshot.serializer(),
        schemaVersion = SCHNEAGG_RODEO_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )
    /** The screen waits for this before auto-starting a run, so a restored one is never overwritten. */
    val restoreChecked = saveSession.restoreChecked

    private val engine = SchneaggRodeoEngine()

    /** All-time highscores of this game, lowest score first; empty while loading, offline or on failure. */
    private var ghosts: List<RodeoGhostUi> = emptyList()
    private var ghostsJob: Job? = null
    private var peopleJob: Job? = null

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = ::pauseAndPersist)
    }

    fun onAction(action: SchneaggRodeoAction) {
        when (action) {
            SchneaggRodeoAction.StartGame -> startGame()
            SchneaggRodeoAction.RestartGame -> startGame()
            SchneaggRodeoAction.StopGame -> stopGame()
            SchneaggRodeoAction.TogglePause -> togglePause()
            SchneaggRodeoAction.LeaveGame -> pauseAndPersist()
            is SchneaggRodeoAction.OnFrame -> onFrame(action.frameSeconds)
            is SchneaggRodeoAction.OnWorldSizeChanged -> onWorldSizeChanged(action.widthPx, action.heightPx)
            SchneaggRodeoAction.OnJumpPressed -> ifRiding { engine.jumpPressed() }
            SchneaggRodeoAction.OnJumpReleased -> engine.jumpReleased()
            SchneaggRodeoAction.OnDivePressed -> ifRiding { engine.divePressed() }
            SchneaggRodeoAction.OnDiveReleased -> engine.diveReleased()
            SchneaggRodeoAction.OnLassoClick -> ifRiding { engine.lassoPressed() }
            SchneaggRodeoAction.OnSuperJumpClick -> ifRiding {
                engine.superJumpPressed()
                publish()
            }
            SchneaggRodeoAction.OnRocketClick -> ifRiding {
                engine.rocketPressed()
                publish()
            }
        }
    }

    private inline fun ifRiding(block: () -> Unit) {
        val current = _state.value
        if (current.isPlaying && !current.isGameOver && !current.isPaused) block()
    }

    private fun startGame() {
        // Restarting from a paused run: a friend riding along still gets the score
        engine.leaveHorse()
        handleEvents()
        engine.reset()
        _state.value = SchneaggRodeoState(isPlaying = true)
        loadGhosts()
        publish()
    }

    private fun stopGame() {
        // Stopping leaves the horse too: a friend riding along gets the score so far
        engine.leaveHorse()
        handleEvents()
        ghostsJob?.cancel()
        saveSession.clear()
        engine.reset()
        ghosts = emptyList()
        engine.ghosts = emptyList()
        _state.value = SchneaggRodeoState()
        _frame.value = SchneaggRodeoFrame()
    }

    private fun togglePause() {
        val current = _state.value
        if (!current.isPlaying || current.isGameOver) return
        // A finger or key still down when pausing must not keep the jump boosted after resuming
        engine.jumpReleased()
        engine.diveReleased()
        // Resuming also dismisses the "friend joined" message
        _state.update { it.copy(isPaused = !it.isPaused, friendJoined = null) }
    }

    private fun onWorldSizeChanged(widthPx: Int, heightPx: Int) {
        val unitPx = heightPx / WORLD_HEIGHT_UNITS
        if (unitPx <= 0f) return
        engine.worldWidth = widthPx / unitPx
        publish()
    }

    private fun onFrame(frameSeconds: Float) {
        val current = _state.value
        if (!current.isPlaying || current.isGameOver || current.isPaused) return
        engine.step(frameSeconds)
        handleEvents()
        publish()
        if (engine.isCaught) gameOver()
    }

    /** Acts on what happened in the engine: friends getting on and off the horse. */
    private fun handleEvents() {
        engine.drainEvents().forEach { event ->
            when (event) {
                is RodeoEvent.FriendJoined -> {
                    // Paused so the player reads whose highscore the run raises from now on
                    engine.jumpReleased()
                    engine.diveReleased()
                    _state.update { it.copy(isPaused = true, friendJoined = event.friend.username) }
                }
                // Fire and forget: the server keeps the friend's best, a lower score changes nothing
                is RodeoEvent.FriendLeft -> viewModelScope.launch {
                    gameHighscoreRepository.submitFriendScore(
                        game = GameId.SCHNEAGG_RODEO,
                        difficulty = RODEO_BOARD,
                        friendId = event.friend.userId,
                        score = event.score,
                        timeMillis = event.timeMillis,
                    )
                }
            }
        }
    }

    /** Pushes the engine's current world and numbers to the screen. */
    private fun publish() {
        _frame.value = engine.toFrame()
        val score = engine.score
        _state.update {
            it.copy(
                score = score,
                // Whole seconds are all the HUD shows; finer steps would only recompose it every frame
                runTimeMillis = engine.runTimeSeconds.toLong() * 1000L,
                snailsCaught = engine.snailsCaught,
                superJumpCharges = engine.superJumpCharges,
                luckyCharms = engine.luckyCharms,
                isOnFoot = engine.isOnFoot,
                ride = engine.rideKind,
                rocketReady = engine.rocketReady,
                speedKmh = engine.speedKmh,
                announcement = engine.announcement,
                nextToBeat = ghosts.firstOrNull { ghost -> ghost.score > score },
            )
        }
    }

    private fun gameOver() {
        // Caught by the pack: a friend riding along gets the final score too
        engine.leaveHorse()
        handleEvents()
        saveSession.clear()
        val finalScore = engine.score.toLong()
        val finalTimeMillis = (engine.runTimeSeconds * 1000f).toLong()
        _state.update {
            it.copy(
                isPlaying = false,
                isGameOver = true,
                isPaused = false,
                runTimeMillis = finalTimeMillis,
            )
        }
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.SCHNEAGG_RODEO,
                difficulty = RODEO_BOARD,
                score = finalScore,
                timeMillis = finalTimeMillis,
            )
        }
    }

    /**
     * Fetches the all-time highscores once per run start, so they can stand on the track as markers
     * (and friends among them come riding along). Any failure (offline, server error) simply leaves
     * the run without markers. Loads the friends shown on the track as well.
     */
    private fun loadGhosts() {
        ghostsJob?.cancel()
        ghosts = emptyList()
        engine.ghosts = emptyList()
        loadPeople()
        ghostsJob = viewModelScope.launch {
            val result = gameHighscoreRepository.getHighscores(
                game = GameId.SCHNEAGG_RODEO,
                difficulty = RODEO_BOARD,
                period = LeaderboardPeriod.ALL_TIME,
            )
            if (result !is NetworkResult.Success) return@launch
            val ownUserId = SessionCache.requireLoggedIn()?.userId
            val friendIds = appRepository.getFriends("").map { it.id }.toSet()
            ghosts = result.data
                .filter { it.score > 0 }
                .map { entry ->
                    RodeoGhostUi(
                        username = entry.username,
                        score = entry.score,
                        isOwn = entry.userId == ownUserId,
                        userId = entry.userId,
                        isFriend = entry.userId in friendIds,
                    )
                }
                .sortedBy { it.score }
            engine.ghosts = ghosts
            publish()
        }
    }

    /**
     * Profile pictures of all friends, for friends on their own horse or riding along. Read from the
     * local cache only; missing ones fall back to a plain head.
     */
    private fun loadPeople() {
        peopleJob?.cancel()
        peopleJob = viewModelScope.launch {
            val picturePaths = appRepository.getFriends("").associate { it.id to it.profilePictureUrl }
            val pictures = withContext(Dispatchers.IO) {
                picturePaths.mapNotNull { (userId, path) -> loadPicture(path)?.let { userId to it } }.toMap()
            }
            _people.value = RodeoPeopleUi(pictures = pictures)
        }
    }

    private fun loadPicture(path: String): ImageBitmap? {
        if (path.isBlank()) return null
        return runCatching {
            SystemFileSystem.source(Path(path)).buffered().use { it.readByteArray() }.decodeToImageBitmap()
        }.getOrNull()
    }

    /** Leaving the screen or backgrounding the app: freeze the run and keep it for the next visit. */
    private fun pauseAndPersist() {
        val current = _state.value
        if (current.isPlaying && !current.isGameOver) {
            engine.jumpReleased()
            engine.diveReleased()
            _state.update { it.copy(isPaused = true) }
        }
        persist()
    }

    private fun persist() = saveSession.persist(snapshotOrNull())

    /** Null when there is no run worth keeping (not started, already over, or score is 0). */
    private fun snapshotOrNull(): SchneaggRodeoSnapshot? {
        val current = _state.value
        if (!current.isPlaying || current.isGameOver) return null
        if (engine.score == 0) return null
        return engine.toSnapshot()
    }

    /** Brings a saved run back paused; frames only arrive again once the user resumes. */
    private fun restore(save: GameSave<SchneaggRodeoSnapshot>) {
        engine.restore(save.data)
        _state.value = SchneaggRodeoState(isPlaying = true, isPaused = true)
        loadGhosts()
        publish()
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled here; the write runs on the application scope
        persist()
    }

    private companion object {
        /** The game has no difficulty setting, so everything goes to its single board. */
        val RODEO_BOARD = GameId.SCHNEAGG_RODEO.leaderboard.boards.first()
    }
}

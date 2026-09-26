package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.domain.dartCounterDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartGame
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartOutMode
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.findCheckouts
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadController
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadStatus
import kotlin.math.roundToLong

class DartCounterViewModel(
    gameSaveRepository: GameSaveRepository,
    gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DartCounterState())

    private val highscoreUpload = HighscoreUploadController(
        game = GameId.DART_COUNTER,
        repository = gameHighscoreRepository,
        scope = viewModelScope,
    )

    /** The leaderboard offer lives in its own holder, so it is folded into the screen state here. */
    val state = combine(_state, highscoreUpload.state) { state, upload ->
        state.copy(highscoreUpload = upload)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DartCounterState(),
    )

    /** The running leg. Mutable game internals never leave the ViewModel - the UI reads [state]. */
    private var game: DartGame? = null

    // Same order as the game's players; keeps the ids of platform users for the leaderboard upload
    private var gamePlayers: List<GamePlayer> = emptyList()

    private val saveSession = GameSaveSession(
        game = GameId.DART_COUNTER,
        serializer = DartCounterSnapshot.serializer(),
        schemaVersion = DART_COUNTER_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = ::persist)
    }

    fun onAction(action: DartCounterAction) {
        when (action) {
            DartCounterAction.OnAddPlayersClick -> _state.update { it.copy(showPlayerSetup = true) }
            DartCounterAction.OnPlayerSetupDismiss -> _state.update { it.copy(showPlayerSetup = false) }
            is DartCounterAction.OnPlayersSelected -> onPlayersSelected(action.players)

            DartCounterAction.OnConfigureGameClick -> _state.update { it.copy(showGameConfig = true) }
            DartCounterAction.OnGameConfigDismiss -> _state.update { it.copy(showGameConfig = false) }
            DartCounterAction.OnConfirmStartGame -> startGame()
            is DartCounterAction.OnCountdownSelect -> _state.update { it.copy(selectedCountdown = action.countdown) }
            is DartCounterAction.OnOutModeSelect -> _state.update { it.copy(selectedOutMode = action.mode) }

            DartCounterAction.OnStopGameClick -> _state.update { it.copy(showStopGameDialog = true) }
            DartCounterAction.OnStopGameDismiss -> _state.update { it.copy(showStopGameDialog = false) }
            DartCounterAction.OnConfirmStopGame -> stopGame()

            DartCounterAction.OnHighscoresClick -> _state.update { it.copy(showHighscores = true) }
            DartCounterAction.OnHighscoresDismiss -> _state.update { it.copy(showHighscores = false) }

            is DartCounterAction.OnMultiplierSelect ->
                _state.update { it.copy(selectedMultiplier = action.multiplier) }
            is DartCounterAction.OnDartThrow -> throwDart(action)
            DartCounterAction.OnUndoThrow -> undoLastThrow()

            DartCounterAction.OnUploadHighscores -> highscoreUpload.upload()
            DartCounterAction.OnDeclineHighscoreUpload -> highscoreUpload.decline()
        }
    }

    private fun onPlayersSelected(players: List<GamePlayer>) {
        gamePlayers = players
        _state.update {
            it.copy(playerNames = players.map { player -> player.name }, showPlayerSetup = false)
        }
    }

    private fun startGame() {
        val names = _state.value.playerNames
        if (names.isEmpty()) return
        highscoreUpload.reset()
        game = DartGame(
            outMode = _state.value.selectedOutMode,
            countdown = _state.value.selectedCountdown,
            playerNames = names,
        )
        _state.update {
            it.copy(
                gameStarted = true,
                showPlayerSetup = false,
                showGameConfig = false,
                selectedMultiplier = DartMultiplier.SINGLE,
            )
        }
        projectGame()
    }

    private fun stopGame() {
        saveSession.clear()
        highscoreUpload.reset()
        game = null
        gamePlayers = emptyList()
        _state.update {
            it.copy(
                gameStarted = false,
                gameOver = false,
                players = emptyList(),
                winnerNames = emptyList(),
                playerNames = emptyList(),
                turnDarts = emptyList(),
                checkout = emptyList(),
                alternativeCheckout = emptyList(),
                canUndo = false,
                showStopGameDialog = false,
                selectedMultiplier = DartMultiplier.SINGLE,
            )
        }
    }

    private fun throwDart(action: DartCounterAction.OnDartThrow) {
        val running = game ?: return
        running.throwDart(action.segment)
        // Every pad press falls back to single, so a double/triple is always a deliberate choice
        _state.update { it.copy(selectedMultiplier = DartMultiplier.SINGLE) }
        projectGame()
        if (running.gameOver) offerHighscoreUpload(running)
    }

    private fun undoLastThrow() {
        val running = game ?: return
        // The leg is already on the leaderboard - re-throwing the winning dart would upload it twice
        if (_state.value.highscoreUpload.status == HighscoreUploadStatus.UPLOADED) return
        if (!running.undoLastThrow()) return
        projectGame()
        // The finish was taken back, so there is no final result to upload anymore
        if (!running.gameOver) highscoreUpload.reset()
    }

    /** Copies everything the UI needs out of the mutable game into the immutable state. */
    private fun projectGame() {
        val running = game
        if (running == null) {
            _state.update { it.copy(players = emptyList(), gameOver = false) }
            return
        }
        val current = running.currentPlayer
        val dartsLeft = running.dartsLeft
        val checkouts =
            if (!running.gameOver && dartsLeft > 0) findCheckouts(current.score, dartsLeft, running.doubleOut)
            else emptyList()

        _state.update { state ->
            state.copy(
                gameOver = running.gameOver,
                players = running.players.mapIndexed { index, player ->
                    player.toDartPlayerUi(
                        countdown = running.countdown,
                        isCurrent = index == running.currentPlayerIndex && !running.gameOver,
                    )
                },
                winnerNames = running.winners.map { it.name },
                currentPlayerName = current.name,
                currentPlayerScore = current.score,
                currentPlayerIndex = running.currentPlayerIndex,
                dartsLeft = dartsLeft,
                turnDarts = running.dartsThisTurn,
                turnTotal = running.turnTotal,
                checkout = checkouts.firstOrNull().orEmpty(),
                alternativeCheckout = checkouts.getOrNull(1).orEmpty(),
                canUndo = running.canUndo,
            )
        }
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    /** Leaving the screen or backgrounding the app keeps the running leg for the next visit. */
    fun persist() = saveSession.persist(snapshotOrNull())

    /** Null when there is no leg worth keeping (not started or already over). */
    private fun snapshotOrNull(): DartCounterSnapshot? {
        val running = game ?: return null
        if (!_state.value.gameStarted || running.gameOver || running.players.isEmpty()) return null
        return running.toSnapshot(userIds = gamePlayers.map { it.userId }).toSnapshot()
    }

    private fun restore(save: GameSave<DartCounterSnapshot>) {
        val data = save.data
        if (data.players.isEmpty()) return
        val names = data.players.map { it.name }
        val restored = DartGame(
            outMode = if (data.doubleOut) DartOutMode.DOUBLE_OUT else DartOutMode.SINGLE_OUT,
            countdown = data.countdown,
            playerNames = names,
        )
        restored.restoreFrom(data.toSnapshotData())
        if (restored.gameOver) return

        game = restored
        gamePlayers = data.players.map { GamePlayer(name = it.name, userId = it.userId) }
        _state.update {
            it.copy(
                gameStarted = true,
                playerNames = names,
                selectedCountdown = data.countdown,
                selectedOutMode = if (data.doubleOut) DartOutMode.DOUBLE_OUT else DartOutMode.SINGLE_OUT,
            )
        }
        projectGame()
    }

    /**
     * Only a finished leg counts: each player's three-dart average over the whole leg (x100),
     * on the board of the leg's countdown. Averages during a running leg are never offered.
     */
    private fun offerHighscoreUpload(finished: DartGame) {
        val results = finished.players.mapIndexedNotNull { index, player ->
            if (player.totalDartsThrown == 0) return@mapIndexedNotNull null
            val average = finished.countdown * 300.0 / player.totalDartsThrown
            val gamePlayer = gamePlayers.getOrNull(index) ?: GamePlayer(name = player.name)
            gamePlayer to average.roundToLong()
        }
        highscoreUpload.offer(dartCounterDifficulty(finished.countdown), results)
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled here; the write runs on the application scope
        persist()
    }
}

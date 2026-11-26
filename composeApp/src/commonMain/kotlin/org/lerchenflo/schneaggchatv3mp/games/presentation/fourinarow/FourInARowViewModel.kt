package org.lerchenflo.schneaggchatv3mp.games.presentation.fourinarow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FOUR_IN_A_ROW_SNAPSHOT_VERSION
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FourInARowEngine
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FourInARowMove
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FourInARowSnapshot
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FourInARowSnapshotPlayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadController

/** The server's maxScore for FOUR_IN_A_ROW: one upload may carry at most this many wins per player. */
private const val MAX_WINS_PER_UPLOAD = 100

class FourInARowViewModel(
    gameSaveRepository: GameSaveRepository,
    gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val engine = FourInARowEngine()

    private val _state = MutableStateFlow(FourInARowState())

    private val highscoreUpload = HighscoreUploadController(
        game = GameId.FOUR_IN_A_ROW,
        repository = gameHighscoreRepository,
        scope = viewModelScope,
    )

    val state = combine(_state, highscoreUpload.state) { state, upload ->
        state.copy(highscoreUpload = upload)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FourInARowState(),
    )

    private val saveSession = GameSaveSession(
        game = GameId.FOUR_IN_A_ROW,
        serializer = FourInARowSnapshot.serializer(),
        schemaVersion = FOUR_IN_A_ROW_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )

    private val moveTimer = MoveTimer(
        scope = viewModelScope,
        onTick = { seconds -> _state.update { it.copy(secondsLeft = seconds) } },
        onTimeout = { applyMove(FourInARowMove.Skip) },
    )

    /** The move timer only runs while the screen is visible. */
    private var screenVisible = true

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = ::persist)
    }

    fun onAction(action: FourInARowAction) {
        when (action) {
            FourInARowAction.OnShowPlayerSelector -> _state.update { it.copy(showPlayerSelector = true) }
            FourInARowAction.OnHidePlayerSelector -> _state.update { it.copy(showPlayerSelector = false) }
            is FourInARowAction.OnPlayersSelected ->
                _state.update { it.copy(players = action.players, showPlayerSelector = false) }
            is FourInARowAction.OnRemovePlayer ->
                _state.update { it.copy(players = it.players.filterIndexed { index, _ -> index != action.index }) }
            is FourInARowAction.OnSetupChange -> _state.update { it.copy(setup = action.setup) }
            FourInARowAction.OnStartGame -> startGame()

            is FourInARowAction.OnColumnClick -> onColumnClick(action.column)
            is FourInARowAction.OnToggleTool -> toggleTool(action.tool)
            FourInARowAction.OnUndo -> undo()
            FourInARowAction.OnNextRound -> nextRound()
            FourInARowAction.OnFinishMatch -> finishMatch()
            FourInARowAction.OnResetGame -> resetGame()

            is FourInARowAction.OnScreenVisibleChange -> onScreenVisibleChange(action.visible)

            FourInARowAction.OnShowRules -> _state.update { it.copy(showRules = true) }
            FourInARowAction.OnHideRules -> _state.update { it.copy(showRules = false) }

            FourInARowAction.OnUploadHighscores -> highscoreUpload.upload()
            FourInARowAction.OnDeclineHighscoreUpload -> highscoreUpload.decline()
        }
    }

    // ─── Match ────────────────────────────────────────────────────────────────

    private fun startGame() {
        val current = _state.value
        if (!current.canStart) return
        highscoreUpload.reset()
        val config = current.setupConfig
        _state.update {
            it.copy(
                round = engine.newRound(config = config, startingPlayer = 0),
                previousRound = null,
                wins = List(config.sideCount) { 0 },
                roundNumber = 1,
                armedTool = null,
            )
        }
        persist()
        syncTimer()
    }

    private fun onColumnClick(column: Int) {
        val move = when (val tool = _state.value.armedTool) {
            is FourInARowTool.Power -> FourInARowMove.Drop(column = column, power = tool.power)
            FourInARowTool.PopOut -> FourInARowMove.PopOut(column)
            null -> FourInARowMove.Drop(column)
        }
        // A tap that is no valid target takes the armed tool back instead of doing nothing
        if (!applyMove(move) && _state.value.armedTool != null) {
            _state.update { it.copy(armedTool = null) }
        }
    }

    /** Applies [move] for the current player; false when it was not allowed. */
    private fun applyMove(move: FourInARowMove): Boolean {
        val round = _state.value.round ?: return false
        val next = engine.apply(round, move)
        if (next == round) return false

        val winner = next.winnerSide
        _state.update {
            it.copy(
                round = next,
                previousRound = round,
                armedTool = null,
                wins = if (winner != null) {
                    it.wins.mapIndexed { side, count -> if (side == winner) count + 1 else count }
                } else {
                    it.wins
                },
            )
        }
        persist()
        syncTimer()
        return true
    }

    private fun toggleTool(tool: FourInARowTool) {
        val round = _state.value.round ?: return
        val player = round.currentPlayer
        val allowed = when (tool) {
            is FourInARowTool.Power -> round.canUsePower(player, tool.power)
            FourInARowTool.PopOut -> round.canPopAny(player)
        }
        if (!allowed) return
        _state.update { it.copy(armedTool = if (it.armedTool == tool) null else tool) }
    }

    /** Takes back the last move; a finished round cannot be undone, its win already counts. */
    private fun undo() {
        val current = _state.value
        if (!current.canUndo) return
        _state.update {
            it.copy(
                // The undone turn was already shown; going back must not replay its animations
                round = it.previousRound?.withoutLastTurnEffects(),
                previousRound = null,
                armedTool = null,
            )
        }
        persist()
        syncTimer()
    }

    /** Same players and options; the loser of the last round starts. */
    private fun nextRound() {
        val current = _state.value
        val round = current.round ?: return
        if (!round.isOver || current.seriesWinner != null) return
        _state.update {
            it.copy(
                round = engine.newRound(config = round.config, startingPlayer = engine.nextStartingPlayer(round)),
                previousRound = null,
                roundNumber = it.roundNumber + 1,
                armedTool = null,
            )
        }
        persist()
        syncTimer()
    }

    /**
     * Ends the match after a round and goes back to the setup. The leaderboard is offered once
     * here: every player gets the wins of their side (their team's wins with teams).
     */
    private fun finishMatch() {
        val current = _state.value
        val round = current.round ?: return
        if (!round.isOver) return
        val results = current.players.mapIndexed { index, player ->
            player to current.wins.getOrElse(round.config.sideOf(index)) { 0 }
        }
            .filter { (_, wins) -> wins > 0 }
            .map { (player, wins) -> player to wins.coerceAtMost(MAX_WINS_PER_UPLOAD).toLong() }
        clearMatch()
        highscoreUpload.offer(GameDifficulty.MEDIUM, results)
    }

    private fun resetGame() {
        highscoreUpload.reset()
        clearMatch()
    }

    private fun clearMatch() {
        _state.update {
            it.copy(round = null, previousRound = null, wins = emptyList(), roundNumber = 1, armedTool = null)
        }
        saveSession.clear()
        syncTimer()
    }

    // ─── Move timer ───────────────────────────────────────────────────────────

    /** Starts a fresh countdown for the player whose turn it is, or stops the timer without a running round. */
    private fun syncTimer() {
        val round = _state.value.round
        val seconds = round?.config?.moveSeconds
        if (round == null || seconds == null || round.isOver) {
            moveTimer.stop()
            _state.update { it.copy(secondsLeft = null) }
            return
        }
        moveTimer.start(seconds = seconds, paused = !screenVisible)
    }

    private fun onScreenVisibleChange(visible: Boolean) {
        screenVisible = visible
        if (visible) moveTimer.resume() else moveTimer.pause()
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    fun persist() = saveSession.persist(snapshotOrNull())

    /** Null while setting up: there is no match worth keeping yet. */
    private fun snapshotOrNull(): FourInARowSnapshot? {
        val current = _state.value
        val round = current.round ?: return null
        return FourInARowSnapshot(
            players = current.players.map { FourInARowSnapshotPlayer(name = it.name, userId = it.userId) },
            wins = current.wins,
            roundNumber = current.roundNumber,
            bestOf = current.setup.bestOf,
            round = round,
        )
    }

    private fun restore(save: GameSave<FourInARowSnapshot>) {
        val data = save.data
        val config = data.round.config
        // A save that does not fit its own rules is useless; start from the setup instead
        if (data.players.size != config.playerCount || data.wins.size != config.sideCount) return
        _state.update {
            it.copy(
                players = data.players.map { player -> GamePlayer(name = player.name, userId = player.userId) },
                setup = FourInARowSetup(
                    teams = config.teams,
                    powerDiscs = config.powerDiscs,
                    boardTwist = config.boardTwist,
                    popOut = config.popOut,
                    moveSeconds = config.moveSeconds,
                    bestOf = data.bestOf,
                ),
                // The last turn was already shown; restoring must not replay its animations
                round = data.round.withoutLastTurnEffects(),
                wins = data.wins,
                roundNumber = data.roundNumber,
            )
        }
        syncTimer()
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.tictactoe

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
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.Cell
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TIC_TAC_TOE_SNAPSHOT_VERSION
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TicTacToeEngine
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TicTacToeMove
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TicTacToeSnapshot
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TicTacToeSnapshotPlayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadController

/** The server's maxScore for TIC_TAC_TOE: one upload may carry at most this many wins per player. */
private const val MAX_WINS_PER_UPLOAD = 100

class TicTacToeViewModel(
    gameSaveRepository: GameSaveRepository,
    gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val engine = TicTacToeEngine()

    private val _state = MutableStateFlow(TicTacToeState())

    private val highscoreUpload = HighscoreUploadController(
        game = GameId.TIC_TAC_TOE,
        repository = gameHighscoreRepository,
        scope = viewModelScope,
    )

    val state = combine(_state, highscoreUpload.state) { state, upload ->
        state.copy(highscoreUpload = upload)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TicTacToeState(),
    )

    private val saveSession = GameSaveSession(
        game = GameId.TIC_TAC_TOE,
        serializer = TicTacToeSnapshot.serializer(),
        schemaVersion = TIC_TAC_TOE_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = ::persist)
    }

    fun onAction(action: TicTacToeAction) {
        when (action) {
            TicTacToeAction.OnShowPlayerSelector -> _state.update { it.copy(showPlayerSelector = true) }
            TicTacToeAction.OnHidePlayerSelector -> _state.update { it.copy(showPlayerSelector = false) }
            is TicTacToeAction.OnPlayersSelected ->
                _state.update { it.copy(players = action.players, showPlayerSelector = false) }
            is TicTacToeAction.OnRemovePlayer ->
                _state.update { it.copy(players = it.players.filterIndexed { index, _ -> index != action.index }) }
            is TicTacToeAction.OnTwistsChange -> _state.update { it.copy(twists = action.twists) }
            TicTacToeAction.OnStartGame -> startGame()

            is TicTacToeAction.OnCellClick -> onCellClick(action.cell)
            TicTacToeAction.OnToggleBomb -> toggleBomb()
            TicTacToeAction.OnNextRound -> nextRound()
            TicTacToeAction.OnFinishMatch -> finishMatch()
            TicTacToeAction.OnResetGame -> resetGame()

            TicTacToeAction.OnShowRules -> _state.update { it.copy(showRules = true) }
            TicTacToeAction.OnHideRules -> _state.update { it.copy(showRules = false) }

            TicTacToeAction.OnUploadHighscores -> highscoreUpload.upload()
            TicTacToeAction.OnDeclineHighscoreUpload -> highscoreUpload.decline()
        }
    }

    // ─── Match ────────────────────────────────────────────────────────────────

    private fun startGame() {
        val current = _state.value
        if (!current.canStart) return
        highscoreUpload.reset()
        _state.update {
            it.copy(
                round = engine.newRound(config = it.setupConfig, startingPlayer = 0),
                wins = List(it.players.size) { 0 },
                roundNumber = 1,
                bombArmed = false,
            )
        }
        persist()
    }

    private fun onCellClick(cell: Cell) {
        val current = _state.value
        val round = current.round ?: return
        if (round.isOver) return

        val move = if (current.bombArmed) TicTacToeMove.Bomb(cell) else TicTacToeMove.Place(cell)
        val next = engine.apply(round, move)
        if (next == round) {
            // A tap that is no valid target takes the bomb back instead of doing nothing
            if (current.bombArmed) _state.update { it.copy(bombArmed = false) }
            return
        }

        val winner = next.winner
        _state.update {
            it.copy(
                round = next,
                bombArmed = false,
                wins = if (winner != null) {
                    it.wins.mapIndexed { index, count -> if (index == winner) count + 1 else count }
                } else {
                    it.wins
                },
            )
        }
        persist()
    }

    private fun toggleBomb() {
        val round = _state.value.round ?: return
        if (!round.canBomb(round.currentPlayer)) return
        _state.update { it.copy(bombArmed = !it.bombArmed) }
    }

    /** Same players and twists, the next player starts. */
    private fun nextRound() {
        val round = _state.value.round ?: return
        if (!round.isOver) return
        _state.update {
            it.copy(
                round = engine.newRound(
                    config = round.config,
                    startingPlayer = (round.startingPlayer + 1) % round.config.playerCount,
                ),
                roundNumber = it.roundNumber + 1,
                bombArmed = false,
            )
        }
        persist()
    }

    /**
     * Ends the match after a round and goes back to the setup. The leaderboard is offered once
     * here, with every player's wins of the whole match, instead of after every round.
     */
    private fun finishMatch() {
        val current = _state.value
        val round = current.round ?: return
        if (!round.isOver) return
        val results = current.players.zip(current.wins)
            .filter { (_, wins) -> wins > 0 }
            .map { (player, wins) -> player to wins.coerceAtMost(MAX_WINS_PER_UPLOAD).toLong() }
        _state.update {
            it.copy(round = null, wins = emptyList(), roundNumber = 1, bombArmed = false)
        }
        saveSession.clear()
        highscoreUpload.offer(GameDifficulty.MEDIUM, results)
    }

    private fun resetGame() {
        highscoreUpload.reset()
        _state.update {
            it.copy(round = null, wins = emptyList(), roundNumber = 1, bombArmed = false)
        }
        saveSession.clear()
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    fun persist() = saveSession.persist(snapshotOrNull())

    /** Null while setting up: there is no match worth keeping yet. */
    private fun snapshotOrNull(): TicTacToeSnapshot? {
        val current = _state.value
        val round = current.round ?: return null
        return TicTacToeSnapshot(
            players = current.players.map { TicTacToeSnapshotPlayer(name = it.name, userId = it.userId) },
            wins = current.wins,
            roundNumber = current.roundNumber,
            round = round,
        )
    }

    private fun restore(save: GameSave<TicTacToeSnapshot>) {
        val data = save.data
        // A save that does not fit its own rules is useless; start from the setup instead
        if (data.players.size != data.round.config.playerCount || data.wins.size != data.players.size) return
        val config = data.round.config
        _state.update {
            it.copy(
                players = data.players.map { player -> GamePlayer(name = player.name, userId = player.userId) },
                twists = TicTacToeTwists(
                    vanishing = config.vanishing,
                    gravity = config.gravity,
                    bombs = config.bombs,
                    chaos = config.chaos,
                ),
                // The last turn was already shown; restoring must not replay its animations
                round = data.round.copy(lastPlacedId = null, lastBombCell = null, lastShift = null),
                wins = data.wins,
                roundNumber = data.roundNumber,
            )
        }
    }
}

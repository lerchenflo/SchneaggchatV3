package org.lerchenflo.schneaggchatv3mp.games.presentation.tictactoe

import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.Cell
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TIC_TAC_TOE_MAX_PLAYERS
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TIC_TAC_TOE_MIN_PLAYERS
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TicTacToeConfig
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TicTacToeRound
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadState

data class TicTacToeTwists(
    val vanishing: Boolean = false,
    val gravity: Boolean = false,
    val bombs: Boolean = false,
    val chaos: Boolean = false,
)

data class TicTacToeState(
    val players: List<GamePlayer> = emptyList(),
    val twists: TicTacToeTwists = TicTacToeTwists(),
    /** null while setting up. */
    val round: TicTacToeRound? = null,
    val wins: List<Int> = emptyList(),
    val roundNumber: Int = 1,
    /** The current player is about to throw their bomb instead of placing a mark. */
    val bombArmed: Boolean = false,
    val showPlayerSelector: Boolean = false,
    val showRules: Boolean = false,
    val highscoreUpload: HighscoreUploadState = HighscoreUploadState(),
) {
    val canStart: Boolean get() = players.size in TIC_TAC_TOE_MIN_PLAYERS..TIC_TAC_TOE_MAX_PLAYERS

    /** The rules the setup would start with, for the board preview. */
    val setupConfig: TicTacToeConfig
        get() = TicTacToeConfig(
            playerCount = players.size.coerceIn(TIC_TAC_TOE_MIN_PLAYERS, TIC_TAC_TOE_MAX_PLAYERS),
            vanishing = twists.vanishing,
            gravity = twists.gravity,
            bombs = twists.bombs,
            chaos = twists.chaos,
        )
}

sealed interface TicTacToeAction {
    data object OnShowPlayerSelector : TicTacToeAction
    data object OnHidePlayerSelector : TicTacToeAction
    data class OnPlayersSelected(val players: List<GamePlayer>) : TicTacToeAction
    data class OnRemovePlayer(val index: Int) : TicTacToeAction
    data class OnTwistsChange(val twists: TicTacToeTwists) : TicTacToeAction
    data object OnStartGame : TicTacToeAction

    data class OnCellClick(val cell: Cell) : TicTacToeAction
    data object OnToggleBomb : TicTacToeAction
    data object OnNextRound : TicTacToeAction
    /** Ends the match after a round and offers everyone's wins for the leaderboard once. */
    data object OnFinishMatch : TicTacToeAction
    /** Back to the setup with the same players and twists; drops the saved match. */
    data object OnResetGame : TicTacToeAction

    data object OnShowRules : TicTacToeAction
    data object OnHideRules : TicTacToeAction

    data object OnUploadHighscores : TicTacToeAction
    data object OnDeclineHighscoreUpload : TicTacToeAction
}

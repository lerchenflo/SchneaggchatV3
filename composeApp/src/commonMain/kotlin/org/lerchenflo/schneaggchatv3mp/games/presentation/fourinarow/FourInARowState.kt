package org.lerchenflo.schneaggchatv3mp.games.presentation.fourinarow

import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FOUR_IN_A_ROW_MAX_PLAYERS
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FOUR_IN_A_ROW_MIN_PLAYERS
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FourInARowConfig
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FourInARowRound
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.PowerDisc
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadState

/** What the setup starts the match with. */
data class FourInARowSetup(
    val teams: Boolean = false,
    val powerDiscs: Boolean = false,
    val boardTwist: Boolean = false,
    val popOut: Boolean = false,
    /** Move timer in seconds, null without a timer. */
    val moveSeconds: Int? = null,
    /** Series length, null for an open match. */
    val bestOf: Int? = null,
)

/** What the current player's next column tap does instead of dropping a normal disc. */
sealed interface FourInARowTool {
    data class Power(val power: PowerDisc) : FourInARowTool
    data object PopOut : FourInARowTool
}

data class FourInARowState(
    val players: List<GamePlayer> = emptyList(),
    val setup: FourInARowSetup = FourInARowSetup(),
    /** null while setting up. */
    val round: FourInARowRound? = null,
    /** The round before the last move, for undo; null when there is nothing to take back. */
    val previousRound: FourInARowRound? = null,
    /** Wins per side: per player, or per team with teams. */
    val wins: List<Int> = emptyList(),
    val roundNumber: Int = 1,
    val armedTool: FourInARowTool? = null,
    /** Seconds left on the move timer, null without a timer or while no round runs. */
    val secondsLeft: Int? = null,
    val showPlayerSelector: Boolean = false,
    val showRules: Boolean = false,
    val highscoreUpload: HighscoreUploadState = HighscoreUploadState(),
) {
    val canStart: Boolean get() = players.size in FOUR_IN_A_ROW_MIN_PLAYERS..FOUR_IN_A_ROW_MAX_PLAYERS

    /** The rules the setup would start with, for the board info and the option texts. */
    val setupConfig: FourInARowConfig
        get() = FourInARowConfig(
            playerCount = players.size.coerceIn(FOUR_IN_A_ROW_MIN_PLAYERS, FOUR_IN_A_ROW_MAX_PLAYERS),
            teams = setup.teams,
            powerDiscs = setup.powerDiscs,
            boardTwist = setup.boardTwist,
            popOut = setup.popOut,
            moveSeconds = setup.moveSeconds,
        )

    /** The side that has won the series, null for an open match or while the series runs. */
    val seriesWinner: Int?
        get() {
            val bestOf = setup.bestOf ?: return null
            val needed = bestOf / 2 + 1
            return wins.indexOfFirst { it >= needed }.takeIf { it >= 0 }
        }

    val canUndo: Boolean get() = previousRound != null && round?.isOver == false
}

sealed interface FourInARowAction {
    data object OnShowPlayerSelector : FourInARowAction
    data object OnHidePlayerSelector : FourInARowAction
    data class OnPlayersSelected(val players: List<GamePlayer>) : FourInARowAction
    data class OnRemovePlayer(val index: Int) : FourInARowAction
    data class OnSetupChange(val setup: FourInARowSetup) : FourInARowAction
    data object OnStartGame : FourInARowAction

    data class OnColumnClick(val column: Int) : FourInARowAction
    /** Arms [tool] for the next column tap, or disarms it when it is already armed. */
    data class OnToggleTool(val tool: FourInARowTool) : FourInARowAction
    data object OnUndo : FourInARowAction
    data object OnNextRound : FourInARowAction
    /** Ends the match and offers everyone's wins for the leaderboard once. */
    data object OnFinishMatch : FourInARowAction
    /** Back to the setup with the same players and options; drops the saved match. */
    data object OnResetGame : FourInARowAction

    /** The screen is visible again or hidden: the move timer only runs while it is visible. */
    data class OnScreenVisibleChange(val visible: Boolean) : FourInARowAction

    data object OnShowRules : FourInARowAction
    data object OnHideRules : FourInARowAction

    data object OnUploadHighscores : FourInARowAction
    data object OnDeclineHighscoreUpload : FourInARowAction
}

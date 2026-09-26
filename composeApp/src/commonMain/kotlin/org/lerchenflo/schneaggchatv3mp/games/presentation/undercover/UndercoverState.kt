package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadState
import org.lerchenflo.schneaggchatv3mp.utilities.UiText

data class UndercoverState(
    val phase: UndercoverPhase = UndercoverPhase.SETUP,

    val setupPlayerNameInput: String = "",
    val setupPlayers: List<String> = emptyList(),
    // Setup player name -> platform user id, for players picked from friends / yourself
    val setupPlayerUserIds: Map<String, String> = emptyMap(),
    val setupMrWhiteCount: Int = 1,
    val setupUndercoverCount: Int = 1,
    /** False until the word list for the current language was loaded; no game can start before that. */
    val wordListReady: Boolean = false,

    val autoHideEnabled: Boolean = false,
    val autoHideSeconds: Int = 5,
    val mrWhiteTipEnabled: Boolean = false,

    val players: List<UndercoverPlayer> = emptyList(),

    val currentRevealIndex: Int = 0,
    /** The word the player in front of the screen is allowed to see, null for Mr. White. */
    val revealWord: String? = null,
    val revealMrWhiteTip: String? = null,

    val selectedStarterPlayerId: String? = null,
    val selectedStarterName: String? = null,

    val votingSelectedPlayerId: String? = null,
    val votingResult: UndercoverVotingResult? = null,

    val mrWhiteGuessInput: String = "",
    val mrWhiteGuessWasCorrect: Boolean? = null,

    val winnerText: UiText? = null,

    val showRulesDialog: Boolean = false,
    val showPlayerSelector: Boolean = false,

    val sniffStep: SniffStep = SniffStep.CLOSED,
    val sniffPlayerId: String? = null,
    /** Word and tip of the sniffing player, only filled once they confirmed their identity. */
    val sniffWord: String? = null,
    val sniffMrWhiteTip: String? = null,

    val highscoreUpload: HighscoreUploadState = HighscoreUploadState(),
) {
    val currentRevealPlayer: UndercoverPlayer? get() = players.getOrNull(currentRevealIndex)

    val currentRevealPlayerName: String get() = currentRevealPlayer?.name.orEmpty()

    val alivePlayers: List<UndercoverPlayer> get() = players.filter { it.isAlive }

    val votingCandidates: List<UndercoverPlayer> get() = alivePlayers

    /** Mr. White never starts the round, they would have nothing to describe. */
    val starterCandidates: List<UndercoverPlayer>
        get() = players.filter { it.isAlive && it.actualRole != UndercoverRole.MR_WHITE }

    val sniffCandidates: List<UndercoverPlayer> get() = alivePlayers

    val sniffPlayer: UndercoverPlayer? get() = players.firstOrNull { it.id == sniffPlayerId }

    /** Sniffing is possible once everyone got their word, until the game is decided. */
    val canSniff: Boolean
        get() = when (phase) {
            UndercoverPhase.CHOOSE_STARTER, UndercoverPhase.DISCUSSION -> true
            UndercoverPhase.VOTING -> votingResult == null
            else -> false
        }

    /** At least three players, and the special roles must never take up every seat. */
    val canStartGame: Boolean
        get() {
            val playerCount = setupPlayers.size
            val special = setupMrWhiteCount + setupUndercoverCount
            return wordListReady &&
                playerCount >= 3 &&
                special < playerCount &&
                setupMrWhiteCount >= 0 &&
                setupUndercoverCount >= 0
        }
}

sealed interface UndercoverAction {
    // Setup
    data object OnShowPlayerSelector : UndercoverAction
    data object OnHidePlayerSelector : UndercoverAction
    data class OnPlayersSelected(val players: List<GamePlayer>) : UndercoverAction
    data class OnRemoveSetupPlayer(val name: String) : UndercoverAction
    data object OnIncrementMrWhiteCount : UndercoverAction
    data object OnDecrementMrWhiteCount : UndercoverAction
    data object OnIncrementUndercoverCount : UndercoverAction
    data object OnDecrementUndercoverCount : UndercoverAction
    data class OnToggleAutoHide(val enabled: Boolean) : UndercoverAction
    data object OnIncrementAutoHideSeconds : UndercoverAction
    data object OnDecrementAutoHideSeconds : UndercoverAction
    data class OnToggleMrWhiteTip(val enabled: Boolean) : UndercoverAction
    data object OnShowRules : UndercoverAction
    data object OnHideRules : UndercoverAction
    data object OnStartGame : UndercoverAction

    // Round
    data object OnConfirmPlayerIdentity : UndercoverAction
    data object OnHideAndPassPhone : UndercoverAction
    data object OnPickRandomStarter : UndercoverAction
    data object OnConfirmStarter : UndercoverAction
    data object OnStartVoting : UndercoverAction
    data class OnSelectVote(val playerId: String) : UndercoverAction
    data object OnConfirmVote : UndercoverAction
    data object OnContinueAfterVotingResult : UndercoverAction
    data class OnMrWhiteGuessChange(val guess: String) : UndercoverAction
    data object OnSubmitMrWhiteGuess : UndercoverAction
    data object OnRestartWithSamePlayers : UndercoverAction
    data object OnResetGame : UndercoverAction

    // Sniff
    data object OnOpenSniff : UndercoverAction
    data class OnSelectSniffPlayer(val playerId: String) : UndercoverAction
    data object OnConfirmSniffIdentity : UndercoverAction
    data object OnCloseSniff : UndercoverAction

    // Leaderboard
    data object OnUploadHighscores : UndercoverAction
    data object OnDeclineHighscoreUpload : UndercoverAction
}

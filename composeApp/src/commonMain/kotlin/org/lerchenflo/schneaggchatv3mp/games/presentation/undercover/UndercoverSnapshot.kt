package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import kotlinx.serialization.Serializable

const val UNDERCOVER_SNAPSHOT_VERSION = 1

@Serializable
data class UndercoverWordPairSnapshot(
    val civilianWord: String,
    val undercoverWord: String,
    val mrWhiteTip: String,
)

@Serializable
data class UndercoverPlayerSnapshot(
    val id: String,
    val name: String,
    val actualRole: UndercoverViewModel.ActualRole,
    val isAlive: Boolean,
    val userId: String? = null,
)

@Serializable
data class UndercoverVotingResultSnapshot(
    val eliminatedPlayerId: String,
    val eliminatedPlayerName: String,
    val revealedRole: UndercoverViewModel.ActualRole,
)

/** Persisted mid-game Undercover state; the winner text only exists once the game is over, which is never saved. */
@Serializable
data class UndercoverSnapshot(
    val phase: UndercoverViewModel.Phase,
    val setupPlayers: List<String>,
    val setupPlayerUserIds: Map<String, String> = emptyMap(),
    val setupMrWhiteCount: Int,
    val setupUndercoverCount: Int,
    val autoHideEnabled: Boolean,
    val autoHideSeconds: Int,
    val mrWhiteTipEnabled: Boolean,
    val wordPair: UndercoverWordPairSnapshot,
    val players: List<UndercoverPlayerSnapshot>,
    val currentRevealIndex: Int,
    val selectedStarterPlayerId: String?,
    val votingSelectedPlayerId: String?,
    val votingResult: UndercoverVotingResultSnapshot?,
    val mrWhiteGuessInput: String,
    val mrWhiteGuessWasCorrect: Boolean?,
)

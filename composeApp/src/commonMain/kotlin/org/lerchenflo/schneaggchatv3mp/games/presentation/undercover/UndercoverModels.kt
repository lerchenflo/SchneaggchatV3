package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import kotlinx.serialization.Serializable

/** The role a player really has; only that player ever gets to see it. */
@Serializable
enum class UndercoverRole {
    CIVILIAN,
    UNDERCOVER,
    MR_WHITE
}

/** Where a round currently stands. */
@Serializable
enum class UndercoverPhase {
    SETUP,
    PASS_PHONE,
    REVEAL,
    CHOOSE_STARTER,
    DISCUSSION,
    VOTING,
    MR_WHITE_GUESS,
    GAME_OVER
}

/** Steps of re-checking one's own word mid-game; never persisted, so a restored game never shows a word. */
enum class SniffStep {
    CLOSED,
    SELECT_PLAYER,
    CONFIRM_IDENTITY,
    REVEAL
}

data class UndercoverPlayer(
    val id: String,
    val name: String,
    val actualRole: UndercoverRole,
    val isAlive: Boolean,
    // Set for platform users; wins are uploaded to this account, never to the device owner's
    val userId: String? = null,
)

data class UndercoverVotingResult(
    val eliminatedPlayerId: String,
    val eliminatedPlayerName: String,
    val revealedRole: UndercoverRole,
)

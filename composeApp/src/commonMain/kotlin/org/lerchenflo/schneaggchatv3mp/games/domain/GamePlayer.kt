package org.lerchenflo.schneaggchatv3mp.games.domain

/**
 * A participant of a shared-device game. [userId] is set for platform users (yourself and friends),
 * whose results can be uploaded to the leaderboard; players added by name only have none.
 */
data class GamePlayer(
    val name: String,
    val userId: String? = null,
)

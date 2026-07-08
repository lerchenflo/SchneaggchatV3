package org.lerchenflo.schneaggchatv3mp.games.domain

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.HighscoreEntryResponse

/**
 * Games with a server-side leaderboard. The name must match the server's Game enum.
 */
enum class GameId {
    TETRIS,
    TOWERSTACK,
    MORSE,
    SCHNEAGGAHUS,
    GRIDRUSH,
}

/**
 * Game difficulty. The name must match the server's Difficulty enum.
 */
enum class GameDifficulty {
    LOW,
    MEDIUM,
    HIGH,
}

data class HighscoreEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val score: Long,
    val timeMillis: Long,
    val achievedAt: Long,
)

fun HighscoreEntryResponse.toHighscoreEntry(): HighscoreEntry = HighscoreEntry(
    rank = rank,
    userId = userId,
    username = username,
    score = score,
    timeMillis = timeMillis,
    achievedAt = achievedAt,
)

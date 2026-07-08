package org.lerchenflo.schneaggchatv3mp.games.domain

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.HighscoreEntryResponse

/**
 * Games with a server-side leaderboard. The name must match the server's Game enum.
 * [daily] marks games with a new board every day.
 */
enum class GameId(val daily: Boolean = false) {
    TETRIS,
    TOWERSTACK,
    MORSE,
    SCHNEAGGAHUS,
    GRIDRUSH(daily = true),
}

/**
 * Game difficulty. The name must match the server's Difficulty enum.
 */
enum class GameDifficulty {
    LOW,
    MEDIUM,
    HIGH,
}

/**
 * Time window a leaderboard is ranked over (calendar-based, not rolling).
 * The name must match the server's LeaderboardPeriod enum.
 */
enum class LeaderboardPeriod {
    DAILY,
    WEEKLY,
    YEARLY,
    ALL_TIME,
}

/** Daily games default to the daily leaderboard, everything else to the current year. */
val GameId.defaultLeaderboardPeriod: LeaderboardPeriod
    get() = if (daily) LeaderboardPeriod.DAILY else LeaderboardPeriod.YEARLY

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

package org.lerchenflo.schneaggchatv3mp.games.domain

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.GlobalRankingEntryResponse
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
    ODDONEOUT,
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

/**
 * One row of the cross-game leaderboard. [points] is the sum of percentile points the user
 * scored over every (game, difficulty) board they played — up to 100 per board.
 */
data class GlobalRankingEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val points: Long,
    val boardsPlayed: Int,
    val gamesPlayed: Int,
)

fun GlobalRankingEntryResponse.toGlobalRankingEntry(): GlobalRankingEntry = GlobalRankingEntry(
    rank = rank,
    userId = userId,
    username = username,
    points = points,
    boardsPlayed = boardsPlayed,
    gamesPlayed = gamesPlayed,
)

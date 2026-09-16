package org.lerchenflo.schneaggchatv3mp.games.domain

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.GlobalRankingEntryResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.HighscoreEntryResponse

/**
 * Games with a server-side leaderboard. The name must match the server's Game enum.
 * [daily] marks games with a new board every day. [indev] marks games still under development -
 * their scores are never submitted to the server (see GameHighscoreRepository.submitScore).
 * [sharedDevice] marks games several people play on one phone: all players' results are uploaded
 * together (GameHighscoreRepository.submitBatchScores) and the server keeps them out of the global ranking.
 */
enum class GameId(
    override val daily: Boolean = false,
    val indev: Boolean = false,
    val sharedDevice: Boolean = false,
) : GameSaveSlot {
    TETRIS,
    TOWERSTACK,
    MORSE,
    SCHNEAGGAHUS,
    GRIDRUSH(daily = true),
    ODDONEOUT,
    GAME_2048,
    CROSSWORD(daily = true),
    // Final score of a finished game
    YATZI(sharedDevice = true),
    // Three-dart average x100 of a finished game; difficulty encodes the countdown (see dartCounterDifficulty)
    DART_COUNTER(sharedDevice = true),
    // Every submission is one win (score = 1); the server ranks the sum of wins (see countsWins)
    UNDERCOVER(sharedDevice = true);

    override val saveKey: String get() = name.lowercase()
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

/** Leaderboards a game has, one per difficulty; shared-device games only use the ones they submit to. */
val GameId.leaderboardDifficulties: List<GameDifficulty>
    get() = when (this) {
        GameId.YATZI, GameId.UNDERCOVER -> listOf(GameDifficulty.MEDIUM)
        GameId.DART_COUNTER -> listOf(GameDifficulty.LOW, GameDifficulty.HIGH)
        else -> GameDifficulty.entries
    }

/** Win-counting games: each upload adds one win and the leaderboard shows the total number of wins. */
val GameId.countsWins: Boolean
    get() = this == GameId.UNDERCOVER

/** Shared-device games have no timer, their submitted time is always 0. */
val GameId.hasTimedScores: Boolean
    get() = !sharedDevice

/** Dart Counter boards: LOW = 301, HIGH = 501. */
fun dartCounterDifficulty(countdown: Int): GameDifficulty =
    if (countdown <= 301) GameDifficulty.LOW else GameDifficulty.HIGH

fun dartCounterCountdown(difficulty: GameDifficulty): Int =
    if (difficulty == GameDifficulty.LOW) 301 else 501

/** Human readable score; the Dart Counter stores its three-dart average x100. */
fun GameId.formatScore(score: Long): String = when (this) {
    GameId.DART_COUNTER -> "${score / 100}.${(score % 100).toString().padStart(2, '0')}"
    else -> score.toString()
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

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
    WORDLE,
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

/** What a game's submitted score means, and therefore how it is ranked and displayed. */
enum class ScoreKind {
    /** Plain points, higher is better; the time is only a tiebreaker. */
    POINTS,

    /** A race: every submission has score 0 and the board is ranked purely by time. */
    TIME_RACE,

    /** Three-dart average, stored as an integer x100. */
    AVERAGE_X100,

    /** Every submission is one win; the server ranks the sum per player. */
    WIN_COUNT,

    /**
     * Wordle: the number of letters typed to solve it, so FEWER is better and the server ranks
     * the board ascending. Shown as the number of guesses.
     */
    WORDLE_LETTERS,
}

/**
 * What a board's difficulty field actually selects. Several games reuse it for something that is
 * not a difficulty at all, which is why the leaderboard must never label it blindly.
 */
enum class BoardAxis {
    /** The real game difficulty. */
    DIFFICULTY,

    /** Crossword: LOW = German, HIGH = English. */
    LANGUAGE,

    /** Dart Counter: LOW = 301, HIGH = 501. */
    DART_COUNTDOWN,

    /** One single board, the difficulty is unused. */
    NONE,
}

/**
 * Everything the leaderboard UI needs about one game: what its score means, what its boards are
 * keyed by, and which boards actually receive submissions. Adding a game is one line in
 * [GameId.leaderboard] - the dialog derives its chips, columns and number formatting from here,
 * so there is no second place that can be forgotten.
 */
data class LeaderboardSpec(
    val scoreKind: ScoreKind,
    val boardAxis: BoardAxis,
    val boards: List<GameDifficulty>,
    /**
     * Whether the board has a time worth a column. Shared-device games are uploaded without one,
     * so a points game played on one phone has to say so explicitly or every row reads 00:00.
     */
    val showTime: Boolean = scoreKind == ScoreKind.POINTS || scoreKind == ScoreKind.TIME_RACE,
) {
    /** A race has nothing to put in a score column - every entry is 0. */
    val showScore: Boolean get() = scoreKind != ScoreKind.TIME_RACE


    /** Win-counting games show a total number of wins instead of a best result. */
    val countsWins: Boolean get() = scoreKind == ScoreKind.WIN_COUNT

    /** False where a smaller score is the better result; must match the server's higherScoreWins. */
    val higherScoreWins: Boolean get() = scoreKind != ScoreKind.WORDLE_LETTERS

    fun formatScore(score: Long): String = when (scoreKind) {
        ScoreKind.AVERAGE_X100 -> "${score / 100}.${(score % 100).toString().padStart(2, '0')}"
        // The stored score is letters typed; a player thinks in guesses
        ScoreKind.WORDLE_LETTERS -> (score / WORDLE_WORD_LENGTH).toString()
        else -> score.toString()
    }
}

val GameId.leaderboard: LeaderboardSpec
    get() = when (this) {
        // Score is how many letters the grid took to fill, the solve time ranks equal grids
        GameId.CROSSWORD -> LeaderboardSpec(
            scoreKind = ScoreKind.POINTS,
            boardAxis = BoardAxis.LANGUAGE,
            boards = listOf(GameDifficulty.LOW, GameDifficulty.HIGH),
        )
        GameId.WORDLE -> LeaderboardSpec(
            scoreKind = ScoreKind.WORDLE_LETTERS,
            boardAxis = BoardAxis.LANGUAGE,
            boards = listOf(GameDifficulty.LOW, GameDifficulty.HIGH),
        )
        GameId.DART_COUNTER -> LeaderboardSpec(
            scoreKind = ScoreKind.AVERAGE_X100,
            boardAxis = BoardAxis.DART_COUNTDOWN,
            boards = listOf(GameDifficulty.LOW, GameDifficulty.HIGH),
        )
        GameId.UNDERCOVER -> LeaderboardSpec(
            scoreKind = ScoreKind.WIN_COUNT,
            boardAxis = BoardAxis.NONE,
            boards = listOf(GameDifficulty.MEDIUM),
        )
        // Points like any other game, but played on one phone and therefore never timed
        GameId.YATZI -> LeaderboardSpec(
            scoreKind = ScoreKind.POINTS,
            boardAxis = BoardAxis.NONE,
            boards = listOf(GameDifficulty.MEDIUM),
            showTime = false,
        )
        else -> LeaderboardSpec(
            scoreKind = ScoreKind.POINTS,
            boardAxis = BoardAxis.DIFFICULTY,
            boards = GameDifficulty.entries,
        )
    }

/** Only games with a fresh board every day have a meaningful daily leaderboard. */
val GameId.leaderboardPeriods: List<LeaderboardPeriod>
    get() = if (daily) LeaderboardPeriod.entries else LeaderboardPeriod.entries - LeaderboardPeriod.DAILY

/** Dart Counter boards: LOW = 301, HIGH = 501. */
fun dartCounterDifficulty(countdown: Int): GameDifficulty =
    if (countdown <= 301) GameDifficulty.LOW else GameDifficulty.HIGH

fun dartCounterCountdown(difficulty: GameDifficulty): Int =
    if (difficulty == GameDifficulty.LOW) 301 else 501

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

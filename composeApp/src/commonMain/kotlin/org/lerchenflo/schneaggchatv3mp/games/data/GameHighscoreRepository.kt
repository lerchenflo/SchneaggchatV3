package org.lerchenflo.schneaggchatv3mp.games.data

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.asEmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.map
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.BatchScoreEntryRequest
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.SubmitGameScoreRequest
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.SubmitGameScoresBatchRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GlobalRankingEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.HighscoreEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.LeaderboardPeriod
import org.lerchenflo.schneaggchatv3mp.games.domain.toGlobalRankingEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.toHighscoreEntry
import org.lerchenflo.schneaggchatv3mp.games.data.remote.GameRemoteDataSource

class GameHighscoreRepository(
    private val gameRemoteDataSource: GameRemoteDataSource,
) {

    suspend fun getHighscores(
        game: GameId,
        difficulty: GameDifficulty,
        period: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME,
    ): DataResult<List<HighscoreEntry>, DataError> {
        return gameRemoteDataSource.getGameHighscores(game.name, difficulty.name, period.name).trackConnectivity().map { response ->
            response.entries.map { it.toHighscoreEntry() }
        }
    }

    /** Cross-game leaderboard over all games and difficulties. */
    suspend fun getGlobalRanking(
        period: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME,
    ): DataResult<List<GlobalRankingEntry>, DataError> {
        return gameRemoteDataSource.getGlobalRanking(period.name).trackConnectivity().map { response ->
            response.entries.map { it.toGlobalRankingEntry() }
        }
    }

    suspend fun submitScore(
        game: GameId,
        difficulty: GameDifficulty,
        score: Long,
        timeMillis: Long,
    ): EmptyDataResult<DataError> {
        // In-dev games have no real leaderboard yet - don't pollute it with test scores.
        if (game.indev) return DataResult.Success(Unit)

        return gameRemoteDataSource.submitGameScore(
            SubmitGameScoreRequest(
                gameId = game.name,
                difficulty = difficulty.name,
                score = score,
                timeMillis = timeMillis,
            )
        ).trackConnectivity().asEmptyDataResult()
    }

    /**
     * Submits [score] for the friend [friendId] - a friend who rode along in the player's run
     * (Schneagg Rodeo). Goes through the batch endpoint, which the server only allows for the
     * player's accepted friends and for games that permit friend scores.
     */
    suspend fun submitFriendScore(
        game: GameId,
        difficulty: GameDifficulty,
        friendId: String,
        score: Long,
        timeMillis: Long,
    ): EmptyDataResult<DataError> {
        if (game.indev) return DataResult.Success(Unit)

        return gameRemoteDataSource.submitGameScoresBatch(
            SubmitGameScoresBatchRequest(
                gameId = game.name,
                difficulty = difficulty.name,
                scores = listOf(BatchScoreEntryRequest(userId = friendId, score = score, timeMillis = timeMillis)),
            )
        ).trackConnectivity().asEmptyDataResult()
    }

    /**
     * Uploads the results of a shared-device game for several platform users at once.
     * [scores] maps user id to score. Only call this after the user explicitly agreed to upload.
     */
    suspend fun submitBatchScores(
        game: GameId,
        difficulty: GameDifficulty,
        scores: Map<String, Long>,
    ): EmptyDataResult<DataError> {
        if (game.indev || scores.isEmpty()) return DataResult.Success(Unit)

        return gameRemoteDataSource.submitGameScoresBatch(
            SubmitGameScoresBatchRequest(
                gameId = game.name,
                difficulty = difficulty.name,
                scores = scores.map { (userId, score) -> BatchScoreEntryRequest(userId = userId, score = score) },
            )
        ).trackConnectivity().asEmptyDataResult()
    }
}

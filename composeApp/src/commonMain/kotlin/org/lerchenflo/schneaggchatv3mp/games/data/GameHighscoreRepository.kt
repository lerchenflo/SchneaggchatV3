package org.lerchenflo.schneaggchatv3mp.games.data

import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.SubmitGameScoreRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.EmptyResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.asEmptyDataResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.map
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GlobalRankingEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.HighscoreEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.LeaderboardPeriod
import org.lerchenflo.schneaggchatv3mp.games.domain.toGlobalRankingEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.toHighscoreEntry

class GameHighscoreRepository(
    private val networkUtils: NetworkUtils,
) {

    suspend fun getHighscores(
        game: GameId,
        difficulty: GameDifficulty,
        period: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME,
    ): NetworkResult<List<HighscoreEntry>, NetworkError> {
        return networkUtils.getGameHighscores(game.name, difficulty.name, period.name).map { response ->
            response.entries.map { it.toHighscoreEntry() }
        }
    }

    /** Cross-game leaderboard over all games and difficulties. */
    suspend fun getGlobalRanking(
        period: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME,
    ): NetworkResult<List<GlobalRankingEntry>, NetworkError> {
        return networkUtils.getGlobalRanking(period.name).map { response ->
            response.entries.map { it.toGlobalRankingEntry() }
        }
    }

    suspend fun submitScore(
        game: GameId,
        difficulty: GameDifficulty,
        score: Long,
        timeMillis: Long,
    ): EmptyResult<NetworkError> {
        return networkUtils.submitGameScore(
            SubmitGameScoreRequest(
                gameId = game.name,
                difficulty = difficulty.name,
                score = score,
                timeMillis = timeMillis,
            )
        ).asEmptyDataResult()
    }
}

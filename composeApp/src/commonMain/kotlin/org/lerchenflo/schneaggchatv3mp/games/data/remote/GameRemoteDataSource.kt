package org.lerchenflo.schneaggchatv3mp.games.data.remote

import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.GameScoreResponse
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.GlobalRankingResponse
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.HighscoresResponse
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.SubmitGameScoreRequest
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.SubmitGameScoresBatchRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapResponse

/** Server calls for games: highscores, global ranking, score submission and the yearly recap. */
interface GameRemoteDataSource {

    suspend fun getGameHighscores(gameId: String, difficulty: String, period: String): NetworkResult<HighscoresResponse, NetworkingError>

    /** Cross-game leaderboard: percentile points summed over every board the user played. */
    suspend fun getGlobalRanking(period: String): NetworkResult<GlobalRankingResponse, NetworkingError>

    suspend fun submitGameScore(
        request: SubmitGameScoreRequest,
    ): NetworkResult<GameScoreResponse, NetworkingError>

    suspend fun submitGameScoresBatch(
        request: SubmitGameScoresBatchRequest,
    ): NetworkResult<List<GameScoreResponse>, NetworkingError>

    suspend fun getRecap(year: Int? = null): NetworkResult<RecapResponse, NetworkingError>
}

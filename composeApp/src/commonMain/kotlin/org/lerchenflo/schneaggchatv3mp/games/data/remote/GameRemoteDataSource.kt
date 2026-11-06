package org.lerchenflo.schneaggchatv3mp.games.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.GameScoreResponse
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.GlobalRankingResponse
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.HighscoresResponse
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.SubmitGameScoreRequest
import org.lerchenflo.schneaggchatv3mp.games.data.remote.dto.SubmitGameScoresBatchRequest
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapResponse

/** Server calls for games: highscores, global ranking, score submission and the yearly recap. */
interface GameRemoteDataSource {

    suspend fun getGameHighscores(gameId: String, difficulty: String, period: String): DataResult<HighscoresResponse, DataError>

    /** Cross-game leaderboard: percentile points summed over every board the user played. */
    suspend fun getGlobalRanking(period: String): DataResult<GlobalRankingResponse, DataError>

    suspend fun submitGameScore(
        request: SubmitGameScoreRequest,
    ): DataResult<GameScoreResponse, DataError>

    suspend fun submitGameScoresBatch(
        request: SubmitGameScoresBatchRequest,
    ): DataResult<List<GameScoreResponse>, DataError>

    suspend fun getRecap(year: Int? = null): DataResult<RecapResponse, DataError>
}

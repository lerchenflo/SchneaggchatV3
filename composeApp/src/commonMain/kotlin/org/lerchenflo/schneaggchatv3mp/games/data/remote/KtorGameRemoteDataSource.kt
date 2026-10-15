package org.lerchenflo.schneaggchatv3mp.games.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.GameScoreResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.GlobalRankingResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.HighscoresResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.SubmitGameScoreRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.SubmitGameScoresBatchRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.games.domain.RecapResponse

/** [GameRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorGameRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : GameRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun getGameHighscores(gameId: String, difficulty: String, period: String): NetworkResult<HighscoresResponse, NetworkingError> {
        return server.safeGet(endpoint = "/games/highscores?gameid=$gameId&difficulty=$difficulty&period=$period")
    }

    override suspend fun getGlobalRanking(period: String): NetworkResult<GlobalRankingResponse, NetworkingError> {
        return server.safeGet(endpoint = "/games/globalranking?period=$period")
    }

    override suspend fun submitGameScore(
        request: SubmitGameScoreRequest,
    ): NetworkResult<GameScoreResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/games/upsert",
            body = request)
    }

    override suspend fun submitGameScoresBatch(
        request: SubmitGameScoresBatchRequest,
    ): NetworkResult<List<GameScoreResponse>, NetworkingError> {
        return server.safePost(
            endpoint = "/games/upsertbatch",
            body = request)
    }

    override suspend fun getRecap(year: Int?): NetworkResult<RecapResponse, NetworkingError> {
        val endpoint = if (year != null) "/recap?year=$year" else "/recap"
        return server.safeGet(endpoint)
    }
}

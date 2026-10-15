package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapEntryRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapEntryResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

/** [MapRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorMapRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : MapRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun mapSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): NetworkResult<MapSyncResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/map/sync?page=$page&page_size=400",
            body = entries,
        )
    }

    override suspend fun upsertMapEntry(
        request: MapEntryRequest,
    ): NetworkResult<MapEntryResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/map/upsert",
            body = request)
    }

    override suspend fun deleteMapEntry(entryId: String): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/map/delete?entryid=$entryId")
    }
}

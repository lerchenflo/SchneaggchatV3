package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.dto.MapEntryRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.MapEntryResponse
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.dto.MapSyncResponse
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
    ): DataResult<MapSyncResponse, DataError> {
        return server.safePost(
            endpoint = "/map/sync?page=$page&page_size=400",
            body = entries,
        )
    }

    override suspend fun upsertMapEntry(
        request: MapEntryRequest,
    ): DataResult<MapEntryResponse, DataError> {
        return server.safePost(
            endpoint = "/map/upsert",
            body = request)
    }

    override suspend fun deleteMapEntry(entryId: String): DataResult<Unit, DataError> {
        return server.safeDelete(endpoint = "/map/delete?entryid=$entryId")
    }
}

package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapEntryRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapEntryResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/** Server calls for map entries: sync, upsert and delete. */
interface MapRemoteDataSource {

    suspend fun mapSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): NetworkResult<MapSyncResponse, NetworkingError>

    suspend fun upsertMapEntry(
        request: MapEntryRequest,
    ): NetworkResult<MapEntryResponse, NetworkingError>

    suspend fun deleteMapEntry(entryId: String): NetworkResult<Unit, NetworkingError>
}

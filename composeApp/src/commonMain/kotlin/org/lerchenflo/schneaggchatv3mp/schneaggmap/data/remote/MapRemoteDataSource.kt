package org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.dto.MapEntryRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.MapEntryResponse
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.dto.MapSyncResponse

/** Server calls for map entries: sync, upsert and delete. */
interface MapRemoteDataSource {

    suspend fun mapSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): DataResult<MapSyncResponse, DataError>

    suspend fun upsertMapEntry(
        request: MapEntryRequest,
    ): DataResult<MapEntryResponse, DataError>

    suspend fun deleteMapEntry(entryId: String): DataResult<Unit, DataError>
}

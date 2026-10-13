package org.lerchenflo.schneaggchatv3mp.schneaggmap.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MapEntry

/**
 * Local (Room) storage for map entries.
 */
interface MapLocalDataSource {

    suspend fun getMapEntryChangeIds(): List<IdChangeDate>

    suspend fun upsertMapEntry(entry: MapEntry)

    suspend fun deleteMapEntry(id: String)

    fun getAllMapEntriesFlow(): Flow<List<MapEntry>>
}

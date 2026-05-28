package org.lerchenflo.schneaggchatv3mp.schneaggmap.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.database.MapEntryDao
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toDto
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toMapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

class MapRepository(
    private val mapEntryDao: MapEntryDao,
) {

    // ─── MapEntry ─────────────────────────────────────────────────────────────

    suspend fun getMapEntryChangeIds(): List<IdChangeDate> =
        mapEntryDao.getMapEntryIdsWithChangeDates()

    suspend fun upsertMapEntry(entry: MapEntry) =
        mapEntryDao.upsert(entry.toDto())

    suspend fun deleteMapEntry(id: String) =
        mapEntryDao.delete(id)

    fun getAllMapEntriesFlow(): Flow<List<MapEntry>> =
        mapEntryDao.getAllFlow().map { list -> list.map { it.toMapEntry() } }
}

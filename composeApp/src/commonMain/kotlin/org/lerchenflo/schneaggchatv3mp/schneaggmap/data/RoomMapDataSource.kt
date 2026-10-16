package org.lerchenflo.schneaggchatv3mp.schneaggmap.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.database.MapEntryDao
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.local.entity.toEntity
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.local.entity.toMapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

class RoomMapDataSource(
    private val database: AppDatabase,
) : MapLocalDataSource {

    // ─── MapEntry ─────────────────────────────────────────────────────────────

    override suspend fun getMapEntryChangeIds(): List<IdChangeDate> =
        database.mapEntryDao().getMapEntryIdsWithChangeDates()

    override suspend fun upsertMapEntry(entry: MapEntry) =
        database.mapEntryDao().upsert(entry.toEntity())

    override suspend fun deleteMapEntry(id: String) =
        database.mapEntryDao().delete(id)

    override fun getAllMapEntriesFlow(): Flow<List<MapEntry>> =
        database.mapEntryDao().getAllFlow().map { list -> list.map { it.toMapEntry() } }
}

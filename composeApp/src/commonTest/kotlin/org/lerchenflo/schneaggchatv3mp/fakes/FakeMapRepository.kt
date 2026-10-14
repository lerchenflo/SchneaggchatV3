package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry

/** In-memory [MapRepository]. The reads serve [entries]; the writes are recorded only. */
class FakeMapRepository : MapRepository {

    val entries = MutableStateFlow<List<MapEntry>>(emptyList())

    var writeResult: Boolean = true

    var onSyncMapEntries: suspend () -> Unit = {}

    val syncCalls = CallLog<Unit>()
    val upsertedEntryIds = CallLog<String?>()
    val deletedEntryIds = CallLog<String>()

    override fun getAllMapEntriesFlow(): Flow<List<MapEntry>> = entries

    override suspend fun syncMapEntries() {
        syncCalls.record(Unit)
        onSyncMapEntries()
    }

    override suspend fun upsertMapEntry(
        entryId: String?,
        name: String,
        description: String,
        lat: Double,
        lon: Double,
        locationData: List<LocationData>,
    ) = upsertedEntryIds.record(entryId)

    override suspend fun deleteMapEntry(entryId: String): Boolean {
        deletedEntryIds.record(entryId)
        return writeResult
    }
}

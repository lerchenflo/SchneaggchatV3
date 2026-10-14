package org.lerchenflo.schneaggchatv3mp.schneaggmap.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel

/**
 * Schneaggmap entries: reads come from the local database, writes go to the server first and
 * update the local row with the server's answer. Errors of the write calls are sent to the
 * [ErrorChannel].
 */
interface MapRepository {

    fun getAllMapEntriesFlow(): Flow<List<MapEntry>>

    /** Pulls changed and deleted map entries from the server into the local database. */
    suspend fun syncMapEntries()

    /** Creates ([entryId] null) or updates a map entry. */
    suspend fun upsertMapEntry(
        entryId: String?,
        name: String,
        description: String,
        lat: Double,
        lon: Double,
        locationData: List<LocationData>,
    )

    /** @return true on success */
    suspend fun deleteMapEntry(entryId: String): Boolean
}

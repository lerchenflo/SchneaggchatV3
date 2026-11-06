package org.lerchenflo.schneaggchatv3mp.schneaggmap.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.domain.asEmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.local.MapLocalDataSource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.dto.MapEntryRequest
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.dto.toMapEntry
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.MapRemoteDataSource

/** [MapRepository] on top of the local [MapLocalDataSource] and the server via [MapRemoteDataSource]. */
class OfflineFirstMapRepository(
    private val mapLocalDataSource: MapLocalDataSource,
    private val mapRemoteDataSource: MapRemoteDataSource,
    private val loggingRepository: Logger,
) : MapRepository {

    override fun getAllMapEntriesFlow(): Flow<List<MapEntry>> =
        mapLocalDataSource.getAllMapEntriesFlow()

    // ─── Map sync (the MAP job of the global dataSync) ─────────────────────────

    override suspend fun syncMapEntries() {

        SessionCache.requireLoggedIn() ?: return


        val localEntries = mapLocalDataSource.getMapEntryChangeIds()
        var currentPage = 0
        var moreEntries = true
        while (moreEntries) {
            val response = mapRemoteDataSource.mapSync(
                entries = localEntries.map { IdTimeStamp(it.id, it.updatedAt) },
                page = currentPage,
            ).trackConnectivity()
            when (response) {
                is DataResult.Error<*> -> { loggingRepository.logWarning("Map entry sync error: ${response.error}"); break }
                is DataResult.Success -> {
                    //println("Map sync success, upserting ${response.data.updatedEntries} entrys")
                    response.data.updatedEntries.forEach { mapLocalDataSource.upsertMapEntry(it.toMapEntry()) }
                    response.data.deletedEntries.forEach { mapLocalDataSource.deleteMapEntry(it) }
                    moreEntries = response.data.moreEntries
                }
            }
            currentPage++
        }
    }



    // ─── Map CRUD ─────────────────────────────────────────────────────────────
    // WS broadcast handles local DB upsert/delete automatically after server write.

    override suspend fun upsertMapEntry(
        entryId: String?,
        name: String,
        description: String,
        lat: Double,
        lon: Double,
        locationData: List<LocationData>,
    ): EmptyDataResult<DataError> =
        mapRemoteDataSource.upsertMapEntry(
            MapEntryRequest(
                entryId = entryId,
                name = name,
                description = description,
                coordinates = LatLong(lat = lat, long = lon),
                locationData = locationData,
            )
        ).trackConnectivity()
            .onSuccess { mapLocalDataSource.upsertMapEntry(it.toMapEntry()) }
            .asEmptyDataResult()

    override suspend fun deleteMapEntry(entryId: String): EmptyDataResult<DataError> =
        mapRemoteDataSource.deleteMapEntry(entryId).trackConnectivity()
            .onSuccess {
                mapLocalDataSource.deleteMapEntry(entryId)
            }
            .asEmptyDataResult()
}

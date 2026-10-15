package org.lerchenflo.schneaggchatv3mp.schneaggmap.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapEntryRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.MapEntryResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toMapEntry
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.MapRemoteDataSource

/** [MapRepository] on top of the local [MapLocalDataSource] and the server via [MapRemoteDataSource]. */
class OfflineFirstMapRepository(
    private val mapLocalDataSource: MapLocalDataSource,
    private val mapRemoteDataSource: MapRemoteDataSource,
    private val loggingRepository: Logger,
    private val errorChannel: ErrorChannel,
) : MapRepository {

    override fun getAllMapEntriesFlow(): Flow<List<MapEntry>> =
        mapLocalDataSource.getAllMapEntriesFlow()

    // ─── Map sync (triggered only from map screen, not part of global dataSync) ─

    override suspend fun syncMapEntries() {

        val userId = SessionCache.requireLoggedIn() ?: return


        val localEntries = mapLocalDataSource.getMapEntryChangeIds()
        var currentPage = 0
        var moreEntries = true
        while (moreEntries) {
            val response = mapRemoteDataSource.mapSync(
                entries = localEntries.map { IdTimeStamp(it.id, it.updatedAt) },
                page = currentPage,
            ).trackConnectivity()
            when (response) {
                is NetworkResult.Error<*> -> { loggingRepository.logWarning("Map entry sync error: ${response.error}"); break }
                is NetworkResult.Success -> {
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
    ) {



        val result = mapRemoteDataSource.upsertMapEntry(
            MapEntryRequest(
                entryId = entryId,
                name = name,
                description = description,
                coordinates = LatLong(lat = lat, long = lon),
                locationData = locationData,
            )
        ).trackConnectivity()


        when (result) {
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = result.error))
            }
            is NetworkResult.Success<MapEntryResponse> -> {
                mapLocalDataSource.upsertMapEntry(result.data.toMapEntry())
            }
        }
    }

    override suspend fun deleteMapEntry(entryId: String) : Boolean {
        return when (val result = mapRemoteDataSource.deleteMapEntry(entryId).trackConnectivity()) {
            is NetworkResult.Error<*> -> {
                errorChannel.sendErrorSuspend(ErrorEvent(error = result.error))
                false
            }
            is NetworkResult.Success<*> -> {
                mapLocalDataSource.deleteMapEntry(entryId)
                true
            }
        }
    }
}

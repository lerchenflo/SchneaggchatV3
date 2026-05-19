package org.lerchenflo.schneaggchatv3mp.schneaggmap.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.datasource.database.MainTypeDao
import org.lerchenflo.schneaggchatv3mp.datasource.database.MapEntryDao
import org.lerchenflo.schneaggchatv3mp.datasource.database.SubtypeDao
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toDto
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toMainType
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toMapEntry
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toSubtype
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MainType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.Subtype

class MapRepository(
    private val mapEntryDao: MapEntryDao,
    private val subtypeDao: SubtypeDao,
    private val mainTypeDao: MainTypeDao,
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

    // ─── Subtype ──────────────────────────────────────────────────────────────

    suspend fun getSubtypeChangeIds(): List<IdChangeDate> =
        subtypeDao.getSubtypeIdsWithChangeDates()

    suspend fun upsertSubtype(subtype: Subtype) =
        subtypeDao.upsert(subtype.toDto())

    suspend fun deleteSubtype(id: String) =
        subtypeDao.delete(id)

    fun getAllSubtypesFlow(): Flow<List<Subtype>> =
        subtypeDao.getAllFlow().map { list -> list.map { it.toSubtype() } }

    // ─── MainType ─────────────────────────────────────────────────────────────

    suspend fun replaceAllMainTypes(mainTypes: List<MainType>) {
        mainTypeDao.deleteAll()
        mainTypes.forEach { mainTypeDao.upsert(it.toDto()) }
    }

    fun getAllMainTypesFlow(): Flow<List<MainType>> =
        mainTypeDao.getAllFlow().map { list -> list.map { it.toMainType() } }
}

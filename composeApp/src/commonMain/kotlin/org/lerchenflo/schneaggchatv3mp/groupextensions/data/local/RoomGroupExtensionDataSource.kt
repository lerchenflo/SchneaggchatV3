package org.lerchenflo.schneaggchatv3mp.groupextensions.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.local.entity.toEntity
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.local.entity.toGroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension

class RoomGroupExtensionDataSource(
    private val database: AppDatabase,
) : GroupExtensionLocalDataSource {

    override suspend fun getExtensionChangeIds(): List<IdChangeDate> =
        database.groupExtensionDao().getExtensionIdsWithChangeDates()

    override suspend fun upsertExtension(extension: GroupExtension) =
        database.groupExtensionDao().upsert(extension.toEntity())

    override suspend fun deleteExtension(id: String) =
        database.groupExtensionDao().delete(id)

    override suspend fun deleteExtensionsForOwners(ownerIds: List<String>) {
        if (ownerIds.isNotEmpty()) database.groupExtensionDao().deleteForOwners(ownerIds)
    }

    override fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>> =
        database.groupExtensionDao().getForOwnerFlow(ownerId).map { list -> list.map { it.toGroupExtension() } }

    override fun getExtensionFlow(id: String): Flow<GroupExtension?> =
        database.groupExtensionDao().getFlow(id).map { it?.toGroupExtension() }
}

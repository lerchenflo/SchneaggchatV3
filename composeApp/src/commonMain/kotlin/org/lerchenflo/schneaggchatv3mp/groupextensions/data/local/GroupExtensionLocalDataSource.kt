package org.lerchenflo.schneaggchatv3mp.groupextensions.data.local

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.database.IdChangeDate
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension

/**
 * Local (Room) storage for group extensions, each stored with its items.
 */
interface GroupExtensionLocalDataSource {

    suspend fun getExtensionChangeIds(): List<IdChangeDate>

    suspend fun upsertExtension(extension: GroupExtension)

    suspend fun deleteExtension(id: String)

    suspend fun deleteExtensionsForOwners(ownerIds: List<String>)

    fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>>

    fun getExtensionFlow(id: String): Flow<GroupExtension?>
}

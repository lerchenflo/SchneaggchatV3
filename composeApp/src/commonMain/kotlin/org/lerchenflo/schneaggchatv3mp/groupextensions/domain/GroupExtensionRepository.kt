package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.coroutines.flow.Flow

/** Read access to the extensions of a group, kept up to date while collected. */
interface GroupExtensionRepository {
    fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>>

    fun getExtensionFlow(id: String): Flow<GroupExtension?>
}

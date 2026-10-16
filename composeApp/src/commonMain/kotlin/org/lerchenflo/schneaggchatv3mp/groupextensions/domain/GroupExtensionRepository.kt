package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.coroutines.flow.Flow

/** Access to the extensions of a group; the flows stay up to date while collected. */
interface GroupExtensionRepository {
    fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>>

    fun getExtensionFlow(id: String): Flow<GroupExtension?>

    fun getItemsForExtensionFlow(extensionId: String): Flow<List<GroupExtensionItem>>

    /** Creates ([itemId] null) or replaces an item of [extensionId]. Returns false when the write failed. */
    suspend fun upsertItem(extensionId: String, itemId: String?, data: ExtensionItemData): Boolean

    /** Returns false when the delete failed. */
    suspend fun deleteItem(itemId: String): Boolean
}

package org.lerchenflo.schneaggchatv3mp.groupextensions.data

import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.dtos.GroupExtensionDto
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.dtos.GroupExtensionItemDto

/**
 * Room store of the group extensions (containers + items with their content). The group sync
 * already writes into it.
 *
 * Reads still come from [DemoGroupExtensionRepository] until the server serves extensions. To switch:
 * add `: GroupExtensionRepository` to this class, uncomment the read block below and swap the
 * binding in Modules.kt.
 */
class RoomGroupExtensionRepository(
    private val database: AppDatabase,
) {

    /** Highest version stored locally, sent as `extensionsSince` with the group sync. */
    suspend fun getLastSyncedVersion(): Long =
        database.groupExtensionDao().getLastSyncedVersion()

    suspend fun applySyncChanges(
        updatedExtensions: List<GroupExtensionDto>,
        deletedExtensionIds: List<String>,
        updatedItems: List<GroupExtensionItemDto>,
        deletedItemIds: List<String>,
    ) = database.groupExtensionDao().applySyncChanges(
        updatedExtensions = updatedExtensions,
        deletedExtensionIds = deletedExtensionIds,
        updatedItems = updatedItems,
        deletedItemIds = deletedItemIds,
    )

    // ─── Reads (disabled while the demo repository is bound) ─────────────────
    // Rows this app version can't decode (unknown type or payload) are skipped, they stay stored.
    /*
    override fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>> =
        database.groupExtensionDao().getExtensionsForOwnerFlow(ownerId)
            .map { list -> list.mapNotNull { it.toGroupExtension() } }

    override fun getExtensionFlow(id: String): Flow<GroupExtension?> =
        database.groupExtensionDao().getExtensionFlow(id)
            .map { it?.toGroupExtension() }

    override fun getItemsForExtensionFlow(extensionId: String): Flow<List<GroupExtensionItem>> =
        database.groupExtensionDao().getItemsForExtensionFlow(extensionId)
            .map { list -> list.mapNotNull { it.toGroupExtensionItem() } }

    // Writes: network first, then the local row - needs the item endpoints (plan §3.2 / §4.3)
    override suspend fun upsertItem(extensionId: String, itemId: String?, data: ExtensionItemData): Boolean =
        TODO("POST /groupextensions/items/upsert, then upsertItems() locally with the response")

    override suspend fun deleteItem(itemId: String): Boolean =
        TODO("DELETE /groupextensions/items/delete, then deleteItemsByIds() locally")
    */
    // Imports needed when uncommenting:
    // kotlinx.coroutines.flow.Flow, kotlinx.coroutines.flow.map,
    // groupextensions.domain.GroupExtension / GroupExtensionItem / GroupExtensionRepository / ExtensionItemData,
    // datasource.network.requestResponseDataClasses.toGroupExtension / toGroupExtensionItem
}

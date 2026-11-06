package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult

/**
 * Extensions of the user's groups: reads come from the local database, writes go to the server
 * first and store the returned extension. Write calls return their error, the caller maps it to UI.
 */
interface GroupExtensionRepository {

    // ─── Reads (local database) ───────────────────────────────────────────────

    fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>>

    /** The extension with its items; the flow stays up to date while collected. */
    fun getExtensionFlow(id: String): Flow<GroupExtension?>

    // ─── Sync ─────────────────────────────────────────────────────────────────

    /** Pulls changed and deleted extensions from the server into the local database. */
    suspend fun syncGroupExtensions()

    /** Removes the stored extensions of groups the user lost (left, removed or deleted). */
    suspend fun deleteExtensionsForGroups(groupIds: List<String>)

    // ─── Writes ───────────────────────────────────────────────────────────────

    /** @return the id of the created extension */
    suspend fun createExtension(
        groupId: String,
        type: GroupExtensionType,
        title: String,
        settings: ExtensionSettings,
    ): DataResult<String, DataError>
    suspend fun renameExtension(extensionId: String, title: String): EmptyDataResult<DataError>

    /** Deletes the extension with all its items (creator or group admin). */
    suspend fun deleteExtension(extensionId: String): EmptyDataResult<DataError>

    /** Creates ([itemId] null) or replaces an item of [extensionId]. */
    suspend fun upsertItem(extensionId: String, itemId: String?, data: ExtensionItemData): EmptyDataResult<DataError>
    suspend fun deleteItem(extensionId: String, itemId: String): EmptyDataResult<DataError>
}

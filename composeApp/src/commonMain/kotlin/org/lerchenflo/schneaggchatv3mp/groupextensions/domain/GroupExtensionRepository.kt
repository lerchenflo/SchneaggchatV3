package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel

/**
 * Extensions of the user's groups: reads come from the local database, writes go to the server
 * first and store the returned extension. Errors of the write calls are sent to the [ErrorChannel].
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

    /** @return the id of the created extension, null on error */
    suspend fun createExtension(
        groupId: String,
        type: GroupExtensionType,
        title: String,
        settings: ExtensionSettings,
    ): String?

    /** @return true on success */
    suspend fun renameExtension(extensionId: String, title: String): Boolean

    /** Deletes the extension with all its items (creator or group admin). @return true on success */
    suspend fun deleteExtension(extensionId: String): Boolean

    /** Creates ([itemId] null) or replaces an item of [extensionId]. @return true on success */
    suspend fun upsertItem(extensionId: String, itemId: String?, data: ExtensionItemData): Boolean

    /** @return true on success */
    suspend fun deleteItem(extensionId: String, itemId: String): Boolean
}

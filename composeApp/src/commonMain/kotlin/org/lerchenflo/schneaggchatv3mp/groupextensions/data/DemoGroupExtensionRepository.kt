package org.lerchenflo.schneaggchatv3mp.groupextensions.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionOwnerType
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionItem
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisLong
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * In-memory stand-in until the server and Room parts exist (GROUP_EXTENSIONS_PLAN.md phase 2).
 * Every group gets two demo extensions the first time its list is observed. Items written through
 * it live until the app is closed.
 * todo: replace with the Room-backed repository - RoomGroupExtensionRepository already holds the
 *  synced data, its read functions and the Koin binding in Modules.kt only need to be uncommented
 */
class DemoGroupExtensionRepository : GroupExtensionRepository {

    private val extensions = MutableStateFlow<Map<String, GroupExtension>>(emptyMap())
    private val items = MutableStateFlow<Map<String, GroupExtensionItem>>(emptyMap())

    override fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>> = extensions
        .onStart { seedDemoExtensions(ownerId) }
        .map { all -> all.values.filter { it.ownerId == ownerId } }

    override fun getExtensionFlow(id: String): Flow<GroupExtension?> = extensions.map { it[id] }

    override fun getItemsForExtensionFlow(extensionId: String): Flow<List<GroupExtensionItem>> = items
        .map { all -> all.values.filter { it.extensionId == extensionId }.sortedBy { it.createdAt } }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun upsertItem(extensionId: String, itemId: String?, data: ExtensionItemData): Boolean {
        val extension = extensions.value[extensionId] ?: return false
        val userId = SessionCache.requireLoggedIn()?.userId ?: return false
        val now = getCurrentTimeMillisLong()

        items.update { current ->
            val existing = itemId?.let { current[it] }
            val item = GroupExtensionItem(
                id = existing?.id ?: "demo-item-${Uuid.random()}",
                extensionId = extensionId,
                ownerType = extension.ownerType,
                ownerId = extension.ownerId,
                type = extension.type,
                data = data,
                createdBy = existing?.createdBy ?: userId,
                createdAt = existing?.createdAt ?: now,
                updatedBy = userId,
                updatedAt = now,
            )
            current + (item.id to item)
        }
        return true
    }

    override suspend fun deleteItem(itemId: String): Boolean {
        items.update { it - itemId }
        return true
    }

    private fun seedDemoExtensions(ownerId: String) {
        extensions.update { current ->
            if (current.values.any { it.ownerId == ownerId }) return@update current

            val demo = listOf(
                demoExtension(ownerId, GroupExtensionType.SPLIT, "Kassa", ExtensionSettings.Split(currency = "EUR")),
                demoExtension(ownerId, GroupExtensionType.QUOTES, "Zitate"),
            )
            current + demo.associateBy { it.id }
        }
    }

    private fun demoExtension(
        ownerId: String,
        type: GroupExtensionType,
        title: String,
        settings: ExtensionSettings = ExtensionSettings.None,
    ) = GroupExtension(
        id = "demo-${type.name}-$ownerId",
        ownerType = ExtensionOwnerType.GROUP,
        ownerId = ownerId,
        type = type,
        title = title,
        settings = settings,
        createdBy = "",
        createdAt = 0L,
        updatedBy = "",
        updatedAt = 0L,
    )
}

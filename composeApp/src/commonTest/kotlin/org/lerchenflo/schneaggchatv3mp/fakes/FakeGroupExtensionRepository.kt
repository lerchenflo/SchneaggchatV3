package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType

/** In-memory [GroupExtensionRepository]. The reads serve [extensions]; the writes are recorded only. */
class FakeGroupExtensionRepository : GroupExtensionRepository {

    val extensions = MutableStateFlow<List<GroupExtension>>(emptyList())

    var writeResult: Boolean = true

    val syncCalls = CallLog<Unit>()
    val purgedGroupIds = CallLog<List<String>>()

    override fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>> =
        extensions.map { list -> list.filter { it.ownerId == ownerId } }

    override fun getExtensionFlow(id: String): Flow<GroupExtension?> =
        extensions.map { list -> list.find { it.id == id } }

    override suspend fun syncGroupExtensions() = syncCalls.record(Unit)

    override suspend fun deleteExtensionsForGroups(groupIds: List<String>) = purgedGroupIds.record(groupIds)

    override suspend fun createExtension(
        groupId: String,
        type: GroupExtensionType,
        title: String,
        settings: ExtensionSettings,
    ): String? = if (writeResult) "fake-extension" else null

    override suspend fun renameExtension(extensionId: String, title: String): Boolean = writeResult

    override suspend fun deleteExtension(extensionId: String): Boolean = writeResult

    override suspend fun upsertItem(extensionId: String, itemId: String?, data: ExtensionItemData): Boolean = writeResult

    override suspend fun deleteItem(extensionId: String, itemId: String): Boolean = writeResult
}

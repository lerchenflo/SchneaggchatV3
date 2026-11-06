package org.lerchenflo.schneaggchatv3mp.groupextensions.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.domain.asEmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.map
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupExtensionResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.local.GroupExtensionLocalDataSource
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.GroupExtensionRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.ExtensionRequest
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.ItemRequest
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.decodeGroupExtensionResponses
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.toGroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionOwnerType
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType

/**
 * [GroupExtensionRepository] on top of the local [GroupExtensionLocalDataSource] and the server via
 * [GroupExtensionRemoteDataSource], shaped like the map repository: IdTimeStamp sync, network-first
 * writes that store the extension the server returns.
 */
class OfflineFirstGroupExtensionRepository(
    private val groupExtensionLocalDataSource: GroupExtensionLocalDataSource,
    private val groupExtensionRemoteDataSource: GroupExtensionRemoteDataSource,
    private val loggingRepository: Logger,
) : GroupExtensionRepository {

    override fun getExtensionsForOwnerFlow(ownerId: String): Flow<List<GroupExtension>> =
        groupExtensionLocalDataSource.getExtensionsForOwnerFlow(ownerId)

    override fun getExtensionFlow(id: String): Flow<GroupExtension?> =
        groupExtensionLocalDataSource.getExtensionFlow(id)

    // ─── Sync ─────────────────────────────────────────────────────────────────

    override suspend fun syncGroupExtensions() {
        SessionCache.requireLoggedIn() ?: return

        val localEntries = groupExtensionLocalDataSource.getExtensionChangeIds()
        var currentPage = 0
        var moreEntries = true
        while (moreEntries) {
            val response = groupExtensionRemoteDataSource.groupExtensionSync(
                entries = localEntries.map { IdTimeStamp(it.id, it.updatedAt) },
                page = currentPage,
            ).trackConnectivity()
            when (response) {
                is DataResult.Error<*> -> { loggingRepository.logWarning("Group extension sync error: ${response.error}"); break }
                is DataResult.Success -> {
                    val updated = response.data.updatedExtensions.decodeGroupExtensionResponses()
                    val skipped = response.data.updatedExtensions.size - updated.size
                    if (skipped > 0) {
                        loggingRepository.logWarning("Group extension sync: skipped $skipped entries this app version can't read")
                    }
                    updated.forEach { groupExtensionLocalDataSource.upsertExtension(it.toGroupExtension()) }
                    response.data.deletedExtensions.forEach { groupExtensionLocalDataSource.deleteExtension(it) }
                    moreEntries = response.data.moreEntries
                }
            }
            currentPage++
        }
    }

    override suspend fun deleteExtensionsForGroups(groupIds: List<String>) =
        groupExtensionLocalDataSource.deleteExtensionsForOwners(groupIds)

    // ─── Writes ───────────────────────────────────────────────────────────────
    // The socket broadcast also reaches the writer; storing the response here is idempotent with it.

    override suspend fun createExtension(
        groupId: String,
        type: GroupExtensionType,
        title: String,
        settings: ExtensionSettings,
    ): DataResult<String, DataError> {
        val result = groupExtensionRemoteDataSource.upsertExtension(
            ExtensionRequest(
                extensionId = null,
                ownerType = ExtensionOwnerType.GROUP,
                ownerId = groupId,
                type = type,
                title = title,
                settings = settings,
            )
        ).trackConnectivity()
        return result.store().map { it.id }
    }

    override suspend fun renameExtension(extensionId: String, title: String): EmptyDataResult<DataError> {
        val extension = groupExtensionLocalDataSource.getExtensionFlow(extensionId).firstOrNull()
            ?: return DataResult.Error(DataError.Unknown(debugInfo = "extension $extensionId is not stored locally"))
        val result = groupExtensionRemoteDataSource.upsertExtension(
            ExtensionRequest(
                extensionId = extension.id,
                ownerType = extension.ownerType,
                ownerId = extension.ownerId,
                type = extension.type,
                title = title,
                settings = extension.settings,
            )
        ).trackConnectivity()
        return result.store().asEmptyDataResult()
    }

    override suspend fun deleteExtension(extensionId: String): EmptyDataResult<DataError> =
        groupExtensionRemoteDataSource.deleteExtension(extensionId).trackConnectivity()
            .onSuccess {
                groupExtensionLocalDataSource.deleteExtension(extensionId)
            }
            .asEmptyDataResult()

    override suspend fun upsertItem(extensionId: String, itemId: String?, data: ExtensionItemData): EmptyDataResult<DataError> {
        val result = groupExtensionRemoteDataSource.upsertItem(
            ItemRequest(extensionId = extensionId, itemId = itemId, data = data)
        ).trackConnectivity()
        return result.store().asEmptyDataResult()
    }

    override suspend fun deleteItem(extensionId: String, itemId: String): EmptyDataResult<DataError> {
        val result = groupExtensionRemoteDataSource.deleteItem(extensionId = extensionId, itemId = itemId).trackConnectivity()
        return result.store().asEmptyDataResult()
    }

    /** Stores the extension the server returned. */
    private suspend fun DataResult<GroupExtensionResponse, DataError>.store(): DataResult<GroupExtension, DataError> =
        map { it.toGroupExtension() }
            .onSuccess { groupExtensionLocalDataSource.upsertExtension(it) }
}

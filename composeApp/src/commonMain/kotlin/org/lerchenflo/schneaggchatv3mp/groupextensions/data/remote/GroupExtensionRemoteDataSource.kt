package org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupExtensionResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.ExtensionRequest
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.GroupExtensionSyncResponse
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.ItemRequest

/** Server calls for group extensions: sync, extension and item writes. Item writes return the whole extension. */
interface GroupExtensionRemoteDataSource {

    suspend fun groupExtensionSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): DataResult<GroupExtensionSyncResponse, DataError>

    suspend fun upsertExtension(request: ExtensionRequest): DataResult<GroupExtensionResponse, DataError>

    suspend fun deleteExtension(extensionId: String): DataResult<Unit, DataError>

    suspend fun upsertItem(request: ItemRequest): DataResult<GroupExtensionResponse, DataError>

    suspend fun deleteItem(extensionId: String, itemId: String): DataResult<GroupExtensionResponse, DataError>
}

package org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupExtensionResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.ExtensionRequest
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.GroupExtensionSyncResponse
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.ItemRequest

/** Server calls for group extensions: sync, extension and item writes. Item writes return the whole extension. */
interface GroupExtensionRemoteDataSource {

    suspend fun groupExtensionSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): NetworkResult<GroupExtensionSyncResponse, NetworkingError>

    suspend fun upsertExtension(request: ExtensionRequest): NetworkResult<GroupExtensionResponse, NetworkingError>

    suspend fun deleteExtension(extensionId: String): NetworkResult<Unit, NetworkingError>

    suspend fun upsertItem(request: ItemRequest): NetworkResult<GroupExtensionResponse, NetworkingError>

    suspend fun deleteItem(extensionId: String, itemId: String): NetworkResult<GroupExtensionResponse, NetworkingError>
}

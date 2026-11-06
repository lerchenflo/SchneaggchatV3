package org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupExtensionResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.ExtensionRequest
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.GroupExtensionSyncResponse
import org.lerchenflo.schneaggchatv3mp.groupextensions.data.remote.dto.ItemRequest

/** [GroupExtensionRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorGroupExtensionRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : GroupExtensionRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun groupExtensionSync(
        entries: List<IdTimeStamp>,
        page: Int,
    ): DataResult<GroupExtensionSyncResponse, DataError> {
        return server.safePost(
            endpoint = "/groupextensions/sync?page=$page&page_size=100",
            body = entries,
        )
    }

    override suspend fun upsertExtension(request: ExtensionRequest): DataResult<GroupExtensionResponse, DataError> {
        return server.safePost(
            endpoint = "/groupextensions/upsert",
            body = request,
        )
    }

    override suspend fun deleteExtension(extensionId: String): DataResult<Unit, DataError> {
        return server.safeDelete(endpoint = "/groupextensions/delete?extensionid=$extensionId")
    }

    override suspend fun upsertItem(request: ItemRequest): DataResult<GroupExtensionResponse, DataError> {
        return server.safePost(
            endpoint = "/groupextensions/items/upsert",
            body = request,
        )
    }

    override suspend fun deleteItem(extensionId: String, itemId: String): DataResult<GroupExtensionResponse, DataError> {
        return server.safeDelete(endpoint = "/groupextensions/items/delete?extensionid=$extensionId&itemid=$itemId")
    }
}

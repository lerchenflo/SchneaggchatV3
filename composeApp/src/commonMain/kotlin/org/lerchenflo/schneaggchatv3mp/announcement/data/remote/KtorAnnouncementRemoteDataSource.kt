package org.lerchenflo.schneaggchatv3mp.announcement.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.AnnouncementRequest
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.AnnouncementResponse
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.LatestAnnouncementResponse
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

/** [AnnouncementRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorAnnouncementRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : AnnouncementRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun getLatestAnnouncement(): DataResult<LatestAnnouncementResponse, DataError> {
        return server.safeGet(endpoint = "/announcements/latest")
    }

    override suspend fun getAllAnnouncements(): DataResult<List<AnnouncementResponse>, DataError> {
        return server.safeGet(endpoint = "/announcements/admin/list")
    }

    override suspend fun createAnnouncement(request: AnnouncementRequest): DataResult<AnnouncementResponse, DataError> {
        return server.safePost(endpoint = "/announcements/admin/create", body = request)
    }

    override suspend fun updateAnnouncement(request: AnnouncementRequest): DataResult<AnnouncementResponse, DataError> {
        return server.safePost(endpoint = "/announcements/admin/update", body = request)
    }

    override suspend fun deleteAnnouncement(id: Long): DataResult<Unit, DataError> {
        return server.safeDelete(endpoint = "/announcements/admin/delete?id=$id")
    }
}

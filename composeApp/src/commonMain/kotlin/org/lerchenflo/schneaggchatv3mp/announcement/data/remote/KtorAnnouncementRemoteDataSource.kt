package org.lerchenflo.schneaggchatv3mp.announcement.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.AnnouncementRequest
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.AnnouncementResponse
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.LatestAnnouncementResponse
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

/** [AnnouncementRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorAnnouncementRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : AnnouncementRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun getLatestAnnouncement(): NetworkResult<LatestAnnouncementResponse, NetworkingError> {
        return server.safeGet(endpoint = "/announcements/latest")
    }

    override suspend fun getAllAnnouncements(): NetworkResult<List<AnnouncementResponse>, NetworkingError> {
        return server.safeGet(endpoint = "/announcements/admin/list")
    }

    override suspend fun createAnnouncement(request: AnnouncementRequest): NetworkResult<AnnouncementResponse, NetworkingError> {
        return server.safePost(endpoint = "/announcements/admin/create", body = request)
    }

    override suspend fun updateAnnouncement(request: AnnouncementRequest): NetworkResult<AnnouncementResponse, NetworkingError> {
        return server.safePost(endpoint = "/announcements/admin/update", body = request)
    }

    override suspend fun deleteAnnouncement(id: Long): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/announcements/admin/delete?id=$id")
    }
}

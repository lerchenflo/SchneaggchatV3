package org.lerchenflo.schneaggchatv3mp.announcement.data.remote

import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.AnnouncementRequest
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.AnnouncementResponse
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.LatestAnnouncementResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/** Server calls for announcements. The admin calls answer NotFound to non-admins. */
interface AnnouncementRemoteDataSource {

    suspend fun getLatestAnnouncement(): NetworkResult<LatestAnnouncementResponse, NetworkingError>

    suspend fun getAllAnnouncements(): NetworkResult<List<AnnouncementResponse>, NetworkingError>

    suspend fun createAnnouncement(request: AnnouncementRequest): NetworkResult<AnnouncementResponse, NetworkingError>

    suspend fun updateAnnouncement(request: AnnouncementRequest): NetworkResult<AnnouncementResponse, NetworkingError>

    suspend fun deleteAnnouncement(id: Long): NetworkResult<Unit, NetworkingError>
}

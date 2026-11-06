package org.lerchenflo.schneaggchatv3mp.announcement.data.remote

import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.AnnouncementRequest
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.AnnouncementResponse
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.LatestAnnouncementResponse
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult

/** Server calls for announcements. The admin calls answer NotFound to non-admins. */
interface AnnouncementRemoteDataSource {

    suspend fun getLatestAnnouncement(): DataResult<LatestAnnouncementResponse, DataError>

    suspend fun getAllAnnouncements(): DataResult<List<AnnouncementResponse>, DataError>

    suspend fun createAnnouncement(request: AnnouncementRequest): DataResult<AnnouncementResponse, DataError>

    suspend fun updateAnnouncement(request: AnnouncementRequest): DataResult<AnnouncementResponse, DataError>

    suspend fun deleteAnnouncement(id: Long): DataResult<Unit, DataError>
}

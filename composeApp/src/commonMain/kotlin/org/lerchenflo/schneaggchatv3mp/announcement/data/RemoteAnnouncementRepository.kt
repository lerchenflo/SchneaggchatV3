package org.lerchenflo.schneaggchatv3mp.announcement.data

import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.AnnouncementRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.toAnnouncement
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.toAnnouncementRequest
import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementDraft
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.EmptyResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.map

/** Talks straight to the server - announcements are never stored on the device. */
class RemoteAnnouncementRepository(
    private val announcementRemoteDataSource: AnnouncementRemoteDataSource,
) : AnnouncementRepository {

    override suspend fun getLatest(): NetworkResult<Announcement?, NetworkingError> =
        announcementRemoteDataSource.getLatestAnnouncement().map { it.announcement?.toAnnouncement() }

    override suspend fun getAll(): NetworkResult<List<Announcement>, NetworkingError> =
        announcementRemoteDataSource.getAllAnnouncements().map { list -> list.map { it.toAnnouncement() } }

    override suspend fun create(draft: AnnouncementDraft): NetworkResult<Announcement, NetworkingError> =
        announcementRemoteDataSource.createAnnouncement(draft.toAnnouncementRequest()).map { it.toAnnouncement() }

    override suspend fun update(id: Long, draft: AnnouncementDraft): NetworkResult<Announcement, NetworkingError> =
        announcementRemoteDataSource.updateAnnouncement(draft.toAnnouncementRequest(id = id)).map { it.toAnnouncement() }

    override suspend fun delete(id: Long): EmptyResult<NetworkingError> =
        announcementRemoteDataSource.deleteAnnouncement(id)
}

package org.lerchenflo.schneaggchatv3mp.announcement.data

import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.AnnouncementRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.toAnnouncement
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto.toAnnouncementRequest
import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementDraft
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.map

/** Talks straight to the server - announcements are never stored on the device. */
class RemoteAnnouncementRepository(
    private val announcementRemoteDataSource: AnnouncementRemoteDataSource,
) : AnnouncementRepository {

    override suspend fun getLatest(): DataResult<Announcement?, DataError> =
        announcementRemoteDataSource.getLatestAnnouncement().map { it.announcement?.toAnnouncement() }

    override suspend fun getAll(): DataResult<List<Announcement>, DataError> =
        announcementRemoteDataSource.getAllAnnouncements().map { list -> list.map { it.toAnnouncement() } }

    override suspend fun create(draft: AnnouncementDraft): DataResult<Announcement, DataError> =
        announcementRemoteDataSource.createAnnouncement(draft.toAnnouncementRequest()).map { it.toAnnouncement() }

    override suspend fun update(id: Long, draft: AnnouncementDraft): DataResult<Announcement, DataError> =
        announcementRemoteDataSource.updateAnnouncement(draft.toAnnouncementRequest(id = id)).map { it.toAnnouncement() }

    override suspend fun delete(id: Long): EmptyDataResult<DataError> =
        announcementRemoteDataSource.deleteAnnouncement(id)
}

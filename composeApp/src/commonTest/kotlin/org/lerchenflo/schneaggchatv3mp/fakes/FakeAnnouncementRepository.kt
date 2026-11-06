package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementDraft
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult

/** [AnnouncementRepository] answering with [latest]; the admin calls answer NotFound like for a non-admin. */
class FakeAnnouncementRepository(
    var latest: Announcement? = null,
) : AnnouncementRepository {

    override suspend fun getLatest(): DataResult<Announcement?, DataError> = DataResult.Success(latest)

    override suspend fun getAll(): DataResult<List<Announcement>, DataError> =
        DataResult.Error(DataError.NotFound())

    override suspend fun create(draft: AnnouncementDraft): DataResult<Announcement, DataError> =
        DataResult.Error(DataError.NotFound())

    override suspend fun update(id: Long, draft: AnnouncementDraft): DataResult<Announcement, DataError> =
        DataResult.Error(DataError.NotFound())

    override suspend fun delete(id: Long): EmptyDataResult<DataError> =
        DataResult.Error(DataError.NotFound())
}

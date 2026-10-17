package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementDraft
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.EmptyResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/** [AnnouncementRepository] answering with [latest]; the admin calls answer NotFound like for a non-admin. */
class FakeAnnouncementRepository(
    var latest: Announcement? = null,
) : AnnouncementRepository {

    override suspend fun getLatest(): NetworkResult<Announcement?, NetworkingError> = NetworkResult.Success(latest)

    override suspend fun getAll(): NetworkResult<List<Announcement>, NetworkingError> =
        NetworkResult.Error(NetworkingError.NotFound())

    override suspend fun create(draft: AnnouncementDraft): NetworkResult<Announcement, NetworkingError> =
        NetworkResult.Error(NetworkingError.NotFound())

    override suspend fun update(id: Long, draft: AnnouncementDraft): NetworkResult<Announcement, NetworkingError> =
        NetworkResult.Error(NetworkingError.NotFound())

    override suspend fun delete(id: Long): EmptyResult<NetworkingError> =
        NetworkResult.Error(NetworkingError.NotFound())
}

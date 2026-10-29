package org.lerchenflo.schneaggchatv3mp.announcement.domain

import org.lerchenflo.schneaggchatv3mp.datasource.network.util.EmptyResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/**
 * Announcements, always read live from the server and never stored. Which one was dismissed
 * lives in the synced settings (`SettingsRepository.setLastDismissedAnnouncementId`).
 */
interface AnnouncementRepository {

    /**
     * The newest active announcement above the account's last dismissed number, or null. The
     * server compares against its stored number, so a dismissal from another device counts even
     * before this device synced its settings.
     */
    suspend fun getLatest(): NetworkResult<Announcement?, NetworkingError>

    // Admin only - the server answers NotFound to everyone else

    /** All announcements including expired ones, newest first. */
    suspend fun getAll(): NetworkResult<List<Announcement>, NetworkingError>

    suspend fun create(draft: AnnouncementDraft): NetworkResult<Announcement, NetworkingError>

    /** Keeps the number, so whoever dismissed it never sees it again. */
    suspend fun update(id: Long, draft: AnnouncementDraft): NetworkResult<Announcement, NetworkingError>

    suspend fun delete(id: Long): EmptyResult<NetworkingError>
}

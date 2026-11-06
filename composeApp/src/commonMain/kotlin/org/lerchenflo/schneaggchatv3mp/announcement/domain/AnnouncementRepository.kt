package org.lerchenflo.schneaggchatv3mp.announcement.domain

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult

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
    suspend fun getLatest(): DataResult<Announcement?, DataError>

    // Admin only - the server answers NotFound to everyone else

    /** All announcements including expired ones, newest first. */
    suspend fun getAll(): DataResult<List<Announcement>, DataError>

    suspend fun create(draft: AnnouncementDraft): DataResult<Announcement, DataError>

    /** Keeps the number, so whoever dismissed it never sees it again. */
    suspend fun update(id: Long, draft: AnnouncementDraft): DataResult<Announcement, DataError>

    suspend fun delete(id: Long): EmptyDataResult<DataError>
}

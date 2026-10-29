package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.data.local.UserLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

//How long a notification may wait for a missing profile picture to download
private val PICTURE_DOWNLOAD_TIMEOUT = 3.seconds

//A user whose picture could not be fetched is not tried again for this long, so every push from
//someone without a picture doesn't pay for another failing download
private val PICTURE_RETRY_INTERVAL = 10.minutes

private val attemptsMutex = Mutex()
private val lastAttempts = mutableMapOf<String, Long>()

/**
 * Downloads [userId]'s profile picture when it is not stored yet (sender not synced yet, a new
 * friend request, a fresh install), so the notification about them can show it. Needs a logged-in
 * session, bounded by [PICTURE_DOWNLOAD_TIMEOUT] and never throws - on any problem the notification
 * just uses the default icon. The picture lands at [PictureManager.getProfilePicFilePath], where the
 * notifier also looks when the user has no database row yet.
 */
suspend fun ensureProfilePictureForNotification(userId: String?) {
    if (userId.isNullOrBlank()) return
    if (SessionCache.requireLoggedIn() == null) return

    val koin = KoinPlatform.getKoin()
    try {
        val pictureManager = koin.get<PictureManager>()
        val storedPath = koin.get<UserLocalDataSource>().getUserById(userId)?.profilePictureUrl
        val hasPicture = listOfNotNull(storedPath?.ifBlank { null }, pictureManager.getProfilePicFilePath(userId, gruppe = false))
            .any { pictureManager.checkImageExists(it) }
        if (hasPicture) return

        val now = Clock.System.now().toEpochMilliseconds()
        val triedRecently = attemptsMutex.withLock {
            val last = lastAttempts[userId]
            if (last != null && now - last < PICTURE_RETRY_INTERVAL.inWholeMilliseconds) {
                true
            } else {
                lastAttempts[userId] = now
                false
            }
        }
        if (triedRecently) return

        val finished = withTimeoutOrNull(PICTURE_DOWNLOAD_TIMEOUT) {
            koin.get<UserRepository>().getProfilePicturesForUserIds(listOf(userId))
        }
        if (finished == null) {
            koin.get<Logger>().logWarning("[Notification] Profile picture of $userId not downloaded within $PICTURE_DOWNLOAD_TIMEOUT, using the default icon")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        runCatching {
            koin.get<Logger>().logError("[Notification] Downloading the profile picture of $userId failed: ${e::class.simpleName}: ${e.message}")
        }
    }
}

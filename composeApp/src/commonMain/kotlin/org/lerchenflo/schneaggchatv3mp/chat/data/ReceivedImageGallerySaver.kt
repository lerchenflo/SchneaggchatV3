package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.flow.first
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.device.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.media.PictureManager

/**
 * Copies a received image into the phone gallery after its first download, when "Save received
 * images to gallery" is on for this device. Skipped: Desktop, own images, re-downloads of an image
 * that was already on this device, and images sent before the start time (so the login sync or a
 * reinstall never floods the gallery).
 */
class ReceivedImageGallerySaver(
    private val pictureManager: PictureManager,
    private val settingsRepository: SettingsRepository,
    private val appVersion: AppVersion,
    private val logger: Logger,
) {

    /**
     * Fixes the start time if it is not set yet. Called at the start of every message sync, so it
     * is set right after login, before the first new image arrives.
     */
    suspend fun ensureStartSet() {
        settingsRepository.getReceivedImagesGalleryStart()
    }

    /** [storedMessage] is the row as it was before the download; [filePath] the downloaded picture. */
    suspend fun saveIfWanted(storedMessage: Message, filePath: String) {
        if (!shouldSave(storedMessage)) return
        val savedTo = pictureManager.saveToGallery(filePath, "Schneaggchat_${storedMessage.id}")
        if (savedTo.isEmpty()) logger.logWarning("Received image ${storedMessage.id}: gallery copy failed")
    }

    private suspend fun shouldSave(message: Message): Boolean {
        val ownId = SessionCache.requireLoggedIn()?.userId ?: return false
        return appVersion.isMobile() &&
            message.senderId != ownId &&
            message.pictureUrl.isNullOrBlank() &&
            settingsRepository.getSaveReceivedImagesToGalleryFlow().first() &&
            message.getSendDateAsLong() >= settingsRepository.getReceivedImagesGalleryStart()
    }
}

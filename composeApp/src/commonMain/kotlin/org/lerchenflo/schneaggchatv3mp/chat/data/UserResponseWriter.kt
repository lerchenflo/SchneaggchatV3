package org.lerchenflo.schneaggchatv3mp.chat.data

import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.chat.data.local.UserLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.toUserEntity
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.UserResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.toSyncedSettings
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager

/**
 * Writes the user rows the server sends (`/users/sync` and the socket `UserChange`) into the local
 * database, so both paths store the same thing. It also applies the profile picture privacy: a
 * picture the server hides (null `profilePicUpdatedAt`) is deleted from this device.
 */
class UserResponseWriter(
    private val userLocalDataSource: UserLocalDataSource,
    private val pictureManager: PictureManager,
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Stores [user], and for the own user its synced settings.
     * @return whether the user's profile picture has to be downloaded
     */
    suspend fun write(user: UserResponse): Boolean {
        val existing = userLocalDataSource.getUserEntityById(user.id)
        val pictureHidden = user.profilePicUpdatedAt == null
        if (pictureHidden) {
            deleteStoredPicture(user.id)
        }

        val profilePictureUrl = if (pictureHidden) "" else existing?.profilePictureUrl ?: ""
        userLocalDataSource.upsertUser(user.toUserEntity(existing, profilePictureUrl))

        if (user is UserResponse.SelfUserResponse) {
            settingsRepository.applyServerSettings(user.settings.toSyncedSettings())
        }

        return needsPictureDownload(storedUpdatedAt = existing?.profilePicUpdatedAt, newUpdatedAt = user.profilePicUpdatedAt)
    }

    /** Deletes a user row together with its stored profile picture. */
    suspend fun delete(userId: String) {
        userLocalDataSource.deleteUser(userId)
        deleteStoredPicture(userId)
    }

    private suspend fun deleteStoredPicture(userId: String) {
        pictureManager.deletePicture(userId + USERPROFILEPICTURE_FILE_NAME)
    }

    // A new row, or a picture that was hidden until now, has no stored file yet
    private fun needsPictureDownload(storedUpdatedAt: Long?, newUpdatedAt: Long?): Boolean {
        if (newUpdatedAt == null) return false
        return storedUpdatedAt == null || storedUpdatedAt < newUpdatedAt
    }
}

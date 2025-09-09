package org.lerchenflo.schneaggchatv3mp.utilities

expect class PictureManager {
    /**
     * Save a base64 encoded picture under [filename].
     * Returns a platform-specific path/uri string on success.
     */
    suspend fun savePictureToStorage(base64Pic: String, filename: String): String

    /**
     * Save a decoded picture byte array under [filename].
     * Returns a platform-specific path/uri string on success.
     */
    suspend fun savePictureToStorage(data: ByteArray, filename: String): String

    /**
     * Load the picture bytes for [filename], or null if not found.
     */
    suspend fun loadPictureFromStorage(filename: String): ByteArray?

    /**
     * Delete picture with [filename]. Returns true if deleted.
     */
    suspend fun deletePicture(filename: String): Boolean
}
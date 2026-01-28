package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.ui.graphics.ImageBitmap

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
    suspend fun loadPictureFromStorage(filename: String): ImageBitmap?

    /**
     * Delete picture with [filename]. Returns true if deleted.
     */
    suspend fun deletePicture(filename: String): Boolean

    /**
     * Get the filepath for a picture
     */
    fun getProfilePicFilePath(id: Long, gruppe: Boolean) : String

    /**
     * Check if a image with this name is in the storage
     */
    fun checkImageExists(filePath: String) : Boolean

    /**
     * Downscale an image to approximately the target size in bytes.
     * Uses iterative quality reduction to achieve target size.
     * @param imageBytes Original image bytes
     * @param targetSizeBytes Target maximum size (default ~500KB)
     * @return Downscaled image bytes as JPEG
     */
    suspend fun downscaleImage(imageBytes: ByteArray, targetSizeBytes: Int = 500_000): ByteArray
}
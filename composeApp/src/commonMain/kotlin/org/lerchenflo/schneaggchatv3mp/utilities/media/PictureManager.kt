package org.lerchenflo.schneaggchatv3mp.utilities.media

import androidx.compose.ui.graphics.ImageBitmap

interface PictureManager {


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

    /**
     * Deletes every picture this app stored (profile, group and message pictures), matched by
     * [org.lerchenflo.schneaggchatv3mp.STORED_PICTURE_FILE_ENDINGS] - the folder also holds other
     * app files. Used on logout. @return the number of deleted files
     */
    suspend fun deleteAllStoredPictures(): Int

    /**
     * Get the filepath for a picture
     */
    fun getProfilePicFilePath(id: String, gruppe: Boolean) : String

    /**
     * Get the filepath for an image message.
     * Generates the same path that savePictureToStorage would produce
     * when saving an image message with the given [messageId].
     */
    fun getImageMessageFilePath(messageId: String): String

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

    /**
     * The picked image behind [uri] exactly as it is stored, metadata included, without the
     * re-encode of the picker's `loadBytes()`: a gallery `content://` uri on Android, the
     * `file:` uri of a camera photo or of the iOS / Desktop picker. Null if it can't be read.
     */
    suspend fun readPickedImage(uri: String): ByteArray?

    /**
     * Saves a ByteArray image to the device's external storage / photo gallery.
     * @return The path or URI string where the file was saved.
     */
    suspend fun downloadImage(
        filePath: String,
        fileName: String
    ): String

    /**
     * Copies the picture file at [filePath] into the device's gallery as it is: full quality, all
     * metadata. Android: album Pictures/Schneaggchat, iOS: Photos (add-only permission, asked on
     * the first save), desktop: ~/Pictures/Schneaggchat. [baseName] gets the extension of the
     * file's real format ([ImageFileFormat]).
     * @return where the copy landed, "" when it failed or the permission was denied
     */
    suspend fun saveToGallery(filePath: String, baseName: String): String

    /**
     * Encode a Compose [ImageBitmap] to PNG bytes.
     */
    suspend fun encodeImageBitmap(bitmap: ImageBitmap): ByteArray
}

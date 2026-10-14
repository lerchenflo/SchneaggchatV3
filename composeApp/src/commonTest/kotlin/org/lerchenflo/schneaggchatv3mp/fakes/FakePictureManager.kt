package org.lerchenflo.schneaggchatv3mp.fakes

import androidx.compose.ui.graphics.ImageBitmap
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager

/**
 * In-memory [PictureManager]: "files" live in [storedPictures], paths are `pictures/<name>`.
 * [downscaleImage] returns [downscaledImage] when set, otherwise the input unchanged.
 */
class FakePictureManager : PictureManager {

    val storedPictures = mutableMapOf<String, ByteArray>()

    var downscaledImage: ByteArray? = null

    val downscaleRequests = CallLog<ByteArray>()
    val downloadedImages = CallLog<Pair<String, String>>()

    override suspend fun savePictureToStorage(base64Pic: String, filename: String): String =
        savePictureToStorage(base64Pic.encodeToByteArray(), filename)

    override suspend fun savePictureToStorage(data: ByteArray, filename: String): String {
        storedPictures[filename] = data
        return path(filename)
    }

    override suspend fun loadPictureFromStorage(filename: String): ByteArray? = storedPictures[filename]

    override suspend fun deletePicture(filename: String): Boolean = storedPictures.remove(filename) != null

    override fun getProfilePicFilePath(id: String, gruppe: Boolean): String = path(if (gruppe) "group_$id" else "user_$id")

    override fun getImageMessageFilePath(messageId: String): String = path("message_$messageId")

    override fun checkImageExists(filePath: String): Boolean = storedPictures.keys.any { path(it) == filePath }

    override suspend fun downscaleImage(imageBytes: ByteArray, targetSizeBytes: Int): ByteArray {
        downscaleRequests.record(imageBytes)
        return downscaledImage ?: imageBytes
    }

    override suspend fun downloadImage(filePath: String, fileName: String): String {
        downloadedImages.record(filePath to fileName)
        return path(fileName)
    }

    override suspend fun encodeImageBitmap(bitmap: ImageBitmap): ByteArray = ByteArray(0)

    private fun path(filename: String) = "pictures/$filename"
}

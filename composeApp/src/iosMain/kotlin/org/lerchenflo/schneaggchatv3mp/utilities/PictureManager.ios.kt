package org.lerchenflo.schneaggchatv3mp.utilities

actual class PictureManager {
    actual suspend fun savePictureToStorage(
        base64Pic: String,
        filename: String
    ): String {
        TODO("Not yet implemented")
    }

    actual suspend fun savePictureToStorage(
        data: ByteArray,
        filename: String
    ): String {
        TODO("Not yet implemented")
    }

    actual suspend fun loadPictureFromStorage(filename: String): ByteArray? {
        TODO("Not yet implemented")
    }

    actual suspend fun deletePicture(filename: String): Boolean {
        TODO("Not yet implemented")
    }
}
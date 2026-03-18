package org.lerchenflo.schneaggchatv3mp.utilities

/**
 * For all the platform specific Code for audio recording
 */
expect class AudioManager {
    /**
     * initialize the Audio Player on Android (because Context is needed)
     */
    fun initializeAudio()
    /**
     * returns a valid Path (no JVM Impementation)
     */
    fun getRecordingPath(filename: String): String

    /**
     * saves an audio to storage
     */
    suspend fun saveAudioToStorage(audioBytes: ByteArray, filename: String) : String

    /**
     * deletes file from storage
     */
    suspend fun deleteAudio(filename: String) : Boolean

    /**
     * checks if file exists
     */

    fun checkAudioExists(filePath: String) : Boolean
}

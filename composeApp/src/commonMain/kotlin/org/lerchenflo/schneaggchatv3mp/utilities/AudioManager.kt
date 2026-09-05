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

    /**
     * returns audio duration
     */
    suspend fun getMediaDuration(path: String): Long

    /**
     * builds and buffers the beep tone up front so that the first [startBeep] starts without the
     * delay of setting up a platform player. Call it when a screen that beeps becomes visible.
     */
    fun prepareBeep(frequencyHz: Int = DEFAULT_BEEP_FREQUENCY_HZ)

    /**
     * starts a continuous beep tone that keeps sounding until [stopBeep] is called
     */
    fun startBeep(frequencyHz: Int = DEFAULT_BEEP_FREQUENCY_HZ)

    /**
     * stops the tone started by [startBeep], does nothing when no tone is running. The prepared
     * player stays alive so the next [startBeep] is instant again.
     */
    fun stopBeep()

    /**
     * stops the tone and frees the prepared player
     */
    fun releaseBeep()
}

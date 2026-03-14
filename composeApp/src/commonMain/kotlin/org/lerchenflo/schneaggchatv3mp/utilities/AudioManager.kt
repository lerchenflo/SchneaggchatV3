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
}

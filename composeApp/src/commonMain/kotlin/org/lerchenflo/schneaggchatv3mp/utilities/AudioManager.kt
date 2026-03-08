package org.lerchenflo.schneaggchatv3mp.utilities

expect class AudioManager {
    fun initializeAudio()
    fun getRecordingPath(filename: String): String
}

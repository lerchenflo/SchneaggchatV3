package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.Context
import io.github.hyochan.audio.initializeAudioRecorderPlayer
import java.io.File

actual class AudioManager(private val context: Context){
    actual fun initializeAudio() {
        initializeAudioRecorderPlayer(context)
    }

    actual fun getRecordingPath(filename: String): String {
        return File(context.cacheDir, filename).absolutePath
    }
}

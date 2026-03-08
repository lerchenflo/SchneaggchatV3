package org.lerchenflo.schneaggchatv3mp.utilities

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual class AudioManager {
    actual fun initializeAudio() {
    }

    actual fun getRecordingPath(filename: String): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val cachesDirectory = paths[0] as String
        return "$cachesDirectory/$filename"
    }
}

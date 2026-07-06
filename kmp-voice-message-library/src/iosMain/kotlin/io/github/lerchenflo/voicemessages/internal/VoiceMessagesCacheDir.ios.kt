package io.github.lerchenflo.voicemessages.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@Composable
internal actual fun rememberVoiceMessagesCacheDir(): String {
    return remember {
        val cachesDir = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true,
        ).first() as String
        "$cachesDir/voice_messages"
    }
}

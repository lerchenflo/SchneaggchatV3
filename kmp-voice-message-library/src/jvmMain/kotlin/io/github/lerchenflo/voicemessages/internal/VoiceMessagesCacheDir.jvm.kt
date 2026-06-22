package io.github.lerchenflo.voicemessages.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File

@Composable
internal actual fun rememberVoiceMessagesCacheDir(): String {
    return remember {
        File(System.getProperty("java.io.tmpdir"), "kmp-voice-messages").absolutePath
    }
}

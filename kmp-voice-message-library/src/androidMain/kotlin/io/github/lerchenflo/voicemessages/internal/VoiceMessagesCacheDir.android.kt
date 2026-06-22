package io.github.lerchenflo.voicemessages.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberVoiceMessagesCacheDir(): String {
    val context = LocalContext.current
    return remember(context) {
        context.cacheDir.resolve("voice_messages").absolutePath
    }
}

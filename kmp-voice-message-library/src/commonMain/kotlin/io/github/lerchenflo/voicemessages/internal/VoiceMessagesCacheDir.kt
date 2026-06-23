package io.github.lerchenflo.voicemessages.internal

import androidx.compose.runtime.Composable

/**
 * Resolves the absolute path to a scratch directory this platform can use for the temporary
 * files [io.github.lerchenflo.voicemessages.ui.VoiceMessageRecorder] and
 * [io.github.lerchenflo.voicemessages.ui.VoiceMessagePlayer] read and write.
 */
@Composable
internal expect fun rememberVoiceMessagesCacheDir(): String

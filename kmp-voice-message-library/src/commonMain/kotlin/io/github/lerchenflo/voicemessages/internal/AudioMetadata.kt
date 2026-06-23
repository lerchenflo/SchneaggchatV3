package io.github.lerchenflo.voicemessages.internal

/**
 * Reads just the duration of the audio file at [filePath] from its format header/metadata,
 * without decoding or buffering the audio data itself. Returns 0 if it can't be determined.
 */
internal expect fun probeDurationMs(filePath: String): Long

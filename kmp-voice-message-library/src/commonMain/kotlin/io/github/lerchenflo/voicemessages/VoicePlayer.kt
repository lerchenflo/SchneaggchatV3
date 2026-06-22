package io.github.lerchenflo.voicemessages

/**
 * Plays a voice message back from a file on disk.
 */
internal expect class VoicePlayer() {

    /** Current playback position, in milliseconds. 0 if nothing is playing. */
    val positionMs: Long

    /** Duration of the voice message currently loaded, in milliseconds. 0 if nothing is loaded. */
    val durationMs: Long

    /** Whether playback is currently in progress. */
    val isPlaying: Boolean

    /** Starts playing the voice message at [filePath] from the beginning. */
    fun play(filePath: String)

    /** Pauses playback. No-op if not currently playing. */
    fun pause()

    /** Stops playback and releases the underlying player. No-op if not currently playing. */
    fun stop()
}

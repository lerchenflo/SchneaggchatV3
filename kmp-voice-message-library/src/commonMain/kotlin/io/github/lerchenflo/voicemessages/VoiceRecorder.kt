package io.github.lerchenflo.voicemessages

/**
 * Records a voice message to a file on disk.
 *
 * Each platform records into its own native audio container (AAC/m4a on Android & iOS, WAV
 * on desktop).
 *
 * On Android, the caller is responsible for requesting the `RECORD_AUDIO` permission before
 * calling [start]. On iOS, `NSMicrophoneUsageDescription` must be set in `Info.plist`.
 */
internal expect class VoiceRecorder() {

    /** Whether a recording is currently in progress. */
    val isRecording: Boolean

    /** Starts recording into [filePath], overwriting it if it already exists. */
    suspend fun start(filePath: String)

    /** Stops the current recording, finalizing the file. No-op if not currently recording. */
    fun stop()
}

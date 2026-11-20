package org.lerchenflo.schneaggchatv3mp.utilities

/**
 * Platform beep tones (Morse game). Voice messages live in the kmp-voice-messages library
 * (`VoiceMessageFiles`, `VoicePlaybackController`, `VoiceRecordingController`).
 */
interface AudioManager {
    /**
     * builds and buffers the beep tone up front so that the first [startBeep] starts without the
     * delay of setting up a platform player. Call it when a screen that beeps becomes visible.
     */
    fun prepareBeep(frequencyHz: Int = DEFAULT_BEEP_FREQUENCY_HZ)

    /**
     * starts a continuous beep tone that keeps sounding until [stopBeep] is called
     */
    fun startBeep(frequencyHz: Int = DEFAULT_BEEP_FREQUENCY_HZ)

    /**
     * stops the tone started by [startBeep], does nothing when no tone is running. The prepared
     * player stays alive so the next [startBeep] is instant again.
     */
    fun stopBeep()

    /**
     * stops the tone and frees the prepared player
     */
    fun releaseBeep()
}

package org.lerchenflo.schneaggchatv3mp.utilities

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

class DesktopAudioManager : AudioManager {

    private var beepClip: Clip? = null
    private var beepClipGain: FloatControl? = null
    private var beepClipFrequencyHz = 0

    override fun prepareBeep(frequencyHz: Int) {
        if (beepClip != null && beepClipFrequencyHz == frequencyHz) return
        releaseBeep()

        try {
            val pcm = generateBeepPcm16(frequencyHz)
            val format = AudioFormat(BEEP_SAMPLE_RATE_HZ.toFloat(), 16, 1, true, false)
            val clip = AudioSystem.getClip()
            clip.open(format, pcm, 0, pcm.size)
            beepClip = clip
            beepClipFrequencyHz = frequencyHz

            // Where the mixer offers a gain control the tone loops silently for as long as it stays
            // prepared, so starting it is only a gain change instead of starting playback.
            beepClipGain = clip.takeIf { it.isControlSupported(FloatControl.Type.MASTER_GAIN) }
                ?.let { it.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl }
            beepClipGain?.let {
                it.value = it.minimum
                clip.loop(Clip.LOOP_CONTINUOUSLY)
            }
        } catch (e: Exception) {
            println("Desktop: Failed to prepare beep: ${e.message}")
        }
    }

    override fun startBeep(frequencyHz: Int) {
        prepareBeep(frequencyHz)
        val clip = beepClip ?: return
        val gain = beepClipGain
        if (gain == null) {
            clip.framePosition = 0
            clip.loop(Clip.LOOP_CONTINUOUSLY)
        } else {
            gain.value = 0f.coerceIn(gain.minimum, gain.maximum)
        }
    }

    override fun stopBeep() {
        val gain = beepClipGain
        if (gain == null) {
            beepClip?.stop()
        } else {
            gain.value = gain.minimum
        }
    }

    override fun releaseBeep() {
        beepClip?.let {
            it.stop()
            it.close()
        }
        beepClip = null
        beepClipGain = null
        beepClipFrequencyHz = 0
    }
}
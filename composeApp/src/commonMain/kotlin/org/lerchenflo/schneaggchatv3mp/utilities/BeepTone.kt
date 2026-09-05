package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

const val DEFAULT_BEEP_FREQUENCY_HZ = 700

// 48 kHz is the native output rate of virtually every phone, and only a tone generated at the
// native rate can take the platform's low latency audio path instead of going through a resampler.
internal const val BEEP_SAMPLE_RATE_HZ = 48000

private const val BEEP_AMPLITUDE = 0.35
private const val MIN_LOOP_SECONDS = 0.2

/**
 * Plays a beep for [durationMs] and stops it again, also when the caller gets cancelled.
 */
suspend fun AudioManager.playBeep(
    durationMs: Long,
    frequencyHz: Int = DEFAULT_BEEP_FREQUENCY_HZ
) {
    startBeep(frequencyHz)
    try {
        delay(durationMs.milliseconds)
    } finally {
        stopBeep()
    }
}

/**
 * Mono 16 bit little endian sine wave holding a whole number of periods, so the platform players
 * can loop it into one continuous tone without a click at the wrap-around point.
 */
internal fun generateBeepPcm16(frequencyHz: Int, sampleRateHz: Int = BEEP_SAMPLE_RATE_HZ): ByteArray {
    val frameCount = seamlessLoopFrameCount(frequencyHz, sampleRateHz)
    val pcm = ByteArray(frameCount * 2)
    for (frame in 0 until frameCount) {
        val angle = 2.0 * PI * frequencyHz * frame / sampleRateHz
        val sample = (sin(angle) * BEEP_AMPLITUDE * Short.MAX_VALUE).roundToInt()
        pcm[frame * 2] = sample.toByte()
        pcm[frame * 2 + 1] = (sample shr 8).toByte()
    }
    return pcm
}

private fun seamlessLoopFrameCount(frequencyHz: Int, sampleRateHz: Int): Int {
    val framesPerPeriodBlock = sampleRateHz / greatestCommonDivisor(sampleRateHz, frequencyHz)
    val minFrames = (sampleRateHz * MIN_LOOP_SECONDS).toInt()
    val blocks = (minFrames + framesPerPeriodBlock - 1) / framesPerPeriodBlock
    return framesPerPeriodBlock * blocks
}

private tailrec fun greatestCommonDivisor(a: Int, b: Int): Int = if (b == 0) a else greatestCommonDivisor(b, a % b)

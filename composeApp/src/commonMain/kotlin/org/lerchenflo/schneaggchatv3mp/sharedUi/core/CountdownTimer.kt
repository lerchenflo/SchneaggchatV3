package org.lerchenflo.schneaggchatv3mp.sharedUi.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Ticks every second and returns the current value of [computeRemainingMillis], clamped to >= 0.
 * [key] restarts the ticking loop - pass the countdown target (or [Unit] if [computeRemainingMillis]
 * re-derives its own target every tick, e.g. "time until next midnight").
 */
@Composable
fun rememberCountdownMillis(key: Any?, computeRemainingMillis: () -> Long): Long {
    var remaining by remember(key) { mutableStateOf(computeRemainingMillis().coerceAtLeast(0L)) }

    LaunchedEffect(key) {
        while (true) {
            remaining = computeRemainingMillis().coerceAtLeast(0L)
            delay(1000L.milliseconds)
        }
    }

    return remaining
}

/** Formats a millisecond duration as "HH:MM:SS", prefixed with "Xd " once it spans a full day. */
fun formatCountdown(remainingMillis: Long): String {
    val d = remainingMillis / (24 * 60 * 60 * 1000)
    val h = ((remainingMillis / (60 * 60 * 1000)) % 24).toString().padStart(2, '0')
    val m = ((remainingMillis / (60 * 1000)) % 60).toString().padStart(2, '0')
    val s = ((remainingMillis / 1000) % 60).toString().padStart(2, '0')
    return if (d > 0) "${d}d $h:$m:$s" else "$h:$m:$s"
}

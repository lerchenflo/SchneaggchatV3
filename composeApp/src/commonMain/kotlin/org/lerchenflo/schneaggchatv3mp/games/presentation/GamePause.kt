package org.lerchenflo.schneaggchatv3mp.games.presentation

import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Suspends while [isPaused] returns true, polling every [tickMs]. Returns the
 * wall-clock time actually spent paused (0 if not currently paused), so a game
 * loop can shift its absolute time anchors (start times, deadlines) forward by
 * that amount instead of letting elapsed/remaining time jump when it resumes.
 *
 * Call this once per loop iteration, before any time-based work for that tick.
 */
suspend fun awaitResume(tickMs: Long = 32L, isPaused: () -> Boolean): Long {
    if (!isPaused()) return 0L
    val pauseStart = Clock.System.now().toEpochMilliseconds()
    while (isPaused()) {
        delay(tickMs.milliseconds)
    }
    return Clock.System.now().toEpochMilliseconds() - pauseStart
}

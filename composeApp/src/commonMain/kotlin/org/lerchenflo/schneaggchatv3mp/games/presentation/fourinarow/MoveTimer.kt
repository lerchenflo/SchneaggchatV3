package org.lerchenflo.schneaggchatv3mp.games.presentation.fourinarow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Counts one move down in whole seconds. [pause] keeps the seconds left, so leaving the screen
 * does not cost the current player their turn.
 */
class MoveTimer(
    private val scope: CoroutineScope,
    private val onTick: (secondsLeft: Int) -> Unit,
    private val onTimeout: () -> Unit,
) {
    private var job: Job? = null
    private var secondsLeft = 0
    private var running = false

    /** Starts a new countdown of [seconds]; runs only while not paused. */
    fun start(seconds: Int, paused: Boolean) {
        secondsLeft = seconds
        running = true
        onTick(secondsLeft)
        if (paused) job?.cancel() else countDown()
    }

    fun stop() {
        running = false
        job?.cancel()
    }

    fun pause() {
        job?.cancel()
    }

    fun resume() {
        if (running) countDown()
    }

    private fun countDown() {
        job?.cancel()
        job = scope.launch {
            while (secondsLeft > 0) {
                delay(1.seconds)
                secondsLeft--
                onTick(secondsLeft)
            }
            running = false
            onTimeout()
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoRunnerManUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.LassoGrab
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

// Very rarely Stanislaus comes running along the track: he runs in from the right, keeps pace next
// to the horse for a few seconds, then falls behind. Lasso him for a big bonus - the pack is so
// baffled it falls all the way back.

/** Game seconds until the first chance he comes, and between two chances; he only comes on some. */
private const val FIRST_CHANCE_SECONDS = 120f
private const val CHANCE_INTERVAL_MIN = 150f
private const val CHANCE_INTERVAL_RANDOM = 120f
private const val COME_CHANCE = 0.5f
/** Where he keeps pace, ahead of the horse in lasso range, and for how long. */
private const val ALONGSIDE_X = HORSE_X + 38f
private const val ALONGSIDE_SECONDS = 7f
/** u/s he drifts in from the right and falls behind afterwards, relative to the horse. */
private const val DRIFT_IN = 30f
private const val FALL_BEHIND = 18f
private const val STRIDE_SPEED = 14f          // hops per second times 2π-ish
private const val HOP_HEIGHT = 1.5f
private const val CATCH_POINTS = 150

private enum class RunnerState { COMING, ALONGSIDE, LEAVING, CAUGHT }

internal class RodeoStanislausRunner {
    private var x = 0f
    private var y = 0f
    private var stride = 0f
    private var stateTime = 0f
    private var state: RunnerState? = null
    private var nextChanceIn = FIRST_CHANCE_SECONDS

    /** He is somewhere in the picture. */
    val isAround: Boolean get() = state != null

    fun reset() {
        state = null
        nextChanceIn = FIRST_CHANCE_SECONDS
    }

    /** Now and then, while [allowed], he may come running in from the right edge. */
    fun tick(dt: Float, worldWidth: Float, allowed: Boolean) {
        if (state != null || !allowed) return
        nextChanceIn -= dt
        if (nextChanceIn > 0f) return
        nextChanceIn = CHANCE_INTERVAL_MIN + Random.nextFloat() * CHANCE_INTERVAL_RANDOM
        if (Random.nextFloat() >= COME_CHANCE) return
        x = worldWidth + 5f
        y = 0f
        enter(RunnerState.COMING)
    }

    fun step(dt: Float) {
        val state = state ?: return
        stateTime += dt
        stride += dt * STRIDE_SPEED
        when (state) {
            RunnerState.COMING -> {
                x = max(ALONGSIDE_X, x - DRIFT_IN * dt)
                if (x <= ALONGSIDE_X) enter(RunnerState.ALONGSIDE)
            }
            RunnerState.ALONGSIDE -> if (stateTime >= ALONGSIDE_SECONDS) enter(RunnerState.LEAVING)
            RunnerState.LEAVING -> {
                x -= FALL_BEHIND * dt
                if (x < -10f) this.state = null
            }
            RunnerState.CAUGHT -> Unit // the lasso moves him
        }
        if (state != RunnerState.CAUGHT) y = HOP_HEIGHT * abs(sin(stride))
    }

    private fun enter(newState: RunnerState) {
        state = newState
        stateTime = 0f
    }

    /** He is in [reach] when the loop arrives in [timeToCatch]; [onCaught] once the loop brought him in. */
    fun lassoGrab(reach: ClosedFloatingPointRange<Float>, timeToCatch: Float, onCaught: () -> Unit): LassoGrab? {
        val current = state ?: return null
        if (current == RunnerState.CAUGHT) return null
        val drift = when (current) {
            RunnerState.COMING -> DRIFT_IN
            RunnerState.LEAVING -> FALL_BEHIND
            else -> 0f
        }
        if (x - drift * timeToCatch !in reach && x !in reach) return null
        return object : LassoGrab {
            override val x: Float get() = this@RodeoStanislausRunner.x
            override val y: Float get() = this@RodeoStanislausRunner.y + 5f
            override fun catch(): Boolean {
                if (state == null || state == RunnerState.CAUGHT) return false
                enter(RunnerState.CAUGHT)
                return true
            }
            override fun follow(tipX: Float, tipY: Float) {
                this@RodeoStanislausRunner.x = tipX
                this@RodeoStanislausRunner.y = tipY - 5f
            }
            override fun release() {
                state = null
                onCaught()
            }
        }
    }

    /** Points for catching him. */
    val catchPoints: Int get() = CATCH_POINTS

    fun ui(): RodeoRunnerManUi? = state?.let { RodeoRunnerManUi(x = x, y = y, hop = stride, caught = it == RunnerState.CAUGHT) }
}

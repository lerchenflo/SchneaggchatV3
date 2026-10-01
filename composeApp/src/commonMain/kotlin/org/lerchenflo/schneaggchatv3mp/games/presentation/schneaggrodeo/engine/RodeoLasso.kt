package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoLassoUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSnailUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.LassoGrab
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

// The lasso flies out from the hand and back over LASSO_DURATION. Halfway (at the far end) it
// decides whether it caught something; a catch dangles in the loop on the way back.
private const val LASSO_RANGE = 37f
private const val LASSO_DURATION = 0.45f   // out and back
/** Counted from the throw, so the next throw is ready LASSO_COOLDOWN - LASSO_DURATION after the loop is back. */
private const val LASSO_COOLDOWN = 0.52f
/** Extra reach at catch time, absorbs stumbles and speed changes. */
private const val LASSO_CATCH_TOLERANCE = 6f

/**
 * The lasso's throw. Three kinds of targets: a snail of [snails] ahead, a [LassoGrab] (a passing
 * vehicle or a vehicle's own target like a flying milk can), or - on foot - the horse's saddle.
 */
internal class RodeoLasso(private val snails: MutableList<Snail>) {
    /** Seconds into the throw; < 0 while no lasso is out. */
    private var time = -1f
    private var cooldown = 0f
    /** The far end was reached and the catch decided. */
    private var resolved = false
    private var target: Snail? = null
    private var grab: LassoGrab? = null
    private var grabbed = false
    private var aimX = 0f
    private var aimY = 0f
    private var tipX = 0f
    private var tipY = 0f

    val isOut: Boolean get() = time >= 0f
    val isReady: Boolean get() = !isOut && cooldown <= 0f

    fun reset() {
        time = -1f
        cooldown = 0f
        target = null
        grab = null
        grabbed = false
    }

    fun cool(dt: Float) {
        cooldown = countDown(cooldown, dt)
    }

    /** Ready to throw again right away (after falling off / getting back up). */
    fun clearCooldown() {
        cooldown = 0f
    }

    /**
     * Thrown from the saddle. [vehicleGrab] offers a vehicle target for the reach and the time the
     * loop takes to get there; it beats any snail. Otherwise the loop homes in on the closest snail
     * in reach, picked by where it will be when the loop arrives - at full speed a snail scrolls
     * ~40 u during the throw, so its current position would already be behind the hand.
     */
    fun throwFromSaddle(
        groundSpeed: Float,
        vehicleGrab: (reach: ClosedFloatingPointRange<Float>, timeToCatch: Float) -> LassoGrab?,
    ) {
        val handX = HORSE_X + HAND_X
        val timeToCatch = LASSO_DURATION / 2f
        startThrow()
        grabbed = false
        grab = vehicleGrab((handX - 2f)..(handX + LASSO_RANGE), timeToCatch)
        target = if (grab != null) null else snails
            .filter { it.state == SnailState.ACTIVE }
            .map { it to it.predictedX(groundSpeed, timeToCatch) }
            .filter { (_, x) -> x > handX - 2f && x < handX + LASSO_RANGE }
            .minByOrNull { (_, x) -> x }
            ?.first
    }

    /** Thrown on foot, ahead at the horse (see [stepToSaddle]). */
    fun throwOnFoot() {
        startThrow()
    }

    private fun startThrow() {
        time = 0f
        cooldown = LASSO_COOLDOWN
        resolved = false
    }

    /**
     * Moves a throw from the saddle on; the hand is at [horseBase] + HAND_Y. [onSnailCaught] rewards
     * a caught snail. [cutShort] ends the throw at once (boarding a vehicle).
     */
    fun step(dt: Float, horseBase: Float, cutShort: Boolean, onSnailCaught: () -> Unit) {
        cool(dt)
        if (time < 0f) return
        time += dt
        val progress = progressOf(time, LASSO_DURATION)
        val handX = HORSE_X + HAND_X
        val handY = horseBase + HAND_Y
        val reach = (handX - LASSO_CATCH_TOLERANCE)..(handX + LASSO_RANGE + LASSO_CATCH_TOLERANCE)
        val target = target
        val grab = grab

        if (grab != null) {
            if (!resolved) {
                aimX = grab.x
                aimY = grab.y
                if (progress >= 0.5f) {
                    resolved = true
                    grabbed = grab.x in reach && grab.catch()
                    if (!grabbed) this.grab = null
                }
            }
        } else if (!resolved) {
            if (target != null && target.state == SnailState.ACTIVE) {
                aimX = target.x
                aimY = target.height + SNAIL_BODY_HEIGHT / 2f
            } else {
                // Nothing to catch: straight ahead, a little down
                aimX = handX + LASSO_RANGE
                aimY = max(horseBase + 2f, handY - 12f)
            }
            if (progress >= 0.5f) {
                resolved = true
                if (target != null && target.state == SnailState.ACTIVE && target.x in reach) {
                    target.state = SnailState.LASSOED
                    target.onFence = null
                    onSnailCaught()
                } else {
                    this.target = null
                }
            }
        }

        moveTip(handX, handY, progress)
        caughtSnail()?.let { caught ->
            caught.x = tipX
            caught.height = tipY - SNAIL_BODY_HEIGHT / 2f
        }
        if (grabbed) this.grab?.follow(tipX, tipY)

        if (progress >= 1f || cutShort) end()
    }

    /**
     * Moves a throw on foot on, from the cowboy's hand at [handX] / [handY] towards the saddle at
     * [seatX] / [seatY] while it is [inReach], otherwise straight ahead as far as [reach]. Returns
     * true once it caught the saddle (halfway through the throw, if it is in reach by then); the
     * throw is over then. Out of reach the loop comes back empty.
     */
    fun stepToSaddle(dt: Float, handX: Float, handY: Float, seatX: Float, seatY: Float, reach: Float, inReach: Boolean): Boolean {
        if (time < 0f) return false
        time += dt
        val progress = progressOf(time, LASSO_DURATION)
        if (!resolved) {
            aimX = if (inReach) seatX else handX + reach
            aimY = if (inReach) seatY else handY
            if (progress >= 0.5f) {
                resolved = true
                if (inReach) {
                    time = -1f
                    return true
                }
            }
        }
        moveTip(handX, handY, progress)
        if (progress >= 1f) time = -1f
        return false
    }

    /** The throw is over: a caught snail is taken off the track, a caught grab let go. */
    fun end() {
        caughtSnail()?.let { snails.remove(it) }
        target = null
        if (grabbed) grab?.release()
        grab = null
        grabbed = false
        time = -1f
    }

    private fun caughtSnail(): Snail? = target?.takeIf { it.state == SnailState.LASSOED }

    /** The loop extends along a sine: out to the aim at half time, back to the hand at the end. */
    private fun moveTip(handX: Float, handY: Float, progress: Float) {
        val extension = sin(PI.toFloat() * progress)
        tipX = handX + (aimX - handX) * extension
        tipY = handY + (aimY - handY) * extension
    }

    /** Rope from the rider's hand while riding, with a caught snail dangling in the loop. */
    fun uiFromSaddle(horseBase: Float): RodeoLassoUi? = if (isOut) {
        RodeoLassoUi(
            handX = HORSE_X + HAND_X,
            handY = horseBase + HAND_Y,
            tipX = tipX,
            tipY = tipY,
            caught = caughtSnail()?.let { RodeoSnailUi(it.x, it.height, facingLeft = true, tiltDeg = -20f) },
        )
    } else null

    /** Rope from the cowboy's hand on foot. */
    fun uiOnFoot(handX: Float, handY: Float): RodeoLassoUi? =
        if (isOut) RodeoLassoUi(handX = handX, handY = handY, tipX = tipX, tipY = tipY, caught = null) else null
}

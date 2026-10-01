package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoDustUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMudUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSpotlightUi
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// The end of a run: the ravens pecked the cowboy once more than he had parts to lose. They grab him
// and lift him out of the saddle, struggle with him and drop him into a puddle of mud, grab him
// again and fly off with him - he dangles under them and waves goodbye. The horse gallops off.
// The view follows him up and zooms in while the picture goes dark round him, closing in on him
// like at the end of a cartoon - then the run is over.

private enum class DeathPhase { LIFT, DROP, DOWN, CARRY }

/** The middle of his body above his feet (see drawCowboy). */
private const val MIDDLE_Y = 9f
/** Where the ravens hold him: this far above his middle (round his shoulders and arms). */
private const val GRIP_ABOVE = 8f
// Lifted out of the saddle, wobbling as they struggle with his weight
private const val LIFT_SECONDS = 1.1f
private const val LIFT_HEIGHT = 12f
private const val STRUGGLE_WOBBLE = 1.2f
// They lose their grip: he tumbles backwards into the mud
private const val GRAVITY = 120f
private const val DROP_BACK = -5f
private const val TUMBLE_SPEED = -320f
/** He lies on his back: rotated a quarter turn, head to the left. */
private const val LYING_ROTATION = -90f
/** His middle above the ground while he lies there (half his thickness). */
private const val LYING_MIDDLE = 1.4f
private const val SETTLE_SECONDS = 0.35f
/** Lying in the mud: a moment of stillness, then the ravens come down and grab him again. */
private const val DOWN_STILL_SECONDS = 0.5f
private const val REGRAB_SECONDS = 0.7f
// Carried off: up and a little back, turning upright to dangle under them
private const val CARRY_ACCELERATION = 26f
private const val CARRY_MAX_SPEED = 34f
private const val CARRY_DRIFT = -4f
private const val UPRIGHT_SECONDS = 0.6f
// The cartoon ending: once he dangles upright the dark closes in round him and the view zooms in,
// holds on him waving, then closes all the way
private const val IRIS_START = 0.6f
private const val IRIS_SHRINK_SECONDS = 1.4f
private const val IRIS_HOLD_SECONDS = 1.4f
private const val IRIS_CLOSE_SECONDS = 0.5f
private const val OVER_PAUSE = 0.3f
/** Radius of the light round him: covering the whole screen at first, then just him and the ravens. */
private const val IRIS_FULL_RADIUS = 110f
private const val IRIS_RADIUS = 14f
private const val IRIS_ZOOM = 1.8f
/** The light is centered this far above his middle, between him and the ravens. */
private const val FOCUS_ABOVE = 3f
/** The view keeps him in the middle of the screen while they carry him up. */
private const val SCREEN_MIDDLE = WORLD_HEIGHT_UNITS / 2f - GROUND_OFFSET_UNITS
/** How fast his limbs flail (radians per second), and how fast he waves. */
private const val FLAIL_SPEED = 14f
private const val DANGLE_FLAIL_SPEED = 5f
private const val WAVE_SPEED = 9f
private const val PUDDLE_WIDTH = 18f
private const val SPLASH_SECONDS = 0.5f
/** The horse shies for a moment, then gallops off and out of the picture. */
private const val HORSE_SHY_SECONDS = 0.4f
private const val HORSE_RUN_ACCELERATION = 70f
private const val HORSE_RUN_SPEED = 80f

internal class RodeoDeath {
    var isActive = false
        private set
    /** He is gone: show the game over screen. */
    var isOver = false
        private set
    private var phase = DeathPhase.LIFT
    private var phaseTime = 0f
    private var x = 0f
    private var middleY = 0f
    private var startY = 0f
    private var vx = 0f
    private var vy = 0f
    private var rotation = 0f
    private var landRotation = 0f
    private var flail = 0f
    private var wave = 0f
    /** Where the ravens are while he lies in the mud: where they dropped him. */
    private var hoverX = 0f
    private var hoverY = 0f
    private var puddleX = 0f
    private var puddleSeed = 0
    private var landedTime = -1f
    private var horseTime = 0f
    private var horseSpeed = 0f
    /** How far the horse ran off to the right since the ravens grabbed him. */
    var horseRun = 0f
        private set

    /** Middle of the flock of ravens and whether they hold him (else they hover, or come down to him). */
    val flockX: Float
        get() = when (phase) {
            DeathPhase.DROP -> hoverX
            DeathPhase.DOWN -> lerp(hoverX, x, regrab())
            else -> x
        }
    val flockY: Float
        get() = when (phase) {
            DeathPhase.DROP -> hoverY
            DeathPhase.DOWN -> lerp(hoverY, middleY + GRIP_ABOVE / 2f, regrab())
            DeathPhase.CARRY -> middleY + lerp(GRIP_ABOVE / 2f, GRIP_ABOVE, upright())
            DeathPhase.LIFT -> middleY + GRIP_ABOVE
        }
    val ravensHold: Boolean get() = phase == DeathPhase.LIFT || phase == DeathPhase.CARRY

    fun reset() {
        isActive = false
        isOver = false
    }

    /** The ravens grab him by his middle at [startX] / [startY]. */
    fun start(startX: Float, startY: Float) {
        isActive = true
        isOver = false
        phase = DeathPhase.LIFT
        phaseTime = 0f
        x = startX
        middleY = startY
        this.startY = startY
        rotation = 0f
        flail = 0f
        wave = 0f
        landedTime = -1f
        puddleSeed = (startX * 7f).toInt()
        horseTime = 0f
        horseSpeed = 0f
        horseRun = 0f
    }

    /** Moves the scene on by [dt]; [onSplash] fires as he lands in the mud at x. */
    fun step(dt: Float, onSplash: (Float) -> Unit) {
        if (!isActive || isOver) return
        phaseTime += dt
        if (landedTime >= 0f) landedTime += dt
        stepHorse(dt)
        when (phase) {
            DeathPhase.LIFT -> {
                flail += FLAIL_SPEED * dt
                val progress = smoothstep(progressOf(phaseTime, LIFT_SECONDS))
                middleY = startY + LIFT_HEIGHT * progress
                // Swaying and jerking as they struggle to hold him
                rotation = STRUGGLE_WOBBLE * 6f * sin(phaseTime * 11f)
                x += STRUGGLE_WOBBLE * sin(phaseTime * 7f) * dt * 4f
                if (phaseTime >= LIFT_SECONDS) {
                    enter(DeathPhase.DROP)
                    hoverX = x
                    hoverY = middleY + GRIP_ABOVE
                    vx = DROP_BACK
                    vy = 0f
                }
            }
            DeathPhase.DROP -> {
                flail += FLAIL_SPEED * dt
                vy -= GRAVITY * dt
                x += vx * dt
                middleY += vy * dt
                rotation += TUMBLE_SPEED * dt
                if (middleY <= LYING_MIDDLE) {
                    middleY = LYING_MIDDLE
                    landRotation = rotation
                    landedTime = 0f
                    puddleX = x - PUDDLE_WIDTH / 2f
                    onSplash(x)
                    enter(DeathPhase.DOWN)
                }
            }
            DeathPhase.DOWN -> {
                // Flops onto his back and goes still, until the ravens have him again
                rotation = lerp(landRotation, nearestLying(landRotation), smoothstep(progressOf(phaseTime, SETTLE_SECONDS)))
                if (phaseTime >= DOWN_STILL_SECONDS + REGRAB_SECONDS) {
                    enter(DeathPhase.CARRY)
                    landRotation = rotation
                    vy = 0f
                }
            }
            DeathPhase.CARRY -> {
                flail += DANGLE_FLAIL_SPEED * dt
                wave += WAVE_SPEED * dt
                vy = min(CARRY_MAX_SPEED, vy + CARRY_ACCELERATION * dt)
                middleY += vy * dt
                x += CARRY_DRIFT * dt
                // Turns upright to dangle under them, swinging a little
                rotation = lerp(landRotation, 0f, upright()) + 6f * sin(phaseTime * 3f) * upright()
                if (phaseTime >= IRIS_START + IRIS_SHRINK_SECONDS + IRIS_HOLD_SECONDS + IRIS_CLOSE_SECONDS + OVER_PAUSE) isOver = true
            }
        }
    }

    private fun enter(next: DeathPhase) {
        phase = next
        phaseTime = 0f
    }

    private fun stepHorse(dt: Float) {
        horseTime += dt
        if (horseTime > HORSE_SHY_SECONDS) {
            horseSpeed = min(HORSE_RUN_SPEED, horseSpeed + HORSE_RUN_ACCELERATION * dt)
            horseRun += horseSpeed * dt
        }
    }

    /** 0..1: the ravens coming back down to him in the mud. */
    private fun regrab(): Float = smoothstep(progressOf((phaseTime - DOWN_STILL_SECONDS).coerceAtLeast(0f), REGRAB_SECONDS))

    /** 0..1: turning upright while carried off. */
    private fun upright(): Float = if (phase == DeathPhase.CARRY) smoothstep(progressOf(phaseTime, UPRIGHT_SECONDS)) else 0f

    /** The lying angle closest to [angle], so he doesn't spin round once he is down. */
    private fun nearestLying(angle: Float): Float {
        var target = LYING_ROTATION
        while (target - angle > 180f) target -= 360f
        while (angle - target > 180f) target += 360f
        return target
    }

    fun cowboyUi(): RodeoCowboyUi? = if (!isActive) null else RodeoCowboyUi(
        x = x,
        height = middleY - MIDDLE_Y,
        rotation = rotation,
        facingLeft = false,
        hatLift = 0f,
        limp = true,
        flail = flail,
        waving = if (phase == DeathPhase.CARRY) wave else null,
    )

    /** How far the view pans up to follow him while he is carried off; null otherwise. */
    fun camera(): Float? = if (isActive && phase == DeathPhase.CARRY) max(0f, middleY + FOCUS_ABOVE - SCREEN_MIDDLE) else null

    /** The light round him while the rest of the picture goes dark, at the end. */
    fun spotlightUi(): RodeoSpotlightUi? {
        if (!isActive || phase != DeathPhase.CARRY || phaseTime < IRIS_START) return null
        val time = phaseTime - IRIS_START
        val radius = when {
            time < IRIS_SHRINK_SECONDS -> lerp(IRIS_FULL_RADIUS, IRIS_RADIUS, smoothstep(time / IRIS_SHRINK_SECONDS))
            time < IRIS_SHRINK_SECONDS + IRIS_HOLD_SECONDS -> IRIS_RADIUS
            else -> lerp(IRIS_RADIUS, 0f, smoothstep(progressOf(time - IRIS_SHRINK_SECONDS - IRIS_HOLD_SECONDS, IRIS_CLOSE_SECONDS)))
        }
        return RodeoSpotlightUi(
            x = x,
            height = middleY + FOCUS_ABOVE,
            radius = radius,
            zoom = lerp(1f, IRIS_ZOOM, smoothstep(progressOf(time, IRIS_SHRINK_SECONDS))),
            sayGoodbye = time > IRIS_SHRINK_SECONDS * 0.7f && time < IRIS_SHRINK_SECONDS + IRIS_HOLD_SECONDS,
        )
    }

    fun puddleUi(): RodeoMudUi? = if (landedTime >= 0f) RodeoMudUi(x = puddleX, width = PUDDLE_WIDTH, seed = puddleSeed) else null

    fun splashUi(): RodeoDustUi? =
        if (landedTime in 0f..SPLASH_SECONDS) RodeoDustUi(x = puddleX + PUDDLE_WIDTH / 2f, progress = landedTime / SPLASH_SECONDS) else null
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoLassoUi
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Falling off: rarely, after a big normal jump while the pack is far away, the horse bucks the
// cowboy off. He has to lasso his horse and swing back into the saddle before the pack gets there.
// Meanwhile the world stands still - only the cowboy, his horse and the snails move.
// An exhausted horse (no hearts left) bucks him off too, but then runs away; a new horse comes
// trotting in from behind and is lassoed instead.
private const val FALL_MIN_JUMP_PEAK = 14f      // jumps peaking at 140 cm and more can throw him
private const val FALL_CHANCE = 0.25f
private const val FALL_MIN_CHASE_GAP = 32f      // only while the pack is well off screen
private const val FALL_MIN_ELAPSED = 20f
private const val FALL_COOLDOWN = 45f           // game seconds between two falls
internal const val BUCK_SECONDS = 0.5f
private const val COWBOY_THROW_VX = 40f         // lands ~30 u ahead: the pacing horse drifts in and out of lasso range
private const val COWBOY_THROW_VY = 35f
private const val COWBOY_GRAVITY = 150f         // floaty, so the flip over the neck is readable
private const val COWBOY_DOWN_SECONDS = 0.8f    // lying in the dirt before he gets up
private const val COWBOY_GET_UP_SECONDS = 0.35f // last part of lying down: rotating back upright
private const val COWBOY_HAND_Y = 10f
// The riderless horse paces nervously behind him, in and out of lasso range
private const val HORSE_WANDER_CENTER = -4f
private const val HORSE_WANDER_AMPLITUDE = 8f
private const val HORSE_WANDER_SPEED = 1.1f     // rad/s
private const val HORSE_WANDER_BLEND_SECONDS = 1f
private const val REMOUNT_SECONDS = 0.6f
private const val REMOUNT_HOP = 5f
// The exhausted horse gallops off to the right after bucking, then the new one trots in from the left
private const val RUN_OFF_SECONDS = 1.3f
private const val ARRIVE_SECONDS = 1.6f
private const val ARRIVE_FROM = -(HORSE_X + 35f)

internal enum class FallPhase {
    /** In the saddle - nothing of this class matters. */
    RIDING,
    /** Flying over the horse's neck. */
    THROWN,
    /** Lying in the dirt, then getting up. */
    DOWN,
    /** Standing, lassoing his horse. */
    ON_FOOT,
    /** The lasso caught the saddle: swinging back up. */
    REMOUNT,
}

/** The cowboy off his horse: thrown, lying down, on foot and climbing back into the saddle. */
internal class RodeoFall {
    var phase = FallPhase.RIDING
        private set
    /** Seconds since the fall started. */
    var clock = 0f
        private set
    /** Seconds in the current phase. */
    private var phaseTime = 0f
    private var lastFallAt = -FALL_COOLDOWN
    private var cowboyX = 0f
    private var cowboyY = 0f
    private var cowboyVx = 0f
    private var cowboyVy = 0f
    private var cowboyRotation = 0f
    private var cowboySpin = 0f
    private var horseWanderPhase = 0f
    private var remountStartX = 0f
    private var remountStartOffset = 0f
    /** The horse runs off and another one comes along (see [start]). */
    private var horseRunsOff = false
    private var runOffDistance = 0f
    private var newHorseCame = false

    val isInSaddle: Boolean get() = phase == FallPhase.RIDING
    /** Lies in the dirt or stands next to his horse - the lasso is his way back up. */
    val isOnFoot: Boolean get() = phase == FallPhase.THROWN || phase == FallPhase.DOWN || phase == FallPhase.ON_FOOT
    /** Back on his feet and a horse is around: the lasso may be thrown at it. */
    val canLasso: Boolean get() = phase == FallPhase.ON_FOOT && horseIsBack

    private val runOffStart: Float get() = BUCK_SECONDS
    private val horseIsBack: Boolean get() = !horseRunsOff || clock >= runOffStart + RUN_OFF_SECONDS + ARRIVE_SECONDS

    fun reset() {
        phase = FallPhase.RIDING
        lastFallAt = -FALL_COOLDOWN
        horseRunsOff = false
    }

    /** Decides on landing whether the horse bucks the cowboy off after a jump peaking at [jumpPeak]. */
    fun shouldThrow(jumpPeak: Float, chaseGap: Float, elapsed: Float): Boolean =
        jumpPeak >= FALL_MIN_JUMP_PEAK &&
                chaseGap >= FALL_MIN_CHASE_GAP &&
                elapsed >= FALL_MIN_ELAPSED &&
                elapsed - lastFallAt >= FALL_COOLDOWN &&
                Random.nextFloat() < FALL_CHANCE

    /**
     * Throws the cowboy off. With [horseRunsOff] (the horse is exhausted) it gallops out of the
     * [worldWidth] wide picture and a new horse comes along; see [step]'s onNewHorse.
     */
    fun start(elapsed: Float, horseRunsOff: Boolean = false, worldWidth: Float = 0f) {
        this.horseRunsOff = horseRunsOff
        runOffDistance = worldWidth
        newHorseCame = false
        phase = FallPhase.THROWN
        clock = 0f
        phaseTime = 0f
        lastFallAt = elapsed
        cowboyX = HORSE_X + COWBOY_SEAT_X
        cowboyY = COWBOY_SEAT_Y
        cowboyVx = COWBOY_THROW_VX
        cowboyVy = COWBOY_THROW_VY
        cowboyRotation = 0f
        // One and a quarter forward flips over the flight, so he lands exactly on his back
        val flightSeconds = (COWBOY_THROW_VY + sqrt(COWBOY_THROW_VY * COWBOY_THROW_VY + 2f * COWBOY_GRAVITY * COWBOY_SEAT_Y)) / COWBOY_GRAVITY
        cowboySpin = 450f / flightSeconds
        // Wander starts at the horse's riding spot (sin = 0.5 -> offset 0) and in lasso range
        horseWanderPhase = PI.toFloat() / 6f
    }

    /**
     * Moves the cowboy (and his wandering horse) on by [dt]; [onLanded] fires when he hits the dirt
     * at the given x, [onNewHorse] once the exhausted horse is out of the picture and another one
     * comes along. Returns true in the frame he is back in the saddle.
     */
    fun step(dt: Float, lasso: RodeoLasso, onLanded: (x: Float) -> Unit, onNewHorse: () -> Unit): Boolean {
        clock += dt
        phaseTime += dt
        horseWanderPhase += dt * HORSE_WANDER_SPEED
        if (horseRunsOff && !newHorseCame && clock >= runOffStart + RUN_OFF_SECONDS) {
            newHorseCame = true
            onNewHorse()
        }
        when (phase) {
            FallPhase.THROWN -> {
                cowboyVy -= COWBOY_GRAVITY * dt
                cowboyX += cowboyVx * dt
                cowboyY += cowboyVy * dt
                cowboyRotation += cowboySpin * dt
                if (cowboyY <= 0f) {
                    cowboyY = 0f
                    cowboyRotation = 90f // flat on his back
                    enter(FallPhase.DOWN)
                    onLanded(cowboyX)
                }
            }
            FallPhase.DOWN -> {
                val getUpStart = COWBOY_DOWN_SECONDS - COWBOY_GET_UP_SECONDS
                val getUp = ((phaseTime - getUpStart) / COWBOY_GET_UP_SECONDS).coerceIn(0f, 1f)
                cowboyRotation = 90f * (1f - smoothstep(getUp))
                if (phaseTime >= COWBOY_DOWN_SECONDS) {
                    cowboyRotation = 0f
                    enter(FallPhase.ON_FOOT)
                }
            }
            FallPhase.ON_FOOT -> {
                val (handX, handY) = cowboyHand()
                val (seatX, seatY) = saddle()
                if (lasso.stepToSaddle(dt, handX, handY, seatX, seatY)) {
                    remountStartX = cowboyX
                    remountStartOffset = horseOffset()
                    enter(FallPhase.REMOUNT)
                }
            }
            FallPhase.REMOUNT -> {
                val progress = progressOf(phaseTime, REMOUNT_SECONDS)
                val (seatX, seatY) = saddle()
                cowboyX = remountStartX + (seatX - remountStartX) * progress
                // Ends with his torso on the saddle, where the seated rider takes over
                cowboyY = (seatY - COWBOY_LEG_LENGTH) * progress + REMOUNT_HOP * sin(PI.toFloat() * progress)
                if (progress >= 1f) {
                    phase = FallPhase.RIDING
                    cowboyRotation = 0f
                    return true
                }
            }
            FallPhase.RIDING -> Unit
        }
        return false
    }

    private fun enter(newPhase: FallPhase) {
        phase = newPhase
        phaseTime = 0f
    }

    /** Offset of the riderless horse from its riding spot. */
    fun horseOffset(): Float = when (phase) {
        FallPhase.RIDING -> 0f
        FallPhase.REMOUNT -> remountStartOffset * (1f - progressOf(phaseTime, REMOUNT_SECONDS))
        else -> if (horseRunsOff) runOffOffset() else {
            val blend = min(1f, clock / HORSE_WANDER_BLEND_SECONDS)
            blend * wander()
        }
    }

    private fun wander(): Float = HORSE_WANDER_CENTER + HORSE_WANDER_AMPLITUDE * sin(horseWanderPhase)

    /** The exhausted horse speeds off to the right, then the new one trots in from the left. */
    private fun runOffOffset(): Float {
        val time = clock - runOffStart
        if (time < 0f) return 0f
        if (time < RUN_OFF_SECONDS) {
            val progress = time / RUN_OFF_SECONDS
            return runOffDistance * progress * progress
        }
        val arrival = progressOf(time - RUN_OFF_SECONDS, ARRIVE_SECONDS)
        return lerp(ARRIVE_FROM, wander(), smoothstep(arrival))
    }

    private fun cowboyHand(): Pair<Float, Float> = (cowboyX - 2f) to (cowboyY + COWBOY_HAND_Y)

    private fun saddle(): Pair<Float, Float> = (HORSE_X + horseOffset() + COWBOY_SEAT_X) to COWBOY_SEAT_Y

    /** The cowboy on his own; null while he is in the saddle. */
    fun cowboyUi(): RodeoCowboyUi? = when (phase) {
        FallPhase.RIDING -> null
        FallPhase.THROWN -> RodeoCowboyUi(cowboyX, cowboyY, cowboyRotation, facingLeft = false, hatLift = 2f)
        FallPhase.DOWN -> RodeoCowboyUi(cowboyX, cowboyY, cowboyRotation, facingLeft = false, hatLift = 0f)
        // Facing his horse while he throws, turning to face forward half way up into the saddle
        FallPhase.ON_FOOT -> RodeoCowboyUi(cowboyX, cowboyY, 0f, facingLeft = true, hatLift = 0f)
        FallPhase.REMOUNT -> RodeoCowboyUi(
            x = cowboyX,
            height = cowboyY,
            rotation = 0f,
            facingLeft = phaseTime < REMOUNT_SECONDS / 2f,
            hatLift = 0f
        )
    }

    /** The rope while he is off the horse: thrown, or taut to the saddle while he swings up. */
    fun lassoUi(lasso: RodeoLasso): RodeoLassoUi? {
        val (handX, handY) = cowboyHand()
        return if (phase == FallPhase.REMOUNT) {
            val (seatX, seatY) = saddle()
            RodeoLassoUi(handX = handX, handY = handY, tipX = seatX, tipY = seatY, caught = null)
        } else {
            lasso.uiOnFoot(handX, handY)
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoLassoUi
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Falling off: rarely, after a big normal jump while the pack is far away, the horse bucks the
// cowboy off and bolts ahead. He gets up and runs after it on his own - the world scrolls at his
// pace - and lassoes the horse once it is in reach. Nothing on the track can stop him: he runs
// around fences and snails, and the pack waits meanwhile.
// An exhausted horse (no hearts left) bucks him off too, but then runs away; a new horse comes
// trotting in from the right, ahead of him, and is lassoed instead.
//
// Just for fun (and speed) he jumps like a plumber on foot: a tap jumps, and tapping again right as he lands chains a higher
// jump with a salto. The third jump of a chain is a big triple jump with a double salto that also
// carries him forward faster.
private const val FALL_MIN_JUMP_PEAK = 14f      // jumps peaking at 140 cm and more can throw him
private const val FALL_CHANCE = 0.05f
private const val FALL_MIN_CHASE_GAP = 32f      // only while the pack is well off screen
private const val FALL_MIN_ELAPSED = 20f
private const val FALL_COOLDOWN = 90f           // game seconds between two falls
internal const val BUCK_SECONDS = 0.5f
private const val COWBOY_THROW_VX = 40f         // lands ~30 u ahead of the pacing horse
private const val COWBOY_THROW_VY = 35f
private const val COWBOY_GRAVITY = 150f         // floaty, so the flip over the neck is readable
private const val COWBOY_DOWN_SECONDS = 0.8f    // lying in the dirt before he gets up
private const val COWBOY_GET_UP_SECONDS = 0.35f // last part of lying down: rotating back upright
private const val COWBOY_HAND_Y = 10f
private const val REMOUNT_SECONDS = 0.6f
private const val REMOUNT_HOP = 5f

// Running after the horse
/** u/s the cowboy runs (16 km/h): the world scrolls by at this pace while he is on his feet. */
internal const val COWBOY_RUN_SPEED = 45f
private const val RUN_SPEED_RESPONSE = 4f       // 1/s, how fast he gets up to speed
/** He drifts back to where the rider sits, so there is room ahead for the horse. */
private const val RUN_X = HORSE_X + COWBOY_SEAT_X
private const val RUN_X_RESPONSE = 1.5f
private const val RUN_STRIDE = 0.35f            // leg swing radians per unit run

// Jumping on foot: single, double (one salto), triple (two saltos, big and fast)
private const val JUMP_GRAVITY = 320f
private val JUMP_VELOCITIES = floatArrayOf(100f, 120f, 160f) // peaks at ~16, ~22 and 40 u
private val JUMP_SALTOS = intArrayOf(0, 1, 2)
/** The world scrolls this much faster under him during the triple jump. */
private const val TRIPLE_JUMP_SPEED_FACTOR = 1.5f
/** Seconds after landing in which a tap still chains the next jump. */
private const val CHAIN_WINDOW = 0.15f
/** Seconds before landing a tap is remembered and fires the chained jump on touchdown. */
private const val PRESS_BUFFER = 0.12f

// The riderless horse (positions are offsets from its riding spot HORSE_X)
/** u/s the horse trots, a bit slower than the running cowboy, so he closes in on it. */
private const val HORSE_TROT = COWBOY_RUN_SPEED - 15f
/** Farthest the horse gets ahead of the cowboy (left edge of the horse), also while he lies in the dirt. */
private const val HORSE_LEAD = 45f
/** Closest the horse lets him come; it keeps that distance until he lassoes it. */
private const val HORSE_MIN_LEAD = 4f
/** Room the horse keeps from the right edge of the picture. */
private const val HORSE_RIGHT_MARGIN = 32f
private const val BOLT_SECONDS = 1.2f
/** The lasso on foot reaches the saddle this far ahead of his hand. */
private const val LASSO_REACH_ON_FOOT = 36f
// The exhausted horse gallops off to the right after bucking, then the new one trots in from the right
private const val RUN_OFF_SECONDS = 1.3f
private const val ARRIVE_SECONDS = 1.6f

internal enum class FallPhase {
    /** In the saddle - nothing of this class matters. */
    RIDING,
    /** Flying over the horse's neck. */
    THROWN,
    /** Lying in the dirt after being thrown, then getting up. */
    DOWN,
    /** Running after his horse, jumping the fences, lassoing it once in reach. */
    ON_FOOT,
    /** The lasso caught the saddle: swinging back up. */
    REMOUNT,
}

/** What the riderless horse is up to. */
private enum class HorseMove {
    /** Kicking the cowboy off. */
    BUCK,
    /** Bolting ahead of the cowboy. */
    BOLT,
    /** The exhausted horse galloping out of the picture. */
    RUN_OFF,
    /** The new horse trotting in from the right. */
    ARRIVE,
    /** Trotting ahead, a little slower than the cowboy runs. */
    TROT,
}

/** The cowboy off his horse: thrown, lying down, running after it and climbing back into the saddle. */
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
    private var remountStartX = 0f
    private var remountStartY = 0f
    private var remountStartOffset = 0f
    /** The horse runs off and another one comes along (see [start]). */
    private var horseRunsOff = false
    private var runOffDistance = 0f
    private var horseMove = HorseMove.BUCK
    private var horseMoveTime = 0f
    private var horseMoveFrom = 0f
    private var horseX = 0f
    private var worldWidth = 0f

    // Running and jumping
    /** u/s the ground scrolls by under the running cowboy; 0 while he lies in the dirt. */
    var groundSpeed = 0f
        private set
    /** How far the riderless horse moved over the ground this frame - its gallop. */
    var horseStride = 0f
        private set
    private var runPhase = 0f
    private var airborne = false
    private var airTime = 0f
    private var airSeconds = 0f
    /** 1, 2 or 3: which jump of a chain he is in (or landed from); 0 before the first. */
    private var jumpInChain = 0
    private var sinceLanding = 0f
    private var bufferedPress = 0f

    val isInSaddle: Boolean get() = phase == FallPhase.RIDING
    /** Lies in the dirt or runs after his horse - the lasso is his way back up. */
    val isOnFoot: Boolean get() = phase == FallPhase.THROWN || phase == FallPhase.DOWN || phase == FallPhase.ON_FOOT
    /** Running: the lasso may be thrown any time, it only catches the horse once it is in reach. */
    val canLasso: Boolean get() = phase == FallPhase.ON_FOOT

    /** The horse is close enough ahead for the lasso to catch its saddle. */
    val horseInReach: Boolean
        get() = phase == FallPhase.ON_FOOT && horseIsBack && saddle().first - cowboyHand().first in 0f..LASSO_REACH_ON_FOOT

    /** The fall came from an exhausted horse, so the cowboy gets (or got) back up on a new one. */
    val hasNewHorse: Boolean get() = horseRunsOff

    /** Where the running cowboy is (his center), null unless he runs; see [runnerHeight]. */
    val runnerX: Float? get() = if (phase == FallPhase.ON_FOOT) cowboyX else null

    /** His feet above the ground while he runs (and jumps). */
    val runnerHeight: Float get() = cowboyY

    private val horseIsBack: Boolean get() = !horseRunsOff || horseMove == HorseMove.TROT

    fun reset() {
        phase = FallPhase.RIDING
        lastFallAt = -FALL_COOLDOWN
        horseRunsOff = false
        groundSpeed = 0f
        horseStride = 0f
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
        horseX = 0f
        moveHorse(HorseMove.BUCK)
        groundSpeed = 0f
        airborne = false
        jumpInChain = 0
        bufferedPress = 0f
    }

    /** Jump tapped on foot: jumps, chains the next jump if he just landed, or remembers it in the air. */
    fun jumpPressed() {
        if (phase != FallPhase.ON_FOOT) return
        if (airborne) {
            bufferedPress = PRESS_BUFFER
            return
        }
        val chained = sinceLanding <= CHAIN_WINDOW && jumpInChain in 1..2
        jump(if (chained) jumpInChain + 1 else 1)
    }

    private fun jump(number: Int) {
        jumpInChain = number
        cowboyVy = JUMP_VELOCITIES[number - 1]
        airSeconds = 2f * cowboyVy / JUMP_GRAVITY
        airTime = 0f
        airborne = true
        bufferedPress = 0f
    }

    /**
     * Moves the cowboy and his horse on by [dt] in a picture [worldWidth] wide; [onLanded] fires
     * when he hits the dirt at the given x, [onNewHorse] once the exhausted horse is out of the
     * picture and another one comes along. Returns true in the frame he is back in the saddle.
     */
    fun step(dt: Float, lasso: RodeoLasso, worldWidth: Float, onLanded: (x: Float) -> Unit, onNewHorse: () -> Unit): Boolean {
        this.worldWidth = worldWidth
        clock += dt
        phaseTime += dt
        val targetSpeed = when (phase) {
            FallPhase.ON_FOOT, FallPhase.REMOUNT ->
                COWBOY_RUN_SPEED * (if (airborne && jumpInChain == 3) TRIPLE_JUMP_SPEED_FACTOR else 1f)
            else -> 0f
        }
        groundSpeed = if (targetSpeed == 0f) 0f else groundSpeed + (targetSpeed - groundSpeed) * min(1f, dt * RUN_SPEED_RESPONSE)
        stepHorse(dt, onNewHorse)
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
                    sinceLanding = CHAIN_WINDOW + 1f
                    enter(FallPhase.ON_FOOT)
                }
            }
            FallPhase.ON_FOOT -> {
                run(dt, onLanded)
                if (lasso.isOut) {
                    val (handX, handY) = cowboyHand()
                    val (seatX, seatY) = saddle()
                    if (lasso.stepToSaddle(dt, handX, handY, seatX, seatY, LASSO_REACH_ON_FOOT, horseInReach)) {
                        remountStartX = cowboyX
                        remountStartY = cowboyY
                        remountStartOffset = horseX
                        cowboyRotation = 0f
                        airborne = false
                        enter(FallPhase.REMOUNT)
                    }
                }
            }
            FallPhase.REMOUNT -> {
                val progress = progressOf(phaseTime, REMOUNT_SECONDS)
                val (seatX, seatY) = saddle()
                cowboyX = remountStartX + (seatX - remountStartX) * progress
                // Ends with his torso on the saddle, where the seated rider takes over
                cowboyY = lerp(remountStartY, seatY - COWBOY_LEG_LENGTH, progress) + REMOUNT_HOP * sin(PI.toFloat() * progress)
                if (progress >= 1f) {
                    phase = FallPhase.RIDING
                    cowboyRotation = 0f
                    groundSpeed = 0f
                    return true
                }
            }
            FallPhase.RIDING -> Unit
        }
        return false
    }

    /** Runs on (back to [RUN_X]) and flies through a jump, salto included. */
    private fun run(dt: Float, onLanded: (x: Float) -> Unit) {
        cowboyX += (RUN_X - cowboyX) * min(1f, dt * RUN_X_RESPONSE)
        bufferedPress = countDown(bufferedPress, dt)
        if (!airborne) {
            sinceLanding += dt
            runPhase += groundSpeed * dt * RUN_STRIDE
            return
        }
        airTime += dt
        cowboyVy -= JUMP_GRAVITY * dt
        cowboyY += cowboyVy * dt
        // The saltos turn forward exactly once (or twice) over the flight, so he lands on his feet
        cowboyRotation = 360f * JUMP_SALTOS[jumpInChain - 1] * progressOf(airTime, airSeconds)
        if (cowboyY <= 0f && cowboyVy < 0f) {
            cowboyY = 0f
            cowboyRotation = 0f
            airborne = false
            sinceLanding = 0f
            onLanded(cowboyX)
            // Tapped just before touchdown: the next jump of the chain goes off right away
            if (bufferedPress > 0f) jump(if (jumpInChain in 1..2) jumpInChain + 1 else 1)
        }
    }

    /** Moves the riderless horse: bucking, bolting (or running off and the new one coming), trotting. */
    private fun stepHorse(dt: Float, onNewHorse: () -> Unit) {
        val before = horseX
        horseMoveTime += dt
        when (horseMove) {
            HorseMove.BUCK -> if (horseMoveTime >= BUCK_SECONDS) {
                moveHorse(if (horseRunsOff) HorseMove.RUN_OFF else HorseMove.BOLT)
            }
            HorseMove.BOLT -> {
                val progress = progressOf(horseMoveTime, BOLT_SECONDS)
                horseX = lerp(horseMoveFrom, leadOffset(), smoothstep(progress))
                if (progress >= 1f) moveHorse(HorseMove.TROT)
            }
            HorseMove.RUN_OFF -> {
                val progress = progressOf(horseMoveTime, RUN_OFF_SECONDS)
                horseX = horseMoveFrom + runOffDistance * progress * progress
                if (progress >= 1f) {
                    onNewHorse()
                    // The new horse comes in from just beyond the right edge, ahead of him
                    horseX = worldWidth - HORSE_X + 2f
                    moveHorse(HorseMove.ARRIVE)
                }
            }
            HorseMove.ARRIVE -> {
                val progress = progressOf(horseMoveTime, ARRIVE_SECONDS)
                horseX = lerp(horseMoveFrom, leadOffset(), smoothstep(progress))
                if (progress >= 1f) moveHorse(HorseMove.TROT)
            }
            HorseMove.TROT -> if (phase != FallPhase.REMOUNT) {
                // Slower than the running cowboy, so he closes in; it never lets him pass, and never
                // gets farther ahead than HORSE_LEAD
                val closest = cowboyX + HORSE_MIN_LEAD - HORSE_X
                horseX = (horseX + (HORSE_TROT - groundSpeed) * dt).coerceIn(closest, max(closest, leadOffset()))
            }
        }
        if (phase == FallPhase.REMOUNT) horseX = remountStartOffset * (1f - progressOf(phaseTime, REMOUNT_SECONDS))
        // Its own gallop over the ground: what it moved on screen plus the ground passing under it
        horseStride = if (horseMove == HorseMove.BUCK) 0f else max(0f, horseX - before + groundSpeed * dt)
    }

    private fun moveHorse(move: HorseMove) {
        horseMove = move
        horseMoveTime = 0f
        horseMoveFrom = horseX
    }

    /** Where the horse waits for him at first: well ahead, but inside the picture. */
    private fun leadOffset(): Float = min(cowboyX + HORSE_LEAD - HORSE_X, maxOffset())

    private fun maxOffset(): Float = worldWidth - HORSE_RIGHT_MARGIN - HORSE_X

    private fun enter(newPhase: FallPhase) {
        phase = newPhase
        phaseTime = 0f
    }

    /** Offset of the riderless horse from its riding spot. */
    fun horseOffset(): Float = if (phase == FallPhase.RIDING) 0f else horseX

    /** He faces his horse ahead: the rope hand is in front of him. */
    private fun cowboyHand(): Pair<Float, Float> = (cowboyX + 2f) to (cowboyY + COWBOY_HAND_Y)

    private fun saddle(): Pair<Float, Float> = (HORSE_X + horseOffset() + COWBOY_SEAT_X) to COWBOY_SEAT_Y

    /** The cowboy on his own; null while he is in the saddle. */
    fun cowboyUi(): RodeoCowboyUi? = when (phase) {
        FallPhase.RIDING -> null
        FallPhase.THROWN -> RodeoCowboyUi(cowboyX, cowboyY, cowboyRotation, facingLeft = false, hatLift = 2f)
        FallPhase.DOWN -> RodeoCowboyUi(cowboyX, cowboyY, cowboyRotation, facingLeft = false, hatLift = 0f)
        FallPhase.ON_FOOT -> {
            val saltos = if (airborne) JUMP_SALTOS[jumpInChain - 1] else 0
            RodeoCowboyUi(
                x = cowboyX,
                height = cowboyY,
                rotation = cowboyRotation,
                facingLeft = false,
                hatLift = if (airborne) 1f else 0f,
                runPhase = if (airborne) null else runPhase,
                // Knees pulled in for the salto, stretched out again for the landing
                tuck = if (saltos > 0) sin(PI.toFloat() * progressOf(airTime, airSeconds)) else 0f,
            )
        }
        FallPhase.REMOUNT -> RodeoCowboyUi(x = cowboyX, height = cowboyY, rotation = 0f, facingLeft = false, hatLift = 0f)
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

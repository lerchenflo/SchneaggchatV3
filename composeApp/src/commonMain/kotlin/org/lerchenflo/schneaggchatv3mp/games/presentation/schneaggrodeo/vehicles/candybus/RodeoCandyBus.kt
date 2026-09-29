package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.candybus

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_LEG_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HAND_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.KMH_PER_UNIT_PER_SECOND
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Free candy bus: a gray, windowless van with "FREE CANDY" sprayed on its side rolls by. Lasso it
// and it backs up over the horse with its rear doors open - the horse ends up in the back - while
// the cowboy jumps over the roof into the driver's seat. Stanislaus keeps showing up by the
// roadside; the lasso button turns into "Capture": the bus drives up next to the closest one ahead,
// stops, slides its side door open and pulls him in. At the end the bus pulls forward, the horse
// is left standing behind it and the cowboy backflips into the saddle.

// Shape, shared with the drawing (grid: x from the rear bumper, y up from the ground)
internal const val BUS_LENGTH = 60f
/** Tall enough to take the whole horse. */
internal const val BUS_ROOF = 27f
internal const val BUS_BOTTOM = 3.5f
/** Sliding side door: left edge, width, floor and top. */
internal const val DOOR_LEFT = 35f
internal const val DOOR_WIDTH = 8f
internal const val DOOR_FLOOR = 4.5f
internal const val DOOR_TOP = 23f
/** The cowboy behind the wheel: where he sits in the driver's window. */
internal const val DRIVER_X = 48f
private const val DRIVER_FEET_Y = 12f
/** Stanislaus is half as tall as the rider. */
internal const val STANISLAUS_HEIGHT = 9f

private const val BUS_PASS_SPEED = 28f           // u/s across the screen while passing by, slow enough to lasso
private const val BUS_HITCH_Y = 10f
/** Where the loop lands on the bus: the middle of the lasso's reach, ahead of the hand. */
private const val BUS_HITCH_AIM = HORSE_X + HAND_X + 16f
/** Seconds the loop takes to reach the bus (half the lasso's throw), aimed at where it will be. */
private const val LASSO_LEAD = 0.225f
/** Rear bumper while riding: the cargo room covers the horse. */
private const val BUS_RIDE_X = HORSE_X - 8f
/** Rear bumper once it pulled forward off the horse at the end. */
private const val BUS_UNLOAD_X = HORSE_X + 34f
private const val BUS_BOARD_SECONDS = 0.9f
private const val BUS_UNLOAD_SECONDS = 0.9f
private const val DRIVER_JUMP_HOP = 22f          // over the roof into the driver's seat
private const val BUS_SPEED_FACTOR = 1.2f        // world scroll while riding, relative to the horse's pace
private const val BUS_LEAVE_SPEED = 60f
private const val BUS_RIDE_SECONDS = 12f
private const val WHEEL_TURN = 0.3f

// Stanislaus by the roadside and capturing him
private const val STANISLAUS_FIRST_SECONDS = 0.5f
private const val STANISLAUS_INTERVAL_MIN = 1.2f
private const val STANISLAUS_INTERVAL_RANDOM = 1f
/** How far ahead of the door (u) the bus drives up to someone. */
private const val CAPTURE_RANGE = 75f
/** Driving up: share of the remaining way per second, and the speed limits (u/s). */
private const val DRIVE_UP_RATE = 5f
private const val DRIVE_UP_MIN_SPEED = 15f
private const val DRIVE_UP_MAX_SPEED = 220f
private const val DOOR_SECONDS = 0.3f
private const val PULL_SECONDS = 0.45f
private const val STANISLAUS_POINTS = 30

private enum class CaptureStep { DRIVE_UP, OPEN, PULL, CLOSE }

/** [x] is his center on the ground; [pull] 0..1 on his way through the door once captured. */
private class Stanislaus(var x: Float) {
    var pulled = false
    var pull = 0f
}

internal class RodeoCandyBus : RodeoVehicle(RodeoVehicleKind.CANDY_BUS) {

    private val waiting = mutableListOf<Stanislaus>()
    private var spawnIn = 0f
    private var target: Stanislaus? = null
    private var captureStep = CaptureStep.DRIVE_UP
    private var stepTime = 0f
    /** u/s the world scrolled by last frame, for the speedometer. */
    private var currentSpeed = 0f
    private var boardStartX = 0f
    private var wheelPhase = 0f

    /** Premium: only joins once every regular vehicle came by. */
    override val special = true

    override val length = BUS_LENGTH
    override val passSpeed = BUS_PASS_SPEED

    /** The horse stands in the back, the cowboy drives. */
    override val riderOnHorse: Boolean get() = !carriesRider
    override val horseHops = false
    override val hidesHorse: Boolean get() = phase == VehiclePhase.RIDING

    /** The whole bus is a target, like the Ford Escort. */
    override fun hitch() = (BUS_HITCH_AIM + BUS_PASS_SPEED * LASSO_LEAD).coerceIn(x + 2f, x + BUS_LENGTH - 2f) to BUS_HITCH_Y

    private val doorCenter: Float get() = x + DOOR_LEFT + DOOR_WIDTH / 2f

    override fun reset() {
        super.reset()
        waiting.clear()
        target = null
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        // A jump in progress ends right here, the bus backs up over the horse
        world.takeHorseOffTheGround()
    }

    override fun worldScroll(world: RodeoWorld, step: Float, dt: Float): Float {
        if (!isRiding) return step
        val target = target
        val scroll = when {
            target == null -> step * BUS_SPEED_FACTOR
            captureStep == CaptureStep.DRIVE_UP -> {
                // Slows down as it gets close
                val remaining = max(0f, target.x - doorCenter)
                val speed = (remaining * DRIVE_UP_RATE).coerceIn(DRIVE_UP_MIN_SPEED, DRIVE_UP_MAX_SPEED)
                min(remaining, speed * dt)
            }
            else -> 0f // standing next to him with the door open
        }
        if (dt > 0f) currentSpeed = scroll / dt
        return scroll
    }

    override fun speedKmh(world: RodeoWorld): Int? =
        if (isRiding) (currentSpeed * KMH_PER_UNIT_PER_SECOND).roundToInt() else null

    /** "Capture": drives up to the closest Stanislaus ahead of the door. */
    override fun onLassoButton(world: RodeoWorld): Boolean {
        if (!isRiding) return false
        if (target != null) return true
        target = waiting
            .filter { !it.pulled && it.x - doorCenter in 0f..CAPTURE_RANGE }
            .minByOrNull { it.x }
            ?.also {
                captureStep = CaptureStep.DRIVE_UP
                stepTime = 0f
            }
        return true
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        wheelPhase += scroll * WHEEL_TURN
        waiting.forEach { if (!it.pulled) it.x -= scroll }
        waiting.removeAll { !it.pulled && it.x < -STANISLAUS_HEIGHT }
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> passBy(dt)
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, BUS_BOARD_SECONDS)
                x = lerp(boardStartX, BUS_RIDE_X, smoothstep(progress))
                ram(world)
                if (progress >= 1f) {
                    enter(VehiclePhase.RIDING)
                    spawnIn = STANISLAUS_FIRST_SECONDS
                    currentSpeed = 0f
                }
            }
            VehiclePhase.RIDING -> ride(world, dt)
            VehiclePhase.UNLOADING -> {
                val progress = progressOf(phaseTime, BUS_UNLOAD_SECONDS)
                x = lerp(BUS_RIDE_X, BUS_UNLOAD_X, smoothstep(progress))
                ram(world)
                if (progress >= 1f) enter(VehiclePhase.LEAVING)
            }
            VehiclePhase.LEAVING -> {
                // Drives off to the right; done once nobody is left standing by the road
                x += BUS_LEAVE_SPEED * dt
                ram(world)
                if (x > world.worldWidth && waiting.isEmpty()) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun ride(world: RodeoWorld, dt: Float) {
        ram(world)
        spawnIn -= dt
        val rideIsOver = phaseTime >= BUS_RIDE_SECONDS
        if (spawnIn <= 0f && !rideIsOver) {
            waiting.add(Stanislaus(x = world.worldWidth + STANISLAUS_HEIGHT))
            spawnIn = STANISLAUS_INTERVAL_MIN + Random.nextFloat() * STANISLAUS_INTERVAL_RANDOM
        }
        capture(world, dt)
        if (rideIsOver && target == null) {
            enter(VehiclePhase.UNLOADING)
            world.resumeFences()
        }
    }

    private fun capture(world: RodeoWorld, dt: Float) {
        val target = target ?: return
        stepTime += dt
        when (captureStep) {
            CaptureStep.DRIVE_UP -> if (target.x - doorCenter < 0.3f) next(CaptureStep.OPEN)
            CaptureStep.OPEN -> if (stepTime >= DOOR_SECONDS) {
                target.pulled = true
                next(CaptureStep.PULL)
            }
            CaptureStep.PULL -> {
                target.pull = progressOf(stepTime, PULL_SECONDS)
                if (target.pull >= 1f) {
                    waiting.remove(target)
                    world.addBonusPoints(STANISLAUS_POINTS)
                    world.sparkle(doorCenter, (DOOR_FLOOR + DOOR_TOP) / 2f)
                    next(CaptureStep.CLOSE)
                }
            }
            CaptureStep.CLOSE -> if (stepTime >= DOOR_SECONDS) this.target = null
        }
    }

    private fun next(newStep: CaptureStep) {
        captureStep = newStep
        stepTime = 0f
    }

    /** Fences and snails in front of the bus are pushed aside. */
    private fun ram(world: RodeoWorld) {
        world.clearTrack(x, x + BUS_LENGTH) { fence -> world.dust(fence.x) }
    }

    override fun cowboy(world: RodeoWorld): RodeoCowboyUi? {
        val saddleX = HORSE_X + COWBOY_SEAT_X
        val saddleY = world.horseHeight + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH
        return when (phase) {
            // Jumps over the roof into the driver's seat...
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, BUS_BOARD_SECONDS)
                RodeoCowboyUi(
                    x = lerp(saddleX, x + DRIVER_X, progress),
                    height = lerp(saddleY, DRIVER_FEET_Y, progress) + DRIVER_JUMP_HOP * sin(PI.toFloat() * progress),
                    rotation = 0f,
                    facingLeft = false,
                    hatLift = 1f,
                )
            }
            // ...and back into the saddle at the end, with a backflip
            VehiclePhase.UNLOADING -> {
                val progress = progressOf(phaseTime, BUS_UNLOAD_SECONDS)
                RodeoCowboyUi(
                    x = lerp(x + DRIVER_X, saddleX, progress),
                    height = lerp(DRIVER_FEET_Y, saddleY, progress) + DRIVER_JUMP_HOP * sin(PI.toFloat() * progress),
                    rotation = -360f * progress,
                    facingLeft = false,
                    hatLift = 1f,
                )
            }
            else -> null
        }
    }

    /** 0..1 how far the side door is slid open. */
    private fun sideDoorOpen(): Float {
        if (target == null) return 0f
        return when (captureStep) {
            CaptureStep.DRIVE_UP -> 0f
            CaptureStep.OPEN -> progressOf(stepTime, DOOR_SECONDS)
            CaptureStep.PULL -> 1f
            CaptureStep.CLOSE -> 1f - progressOf(stepTime, DOOR_SECONDS)
        }
    }

    /** 0..1 how far the rear doors swing open: while the horse gets in and out. */
    private fun rearDoorsOpen(): Float = when (phase) {
        // Open from the start, shut once the bus is over the horse
        VehiclePhase.BOARDING -> min(1f, 4f * (1f - progressOf(phaseTime, BUS_BOARD_SECONDS)))
        VehiclePhase.UNLOADING -> min(1f, 4f * progressOf(phaseTime, BUS_UNLOAD_SECONDS))
        VehiclePhase.LEAVING -> max(0f, 1f - 4f * phaseTime)
        else -> 0f
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoCandyBusUi(
            x = x,
            wheelPhase = wheelPhase,
            doorOpen = sideDoorOpen(),
            rearDoorsOpen = rearDoorsOpen(),
            hasDriver = phase == VehiclePhase.RIDING,
            stanislaus = waiting.map { RodeoStanislausUi(x = it.x, pull = it.pull) },
            lassoHint = lassoHint(x + BUS_LENGTH / 2f, BUS_ROOF + 6f),
        )
    }
}

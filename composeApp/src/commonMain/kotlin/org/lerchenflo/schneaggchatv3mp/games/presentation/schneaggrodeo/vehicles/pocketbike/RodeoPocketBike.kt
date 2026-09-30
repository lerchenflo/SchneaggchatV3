package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pocketbike

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_LEG_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
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
import kotlin.math.sin
import kotlin.random.Random

// Pocket bike: a tiny red pit bike (flame decals, knobby tyres, high bars) putters by. Lasso it and the cowboy hops on - knees up at his
// chin - while his horse gallops on riderless behind him. Full throttle for two seconds in a huge
// cloud of two-stroke smoke, then the engine dies with a bang, the cowboy jumps back into the saddle
// and the bike tips over and stays behind.

// Shape, shared with the drawing (grid: x from the rear of the bike, y up from the ground)
internal const val BIKE_LENGTH = 10f
internal const val BIKE_WHEEL_RADIUS = 1.5f
internal const val BIKE_REAR_WHEEL_X = 2f
internal const val BIKE_FRONT_WHEEL_X = 8.4f
/** The rider's hips on the seat. */
internal const val BIKE_SEAT_X = 3.6f
internal const val BIKE_SEAT_Y = 5.2f
/** Silencer end at the back, where the smoke comes out. */
internal const val BIKE_EXHAUST_X = 0.3f
internal const val BIKE_EXHAUST_Y = 2.8f

private const val BIKE_PASS_SPEED = 22f          // u/s across the screen while puttering by, slow enough to lasso
private const val BIKE_HITCH_Y = 4f
/** Where the loop lands on the bike: the middle of the lasso's reach, ahead of the hand. */
/** Seconds the loop takes to reach the bike (half the lasso's throw), aimed at where it will be. */
/** Ahead of the horse, which gallops on riderless behind it. */
private const val BIKE_RIDE_X = HORSE_X + 34f
private const val BIKE_BOARD_SECONDS = 0.6f
private const val BIKE_RIDE_SECONDS = 2f
private const val BIKE_UNLOAD_SECONDS = 0.7f
private const val BIKE_JUMP_HOP = 7f
private const val BIKE_RATTLE_DEGREES = 2f
private const val WHEEL_TURN = 0.9f              // radians per unit of ground: tiny wheels spin fast
/** Lying on its side once the engine died. */
private const val FALLEN_DEGREES = 80f
private const val FALL_SECONDS = 0.4f
private const val BIKE_POINTS = 20

// Smoke: lots of it
private const val SMOKE_INTERVAL_RIDING = 0.02f
private const val SMOKE_INTERVAL_IDLE = 0.15f
private const val SMOKE_INTERVAL_DYING = 0.05f
private const val SMOKE_SECONDS = 1.8f
private const val SMOKE_DRIFT_BACK = 14f         // u/s the puffs are blown back, on top of the ground scrolling by
private const val SMOKE_RISE = 5f
private const val BANG_PUFFS = 14

/** A puff of smoke; [x] / [y] its center, grows and fades over its [age]. */
private class Puff(var x: Float, var y: Float, val vx: Float, val vy: Float, val size: Float) {
    var age = 0f
}

internal class RodeoPocketBike : RodeoVehicle(RodeoVehicleKind.POCKET_BIKE) {

    private var boardStartX = 0f
    private var wheelPhase = 0f
    private var rotation = 0f
    private var shake = 0f
    private val smoke = mutableListOf<Puff>()
    private var smokeIn = 0f

    override val length = BIKE_LENGTH
    override val passSpeed = BIKE_PASS_SPEED
    override val carriesHorse = false
    override val horseHops = false

    /** The horse gallops on without its rider while he is on the bike. */
    override val horseRunsRiderless: Boolean
        get() = phase == VehiclePhase.RIDING || phase == VehiclePhase.UNLOADING

    override val riderOnHorse: Boolean get() = !carriesRider

    /** The whole bike is a target, like the Ford Escort. */
    override fun hitch() = hitchX(inset = 1f) to BIKE_HITCH_Y

    override fun reset() {
        super.reset()
        smoke.clear()
    }

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        rotation = 0f
        smoke.clear()
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        world.takeHorseOffTheGround()
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        shake += dt
        stepSmoke(dt, scroll)
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> {
                wheelPhase += scroll * WHEEL_TURN
                puff(dt, SMOKE_INTERVAL_IDLE)
                passBy(dt)
            }
            VehiclePhase.BOARDING -> {
                wheelPhase += scroll * WHEEL_TURN
                puff(dt, SMOKE_INTERVAL_IDLE)
                x = lerp(boardStartX, BIKE_RIDE_X, smoothstep(progressOf(phaseTime, BIKE_BOARD_SECONDS)))
                if (phaseTime >= BIKE_BOARD_SECONDS) enter(VehiclePhase.RIDING)
            }
            VehiclePhase.RIDING -> {
                wheelPhase += scroll * WHEEL_TURN
                puff(dt, SMOKE_INTERVAL_RIDING)
                // Pushes snails and fences aside, it is too small to care
                world.clearTrack(x, x + BIKE_LENGTH) { fence -> world.dust(fence.x) }
                if (phaseTime >= BIKE_RIDE_SECONDS) {
                    // The engine dies with a bang
                    repeat(BANG_PUFFS) { addPuff(spread = 6f) }
                    world.addBonusPoints(BIKE_POINTS)
                    world.resumeFences()
                    enter(VehiclePhase.UNLOADING)
                }
            }
            VehiclePhase.UNLOADING -> {
                // Rolls out and falls behind while the cowboy jumps back
                x -= scroll * progressOf(phaseTime, BIKE_UNLOAD_SECONDS)
                puff(dt, SMOKE_INTERVAL_DYING)
                if (phaseTime >= BIKE_UNLOAD_SECONDS) enter(VehiclePhase.LEAVING)
            }
            VehiclePhase.LEAVING -> {
                // Tips over and stays behind, still smoking a bit
                x -= scroll
                rotation = FALLEN_DEGREES * smoothstep(progressOf(phaseTime, FALL_SECONDS))
                puff(dt, SMOKE_INTERVAL_IDLE)
                if (x + BIKE_LENGTH < 0f && smoke.isEmpty()) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun puff(dt: Float, interval: Float) {
        smokeIn -= dt
        if (smokeIn > 0f) return
        smokeIn = interval
        addPuff(spread = 0.5f)
    }

    private fun addPuff(spread: Float) {
        smoke.add(
            Puff(
                x = x + BIKE_EXHAUST_X + (Random.nextFloat() - 0.5f) * spread,
                y = BIKE_EXHAUST_Y + Random.nextFloat() * spread * 0.5f,
                vx = -SMOKE_DRIFT_BACK * (0.6f + Random.nextFloat() * 0.8f),
                vy = SMOKE_RISE * (0.5f + Random.nextFloat()),
                size = 1f + Random.nextFloat() * 1.2f,
            )
        )
    }

    private fun stepSmoke(dt: Float, scroll: Float) {
        smoke.forEach { puff ->
            puff.age += dt
            puff.x += puff.vx * dt - scroll
            puff.y += puff.vy * dt
        }
        smoke.removeAll { it.age >= SMOKE_SECONDS }
    }

    override fun cowboy(world: RodeoWorld): RodeoCowboyUi? {
        val saddleX = HORSE_X + COWBOY_SEAT_X
        val saddleY = world.riderlessHop + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH
        val seatY = BIKE_SEAT_Y - 1f
        return when (phase) {
            // Jumps off the horse onto the tiny seat...
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, BIKE_BOARD_SECONDS)
                RodeoCowboyUi(
                    x = lerp(saddleX, x + BIKE_SEAT_X, progress),
                    height = lerp(world.horseHeight + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, seatY, progress) + BIKE_JUMP_HOP * sin(PI.toFloat() * progress),
                    rotation = 0f,
                    facingLeft = false,
                    hatLift = 1f,
                )
            }
            // ...and back into the saddle once the engine died
            VehiclePhase.UNLOADING -> {
                val progress = progressOf(phaseTime, BIKE_UNLOAD_SECONDS)
                RodeoCowboyUi(
                    x = lerp(x + BIKE_SEAT_X, saddleX, progress),
                    height = lerp(seatY, saddleY, progress) + BIKE_JUMP_HOP * sin(PI.toFloat() * progress),
                    rotation = -360f * progress,
                    facingLeft = false,
                    hatLift = 1f,
                )
            }
            else -> null
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        val riding = phase == VehiclePhase.RIDING
        return RodeoPocketBikeUi(
            x = x,
            rotation = if (riding) BIKE_RATTLE_DEGREES * sin(shake * 70f) else rotation,
            wheelPhase = wheelPhase,
            hasRider = riding,
            smoke = smoke.map { puff ->
                val life = puff.age / SMOKE_SECONDS
                RodeoSmokePuffUi(x = puff.x, y = puff.y, radius = puff.size * (1f + 3f * life), alpha = 1f - life)
            },
            lassoHint = lassoHint(x + BIKE_LENGTH / 2f, 10f),
        )
    }
}

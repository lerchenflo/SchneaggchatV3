package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.train

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.KMH_PER_UNIT_PER_SECOND
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoDeckVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Wälderbähnle: a little steam train puffs by. Lasso its coach and the horse hops onto the roof.
// The train runs a bit faster than the horse and ducks under low bridges - so does the rider, by
// holding the jump input. Every bridge cleared gives points; one hit knocks horse and rider off.

// Shape, shared with the drawing (grid: x from the rear of the coach, y up from the ground)
internal const val TRAIN_ROOF = 13.5f                // the horse's hooves stand on the coach roof
internal const val TRAIN_HITCH_X = 2f                // grab rail at the back of the coach
private const val TRAIN_HITCH_Y = 8f
internal const val TRAIN_CHIMNEY_X = 54f
internal const val TRAIN_CHIMNEY_TOP = 19.5f
// Bridges: the deck's underside clears the horse ducking (hooves + 17) but not standing (hooves + 30)
internal const val BRIDGE_WIDTH = 14f
internal const val BRIDGE_UNDERSIDE = TRAIN_ROOF + 20.5f
internal const val BRIDGE_DECK = 5f

private const val TRAIN_LENGTH = 64f
private const val TRAIN_PASS_SPEED = 22f             // u/s across the screen while passing by
private const val TRAIN_RIDE_X = HORSE_X - 8f        // coach under the horse while riding
private const val TRAIN_SPEED_FACTOR = 1.5f          // world scroll while riding, relative to the horse's pace
private const val TRAIN_LEAVE_SPEED = 50f            // u/s it puffs off to the right afterwards
private const val TRAIN_ROCK_DEGREES = 0.6f

private const val BRIDGE_FIRST_SECONDS = 1f
private const val BRIDGE_INTERVAL_MIN = 1f
private const val BRIDGE_INTERVAL_RANDOM = 0.7f
private const val BRIDGE_LAST_SECONDS = 1.3f         // no new bridge this close to the end of the ride
private const val BRIDGE_POINTS = 15
private const val BRIDGE_CRASH_PENALTY = 12f
private const val HORSE_STAND_TOP = 30f              // horse and rider above the hooves, hat included
private const val HORSE_DUCK_DROP = 13f              // how much lower they get fully ducked
private const val DUCK_RESPONSE = 18f                // 1/s
// Horse and rider span this much of the horse grid horizontally (tail to nose)
private const val HORSE_SPAN_LEFT = 4f
private const val HORSE_SPAN_RIGHT = 28f

private const val SMOKE_INTERVAL = 0.06f
private const val SMOKE_SECONDS = 1.4f

private class Bridge(var x: Float) {
    var done = false
}

private class Smoke(var x: Float, var y: Float) {
    var age = 0f
}

internal class RodeoTrain : RodeoDeckVehicle(RodeoVehicleKind.TRAIN) {

    private val bridges = mutableListOf<Bridge>()
    private val smoke = mutableListOf<Smoke>()
    private var nextBridgeIn = 0f
    private var nextSmokeIn = 0f
    private var duckHeld = false
    private var duck = 0f

    override val length = TRAIN_LENGTH
    override val passSpeed = TRAIN_PASS_SPEED
    override val deckHeight = TRAIN_ROOF
    override val rideX = TRAIN_RIDE_X
    override val rideSeconds = 7f
    override val boardSeconds = 0.7f
    override val wheelTurn = 0.4f

    override fun hitch() = (x + TRAIN_HITCH_X) to TRAIN_HITCH_Y

    override fun reset() {
        super.reset()
        bridges.clear()
        smoke.clear()
        duck = 0f
        duckHeld = false
    }

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        duck = 0f
        duckHeld = false
    }

    override fun onJump(pressed: Boolean) {
        duckHeld = pressed
    }

    override fun worldScroll(world: RodeoWorld, step: Float, dt: Float): Float =
        if (isRiding) step * TRAIN_SPEED_FACTOR else step

    override fun speedKmh(world: RodeoWorld): Int? =
        if (isRiding) (world.speed * TRAIN_SPEED_FACTOR * KMH_PER_UNIT_PER_SECOND).roundToInt() else null

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? = if (carriesRider && phase != VehiclePhase.BOARDING) {
        RodeoRidePose(
            pitch = TRAIN_ROCK_DEGREES * sin(runTimeSeconds * 20f),
            hatLift = 0.5f + 0.4f * sin(runTimeSeconds * 30f),
            duck = duck,
        )
    } else null

    override fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) {
        val duckTarget = if (duckHeld && isRiding) 1f else 0f
        duck += (duckTarget - duck) * min(1f, dt * DUCK_RESPONSE)
        stepSmoke(dt, scroll)
        bridges.forEach { it.x -= scroll }
        bridges.removeAll { it.x + BRIDGE_WIDTH < 0f }
    }

    /** The cowcatcher clears the track: fences and snails in the way are swept aside for free. */
    override fun whileBoarding(world: RodeoWorld) = world.clearTrack(x, x + TRAIN_LENGTH)

    override fun onRideStart() {
        nextBridgeIn = BRIDGE_FIRST_SECONDS
    }

    override fun ride(world: RodeoWorld, dt: Float) {
        world.clearTrack(x, x + TRAIN_LENGTH)
        nextBridgeIn -= dt
        if (nextBridgeIn <= 0f && phaseTime < rideSeconds - BRIDGE_LAST_SECONDS) {
            bridges.add(Bridge(x = world.worldWidth + 2f))
            nextBridgeIn = BRIDGE_INTERVAL_MIN + Random.nextFloat() * BRIDGE_INTERVAL_RANDOM
        }

        val left = HORSE_X + HORSE_SPAN_LEFT
        val right = HORSE_X + HORSE_SPAN_RIGHT
        val top = horseLift + HORSE_STAND_TOP - HORSE_DUCK_DROP * duck
        bridges.forEach { bridge ->
            if (bridge.done) return@forEach
            val overlaps = bridge.x < right && bridge.x + BRIDGE_WIDTH > left
            if (overlaps && top > BRIDGE_UNDERSIDE) {
                // Knocked off the roof
                bridge.done = true
                world.crash(BRIDGE_CRASH_PENALTY)
                world.dust(HORSE_X + 20f)
                getOff(world)
                return
            }
            if (bridge.x + BRIDGE_WIDTH < left) {
                bridge.done = true
                world.addBonusPoints(BRIDGE_POINTS)
                world.sparkle(bridge.x + BRIDGE_WIDTH, BRIDGE_UNDERSIDE)
            }
        }
        if (rideIsOver) getOff(world)
    }

    /** Puffs off to the right; done once it and its smoke and bridges are out of the picture. */
    override fun leave(world: RodeoWorld, dt: Float, scroll: Float) {
        x += TRAIN_LEAVE_SPEED * dt
        if (x > world.worldWidth && bridges.isEmpty() && smoke.isEmpty()) enter(VehiclePhase.IDLE)
    }

    private fun stepSmoke(dt: Float, scroll: Float) {
        smoke.forEach { puff ->
            puff.age += dt
            // Hangs in the air: stays behind with the ground, drifting up and back
            puff.x -= scroll + 6f * dt
            puff.y += (7f + 3f * puff.age) * dt
        }
        smoke.removeAll { it.age >= SMOKE_SECONDS }
        if (phase == VehiclePhase.IDLE) return
        nextSmokeIn -= dt
        if (nextSmokeIn <= 0f) {
            nextSmokeIn = SMOKE_INTERVAL
            smoke.add(Smoke(x = x + TRAIN_CHIMNEY_X, y = TRAIN_CHIMNEY_TOP))
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoTrainUi(
            x = x,
            wheelPhase = wheelPhase,
            bridges = bridges.map { it.x },
            smoke = smoke.map { RodeoSmokeUi(x = it.x, y = it.y, progress = it.age / SMOKE_SECONDS) },
            lassoHint = lassoHint(x + 15f, 19f),
        )
    }
}

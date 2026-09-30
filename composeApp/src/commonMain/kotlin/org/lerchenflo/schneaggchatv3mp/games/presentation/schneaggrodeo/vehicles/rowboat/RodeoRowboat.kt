package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rowboat

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEA_WATER_LINE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.LassoGrab
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoDeckVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.sin
import kotlin.random.Random

// Rowboat (underground sea): a little wooden rowboat drifts by. Lasso it and the horse hops in; the
// oars dip on their own while fish keep jumping out of the water ahead - lasso them for points
// before they splash back in. The boat pushes the buoys aside.

// Shape, shared with the drawing (grid: x from the stern, y up from the ground under the water)
internal const val BOAT_LENGTH = 34f
/** Where the horse's hooves stand inside the boat. */
internal const val BOAT_FLOOR = 3.5f
/** Top edge of the boat's side, covering the horse's legs. */
internal const val BOAT_GUNWALE = 8f
internal const val FISH_LENGTH = 3f

private const val BOAT_PASS_SPEED = 22f
private const val BOAT_HITCH_Y = 6f
private const val BOAT_RIDE_X = HORSE_X - 3f
private const val BOAT_SPEED_FACTOR = 1.1f
private const val BOAT_LEAVE_SPEED = 40f
private const val BOAT_ROCK_DEGREES = 1.5f
private const val FISH_INTERVAL_MIN = 0.45f
private const val FISH_INTERVAL_RANDOM = 0.35f
private const val FISH_GRAVITY = 80f
private const val FISH_MIN_VY = 45f
private const val FISH_RANDOM_VY = 20f
private const val FISH_POINTS = 15

private enum class FishState { JUMPING, CAUGHT }

/** [x] / [y] the fish's center; [vx] its own swim on top of the ground scrolling by. */
private class Fish(var x: Float, var y: Float, val vx: Float, var vy: Float) {
    var state = FishState.JUMPING
    var rotation = 0f
}

internal class RodeoRowboat : RodeoDeckVehicle(RodeoVehicleKind.ROWBOAT) {

    private val fish = mutableListOf<Fish>()
    private var fishIn = 0f
    private var rowPhase = 0f

    override val length = BOAT_LENGTH
    override val passSpeed = BOAT_PASS_SPEED
    override val deckHeight = BOAT_FLOOR
    override val rideX = BOAT_RIDE_X
    override val rideSeconds = 8f
    override val allowsLasso = true
    override val hop = 5f

    /** The whole boat is a target. */
    override fun hitch() = hitchX() to BOAT_HITCH_Y

    override fun reset() {
        super.reset()
        fish.clear()
    }

    override val rideSpeedFactor = BOAT_SPEED_FACTOR

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? =
        if (isRiding) RodeoRidePose(pitch = BOAT_ROCK_DEGREES * sin(runTimeSeconds * 3f)) else null

    /** The jumping fish closest to the hand that will be within reach when the loop arrives. */
    override fun rideLassoGrab(world: RodeoWorld, handX: Float, reach: ClosedFloatingPointRange<Float>, timeToCatch: Float): LassoGrab? {
        val groundSpeed = world.speed * BOAT_SPEED_FACTOR
        val target = fish
            .filter { it.state == FishState.JUMPING && it.x + (it.vx - groundSpeed) * timeToCatch in reach }
            .minByOrNull { it.x }
            ?: return null
        return object : LassoGrab {
            override val x: Float get() = target.x
            override val y: Float get() = target.y
            override fun catch(): Boolean {
                if (target.state != FishState.JUMPING) return false
                target.state = FishState.CAUGHT
                world.addBonusPoints(FISH_POINTS)
                world.sparkle(target.x, target.y)
                return true
            }
            override fun follow(tipX: Float, tipY: Float) {
                target.x = tipX
                target.y = tipY
                target.rotation = 70f
            }
            override fun release() {
                fish.remove(target)
            }
        }
    }

    override fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) {
        rowPhase += dt * if (isRiding) 5f else 2f
        fish.forEach { f ->
            if (f.state == FishState.JUMPING) {
                f.vy -= FISH_GRAVITY * dt
                f.x += f.vx * dt - scroll
                f.y += f.vy * dt
                f.rotation = -f.vy * 0.8f
            }
        }
        fish.removeAll { it.state == FishState.JUMPING && it.y < SEA_WATER_LINE - 1f && it.vy < 0f }
    }

    override fun whileBoarding(world: RodeoWorld) = push(world)

    override fun onRideStart() {
        fishIn = 0.3f
    }

    override fun ride(world: RodeoWorld, dt: Float) {
        push(world)
        fishIn -= dt
        if (fishIn <= 0f) {
            fishIn = FISH_INTERVAL_MIN + Random.nextFloat() * FISH_INTERVAL_RANDOM
            fish.add(
                Fish(
                    x = world.worldWidth * (0.45f + Random.nextFloat() * 0.5f),
                    y = SEA_WATER_LINE,
                    vx = -6f + Random.nextFloat() * 12f,
                    vy = FISH_MIN_VY + Random.nextFloat() * FISH_RANDOM_VY,
                )
            )
        }
        if (rideIsOver) getOff(world)
    }

    override fun leave(world: RodeoWorld, dt: Float, scroll: Float) {
        x += BOAT_LEAVE_SPEED * dt
        if (x > world.worldWidth && fish.isEmpty()) enter(VehiclePhase.IDLE)
    }

    /** Buoys and snails in front of the boat are pushed aside. */
    private fun push(world: RodeoWorld) {
        world.clearTrack(x, x + BOAT_LENGTH) { fence -> world.dust(fence.x) }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoRowboatUi(
            x = x,
            rowPhase = rowPhase,
            fish = fish.map { RodeoFishUi(x = it.x, y = it.y, rotation = it.rotation) },
            lassoHint = lassoHint(x + BOAT_LENGTH / 2f, BOAT_GUNWALE + 6f),
        )
    }
}

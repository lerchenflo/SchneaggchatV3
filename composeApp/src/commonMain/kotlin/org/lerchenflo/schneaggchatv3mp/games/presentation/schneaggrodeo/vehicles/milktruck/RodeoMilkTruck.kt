package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.milktruck

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.KMH_PER_UNIT_PER_SECOND
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SNAIL_SIZE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.LassoGrab
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

// Milk truck: a cow-spotted milk truck rolls by. Lasso it and the horse hops onto the cargo box.
// The truck rams through the fences, and every hit shakes a milk can off the rack on its roof, high
// up into the air ahead - lasso the flying cans for points before they splash onto the road.

// Shape, shared with the drawing (grid: x from the rear of the cargo box, y up from the ground)
internal const val TRUCK_ROOF = 17f                  // the horse's hooves stand on the cargo box
internal const val TRUCK_HITCH_X = 1f                // rear door handle
private const val TRUCK_HITCH_Y = 9f
internal const val RACK_X = 24f                      // first can of the rack
internal const val RACK_SLOTS = 3
internal const val CAN_WIDTH = 2f
internal const val CAN_HEIGHT = 3.4f
/** Space between two cans in the rack. */
internal const val CAN_SPACING = 0.6f

private const val TRUCK_LENGTH = 44f
private const val TRUCK_PASS_SPEED = 26f             // u/s across the screen while passing by
private const val TRUCK_RIDE_X = HORSE_X - 5f
private const val TRUCK_SPEED_FACTOR = 1.3f          // world scroll while riding, relative to the horse's pace
private const val TRUCK_LEAVE_SPEED = 60f
private const val TRUCK_BOUNCE_DEGREES = 0.7f
private const val TRUCK_FIRST_FENCE_SECONDS = 0.5f
private const val TRUCK_FENCE_INTERVAL_MIN = 0.55f
private const val TRUCK_FENCE_INTERVAL_RANDOM = 0.35f

// Milk cans: stand in the rack on the roof in front of the horse and fly up when the truck hits a fence
private const val MILK_CANS = 8
private const val CAN_GRAVITY = 80f
private const val CAN_MIN_VX = 20f                   // u/s forward on screen: the truck throws them ahead
private const val CAN_RANDOM_VX = 25f
private const val CAN_MIN_VY = 55f
private const val CAN_RANDOM_VY = 17f
private const val CAN_POINTS = 20

private enum class CanState { FLYING, CAUGHT, SPILLED }

/** [x] is the can's center, [y] its middle above the ground. */
private class MilkCan(var x: Float, var y: Float, val vx: Float, var vy: Float, val spin: Float) {
    var state = CanState.FLYING
    var rotation = 0f
}

/** Center of the can in rack [slot], relative to the truck. */
internal fun rackSlotX(slot: Int) = RACK_X + slot * (CAN_WIDTH + CAN_SPACING) + CAN_WIDTH / 2f

internal class RodeoMilkTruck : RodeoDeckVehicle(RodeoVehicleKind.MILK_TRUCK) {

    private val cans = mutableListOf<MilkCan>()
    private var fenceIn = 0f
    private var cansLeft = MILK_CANS

    override val length = TRUCK_LENGTH
    override val passSpeed = TRUCK_PASS_SPEED
    override val deckHeight = TRUCK_ROOF
    override val rideX = TRUCK_RIDE_X
    override val rideSeconds = 7f
    override val unloadSeconds = 0.7f
    override val allowsLasso = true

    override fun hitch() = (x + TRUCK_HITCH_X) to TRUCK_HITCH_Y

    override fun reset() {
        super.reset()
        cans.clear()
    }

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        cansLeft = MILK_CANS
    }

    override fun worldScroll(world: RodeoWorld, step: Float, dt: Float): Float =
        if (isRiding) step * TRUCK_SPEED_FACTOR else step

    override fun speedKmh(world: RodeoWorld): Int? =
        if (isRiding) (world.speed * TRUCK_SPEED_FACTOR * KMH_PER_UNIT_PER_SECOND).roundToInt() else null

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? = if (isRiding) {
        RodeoRidePose(
            pitch = TRUCK_BOUNCE_DEGREES * sin(runTimeSeconds * 25f),
            hatLift = 0.6f + 0.4f * sin(runTimeSeconds * 33f),
        )
    } else null

    /** The flying can closest to the hand that will be within reach when the loop arrives. */
    override fun rideLassoGrab(world: RodeoWorld, handX: Float, reach: ClosedFloatingPointRange<Float>, timeToCatch: Float): LassoGrab? {
        val can = cans
            .filter { it.state == CanState.FLYING && it.x + it.vx * timeToCatch in reach }
            .minByOrNull { it.x }
            ?: return null
        return object : LassoGrab {
            override val x: Float get() = can.x
            override val y: Float get() = can.y
            override fun catch(): Boolean {
                if (can.state != CanState.FLYING) return false
                // Points right away; it dangles back to the hand
                can.state = CanState.CAUGHT
                world.addBonusPoints(CAN_POINTS)
                world.sparkle(can.x, can.y)
                return true
            }
            override fun follow(tipX: Float, tipY: Float) {
                can.x = tipX
                can.y = tipY - CAN_HEIGHT / 2f
                can.rotation = -20f
            }
            override fun release() {
                cans.remove(can)
            }
        }
    }

    override fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) = stepCans(world, dt, scroll)

    override fun whileBoarding(world: RodeoWorld) = ram(world)

    override fun onRideStart() {
        fenceIn = TRUCK_FIRST_FENCE_SECONDS
    }

    override fun ride(world: RodeoWorld, dt: Float) {
        fenceIn -= dt
        if (fenceIn <= 0f) {
            world.addFence(x = world.worldWidth, gapAfter = TRUCK_LENGTH)
            fenceIn = TRUCK_FENCE_INTERVAL_MIN + Random.nextFloat() * TRUCK_FENCE_INTERVAL_RANDOM
        }
        ram(world)
        if (rideIsOver) getOff(world)
    }

    /** Drives off to the right; done once no can is left lying on the road. */
    override fun leave(world: RodeoWorld, dt: Float, scroll: Float) {
        x += TRUCK_LEAVE_SPEED * dt
        if (x > world.worldWidth && cans.none { it.state != CanState.CAUGHT }) enter(VehiclePhase.IDLE)
    }

    /** Fences and snails in front of the truck are rammed aside; every fence shakes a can loose. */
    private fun ram(world: RodeoWorld) {
        world.clearTrack(x, x + TRUCK_LENGTH) { fence ->
            world.dust(fence.x)
            if (isRiding) launchCan()
        }
    }

    private fun launchCan() {
        if (cansLeft <= 0) return
        cansLeft--
        cans.add(
            MilkCan(
                x = x + rackSlotX(cansLeft % RACK_SLOTS),
                y = TRUCK_ROOF + CAN_HEIGHT / 2f,
                vx = CAN_MIN_VX + Random.nextFloat() * CAN_RANDOM_VX,
                vy = CAN_MIN_VY + Random.nextFloat() * CAN_RANDOM_VY,
                spin = (if (Random.nextBoolean()) 1f else -1f) * (200f + Random.nextFloat() * 300f),
            )
        )
    }

    private fun stepCans(world: RodeoWorld, dt: Float, scroll: Float) {
        cans.forEach { can ->
            when (can.state) {
                CanState.FLYING -> {
                    can.vy -= CAN_GRAVITY * dt
                    can.x += can.vx * dt
                    can.y += can.vy * dt
                    can.rotation += can.spin * dt
                    if (can.y <= CAN_WIDTH / 2f) {
                        // Splat: it lies on the road in a puddle of milk and scrolls away
                        can.state = CanState.SPILLED
                        can.y = CAN_WIDTH / 2f
                        can.rotation = 90f
                        world.dust(can.x)
                    }
                }
                CanState.SPILLED -> can.x -= scroll
                CanState.CAUGHT -> Unit // follows the lasso
            }
        }
        cans.removeAll { it.state == CanState.SPILLED && it.x < -SNAIL_SIZE }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoMilkTruckUi(
            x = x,
            wheelPhase = wheelPhase,
            cansInRack = min(RACK_SLOTS, cansLeft),
            cans = cans.map { RodeoMilkCanUi(x = it.x, y = it.y, rotation = it.rotation, spilled = it.state == CanState.SPILLED) },
            lassoHint = lassoHint(x + TRUCK_LENGTH / 2f, TRUCK_ROOF + 6f),
        )
    }
}

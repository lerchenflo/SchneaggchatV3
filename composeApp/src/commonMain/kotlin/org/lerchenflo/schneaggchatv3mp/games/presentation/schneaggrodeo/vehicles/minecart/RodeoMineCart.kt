package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.minecart

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoDeckVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.sin
import kotlin.random.Random

// Mine cart ("Grubenhunt", the mine): a rusty mine cart rolls by on rails. Lasso it and the horse
// hops in. The cart races down the rails at twice the pace, sparks flying from its wheels, and
// smashes every crate and rock pile in the way - points for each. Then the horse hops out again.

// Shape, shared with the drawing (grid: x from the cart's rear, y up from the rails)
internal const val CART_LENGTH = 22f
/** The cart's floor, where the horse's hooves stand. */
internal const val CART_FLOOR = 3.5f
/** Top edge of the cart's side, covering the horse's legs. */
internal const val CART_RIM = 9f
internal const val CART_WHEEL_RADIUS = 1.5f

private const val CART_PASS_SPEED = 26f
private const val CART_HITCH_Y = 6f
private const val CART_RIDE_X = HORSE_X + 2f
private const val CART_SPEED_FACTOR = 2f
private const val CART_LEAVE_SPEED = 80f
private const val CART_RATTLE_DEGREES = 1.2f
private const val CRATE_POINTS = 6
private const val CART_FENCE_SECONDS = 0.5f

internal class RodeoMineCart : RodeoDeckVehicle(RodeoVehicleKind.MINE_CART) {

    private var fenceIn = 0f
    /** How far the rails scrolled, for drawing the sleepers. */
    private var railOffset = 0f
    private var sparks = 0f

    override val length = CART_LENGTH
    override val passSpeed = CART_PASS_SPEED
    override val deckHeight = CART_FLOOR
    override val rideX = CART_RIDE_X
    override val rideSeconds = 7f
    override val hop = 6f
    override val wheelTurn = 0.6f

    override val shaking: Boolean get() = isRiding

    /** The whole cart is a target. */
    override fun hitch() = hitchX() to CART_HITCH_Y

    override val rideSpeedFactor = CART_SPEED_FACTOR

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? =
        if (isRiding) RodeoRidePose(pitch = CART_RATTLE_DEGREES * sin(runTimeSeconds * 40f), hatLift = 0.5f) else null

    override val riderLeans: Boolean get() = isRiding

    override fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) {
        railOffset += scroll
        if (isRiding) sparks += dt
    }

    override fun whileBoarding(world: RodeoWorld) = smash(world, points = false)

    override fun onRideStart() {
        fenceIn = 0.4f
    }

    override fun ride(world: RodeoWorld, dt: Float) {
        fenceIn -= dt
        if (fenceIn <= 0f) {
            world.addFence(x = world.worldWidth, gapAfter = CART_LENGTH * 2f)
            fenceIn = CART_FENCE_SECONDS + Random.nextFloat() * 0.3f
        }
        smash(world, points = true)
        if (rideIsOver) getOff(world)
    }

    override fun leave(world: RodeoWorld, dt: Float, scroll: Float) {
        x += CART_LEAVE_SPEED * dt
        if (x > world.worldWidth) enter(VehiclePhase.IDLE)
    }

    /** Crates and rock piles in front of the cart are smashed. */
    private fun smash(world: RodeoWorld, points: Boolean) {
        world.clearTrack(x, x + CART_LENGTH) { fence ->
            world.dust(fence.x)
            if (points) world.addBonusPoints(CRATE_POINTS)
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoMineCartUi(
            x = x,
            wheelPhase = wheelPhase,
            railOffset = railOffset,
            sparks = if (isRiding) sparks else null,
            lassoHint = lassoHint(x + CART_LENGTH / 2f, CART_RIM + 6f),
        )
    }
}

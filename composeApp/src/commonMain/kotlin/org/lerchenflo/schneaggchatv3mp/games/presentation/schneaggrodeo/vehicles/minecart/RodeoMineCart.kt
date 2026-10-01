package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.minecart

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoMap
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoDeckVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.sin
import kotlin.random.Random

// Mine cart ("Grubenhunt", the cave): a rusty mine cart rolls by on rails. Lasso it and the horse
// hops in. The cart races down the rails at twice the pace, sparks flying from its wheels, and
// smashes every stalagmite in the way - points for each. Then the horse hops out again.
// On the surface one full of gold comes [toCave] out of the gold mine behind the track (see
// RodeoMapSwitch) and rolls along beside the horse for a while, like a wild horse. Lasso it: the gold
// is yours and it rattles down into the cave with horse and rider. Leave it and it falls behind -
// the run stays up on the surface.

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
private const val STALAGMITE_POINTS = 6
private const val CART_FENCE_SECONDS = 0.5f
// The cart out of the gold mine: rolls beside the horse (screen x) for a while, then falls behind;
// caught, it races off down into the cave after a moment
private const val BESIDE_X = HORSE_X + 34f
private const val BESIDE_SECONDS = 5f
private const val FALL_BEHIND_ACCELERATION = 40f
private const val GOLD_POINTS = 30
private const val TO_CAVE_SECONDS = 1.2f
/** Safety net: should the trip down not start, the horse hops out after this long. */
private const val TO_CAVE_GIVE_UP_SECONDS = 5f

internal class RodeoMineCart(private val toCave: Boolean = false) : RodeoDeckVehicle(RodeoVehicleKind.MINE_CART) {

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
    /** Rolling beside the horse since it caught up with it; negative while it still comes out of the mine. */
    private var besideTime = -1f
    private var tookOff = false

    override val shaking: Boolean get() = isRiding

    /** Comes out of the mine at [x]. */
    fun parkAt(x: Float) {
        this.x = x
        besideTime = -1f
    }

    override fun approach(world: RodeoWorld, dt: Float, scroll: Float) {
        if (!toCave) return passBy(dt)
        when {
            // Out of the mine: stays with the ground until the horse comes up beside it
            besideTime < 0f -> {
                x -= scroll
                if (x <= BESIDE_X) besideTime = 0f
            }
            // Keeps pace beside the horse, bumping along
            besideTime < BESIDE_SECONDS -> {
                besideTime += dt
                x = BESIDE_X + 0.8f * sin(besideTime * 3f)
            }
            // Then it falls behind and is gone
            else -> {
                besideTime += dt
                x -= FALL_BEHIND_ACCELERATION * (besideTime - BESIDE_SECONDS) * dt
                if (x + length < 0f) enter(VehiclePhase.IDLE)
            }
        }
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        if (toCave) {
            tookOff = false
            // The gold in it is the rider's
            world.addBonusPoints(GOLD_POINTS)
            world.sparkle(x + CART_LENGTH / 2f, CART_RIM + 2f)
        }
    }

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
        if (toCave) {
            // Rattles off down into the cave: the fade ends the ride down there; no stalagmites up here
            smash(world, points = false)
            if (!tookOff && phaseTime >= TO_CAVE_SECONDS) {
                tookOff = true
                world.travelTo(RodeoMap.CAVE)
            }
            if (phaseTime >= TO_CAVE_GIVE_UP_SECONDS) getOff(world)
            return
        }
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

    /** Stalagmites in front of the cart are smashed. */
    private fun smash(world: RodeoWorld, points: Boolean) {
        world.clearTrack(x, x + CART_LENGTH) { fence ->
            world.dust(fence.x)
            if (points) world.addBonusPoints(STALAGMITE_POINTS)
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
            // Up on the surface it rolls along beside the track on its own rails, full of gold
            railsAcross = !toCave,
            gold = toCave && !carriesRider,
        )
    }
}

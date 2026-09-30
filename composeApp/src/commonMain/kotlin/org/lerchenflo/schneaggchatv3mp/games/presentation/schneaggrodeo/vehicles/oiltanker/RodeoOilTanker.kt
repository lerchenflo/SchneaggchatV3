package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.oiltanker

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.KMH_PER_UNIT_PER_SECOND
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoDeckVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.roundToInt
import kotlin.math.sin

// Oil tanker (sea): a huge black tanker steams by. Lasso it and the horse hops up onto
// its long deck. It plows slowly through every buoy (points for each), smoke pouring from its
// funnel and its horn blaring now and then.

// Shape, shared with the drawing (grid: x from the stern, y up from the ground under the water)
internal const val TANKER_LENGTH = 96f
/** The deck the horse stands on. */
internal const val TANKER_DECK = 12f
/** The bridge (superstructure) at the stern, and the funnel on top of it. */
internal const val BRIDGE_X = 4f
internal const val BRIDGE_WIDTH = 14f
internal const val BRIDGE_TOP = 26f
internal const val FUNNEL_X = 8f

private const val TANKER_PASS_SPEED = 18f
private const val TANKER_HITCH_Y = 10f
/** Stern while riding: the horse stands on the deck in front of the bridge. */
private const val TANKER_RIDE_X = HORSE_X - 22f
private const val TANKER_SPEED_FACTOR = 0.85f
private const val TANKER_LEAVE_SPEED = 30f
private const val TANKER_ROLL_DEGREES = 0.8f
private const val BUOY_POINTS = 6
private const val HORN_INTERVAL = 2.5f
private const val HORN_SECONDS = 0.7f

internal class RodeoOilTanker : RodeoDeckVehicle(RodeoVehicleKind.OIL_TANKER) {

    private var smoke = 0f
    private var hornIn = 0f
    private var hornTime = HORN_SECONDS

    override val length = TANKER_LENGTH
    override val passSpeed = TANKER_PASS_SPEED
    override val deckHeight = TANKER_DECK
    override val rideX = TANKER_RIDE_X
    override val rideSeconds = 9f
    override val boardSeconds = 0.8f
    override val hop = 8f

    /** The whole ship is a target. */
    override fun hitch() = hitchX() to TANKER_HITCH_Y

    override fun worldScroll(world: RodeoWorld, step: Float, dt: Float): Float =
        if (isRiding) step * TANKER_SPEED_FACTOR else step

    override fun speedKmh(world: RodeoWorld): Int? =
        if (isRiding) (world.speed * TANKER_SPEED_FACTOR * KMH_PER_UNIT_PER_SECOND).roundToInt() else null

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? =
        if (isRiding) RodeoRidePose(pitch = TANKER_ROLL_DEGREES * sin(runTimeSeconds * 1.2f)) else null

    override fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) {
        smoke += dt
        hornTime += dt
    }

    override fun whileBoarding(world: RodeoWorld) = plow(world, points = false)

    override fun onRideStart() {
        hornIn = 0.3f
    }

    override fun ride(world: RodeoWorld, dt: Float) {
        plow(world, points = true)
        hornIn -= dt
        if (hornIn <= 0f) {
            hornIn = HORN_INTERVAL
            hornTime = 0f
        }
        if (rideIsOver) getOff(world)
    }

    override fun leave(world: RodeoWorld, dt: Float, scroll: Float) {
        x += TANKER_LEAVE_SPEED * dt
        if (x > world.worldWidth) enter(VehiclePhase.IDLE)
    }

    /** Buoys and snails in front of the bow are pushed aside. */
    private fun plow(world: RodeoWorld, points: Boolean) {
        world.clearTrack(x, x + TANKER_LENGTH) { fence ->
            world.dust(fence.x)
            if (points) world.addBonusPoints(BUOY_POINTS)
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoOilTankerUi(
            x = x,
            smoke = smoke,
            horn = if (hornTime < HORN_SECONDS) hornTime / HORN_SECONDS else null,
            lassoHint = lassoHint(x + TANKER_LENGTH / 2f, TANKER_DECK + 8f),
        )
    }
}

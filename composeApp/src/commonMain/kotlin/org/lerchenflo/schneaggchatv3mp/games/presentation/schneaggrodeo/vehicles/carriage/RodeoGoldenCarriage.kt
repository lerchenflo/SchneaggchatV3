package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.carriage

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

// Golden carriage: never comes along on its own - it is bought with SNAILS_PER_CARRIAGE saved-up
// snails (and pays out a big bonus right away, see the engine). Two golden horses pull it in at
// once, the horse hops up onto its roof and it races along, the golden horses smashing through
// every fence - points for each. No input needed.

internal const val SNAILS_PER_CARRIAGE = 50

// Shape, shared with the drawing (grid: x from the carriage's rear, y up from the ground)
internal const val CARRIAGE_BODY_LENGTH = 30f
/** The roof the horse stands on. */
internal const val CARRIAGE_ROOF = 18f
/** Where the pulling horses begin (their left edge), the one behind a bit further back. */
internal const val PULL_HORSE_X = 38f
internal const val CARRIAGE_LENGTH = PULL_HORSE_X + 32f

/** The horse stands in the middle of the roof. */
private const val CARRIAGE_RIDE_X = HORSE_X - 1f
private const val CARRIAGE_SPEED_FACTOR = 1.4f
private const val CARRIAGE_LEAVE_SPEED = 40f
private const val FENCE_POINTS = 20
private const val GAIT_SPEED = 16f
private const val SWAY_DEGREES = 1f

internal class RodeoGoldenCarriage : RodeoDeckVehicle(RodeoVehicleKind.GOLDEN_CARRIAGE) {

    private var gait = 0f

    override val length = CARRIAGE_LENGTH
    // Never passes by waiting for the lasso
    override val passSpeed = 0f
    override val deckHeight = CARRIAGE_ROOF
    override val rideX = CARRIAGE_RIDE_X
    override val rideSeconds = 12f
    override val boardSeconds = 1.2f
    override val unloadSeconds = 0.8f
    override val hop = 10f

    override fun hitch() = x to 0f

    /** Pulled in at once, the horse hopping onto the roof as it arrives. */
    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        board(world)
    }

    override fun worldScroll(world: RodeoWorld, step: Float, dt: Float): Float =
        if (isRiding) step * CARRIAGE_SPEED_FACTOR else step

    override fun speedKmh(world: RodeoWorld): Int? =
        if (isRiding) (world.speed * CARRIAGE_SPEED_FACTOR * KMH_PER_UNIT_PER_SECOND).roundToInt() else null

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? =
        if (isRiding) RodeoRidePose(pitch = SWAY_DEGREES * sin(runTimeSeconds * 5f)) else null

    override fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) {
        gait += dt * GAIT_SPEED
    }

    override fun whileBoarding(world: RodeoWorld) = smash(world, points = false)

    override fun ride(world: RodeoWorld, dt: Float) {
        smash(world, points = true)
        if (rideIsOver) getOff(world)
    }

    override fun leave(world: RodeoWorld, dt: Float, scroll: Float) {
        x += CARRIAGE_LEAVE_SPEED * dt
        if (x > world.worldWidth) enter(VehiclePhase.IDLE)
    }

    /** The golden horses trample every fence and snail in front of them. */
    private fun smash(world: RodeoWorld, points: Boolean) {
        world.clearTrack(x, x + CARRIAGE_LENGTH) { fence ->
            world.sparkle(fence.x, fence.top)
            if (points) world.addBonusPoints(FENCE_POINTS)
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoGoldenCarriageUi(x = x, wheelPhase = wheelPhase, gait = gait)
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cablecar

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.CABLE_FOOT_HEIGHT
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.CABLE_STATION_MARGIN
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.Horseshoe
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MOUNTAIN_WIDTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.cableHeightAt
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.hopArc
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoBoardingHop
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import kotlin.random.Random

// Cable car: comes along with a mountain (see engine/RodeoLandscape). A gondola glides by low on
// its cable before the valley station; lasso it and horse and rider hop in and float over the
// mountain along the cable, much faster than galloping, collecting the horseshoes up there. It sets
// them down behind the mountain. Missed it? Then it's the long, tiring way over the top.

// Shape, shared with the drawing (grid: x from the cabin's left edge, y up from its floor)
internal const val CABIN_WIDTH = 34f
internal const val CABIN_HEIGHT = 32f
/** Cable above the cabin floor. */
internal const val CABIN_HANG = 38f

internal const val CABLE_CAR_PASS_SPEED = 30f
private const val CABIN_HITCH_Y = 8f
/** Where the loop lands on the cabin: the middle of the lasso's reach, ahead of the hand. */
/** Seconds the loop takes to reach the cabin (half the lasso's throw), aimed at where it will be. */
private const val CABIN_RIDE_X = HORSE_X - 2f
private const val CABIN_BOARD_SECONDS = 0.8f
private const val CABIN_UNLOAD_SECONDS = 0.7f
private const val CABIN_HOP = 6f
/** The world glides by this much faster than the horse would gallop. */
private const val CABLE_CAR_SCROLL_FACTOR = 1.8f
/** Never longer than this, even if the mountain somehow went missing. */
private const val CABLE_CAR_MAX_RIDE_SECONDS = 20f
private const val CABLE_CAR_ARRIVAL_POINTS = 50
private const val HORSESHOE_SECONDS = 0.9f

internal class RodeoCableCar : RodeoVehicle(RodeoVehicleKind.CABLE_CAR) {

    /** Height of the cabin floor. */
    private var floor = 0f
    private var lift = 0f
    private val boardingHop = RodeoBoardingHop()
    private var horseshoeIn = 0f

    override val length = CABIN_WIDTH
    override val passSpeed = CABLE_CAR_PASS_SPEED

    override val horseLift: Float get() = if (carriesRider) lift else 0f

    override fun hitch() =
        hitchX() to floor + CABIN_HITCH_Y

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        floor = CABLE_FOOT_HEIGHT - CABIN_HANG
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardingHop.start(x, world)
    }

    override fun worldScroll(world: RodeoWorld, step: Float, dt: Float): Float =
        if (isRiding) step * CABLE_CAR_SCROLL_FACTOR else step

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> passBy(dt)
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, CABIN_BOARD_SECONDS)
                x = boardingHop.slideX(CABIN_RIDE_X, progress)
                lift = boardingHop.lift(floor, progress, CABIN_HOP)
                if (progress >= 1f) {
                    enter(VehiclePhase.RIDING)
                    lift = floor
                    horseshoeIn = HORSESHOE_SECONDS
                }
            }
            VehiclePhase.RIDING -> ride(world, dt)
            VehiclePhase.UNLOADING -> {
                val progress = progressOf(phaseTime, CABIN_UNLOAD_SECONDS)
                lift = hopArc(floor, 0f, progress, CABIN_HOP)
                if (progress >= 1f) {
                    lift = 0f
                    enter(VehiclePhase.LEAVING)
                }
            }
            VehiclePhase.LEAVING -> {
                // Glides on along the cable, out of the picture
                x -= CABLE_CAR_PASS_SPEED * dt
                if (x + CABIN_WIDTH < 0f) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun ride(world: RodeoWorld, dt: Float) {
        val mountainX = world.mountainX
        val cabinCenter = x + CABIN_WIDTH / 2f
        floor = (if (mountainX != null) cableHeightAt(cabinCenter, mountainX) else CABLE_FOOT_HEIGHT) - CABIN_HANG
        lift = floor

        // Horseshoes float up here, right at the horse's height
        horseshoeIn -= dt
        if (horseshoeIn <= 0f) {
            horseshoeIn = HORSESHOE_SECONDS
            world.horseshoes.add(Horseshoe(x = world.worldWidth, height = floor + 18f + Random.nextFloat() * 8f))
        }

        val pastMountain = mountainX == null || cabinCenter > mountainX + MOUNTAIN_WIDTH + CABLE_STATION_MARGIN
        if (pastMountain || phaseTime >= CABLE_CAR_MAX_RIDE_SECONDS) {
            world.addBonusPoints(CABLE_CAR_ARRIVAL_POINTS)
            world.sparkle(HORSE_X + 15f, floor + 20f)
            enter(VehiclePhase.UNLOADING)
            world.resumeFences()
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoCableCarUi(
            x = x,
            floor = floor,
            // Before and after the mountain the cable runs level across the picture
            levelCable = phase == VehiclePhase.APPROACH || phase == VehiclePhase.LEAVING,
            lassoHint = lassoHint(x + CABIN_WIDTH / 2f, floor + CABIN_HEIGHT + 3f),
        )
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.trafficjam

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HOOVES_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
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

// Traffic jam: a row of cars stands still on the track, honking. The horse hops up onto the first
// roof on its own (or sooner, lassoed) and gallops from roof to roof all the way along - points for
// every car - then hops back down behind the last one. No input needed.

// Shapes, shared with the drawing (grid: x from a car's rear, y up from the ground)
internal const val JAM_CAR_COUNT = 4
private const val JAM_GAP = 4f
/** Where the horse's hooves are, measured from HORSE_X. */
private const val HOOVES_AT = HORSE_X + HOOVES_X
/** A hop to the next roof starts this far before its car. */
private const val HOP_LEAD = 6f
private const val HOP_SECONDS = 0.4f
private const val HOP_HEIGHT = 5f
private const val BOARD_SECONDS = 0.45f
private const val BOARD_HOP = 7f
private const val UNLOAD_SECONDS = 0.5f
private const val UNLOAD_HOP = 5f
private const val CAR_POINTS = 15
private const val JAM_HITCH_Y = 8f

/** The kinds of cars in the jam, the size of the Ford Escort: how long they are and how high their roof is. */
enum class JamCarStyle(val length: Float, val roof: Float) {
    HATCHBACK(30f, 13f),
    SEDAN(36f, 12.5f),
    VAN(38f, 16f),
}

internal class RodeoTrafficJam : RodeoVehicle(RodeoVehicleKind.TRAFFIC_JAM) {

    private var cars: List<RodeoJamCarUi> = emptyList()
    private var totalLength = 0f
    /** The ground's pace; the cars stand still, so they pass by with it. */
    private var groundSpeed = 30f
    private var lift = 0f
    private val boardingHop = RodeoBoardingHop()
    /** The car the horse stands on (or hops onto). */
    private var carIndex = 0
    private var hopFrom = 0f
    private var hopTime = HOP_SECONDS
    private var time = 0f

    /** The row of cars is too long to tilt as one piece over the hills. */
    override val needsFlatTrack = true
    override val length: Float get() = totalLength
    override val passSpeed: Float get() = groundSpeed
    override val carriesHorse = false
    override val horseLift: Float get() = if (carriesRider) lift else 0f
    override val horseHops: Boolean
        get() = phase == VehiclePhase.BOARDING || phase == VehiclePhase.UNLOADING || hopTime < HOP_SECONDS

    /** No fences in between the cars. */
    override val blocksFences: Boolean get() = phase != VehiclePhase.IDLE && phase != VehiclePhase.LEAVING

    /** The whole jam is a target. */
    override fun hitch() = hitchX() to JAM_HITCH_Y

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        groundSpeed = world.speed
        var offset = 0f
        cars = List(JAM_CAR_COUNT) { index ->
            val style = JamCarStyle.entries.random()
            RodeoJamCarUi(offset = offset, style = style, color = Random.nextInt(JAM_CAR_COLOR_COUNT), seed = index * 7 + Random.nextInt(50)).also {
                offset += style.length + JAM_GAP
            }
        }
        totalLength = offset - JAM_GAP
        lift = 0f
        hopTime = HOP_SECONDS
        time = 0f
        // Nothing in the way of the cars
        world.fences.removeAll { it.x + it.width > x }
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardingHop.start(x, world)
        carIndex = 0
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        groundSpeed = world.speed
        time += dt
        hopTime += dt
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> {
                x -= scroll
                // Not lassoed: the horse hops up on its own once it reaches the first car
                if (x < HOOVES_AT + HOP_LEAD) board(world)
            }
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, BOARD_SECONDS)
                // Lassoed from afar the jam is pulled in; otherwise it just stands there
                val target = HOOVES_AT - 3f
                x = if (boardingHop.startX > target) boardingHop.slideX(target, progress) else x - scroll
                lift = boardingHop.lift(cars.first().style.roof, progress, BOARD_HOP)
                if (progress >= 1f) {
                    enter(VehiclePhase.RIDING)
                    world.addBonusPoints(CAR_POINTS)
                }
            }
            VehiclePhase.RIDING -> {
                x -= scroll
                gallopOverRoofs(world)
            }
            VehiclePhase.UNLOADING -> {
                x -= scroll
                val progress = progressOf(phaseTime, UNLOAD_SECONDS)
                lift = hopArc(cars.last().style.roof, 0f, progress, UNLOAD_HOP)
                if (progress >= 1f) {
                    lift = 0f
                    enter(VehiclePhase.LEAVING)
                    world.resumeFences()
                }
            }
            VehiclePhase.LEAVING -> {
                x -= scroll
                if (x + totalLength < 0f) enter(VehiclePhase.IDLE)
            }
        }
    }

    /** Hops onto the next roof as it comes, and down behind the last car. */
    private fun gallopOverRoofs(world: RodeoWorld) {
        val hooves = HOOVES_AT - x
        val next = cars.getOrNull(carIndex + 1)
        if (next != null && hooves > next.offset - HOP_LEAD) {
            hopFrom = cars[carIndex].style.roof
            carIndex++
            hopTime = 0f
            world.addBonusPoints(CAR_POINTS)
            world.sparkle(x + next.offset + next.style.length / 2f, next.style.roof + 4f)
        }
        val roof = cars[carIndex].style.roof
        lift = if (hopTime < HOP_SECONDS) hopArc(hopFrom, roof, progressOf(hopTime, HOP_SECONDS), HOP_HEIGHT) else roof
        val last = cars.last()
        if (next == null && hooves > last.offset + last.style.length - 2f) enter(VehiclePhase.UNLOADING)
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        val rear = cars.first().style
        return RodeoTrafficJamUi(
            x = x,
            cars = cars,
            time = time,
            lassoHint = lassoHint(x + rear.length / 2f, rear.roof + 6f),
        )
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoMap
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.balloon.RodeoBalloon
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.bull.RodeoBull
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cablecar.RodeoCableCar
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.carriage.RodeoGoldenCarriage
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.candybus.RodeoCandyBus
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cow.RodeoCow
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.drill.RodeoDrill
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pirateship.RodeoPirateShip
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.trafficjam.RodeoTrafficJam
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.flamingo.RodeoFlamingo
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.fordescort.RodeoFordEscort
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.milktruck.RodeoMilkTruck
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.minecart.RodeoMineCart
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.oiltanker.RodeoOilTanker
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.plane.RodeoPlane
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pocketbike.RodeoPocketBike
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rocket.RodeoRocket
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rowboat.RodeoRowboat
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.shoppingcart.RodeoShoppingCart
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.tractor.RodeoLawnTractor
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.train.RodeoTrain
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.uboat.RodeoUBoat

/**
 * All vehicles of the game and which one is in the picture - there is never more than one. Sends
 * them along via [RodeoVehicleRotation] and notices when the rider gets on and off.
 */
internal class RodeoTraffic {

    private val rocket = RodeoRocket()
    /** Bought with snails like the rocket. */
    private val carriage = RodeoGoldenCarriage()
    /** Only comes with its landscape, before a mountain. */
    private val cableCar = RodeoCableCar()

    /** The regular vehicles of each map - register new ones here. */
    private val surfaceVehicles: List<RodeoVehicle> = listOf(
        RodeoPlane(), RodeoTrain(), RodeoMilkTruck(), RodeoFordEscort(), RodeoBull(), RodeoLawnTractor(),
        RodeoCandyBus(), RodeoPocketBike(), RodeoShoppingCart(), RodeoBalloon(), RodeoCow(), RodeoTrafficJam(),
    )
    private val seaVehicles: List<RodeoVehicle> = listOf(RodeoRowboat(), RodeoUBoat(), RodeoOilTanker(), RodeoFlamingo(), RodeoPirateShip())
    private val mineVehicles: List<RodeoVehicle> = listOf(RodeoMineCart(), RodeoDrill())

    /** Every vehicle of the game. */
    private val all: List<RodeoVehicle> = surfaceVehicles + seaVehicles + mineVehicles + rocket + carriage + cableCar

    /**
     * Which vehicles come along on which map; the rocket is bought with snails and the cable car
     * comes with its mountain, so neither is sent along. The short visits to the other maps get their
     * vehicles sooner.
     */
    private val rotations: Map<RodeoMap, RodeoVehicleRotation> = mapOf(
        RodeoMap.SURFACE to RodeoVehicleRotation(surfaceVehicles),
        RodeoMap.SEA to RodeoVehicleRotation(seaVehicles, firstSeconds = 4f, intervalMin = 9f, intervalRandom = 6f),
        RodeoMap.MINE to RodeoVehicleRotation(mineVehicles, firstSeconds = 6f, intervalMin = 12f, intervalRandom = 6f),
    )
    /** The map of the last frame; a new map restarts its rotation's timer. */
    private var lastMap = RodeoMap.SURFACE

    /** The vehicle in the picture, if any. */
    var current: RodeoVehicle? = null
        private set

    /** Carried the rider in the last frame; detects getting on and off. */
    private var wasCarrying = false

    /** The vehicle currently carrying the rider (or getting him on / off). */
    val ride: RodeoVehicle? get() = current?.takeIf { it.carriesRider }

    val isClear: Boolean get() = current == null

    fun reset() {
        all.forEach { it.reset() }
        rotations.values.forEach { it.reset() }
        lastMap = RodeoMap.SURFACE
        current = null
        wasCarrying = false
    }

    /** Sends the rocket in; it picks up horse and rider right away. */
    fun launchRocket(world: RodeoWorld) {
        current = rocket
        rocket.spawn(world)
    }

    /** A vehicle in the picture keeps the ground flat (see RodeoVehicle.needsFlatTrack). */
    val needsFlatTrack: Boolean get() = current?.needsFlatTrack == true

    /** The map [kind] comes along on (the surface for the rocket, carriage and cable car). */
    fun mapOf(kind: RodeoVehicleKind): RodeoMap =
        rotations.entries.firstOrNull { (_, rotation) -> rotation.has(kind) }?.key ?: RodeoMap.SURFACE

    /**
     * Test mode (see RodeoTest): sends the vehicle of [kind] in right now, if the track is clear. The
     * rocket and the carriage pick the rider up at once; the cable car needs its mountain first.
     */
    fun sendTest(kind: RodeoVehicleKind, world: RodeoWorld): Boolean {
        if (!isClear) return false
        val vehicle = all.first { it.kind == kind }
        current = vehicle
        vehicle.spawn(world)
        return true
    }

    /** Sends the golden carriage in; it picks up horse and rider right away. */
    fun sendCarriage(world: RodeoWorld) {
        current = carriage
        carriage.spawn(world)
    }

    /** Sends the cable car in along with a mountain; false if another vehicle is still around. */
    fun sendCableCar(world: RodeoWorld): Boolean = sendNow(cableCar, world)



    private fun sendNow(vehicle: RodeoVehicle, world: RodeoWorld): Boolean {
        if (!isClear) return false
        current = vehicle
        vehicle.spawn(world)
        return true
    }

    /**
     * Sends the next vehicle of [map] once it is due (and [maySend] allows it), and moves the
     * current one.
     */
    fun step(world: RodeoWorld, dt: Float, scroll: Float, maySend: Boolean, map: RodeoMap, flatTrack: Boolean) {
        if (map != lastMap) {
            lastMap = map
            rotations[map]?.restartTimer()
        }
        if (current == null && maySend) {
            rotations[map]?.tick(dt, flatTrack)?.let { next ->
                current = next
                next.spawn(world)
            }
        }
        current?.step(world, dt, scroll)
    }

    /**
     * Call once per frame after everything that can board a vehicle (the lasso): reports getting on
     * and off, then lets go of a vehicle that left the picture.
     */
    fun checkRider(onBoarded: (RodeoVehicleKind) -> Unit, onLeft: () -> Unit) {
        val vehicle = current
        val carrying = vehicle?.carriesRider == true
        if (carrying && !wasCarrying) onBoarded(vehicle.kind)
        if (!carrying && wasCarrying) onLeft()
        wasCarrying = carrying
        if (vehicle?.isActive == false) current = null
    }

    fun releaseJump() = all.forEach { it.onJump(false) }

    fun releaseDive() = all.forEach { it.onDive(false) }
}

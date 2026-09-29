package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.bear.RodeoBear
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.bull.RodeoBull
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cablecar.RodeoCableCar
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.candybus.RodeoCandyBus
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.carwreck.RodeoCarWreck
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.milktruck.RodeoMilkTruck
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.plane.RodeoPlane
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rocket.RodeoRocket
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.tractor.RodeoLawnTractor
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.train.RodeoTrain

/**
 * All vehicles of the game and which one is in the picture - there is never more than one. Sends
 * them along via [RodeoVehicleRotation] and notices when the rider gets on and off.
 */
internal class RodeoTraffic {

    private val rocket = RodeoRocket()
    /** Only come with their landscape: the cable car before a mountain, the bear in a forest. */
    private val cableCar = RodeoCableCar()
    private val bear = RodeoBear()

    /** Every vehicle of the game - register new ones here. */
    private val all: List<RodeoVehicle> =
        listOf(RodeoPlane(), RodeoTrain(), RodeoMilkTruck(), RodeoCarWreck(), RodeoBull(), RodeoLawnTractor(), RodeoCandyBus(), rocket, cableCar, bear)

    /** The rocket is bought with snails and the landscape brings its own; never sent along. */
    private val rotation = RodeoVehicleRotation(all - rocket - cableCar - bear)

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
        rotation.reset()
        current = null
        wasCarrying = false
    }

    /** Sends the rocket in; it picks up horse and rider right away. */
    fun launchRocket(world: RodeoWorld) {
        current = rocket
        rocket.spawn(world)
    }

    /** Sends the cable car in along with a mountain; false if another vehicle is still around. */
    fun sendCableCar(world: RodeoWorld): Boolean = sendNow(cableCar, world)

    /** Sends a bear in along with a forest; false if another vehicle is still around. */
    fun sendBear(world: RodeoWorld): Boolean = sendNow(bear, world)


    private fun sendNow(vehicle: RodeoVehicle, world: RodeoWorld): Boolean {
        if (!isClear) return false
        current = vehicle
        vehicle.spawn(world)
        return true
    }

    /** Sends the next vehicle once it is due (and [maySend] allows it), and moves the current one. */
    fun step(world: RodeoWorld, dt: Float, scroll: Float, maySend: Boolean) {
        if (current == null && maySend) {
            rotation.tick(dt)?.let { next ->
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

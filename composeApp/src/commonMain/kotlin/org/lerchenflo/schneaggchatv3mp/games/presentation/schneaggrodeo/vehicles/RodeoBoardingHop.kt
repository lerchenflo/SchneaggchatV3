package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.hopArc
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep

/**
 * The horse's hop from the track onto a vehicle: remembers where the vehicle and the hooves were
 * when it began, so the vehicle can slide under the horse while the horse hops up in an arc.
 */
internal class RodeoBoardingHop {
    /** Where the vehicle was when the hop began. */
    var startX = 0f
        private set
    private var startHeight = 0f

    /** Begins the hop; a jump in progress ends on the vehicle instead of the ground. */
    fun start(vehicleX: Float, world: RodeoWorld) {
        startX = vehicleX
        startHeight = world.takeHorseOffTheGround()
    }

    /** Where the vehicle is at [progress] (0..1), sliding from where it was to [toX]. */
    fun slideX(toX: Float, progress: Float): Float = lerp(startX, toX, smoothstep(progress))

    /** Height of the hooves at [progress] (0..1), up onto [deck] in an arc [hop] high. */
    fun lift(deck: Float, progress: Float, hop: Float): Float = hopArc(startHeight, deck, progress, hop)
}

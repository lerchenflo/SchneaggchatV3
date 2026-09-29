package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.hopArc
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep

/**
 * A vehicle the horse hops onto and rides standing on its deck (train roof, truck box, tractor
 * deck). This base does the whole choreography: it rolls by, slides under the horse while the horse
 * hops up onto [deckHeight], rides for [rideSeconds] ([ride] is the vehicle's own part), the horse
 * hops back down and the vehicle leaves ([leave]).
 */
internal abstract class RodeoDeckVehicle(kind: RodeoVehicleKind) : RodeoVehicle(kind) {

    /** Height of the deck the horse's hooves stand on. */
    protected abstract val deckHeight: Float
    /** Where [x] is while riding, so the horse stands on the deck. */
    protected abstract val rideX: Float
    protected abstract val rideSeconds: Float
    protected open val boardSeconds: Float = 0.6f
    protected open val unloadSeconds: Float = 0.6f
    /** Height of the hop onto and off the deck. */
    protected open val hop: Float = 6f
    /** How fast the wheels turn per unit of ground scrolled by. */
    protected open val wheelTurn: Float = 0.3f

    /** Current height of the horse's hooves while on board. */
    private var lift = 0f
    private var boardStartX = 0f
    private var boardStartHeight = 0f
    /** Wheel rotation in radians, for drawing. */
    protected var wheelPhase = 0f
        private set

    override val horseLift: Float get() = if (carriesRider) lift else 0f

    /** The ride is over; [ride] should call [getOff]. */
    protected val rideIsOver: Boolean get() = phaseTime >= rideSeconds

    override fun reset() {
        super.reset()
        lift = 0f
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        // A jump in progress ends on the deck instead of the ground
        boardStartHeight = world.takeHorseOffTheGround()
    }

    final override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        wheelPhase += scroll * wheelTurn
        updateAlways(world, dt, scroll)
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> passBy(dt)
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, boardSeconds)
                x = lerp(boardStartX, rideX, smoothstep(progress))
                lift = hopArc(boardStartHeight, deckHeight, progress, hop)
                whileBoarding(world)
                if (progress >= 1f) {
                    enter(VehiclePhase.RIDING)
                    lift = deckHeight
                    onRideStart()
                }
            }
            VehiclePhase.RIDING -> ride(world, dt)
            VehiclePhase.UNLOADING -> {
                val progress = progressOf(phaseTime, unloadSeconds)
                lift = hopArc(deckHeight, 0f, progress, hop)
                whileUnloading(dt, scroll)
                if (progress >= 1f) {
                    lift = 0f
                    enter(VehiclePhase.LEAVING)
                }
            }
            VehiclePhase.LEAVING -> leave(world, dt, scroll)
        }
    }

    /** Ends the ride: the horse hops off, the regular fences come back. */
    protected fun getOff(world: RodeoWorld) {
        enter(VehiclePhase.UNLOADING)
        world.resumeFences()
    }

    /** Every frame in any phase, before the phase itself (particles, debris, ...). */
    protected open fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) = Unit

    /** Every frame while it slides under the horse. */
    protected open fun whileBoarding(world: RodeoWorld) = Unit

    /** The horse stands on the deck, the ride begins. */
    protected open fun onRideStart() = Unit

    /** Every frame of the ride; ends it with [getOff] (usually once [rideIsOver]). */
    protected abstract fun ride(world: RodeoWorld, dt: Float)

    /** Every frame while the horse hops off. */
    protected open fun whileUnloading(dt: Float, scroll: Float) = Unit

    /** Every frame after the horse is off; ends with enter(IDLE) once everything left the picture. */
    protected abstract fun leave(world: RodeoWorld, dt: Float, scroll: Float)
}

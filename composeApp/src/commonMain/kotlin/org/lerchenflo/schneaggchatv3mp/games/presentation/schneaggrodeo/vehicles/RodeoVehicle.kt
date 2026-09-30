package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HAND_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.KMH_PER_UNIT_PER_SECOND
import kotlin.math.roundToInt

// Every vehicle follows the same life cycle (VehiclePhase): it passes by with a lasso hint, the
// lasso catches it, horse and / or cowboy board it, ride it for a while and get back on the track,
// then it leaves the picture. Each vehicle lives in its own package: the logic in Rodeo<Name>.kt,
// its render model and drawing in Rodeo<Name>Drawing.kt.
//
// Adding a vehicle:
//  1. A RodeoVehicleKind entry (banner name, ride hint).
//  2. A RodeoVehicle subclass - RodeoDeckVehicle already does all the hopping on and off for
//     anything the horse stands on; RodeoSteering flies like the plane.
//  3. A RodeoVehicleUi data class that draws itself.
//  4. Register it in RodeoTraffic.

/** Where a lasso thrown at a passing vehicle aims, and how far ahead of it (seconds of its pass speed). */
private const val LASSO_AIM_X = HORSE_X + HAND_X + 16f
private const val LASSO_LEAD_SECONDS = 0.225f

/** Something the lasso can catch other than a snail: a vehicle passing by or a flying milk can. */
internal interface LassoGrab {
    /** Where the loop flies to right now. */
    val x: Float
    val y: Float
    /** The loop arrived at [x] within reach; returns true if it caught. */
    fun catch(): Boolean
    /** A caught thing dangles from the loop on its way back. */
    fun follow(tipX: Float, tipY: Float) = Unit
    /** The loop is back at the hand. */
    fun release() = Unit
}

/** How the horse and rider stand while riding; see [RodeoVehicle.ridePose]. */
internal data class RodeoRidePose(
    val pitch: Float = 0f,
    val hatLift: Float = 0f,
    /** 0..1: horse crouched and rider flat on its neck. */
    val duck: Float = 0f,
    /** Floating down under a parachute. */
    val parachute: Boolean = false,
)

internal enum class VehiclePhase {
    /** Not in the picture. */
    IDLE,
    /** Passing by, waiting for the lasso. */
    APPROACH,
    BOARDING,
    RIDING,
    /** Getting off again, back onto the track. */
    UNLOADING,
    /** The rider is back on the horse, the vehicle (or its wreck) still leaves the picture. */
    LEAVING,
}

internal abstract class RodeoVehicle(val kind: RodeoVehicleKind) {

    /** Only joins the rotation once every regular vehicle came by at least once. */
    open val special: Boolean = false

    /** Too fast for the hills: only comes on a flat stretch, and the ground stays flat while it is around. */
    open val needsFlatTrack: Boolean = false

    var phase = VehiclePhase.IDLE
        private set
    /** Seconds in the current [phase]. */
    protected var phaseTime = 0f
        private set

    /** Horizontal position: the vehicle's left (rear) end. */
    protected var x = 0f

    /** Overall length; it is out of the picture once [x] is this far past the left edge. */
    protected abstract val length: Float

    /** Screen speed (u/s, to the left) while passing by; used to aim the lasso ahead. */
    protected abstract val passSpeed: Float

    /** Where the lasso grabs it while it passes by. */
    protected abstract fun hitch(): Pair<Float, Float>

    /**
     * Where along the vehicle the lasso aims while it passes by: just ahead of the hand, where the
     * loop meets it, but always on the vehicle ([inset] from its ends).
     */
    protected fun hitchX(inset: Float = 2f): Float =
        (LASSO_AIM_X + passSpeed * LASSO_LEAD_SECONDS).coerceIn(x + inset, x + length - inset)

    /** Shows up at the right edge, passing by. */
    open fun spawn(world: RodeoWorld) {
        enter(VehiclePhase.APPROACH)
        x = world.worldWidth + 5f
    }

    /** Advances everything of this vehicle by [dt]; [scroll] is how far the ground moved this frame. */
    fun step(world: RodeoWorld, dt: Float, scroll: Float) {
        phaseTime += dt
        update(world, dt, scroll)
    }

    /** The vehicle's own [step]; [phaseTime] already counts this frame. */
    protected abstract fun update(world: RodeoWorld, dt: Float, scroll: Float)

    /** Render model, or null while it is not in the picture. */
    abstract fun ui(): RodeoVehicleUi?

    open fun reset() {
        enter(VehiclePhase.IDLE)
    }

    /** Anything of it is in the picture; no other vehicle comes along meanwhile. */
    val isActive: Boolean get() = phase != VehiclePhase.IDLE

    /** Horse and / or cowboy are on it (or getting on / off). */
    val carriesRider: Boolean
        get() = phase == VehiclePhase.BOARDING || phase == VehiclePhase.RIDING || phase == VehiclePhase.UNLOADING

    val isRiding: Boolean get() = phase == VehiclePhase.RIDING

    // --- How it affects horse, rider and world. The defaults fit a vehicle the horse stands on.

    /** The horse stands on it (as opposed to the plane, which only takes the cowboy). */
    open val carriesHorse: Boolean = true

    /** Height of the horse's hooves on the vehicle. */
    open val horseLift: Float = 0f

    /** Hopping on or off: the horse is drawn in its jumping pose. */
    open val horseHops: Boolean
        get() = phase == VehiclePhase.BOARDING || phase == VehiclePhase.UNLOADING

    /** The horse runs on without its rider (plane); it hops the fences on its own. */
    open val horseRunsRiderless: Boolean = false

    /** The cowboy sits on the horse (false while he is up in the plane). */
    open val riderOnHorse: Boolean = true

    /** The lasso stays usable while riding (for targets the vehicle offers). */
    open val allowsLasso: Boolean = false

    /** No regular fences spawn meanwhile; the vehicle places its own obstacles. */
    open val blocksFences: Boolean get() = carriesRider

    /** Rider leans forward into the wind. */
    open val riderLeans: Boolean = false

    /** The whole picture rattles and the ground smears into streaks. */
    open val shaking: Boolean = false

    /** Horse and rider are inside the vehicle and not drawn on their own. */
    open val hidesHorse: Boolean = false

    /** How far the view is panned up, or null to follow the horse as usual. */
    open fun camera(): Float? = null

    /** 0..1: how far the sky has turned into space. */
    open fun space(): Float = 0f

    /** While riding, the world scrolls this many times as fast as the horse's own pace (null: as fast). */
    protected open val rideSpeedFactor: Float? = null

    /** How far the world scrolls this frame; [step] is how far the horse's own pace carries it. */
    open fun worldScroll(world: RodeoWorld, step: Float, dt: Float): Float {
        val factor = rideSpeedFactor
        return if (isRiding && factor != null) step * factor else step
    }

    /** Shown on the speedometer while riding, or null for the horse's pace. */
    open fun speedKmh(world: RodeoWorld): Int? {
        val factor = rideSpeedFactor
        return if (isRiding && factor != null) (world.speed * factor * KMH_PER_UNIT_PER_SECOND).roundToInt() else null
    }

    open fun ridePose(runTimeSeconds: Float): RodeoRidePose? = null

    /** The cowboy while he is off the horse because of this vehicle, if he is drawn on his own. */
    open fun cowboy(world: RodeoWorld): RodeoCowboyUi? = null

    /** Jump input while riding; the jump itself is disabled meanwhile. Released on any vehicle. */
    open fun onJump(pressed: Boolean) = Unit

    /** Dive input (lower half / arrow down) while riding. Released on any vehicle. */
    open fun onDive(pressed: Boolean) = Unit

    // --- Lasso

    /**
     * The lasso button while riding, before any rope is thrown: returns true if the vehicle does
     * something of its own with it (see RodeoVehicleKind.lassoLabel).
     */
    open fun onLassoButton(world: RodeoWorld): Boolean = false

    /** A lasso thrown while riding: targets of the vehicle's own, or null for none in reach. */
    protected open fun rideLassoGrab(world: RodeoWorld, handX: Float, reach: ClosedFloatingPointRange<Float>, timeToCatch: Float): LassoGrab? = null

    /** Called once the lasso caught it. */
    protected open fun board(world: RodeoWorld) {
        enter(VehiclePhase.BOARDING)
    }

    /**
     * What a lasso thrown now would grab: the vehicle itself while it passes by (aimed where it will
     * be when the loop arrives), or a target of its own while riding.
     */
    fun lassoGrab(world: RodeoWorld, handX: Float, reach: ClosedFloatingPointRange<Float>, timeToCatch: Float): LassoGrab? {
        if (phase == VehiclePhase.RIDING) return rideLassoGrab(world, handX, reach, timeToCatch)
        if (phase != VehiclePhase.APPROACH) return null
        if (hitch().first - passSpeed * timeToCatch !in reach) return null
        return object : LassoGrab {
            override val x: Float get() = hitch().first
            override val y: Float get() = hitch().second
            override fun catch(): Boolean {
                if (phase != VehiclePhase.APPROACH) return false
                board(world)
                return true
            }
        }
    }

    // --- Helpers for subclasses

    /** The "lasso it" hint above it at [hintX] / [hintY] while it passes by. */
    protected fun lassoHint(hintX: Float, hintY: Float): RodeoLassoHintUi? =
        if (phase == VehiclePhase.APPROACH) RodeoLassoHintUi(hintX, hintY) else null

    /** Rolls by at [passSpeed]; gone once it left the picture on the left without being caught. */
    protected fun passBy(dt: Float) {
        x -= passSpeed * dt
        if (x + length < 0f) enter(VehiclePhase.IDLE)
    }

    protected fun enter(newPhase: VehiclePhase) {
        phase = newPhase
        phaseTime = 0f
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.helicopter

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.GROUND_OFFSET_UNITS
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoMap
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.WORLD_HEIGHT_UNITS
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoSteering
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import kotlin.math.max
import kotlin.math.sin

// Helicopter: hovers by with a sling hanging from its hook. Lasso the hook and the sling closes
// round the horse's belly: horse and rider dangle under it. Hold to climb, let go to sink. Reach
// the rainbow up high before the ride is over and the run goes on up there (see RodeoMapSwitch);
// otherwise it sets the horse back down on the track and flies off.

// Shape, shared with the drawing (grid: x from the tail's end, y up from the skids)
internal const val HELI_LENGTH = 27f
internal const val HELI_HEIGHT = 11f
/** Middle of the cabin, where the hook hangs. */
internal const val HELI_CABIN_X = 19f
/** The rope from the belly down to the hook while it hovers by. */
internal const val HELI_ROPE = 18f
/** The skids above the horse's hooves while it carries the horse: room for the rider and the ropes. */
internal const val HELI_ABOVE_HOOVES = 38f
/** Height of the sling round the horse's belly, above its hooves. */
internal const val HELI_SLING_Y = 9f
/** Hooves this high reach the rainbow; the drawing shows it up there. */
internal const val RAINBOW_LIFT = 55f

private const val PASS_SPEED = 20f
private const val APPROACH_Y = 36f
/** The cabin hovers over the horse's middle. */
private const val FLY_X = HORSE_X + 15f - HELI_CABIN_X
private const val BOARD_SECONDS = 0.8f
private const val BOARD_LIFT = 3f
private const val RIDE_SECONDS = 8f
private const val LAND_SPEED = 18f
private const val LEAVE_CLIMB_SPEED = 22f
private const val LEAVE_FLY_SPEED = 14f
/** The view pans up so the horse and the helicopter above it stay in the picture. */
private const val CAMERA_TOP = HELI_ABOVE_HOOVES + HELI_HEIGHT + 1f
private const val VISIBLE_TOP = WORLD_HEIGHT_UNITS - GROUND_OFFSET_UNITS - 2f
/** How fast the rotor turns (radians per second). */
private const val ROTOR_SPEED = 30f

internal class RodeoHelicopter : RodeoVehicle(RodeoVehicleKind.HELICOPTER) {

    private val steering = RodeoSteering(
        climbAccel = 40f,
        diveAccel = 0f,
        sinkAccel = 18f,
        maxClimb = 15f,
        maxSink = 10f,
    )
    /** Skids above the ground while it hovers by or flies off. */
    private var y = 0f
    /** Hooves above the ground while it carries the horse. */
    private var lift = 0f
    private var boardStartX = 0f
    private var boardStartY = 0f
    private var rotor = 0f
    /** Reached the rainbow: the run goes on up there once the fade is dark. */
    private var arrived = false

    override val length = HELI_LENGTH
    override val passSpeed = PASS_SPEED
    override val needsFlatTrack = true
    override val horseHops = false
    override val horseLift: Float get() = if (carriesRider) lift else 0f

    /** The hook under the cabin. */
    override fun hitch() = (x + HELI_CABIN_X) to (y - HELI_ROPE)

    override fun camera(): Float? =
        if (carriesRider) max(0f, lift + CAMERA_TOP - VISIBLE_TOP) else null

    override fun ridePose(runTimeSeconds: Float) = RodeoRidePose(pitch = 3f * sin(runTimeSeconds * 2f))

    override fun reset() {
        super.reset()
        steering.release()
    }

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        y = APPROACH_Y
        lift = 0f
        arrived = false
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        boardStartY = y
        lift = 0f
        steering.stop()
        steering.release()
    }

    override fun onJump(pressed: Boolean) {
        steering.climb = (pressed && phase == VehiclePhase.RIDING) || arrived
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        rotor += ROTOR_SPEED * dt
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> passBy(dt)
            VehiclePhase.BOARDING -> {
                val progress = smoothstep(progressOf(phaseTime, BOARD_SECONDS))
                x = lerp(boardStartX, FLY_X, progress)
                lift = BOARD_LIFT * progress
                y = lerp(boardStartY, lift + HELI_ABOVE_HOOVES, progress)
                if (phaseTime >= BOARD_SECONDS) enter(VehiclePhase.RIDING)
            }
            VehiclePhase.RIDING -> fly(world, dt)
            VehiclePhase.UNLOADING -> {
                // Sets the horse down gently
                lift = max(0f, lift - LAND_SPEED * dt)
                y = lift + HELI_ABOVE_HOOVES
                if (lift <= 0f) {
                    enter(VehiclePhase.LEAVING)
                    world.splash()
                    world.resumeFences()
                }
            }
            VehiclePhase.LEAVING -> {
                // Up and away, with the empty sling swinging
                y += LEAVE_CLIMB_SPEED * dt
                x += LEAVE_FLY_SPEED * dt
                if (y > WORLD_HEIGHT_UNITS + 20f || x > world.worldWidth) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun fly(world: RodeoWorld, dt: Float) {
        lift = max(BOARD_LIFT, steering.fly(lift, dt, ceiling = RAINBOW_LIFT + 10f))
        y = lift + HELI_ABOVE_HOOVES
        if (!arrived && lift >= RAINBOW_LIFT && world.map == RodeoMap.SURFACE) {
            // Up on the rainbow: it keeps climbing until the fade is dark and the ride ends there
            arrived = true
            steering.climb = true
            world.sparkle(HORSE_X + 15f, lift)
            world.travelTo(RodeoMap.RAINBOW)
        }
        if (!arrived && phaseTime >= RIDE_SECONDS) {
            enter(VehiclePhase.UNLOADING)
            steering.release()
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoHelicopterUi(
            x = x,
            y = y,
            rotor = rotor,
            carrying = carriesRider,
            slingY = if (carriesRider) lift + HELI_SLING_Y else null,
            // The rainbow up high shows where to fly while the horse dangles under it
            rainbowAt = if (phase == VehiclePhase.BOARDING || phase == VehiclePhase.RIDING) RAINBOW_LIFT else null,
            lassoHint = lassoHint(x + HELI_CABIN_X, y + HELI_HEIGHT + 4f),
        )
    }
}

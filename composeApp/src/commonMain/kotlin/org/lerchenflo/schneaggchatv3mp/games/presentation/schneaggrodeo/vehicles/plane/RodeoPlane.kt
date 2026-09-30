package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.plane

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.OnTrack
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.scrollAlong
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_LEG_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.Horseshoe
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoSteering
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Plane: an airfield comes along the track with the plane rolling down its runway, taking off. Lasso
// it while it rolls and the cowboy jumps into the cockpit and lifts off, then flies over a city
// skyline until he crashes into a building or the ground (or the tank runs dry), then drops back into
// the saddle. The riderless horse gallops on below, hopping the fences.

// Shape, shared with the drawing (grid: x from the plane's left edge, y up from the fuselage underside)
internal const val PLANE_LENGTH = 22f
/** Landing gear: the wheels hang this far below the fuselage while it rolls. */
internal const val PLANE_GEAR_HEIGHT = 2.5f
/** The airfield: runway length, the plane starts [AIRPORT_PLANE_START] into it. */
internal const val AIRPORT_LENGTH = 110f
private const val AIRPORT_PLANE_START = 20f
internal const val PLANE_PILOT_X = 11f               // cowboy's feet in the cockpit
private const val PLANE_PILOT_Y = 1f
/** Storm clouds are a bit wider than the building below. */
internal const val CLOUD_OVERHANG = 3f

private const val PLANE_PASS_SPEED = 16f             // u/s across the screen while rolling down the runway
private const val PLANE_HITCH_Y = 5f
/** Not caught by here: it lifts off without the cowboy. */
private const val PLANE_LIFTOFF_X = HORSE_X - 6f
private const val PLANE_LIFTOFF_CLIMB = 14f
private const val PLANE_BOARD_SECONDS = 1.4f
private const val PLANE_TAKEOFF_HEIGHT = 20f
private const val PLANE_BOARD_HOP = 8f
/** The tank runs dry after this long: the cowboy jumps back into the saddle, the plane flies off. */
private const val PLANE_FLIGHT_SECONDS = 16f
private const val PLANE_LEAVE_SPEED = 25f
/** Nose up while climbing, down while diving: degrees per u/s of vertical speed, and the limit. */
private const val PLANE_PITCH_PER_SPEED = 0.7f
private const val PLANE_MAX_PITCH = 28f
private const val PLANE_PITCH_RESPONSE = 8f
private const val PLANE_TAKEOFF_PITCH = -14f
private const val PLANE_FLY_X = 16f                  // left edge of the plane while flying (HORSE_X + 4)
private const val PLANE_MAX_HEIGHT = 55f             // keeps the plane inside the canvas
private const val PLANE_HIT_LEFT = 2f                // hitbox relative to the plane's left edge / underside
private const val PLANE_HIT_RIGHT = 20f
private const val PLANE_HIT_TOP = 7f
private const val PLANE_DROP_SECONDS = 0.8f
private const val PLANE_DROP_HOP = 8f
private const val PLANE_WRECK_FALL_SPEED = 30f
private const val PLANE_WRECK_SPIN = 220f            // degrees per second
private const val BUILDING_MIN_HEIGHT = 12f
private const val BUILDING_MAX_HEIGHT = 32f
private const val BUILDING_MIN_WIDTH = 14f
private const val BUILDING_MAX_WIDTH = 22f
private const val BUILDING_POINTS = 5                // per building flown past
private const val FLIGHT_HORSESHOE_CHANCE = 0.6f
// Storm clouds hang from the sky above the buildings, leaving a passage over the roof that gets
// narrower and more frequent the longer the flight lasts
private const val CLOUD_START_CHANCE = 0.2f
private const val CLOUD_CHANCE_PER_SECOND = 0.01f
private const val CLOUD_MAX_CHANCE = 0.4f
private const val CLOUD_START_GAP = 32f              // roof to cloud at the start of a flight
private const val CLOUD_MIN_GAP = 22f                // never narrower than this (plane is 7 high)
private const val CLOUD_GAP_SHRINK_PER_SECOND = 0.4f
private const val CLOUD_GAP_RANDOM = 5f

/** A building of the skyline under the plane; [x] is its left edge. */
private class Building(
    override var x: Float,
    val width: Float,
    val height: Float,
    val seed: Int,
    /** Underside of the storm cloud hanging above this building, or null for open sky. */
    val cloudBottom: Float?,
) : OnTrack {
    var passed = false
}

internal class RodeoPlane : RodeoVehicle(RodeoVehicleKind.PLANE) {

    private val buildings = mutableListOf<Building>()
    private val steering = RodeoSteering(
        climbAccel = 150f,
        diveAccel = 170f,
        sinkAccel = 45f, // it slowly goes down without input
        maxClimb = 35f,
        maxSink = 45f,
    )
    private var y = 0f          // underside of the fuselage
    private var rotation = 0f   // degrees clockwise: nose down while diving, tumbling as a wreck
    private var boardStartX = 0f
    private var propellerPhase = 0f
    private var nextBuildingIn = 0f
    private var dropStartX = 0f
    private var dropStartY = 0f
    /** Left end of the airfield's runway; scrolls with the ground. */
    private var airportX = 0f
    /** The flight ended because the tank ran dry, not in a crash: the plane flies off in one piece. */
    private var outOfFuel = false

    override val length = PLANE_LENGTH
    // The skyline and its storm clouds are laid out on the flat
    override val needsFlatTrack = true
    override val passSpeed = PLANE_PASS_SPEED
    override val carriesHorse = false
    override val horseHops = false

    override val horseRunsRiderless: Boolean
        get() = phase == VehiclePhase.RIDING || phase == VehiclePhase.UNLOADING

    override val riderOnHorse: Boolean get() = !carriesRider

    /** The whole plane on the runway is a target. */
    override fun hitch() = hitchX() to y + PLANE_HITCH_Y

    /** No fences on the runway ahead. */
    override val blocksFences: Boolean
        get() = carriesRider || (phase == VehiclePhase.APPROACH && airportX + AIRPORT_LENGTH > HORSE_X)

    private val airportVisible: Boolean get() = airportX + AIRPORT_LENGTH > -5f

    override fun reset() {
        super.reset()
        buildings.clear()
        steering.release()
    }

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        airportX = world.worldWidth
        x = airportX + AIRPORT_PLANE_START
        y = PLANE_GEAR_HEIGHT
        rotation = 0f
        outOfFuel = false
        // Clear the runway of what is already lying there
        world.fences.removeAll { it.x + it.width > airportX }
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        steering.stop()
        steering.release()
        // Buildings start right of the screen; the fences on screen scroll away under the horse
        nextBuildingIn = 0f
    }

    override fun onJump(pressed: Boolean) {
        steering.climb = pressed && phase == VehiclePhase.RIDING
    }

    override fun onDive(pressed: Boolean) {
        steering.dive = pressed && phase == VehiclePhase.RIDING
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        propellerPhase += dt * 40f

        // The skyline and the airfield scroll with the ground and outlive the flight until they leave the screen
        airportX -= scroll
        buildings.scrollAlong(scroll) { it.x + it.width < 0f }

        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> {
                // Rolling down the runway; missed, it lifts off without the cowboy
                x -= passSpeed * dt
                if (x < PLANE_LIFTOFF_X) y += PLANE_LIFTOFF_CLIMB * dt
                if (x + length < 0f) enter(VehiclePhase.LEAVING)
            }
            VehiclePhase.BOARDING -> {
                // Take-off with the cowboy aboard
                val progress = progressOf(phaseTime, PLANE_BOARD_SECONDS)
                x = lerp(boardStartX, PLANE_FLY_X, smoothstep(progress))
                y = lerp(PLANE_GEAR_HEIGHT, PLANE_TAKEOFF_HEIGHT, smoothstep(progress))
                // Nose up for the take-off, level again at the top
                rotation = PLANE_TAKEOFF_PITCH * sin(PI.toFloat() * progress)
                if (progress >= 1f) enter(VehiclePhase.RIDING)
            }
            VehiclePhase.RIDING -> {
                fly(world, dt, scroll)
                // Turns its nose up while climbing and down while diving (clockwise is nose down)
                val pitch = (-steering.vy * PLANE_PITCH_PER_SPEED).coerceIn(-PLANE_MAX_PITCH, PLANE_MAX_PITCH)
                rotation += (pitch - rotation) * min(1f, dt * PLANE_PITCH_RESPONSE)
                if (phase == VehiclePhase.RIDING && phaseTime >= PLANE_FLIGHT_SECONDS) {
                    outOfFuel = true
                    crash(world)
                }
            }
            VehiclePhase.UNLOADING -> if (outOfFuel) {
                // Flies off in one piece, nose up and away
                rotation += (PLANE_TAKEOFF_PITCH - rotation) * min(1f, dt * PLANE_PITCH_RESPONSE)
                x += PLANE_LEAVE_SPEED * dt
                y += PLANE_LEAVE_SPEED * 0.5f * dt
                if (phaseTime >= PLANE_DROP_SECONDS) {
                    enter(VehiclePhase.LEAVING)
                    world.resumeFences()
                }
            } else {
                // The wreck tumbles down and away behind the horse
                x -= scroll * 0.6f
                y -= PLANE_WRECK_FALL_SPEED * dt
                rotation += PLANE_WRECK_SPIN * dt
                if (phaseTime >= PLANE_DROP_SECONDS) {
                    enter(VehiclePhase.LEAVING)
                    world.splash()
                    world.resumeFences()
                }
            }
            VehiclePhase.LEAVING -> {
                if (outOfFuel) {
                    x += PLANE_LEAVE_SPEED * dt
                    y += PLANE_LEAVE_SPEED * 0.5f * dt
                }
                if (buildings.isEmpty() && !airportVisible) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun fly(world: RodeoWorld, dt: Float, scroll: Float) {
        y = steering.fly(y, dt, ceiling = PLANE_MAX_HEIGHT)

        nextBuildingIn -= scroll
        if (nextBuildingIn <= 0f) spawnBuilding(world)

        val hitLeft = x + PLANE_HIT_LEFT
        val hitRight = x + PLANE_HIT_RIGHT
        buildings.forEach { building ->
            if (!building.passed && building.x + building.width < hitLeft) {
                building.passed = true
                world.addBonusPoints(BUILDING_POINTS)
            }
        }
        world.horseshoes.removeAll { shoe ->
            val collected = shoe.x > hitLeft - 2f && shoe.x < hitRight + 2f &&
                    shoe.height > y - 2f && shoe.height < y + PLANE_HIT_TOP + 2f
            if (collected) world.collectHorseshoe(shoe)
            collected
        }

        val hitBuilding = buildings.any { building ->
            val overlapsBuilding = building.x < hitRight && building.x + building.width > hitLeft
            val overlapsCloud = building.x - CLOUD_OVERHANG < hitRight &&
                    building.x + building.width + CLOUD_OVERHANG > hitLeft
            (overlapsBuilding && y < building.height) ||
                    (overlapsCloud && building.cloudBottom != null && y + PLANE_HIT_TOP > building.cloudBottom)
        }
        if (hitBuilding || y <= 0f) {
            y = max(0f, y)
            crash(world)
        }
    }

    private fun crash(world: RodeoWorld) {
        enter(VehiclePhase.UNLOADING)
        steering.release()
        dropStartX = x + PLANE_PILOT_X
        dropStartY = y + PLANE_PILOT_Y
        if (!outOfFuel) world.sparkle(x + PLANE_HIT_RIGHT, y + PLANE_HIT_TOP / 2f)
    }

    private fun spawnBuilding(world: RodeoWorld) {
        val width = BUILDING_MIN_WIDTH + Random.nextFloat() * (BUILDING_MAX_WIDTH - BUILDING_MIN_WIDTH)
        val height = BUILDING_MIN_HEIGHT + Random.nextFloat() * (BUILDING_MAX_HEIGHT - BUILDING_MIN_HEIGHT)
        val cloudChance = min(CLOUD_MAX_CHANCE, CLOUD_START_CHANCE + CLOUD_CHANCE_PER_SECOND * phaseTime)
        val cloudGap = max(CLOUD_MIN_GAP, CLOUD_START_GAP - CLOUD_GAP_SHRINK_PER_SECOND * phaseTime) +
                Random.nextFloat() * CLOUD_GAP_RANDOM
        // Only where the cloud reaches into the flyable sky
        val cloudBottom = (height + cloudGap).takeIf {
            Random.nextFloat() < cloudChance && it < PLANE_MAX_HEIGHT + PLANE_HIT_TOP - 2f
        }
        buildings.add(
            Building(x = world.worldWidth, width = width, height = height, seed = Random.nextInt(1000), cloudBottom = cloudBottom)
        )
        val gap = world.speed * (0.7f + Random.nextFloat() * 0.5f)
        nextBuildingIn = width + gap
        // Horseshoes float in the gaps, some above the rooftops, some low between the houses
        if (Random.nextFloat() < FLIGHT_HORSESHOE_CHANCE) {
            val shoeHeight = 14f + Random.nextFloat() * (PLANE_MAX_HEIGHT - 14f)
            world.horseshoes.add(Horseshoe(x = world.worldWidth + width + gap / 2f, height = shoeHeight))
        }
    }

    override fun cowboy(world: RodeoWorld): RodeoCowboyUi? = when (phase) {
        // Jumping over into the cockpit, landing there halfway through the take-off
        VehiclePhase.BOARDING -> {
            val progress = min(1f, progressOf(phaseTime, PLANE_BOARD_SECONDS) * 2f)
            RodeoCowboyUi(
                x = lerp(HORSE_X + COWBOY_SEAT_X, x + PLANE_PILOT_X, progress),
                height = lerp(world.horseHeight + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, y + PLANE_PILOT_Y, progress) +
                        PLANE_BOARD_HOP * sin(PI.toFloat() * progress),
                rotation = 0f,
                facingLeft = false,
                hatLift = 0f,
            )
        }
        // Dropping back into the saddle after the crash
        VehiclePhase.UNLOADING -> {
            val progress = progressOf(phaseTime, PLANE_DROP_SECONDS)
            val endY = world.riderlessHop + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH
            RodeoCowboyUi(
                x = lerp(dropStartX, HORSE_X + COWBOY_SEAT_X, progress),
                height = lerp(dropStartY, endY, progress) + PLANE_DROP_HOP * sin(PI.toFloat() * progress),
                rotation = 360f * progress, // a full flip on the way down
                facingLeft = false,
                hatLift = 2f * (1f - progress),
            )
        }
        else -> null
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        // The wreck is gone once it fell out of the picture; flown off, the plane is gone once out of view
        val planeVisible = when (phase) {
            VehiclePhase.LEAVING -> outOfFuel
            VehiclePhase.UNLOADING -> y >= -PLANE_LENGTH
            else -> true
        } && x + PLANE_LENGTH > 0f && y < PLANE_MAX_HEIGHT + 30f
        return RodeoPlaneUi(
            x = x,
            y = y,
            rotation = rotation,
            propellerPhase = propellerPhase,
            visible = planeVisible,
            hasPilot = phase == VehiclePhase.RIDING || (phase == VehiclePhase.BOARDING && progressOf(phaseTime, PLANE_BOARD_SECONDS) >= 0.5f),
            gearDown = phase == VehiclePhase.APPROACH || phase == VehiclePhase.BOARDING,
            airportX = airportX.takeIf { airportVisible },
            buildings = buildings.map {
                RodeoBuildingUi(x = it.x, width = it.width, height = it.height, seed = it.seed, cloudBottom = it.cloudBottom)
            },
            lassoHint = lassoHint(x + PLANE_LENGTH / 2f, y + 16f),
        )
    }
}

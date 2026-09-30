package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rocket

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.GROUND_OFFSET_UNITS
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.WORLD_HEIGHT_UNITS
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.hopArc
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoSteering
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoBoardingHop
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Rocket (easter egg): never comes along on its own - it is bought with SNAILS_PER_ROCKET saved-up
// snails. It slides in, the horse hops into its glass dome, it counts down and shoots up past the
// clouds into space. Up there it is steered like the plane through a field of planets until it hits
// one, falls too low or runs out of fuel. Then horse and rider pop out and float back down on a
// parachute, far ahead of the pack.
//
// Space is simply high up: the flight happens at SPACE_CAMERA and above, with the view panned up
// there, so launch, flight and the parachute descent all share one coordinate system.
internal const val SNAILS_PER_ROCKET = 25

// Shape, shared with the drawing (grid: x from the rear of the body, y up from its underside)
internal const val ROCKET_BODY_LENGTH = 44f
internal const val ROCKET_LENGTH = 52f               // nose tip included
internal const val ROCKET_BODY_HEIGHT = 9f
internal const val ROCKET_PAD_HEIGHT = 1f            // body underside while it stands on its fins
internal const val DOME_X = 17f                      // left end of the glass dome on top
internal const val DOME_WIDTH = 14f
internal const val DOME_HEIGHT = 6f
internal const val PLANET_KINDS = 5

private const val ROCKET_RIDE_X = HORSE_X - 8f
private const val ROCKET_START_X = -ROCKET_LENGTH - 5f
private const val ROCKET_BOARD_SECONDS = 0.7f
private const val ROCKET_HOP = 8f
private const val ROCKET_COUNTDOWN_SECONDS = 0.6f     // rumbling on the ground
private const val ROCKET_LAUNCH_SECONDS = 1.8f
private const val ROCKET_CLIMB_TILT = 18f             // degrees nose up while climbing
private const val ROCKET_LAUNCH_SCROLL_SPEED = 2500f  // u/s the ground races by during the launch
private const val ROCKET_SPEED_KMH = 28_000           // orbital speed, roughly
/** Top of the rocket (dome included) above its underside; the view keeps it inside the canvas. */
private const val ROCKET_TOP = ROCKET_BODY_HEIGHT + DOME_HEIGHT
private const val CAMERA_TOP = WORLD_HEIGHT_UNITS - GROUND_OFFSET_UNITS - 2f

// Space: the view stays panned up by SPACE_CAMERA; the rocket flies between its bottom and top edge
private const val SPACE_CAMERA = 100f
private const val SPACE_ENTRY = SPACE_CAMERA + CAMERA_TOP - ROCKET_TOP  // the launch ends up here, at the top edge
private const val SPACE_FLOOR = SPACE_CAMERA - GROUND_OFFSET_UNITS + 2f  // falling below this ends the flight
private const val SPACE_CEILING = SPACE_ENTRY
private const val SPACE_FADE_START = 50f              // altitude where the sky starts turning into space
private const val SPACE_FADE_RANGE = 60f
private const val SPACE_SECONDS = 25f                 // fuel
private const val SPACE_FINISH_POINTS = 50            // for making it until the fuel runs out
private const val HIT_LEFT = 3f                       // hitbox relative to the rear end / underside
private const val HIT_RIGHT = 49f
private const val HIT_BOTTOM = -0.5f

// Planets drift towards the rocket; they come closer together and more often in pairs over time
private const val PLANET_SPEED = 140f                 // u/s across the screen
private const val PLANET_MIN_RADIUS = 5f
private const val PLANET_MAX_RADIUS = 13f
private const val PLANET_GAP_MIN = 55f                // free space between two planets, in units
private const val PLANET_GAP_RANDOM = 35f
private const val PLANET_GAP_SHRINK_PER_SECOND = 1f
private const val PLANET_GAP_FLOOR = 40f
private const val PLANET_PAIR_START_SECONDS = 6f
private const val PLANET_PAIR_CHANCE = 0.35f
private const val PLANET_PAIR_PASSAGE = 24f           // room between a pair of planets; the rocket is 15 high
private const val PLANET_HIT_FACTOR = 0.9f            // a little forgiving at the edges
private const val PLANET_POINTS = 10

// Parachute: sinks fast from high up, gently near the ground
private const val PARACHUTE_MIN_SPEED = 18f
private const val PARACHUTE_SPEED_PER_UNIT = 0.55f
/** Canopy top above the hooves; the view keeps it inside the canvas on the way down. */
private const val PARACHUTE_TOP = 54f

private const val WRECK_FALL_SPEED = 40f
private const val WRECK_DRIFT = 30f                   // u/s the empty wreck drifts back
private const val WRECK_SPIN = 200f                   // degrees per second
private const val LEAVE_SPEED_X = 160f
private const val LEAVE_SPEED_Y = 50f

private enum class RocketStage { COUNTDOWN, LAUNCH, SPACE }

/** [x] / [y] are the planet's center; y is an altitude like everything else. */
private class Planet(var x: Float, val y: Float, val radius: Float, val kind: Int, val seed: Int) {
    var passed = false
}

internal class RodeoRocket : RodeoVehicle(RodeoVehicleKind.ROCKET) {

    private val planets = mutableListOf<Planet>()
    private val steering = RodeoSteering(
        climbAccel = 150f, // like the plane
        diveAccel = 170f,
        sinkAccel = 40f,
        maxClimb = 35f,
        maxSink = 45f,
    )
    private var stage = RocketStage.COUNTDOWN
    private var stageTime = 0f
    private var y = 0f           // underside of the body
    private var tilt = 0f        // degrees nose up
    private val boardingHop = RodeoBoardingHop()
    private var lift = 0f
    private var thrust = 0f      // 0..1, size of the flame
    private var nextPlanetIn = 0f
    private var crashed = false

    // Bought, never sent along by the rotation; it has no lasso phase either
    override val length = ROCKET_LENGTH
    override val passSpeed = 0f
    override val horseLift: Float get() = if (carriesRider) lift else 0f
    override val riderLeans: Boolean get() = isRiding
    override val shaking: Boolean get() = isRiding && stage != RocketStage.SPACE

    /** Horse and rider sit inside the glass dome while it flies. */
    override val hidesHorse: Boolean get() = isRiding

    override fun hitch() = x to y

    override fun reset() {
        super.reset()
        planets.clear()
        lift = 0f
        thrust = 0f
        steering.release()
    }

    /** Slides in under the horse right away. */
    override fun spawn(world: RodeoWorld) {
        planets.clear()
        x = ROCKET_START_X
        y = ROCKET_PAD_HEIGHT
        steering.stop()
        tilt = 0f
        thrust = 0f
        crashed = false
        board(world)
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardingHop.start(x, world)
    }

    override fun onJump(pressed: Boolean) {
        steering.climb = pressed
    }

    override fun onDive(pressed: Boolean) {
        steering.dive = pressed
    }

    override fun worldScroll(world: RodeoWorld, step: Float, dt: Float): Float =
        if (isRiding && stage == RocketStage.LAUNCH) lerp(step, ROCKET_LAUNCH_SCROLL_SPEED * dt, thrust) else step

    override fun speedKmh(world: RodeoWorld): Int? = when {
        !isRiding || stage == RocketStage.COUNTDOWN -> null
        stage == RocketStage.LAUNCH -> (ROCKET_SPEED_KMH * progressOf(stageTime, ROCKET_LAUNCH_SECONDS)).toInt()
        else -> ROCKET_SPEED_KMH
    }

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? =
        if (phase == VehiclePhase.UNLOADING) RodeoRidePose(parachute = true) else null

    override fun camera(): Float? = when {
        phase == VehiclePhase.RIDING && stage == RocketStage.SPACE -> SPACE_CAMERA
        phase == VehiclePhase.RIDING -> max(0f, y + ROCKET_TOP - CAMERA_TOP)
        // Follows the parachute down, never higher than space
        phase == VehiclePhase.UNLOADING -> (lift + PARACHUTE_TOP - CAMERA_TOP).coerceIn(0f, SPACE_CAMERA)
        else -> null
    }

    override fun space(): Float {
        val altitude = when (phase) {
            VehiclePhase.RIDING -> if (stage == RocketStage.SPACE) return 1f else y
            // Fades back to the sky as the view follows the parachute down
            VehiclePhase.UNLOADING -> return ((camera() ?: 0f) / SPACE_CAMERA).coerceIn(0f, 1f)
            else -> return 0f
        }
        return ((altitude - SPACE_FADE_START) / SPACE_FADE_RANGE).coerceIn(0f, 1f)
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        stageTime += dt
        planets.forEach { it.x -= PLANET_SPEED * dt }
        planets.removeAll { it.x + it.radius * 2f < 0f }

        when (phase) {
            VehiclePhase.IDLE, VehiclePhase.APPROACH -> Unit
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, ROCKET_BOARD_SECONDS)
                x = lerp(ROCKET_START_X, ROCKET_RIDE_X, smoothstep(progress))
                // Hops up into the dome
                lift = boardingHop.lift(y + ROCKET_BODY_HEIGHT, progress, ROCKET_HOP)
                clearPad(world)
                if (progress >= 1f) {
                    enter(VehiclePhase.RIDING)
                    enterStage(RocketStage.COUNTDOWN)
                }
            }
            VehiclePhase.RIDING -> when (stage) {
                RocketStage.COUNTDOWN -> {
                    thrust = 0.4f * stageTime / ROCKET_COUNTDOWN_SECONDS
                    clearPad(world)
                    if (stageTime >= ROCKET_COUNTDOWN_SECONDS) enterStage(RocketStage.LAUNCH)
                }
                RocketStage.LAUNCH -> launch()
                RocketStage.SPACE -> fly(world, dt)
            }
            VehiclePhase.UNLOADING -> {
                // The empty rocket tumbles away (or flies off with its last fuel)
                if (crashed) {
                    x -= WRECK_DRIFT * dt
                    y -= WRECK_FALL_SPEED * dt
                    tilt -= WRECK_SPIN * dt
                } else {
                    x += LEAVE_SPEED_X * dt
                    y += LEAVE_SPEED_Y * dt
                }
                lift = max(0f, lift - (PARACHUTE_MIN_SPEED + lift * PARACHUTE_SPEED_PER_UNIT) * dt)
                if (lift <= 0f) {
                    world.escapePack()
                    world.resumeFences()
                    enter(VehiclePhase.LEAVING)
                }
            }
            // Planets that are still on screen fly off before the next vehicle may come
            VehiclePhase.LEAVING -> if (planets.isEmpty()) enter(VehiclePhase.IDLE)
        }
        // Horse and rider sit in the dome
        if (carriesRider && phase != VehiclePhase.BOARDING && phase != VehiclePhase.UNLOADING) lift = y + ROCKET_BODY_HEIGHT
    }

    private fun enterStage(newStage: RocketStage) {
        stage = newStage
        stageTime = 0f
    }

    private fun launch() {
        val progress = progressOf(stageTime, ROCKET_LAUNCH_SECONDS)
        y = lerp(ROCKET_PAD_HEIGHT, SPACE_ENTRY, smoothstep(progress))
        tilt = ROCKET_CLIMB_TILT * sin(PI.toFloat() * progress)
        thrust = 0.4f + 0.6f * min(1f, progress * 3f)
        if (progress >= 1f) {
            enterStage(RocketStage.SPACE)
            tilt = 0f
            steering.stop()
            nextPlanetIn = 0f
        }
    }

    private fun fly(world: RodeoWorld, dt: Float) {
        y = steering.fly(y, dt, ceiling = SPACE_CEILING)
        // Nose follows the steering a little
        tilt = steering.vy / steering.maxClimb * 8f
        thrust = if (steering.climb) 1f else 0.55f

        nextPlanetIn -= PLANET_SPEED * dt
        if (nextPlanetIn <= 0f) spawnPlanets(world)

        val hitLeft = x + HIT_LEFT
        val hitRight = x + HIT_RIGHT
        val hitBottom = y + HIT_BOTTOM
        val hitTop = y + ROCKET_TOP
        planets.forEach { planet ->
            if (!planet.passed && planet.x + planet.radius < hitLeft) {
                planet.passed = true
                world.addBonusPoints(PLANET_POINTS)
            }
        }
        val hitPlanet = planets.firstOrNull { planet ->
            // Closest point of the rocket's box to the planet's center
            val closestX = planet.x.coerceIn(hitLeft, hitRight)
            val closestY = planet.y.coerceIn(hitBottom, hitTop)
            val dx = planet.x - closestX
            val dy = planet.y - closestY
            sqrt(dx * dx + dy * dy) < planet.radius * PLANET_HIT_FACTOR
        }
        when {
            hitPlanet != null || y <= SPACE_FLOOR -> {
                world.sparkle(x + HIT_RIGHT, y + ROCKET_BODY_HEIGHT / 2f)
                bailOut(crashed = true)
            }
            stageTime >= SPACE_SECONDS -> {
                world.addBonusPoints(SPACE_FINISH_POINTS)
                bailOut(crashed = false)
            }
        }
    }

    /** Horse and rider pop out of the dome and open the parachute. */
    private fun bailOut(crashed: Boolean) {
        this.crashed = crashed
        lift = y + ROCKET_BODY_HEIGHT
        steering.release()
        thrust = if (crashed) 0f else 1f
        enter(VehiclePhase.UNLOADING)
    }

    private fun spawnPlanets(world: RodeoWorld) {
        val gap = max(PLANET_GAP_FLOOR, PLANET_GAP_MIN - PLANET_GAP_SHRINK_PER_SECOND * stageTime) +
                Random.nextFloat() * PLANET_GAP_RANDOM
        val low = SPACE_FLOOR
        val high = SPACE_CEILING + ROCKET_TOP
        val widest: Float
        if (stageTime > PLANET_PAIR_START_SECONDS && Random.nextFloat() < PLANET_PAIR_CHANCE) {
            // One planet above, one below, with a passage between them
            val passage = low + PLANET_PAIR_PASSAGE / 2f + Random.nextFloat() * (high - low - PLANET_PAIR_PASSAGE)
            val top = randomRadius()
            val bottom = randomRadius()
            addPlanet(world, passage + PLANET_PAIR_PASSAGE / 2f + top, top)
            addPlanet(world, passage - PLANET_PAIR_PASSAGE / 2f - bottom, bottom)
            widest = max(top, bottom)
        } else {
            val radius = randomRadius()
            addPlanet(world, low + Random.nextFloat() * (high - low), radius)
            widest = radius
        }
        nextPlanetIn = widest * 2f + gap
    }

    private fun randomRadius() = PLANET_MIN_RADIUS + Random.nextFloat() * (PLANET_MAX_RADIUS - PLANET_MIN_RADIUS)

    private fun addPlanet(world: RodeoWorld, centerY: Float, radius: Float) {
        planets.add(
            Planet(
                x = world.worldWidth + radius,
                y = centerY,
                radius = radius,
                kind = Random.nextInt(PLANET_KINDS),
                seed = Random.nextInt(1000),
            )
        )
    }

    /** Fences and snails on the launch pad are blown aside. */
    private fun clearPad(world: RodeoWorld) = world.clearTrack(x, x + ROCKET_LENGTH)

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoRocketUi(
            x = x,
            y = y,
            tilt = tilt,
            thrust = thrust,
            flicker = phaseTime * 30f,
            // Gone once it tumbled or flew out of the picture
            visible = phase != VehiclePhase.LEAVING,
            passengers = isRiding,
            planets = planets.map { RodeoPlanetUi(x = it.x, y = it.y, radius = it.radius, kind = it.kind, seed = it.seed) },
        )
    }
}

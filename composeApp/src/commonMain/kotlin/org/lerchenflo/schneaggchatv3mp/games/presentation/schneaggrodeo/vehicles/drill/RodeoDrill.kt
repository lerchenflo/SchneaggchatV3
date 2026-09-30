package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.drill

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.OnTrack
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.scrollAlong
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HOOVES_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.hopArc
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.circleTouchesBox
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoBoardingHop
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import kotlin.math.sin
import kotlin.random.Random

// Drill machine (mine): a yellow tracked machine with a huge drill at its front rolls by. Lasso it
// and horse and rider climb in through the hatch (only the hat sticks out). Hold to drill down into
// the earth below the track, let go and it comes back up. Gold nuggets down there give points; the
// hard gray rocks end the ride early (without any harm). Afterwards it surfaces and the horse hops out.

// Shape, shared with the drawing (grid: x from the machine's rear, y up from the underside of its
// tracks). The machine is drawn [DRILL_SCALE] times as big as this grid, big enough for horse and rider.
internal const val DRILL_SCALE = 1.7f
internal const val DRILL_BODY_LENGTH = 18f
internal const val DRILL_LENGTH = 27f
internal const val DRILL_BODY_TOP = 9f
internal const val DRILL_TRACK_HEIGHT = 2.8f
internal const val DRILL_HATCH_X = 7f
/** The same in world units. */
private const val BODY_LENGTH = DRILL_BODY_LENGTH * DRILL_SCALE
private const val FULL_LENGTH = DRILL_LENGTH * DRILL_SCALE
private const val BODY_TOP = DRILL_BODY_TOP * DRILL_SCALE
private const val HATCH_X = DRILL_HATCH_X * DRILL_SCALE
/** Radius of a nugget and a rock down in the earth. */
internal const val NUGGET_RADIUS = 1.3f
internal const val ROCK_RADIUS = 3f

private const val DRILL_PASS_SPEED = 16f
private const val DRILL_HITCH_Y = 8f
/** While riding, the hatch is where the horse's hooves were. */
private const val DRILL_RIDE_X = HORSE_X + HOOVES_X - HATCH_X - 1f
private const val BOARD_SECONDS = 0.7f
private const val UNLOAD_SECONDS = 0.6f
private const val HOP = 6f
private const val RIDE_SECONDS = 12f
private const val DIG_SPEED = 18f
private const val RISE_SPEED = 15f
private const val SURFACE_SPEED = 18f
/** How deep the machine's underside goes. */
private const val MAX_DEPTH = 34f
private const val SPEED_FACTOR = 0.75f
/** The view follows the machine down. */
private const val CAMERA_FOLLOW = 0.9f
private const val CAMERA_MAX_DOWN = 30f
private const val FIND_GAP_MIN = 10f
private const val FIND_GAP_RANDOM = 10f
private const val ROCK_CHANCE = 0.3f
/** Finds lie between these depths (their middle). */
private const val FIND_TOP = 4f
private const val FIND_BOTTOM = 26f
private const val NUGGET_POINTS = 20
private const val TRAIL_SPACING = 1.5f
private const val LEAVE_SINK_SPEED = 10f

/** A nugget or a rock in the earth; moves with the ground. [y] is its middle (negative: below the ground). */
private class Find(override var x: Float, val y: Float, val rock: Boolean, val seed: Int) : OnTrack

internal class RodeoDrill : RodeoVehicle(RodeoVehicleKind.DRILL) {

    /** Underside of the tracks; negative while underground. */
    private var y = 0f
    private var drilling = false
    /** The ride is over (time up or a rock): the machine comes back up no matter what. */
    private var surfacing = false
    private var lift = 0f
    private val boardingHop = RodeoBoardingHop()
    private var drillPhase = 0f
    private var nextFindIn = 0f
    private val finds = mutableListOf<Find>()
    /** The tunnel dug so far: middle points, moving with the ground. */
    private val trail = mutableListOf<Pair<Float, Float>>()
    private var trailScroll = 0f

    override val length = FULL_LENGTH
    override val passSpeed = DRILL_PASS_SPEED

    override val horseLift: Float get() = if (carriesRider) lift else 0f
    override val horseHops: Boolean get() = phase == VehiclePhase.BOARDING || phase == VehiclePhase.UNLOADING
    override val hidesHorse: Boolean get() = phase == VehiclePhase.RIDING

    /** The whole machine is a target. */
    override fun hitch() = hitchX() to DRILL_HITCH_Y

    override fun camera(): Float? =
        if (phase == VehiclePhase.RIDING) (y * CAMERA_FOLLOW).coerceIn(-CAMERA_MAX_DOWN, 0f) else null

    override val rideSpeedFactor = SPEED_FACTOR

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        y = 0f
        surfacing = false
        finds.clear()
        trail.clear()
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardingHop.start(x, world)
        drilling = false
        surfacing = false
        nextFindIn = 6f
    }

    override fun onJump(pressed: Boolean) {
        drilling = pressed && phase == VehiclePhase.RIDING
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        finds.scrollAlong(scroll) { it.x < -ROCK_RADIUS }
        trail.replaceAll { (trailX, trailY) -> trailX - scroll to trailY }
        trail.removeAll { it.first < -6f }
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> {
                drillPhase += dt * 4f
                passBy(dt)
            }
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, BOARD_SECONDS)
                x = boardingHop.slideX(DRILL_RIDE_X, progress)
                lift = boardingHop.lift(BODY_TOP, progress, HOP)
                if (progress >= 1f) enter(VehiclePhase.RIDING)
            }
            VehiclePhase.RIDING -> dig(world, dt, scroll)
            VehiclePhase.UNLOADING -> {
                val progress = progressOf(phaseTime, UNLOAD_SECONDS)
                lift = hopArc(BODY_TOP, 0f, progress, HOP)
                if (progress >= 1f) {
                    lift = 0f
                    enter(VehiclePhase.LEAVING)
                    world.resumeFences()
                }
            }
            VehiclePhase.LEAVING -> {
                // Drills down into the ground and is gone
                drillPhase += dt * 12f
                x -= scroll
                y -= LEAVE_SINK_SPEED * dt
                if (y < -BODY_TOP - 2f && trail.isEmpty()) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun dig(world: RodeoWorld, dt: Float, scroll: Float) {
        drillPhase += dt * 12f
        val velocity = when {
            surfacing -> SURFACE_SPEED
            drilling -> -DIG_SPEED
            else -> RISE_SPEED
        }
        y = (y + velocity * dt).coerceIn(-MAX_DEPTH, 0f)
        lift = BODY_TOP + y

        // The tunnel behind the machine
        trailScroll += scroll
        if (y < 0f && trailScroll >= TRAIL_SPACING) {
            trailScroll = 0f
            trail += (x + BODY_LENGTH / 2f) to (y + BODY_TOP / 2f)
        }

        // Nuggets and rocks come along in the earth ahead
        nextFindIn -= scroll
        if (nextFindIn <= 0f && !surfacing) {
            nextFindIn = FIND_GAP_MIN + Random.nextFloat() * FIND_GAP_RANDOM
            finds += Find(
                x = world.worldWidth + ROCK_RADIUS,
                y = -(FIND_TOP + Random.nextFloat() * (FIND_BOTTOM - FIND_TOP)),
                rock = Random.nextFloat() < ROCK_CHANCE,
                seed = Random.nextInt(100),
            )
        }

        // The drill's tip reaches them
        val tipLeft = x + BODY_LENGTH
        val tipRight = x + FULL_LENGTH
        val bottom = y + 1f
        val top = y + BODY_TOP - 1f
        finds.removeAll { find ->
            val radius = if (find.rock) ROCK_RADIUS else NUGGET_RADIUS
            val hit = circleTouchesBox(find.x, find.y, radius, left = tipLeft, right = tipRight, bottom = bottom, top = top)
            if (hit && find.rock && !surfacing) {
                // Too hard to drill through: back up to the surface
                world.sparkle(find.x, find.y)
                surfacing = true
                drilling = false
            } else if (hit && !find.rock) {
                world.sparkle(find.x, find.y)
                world.addBonusPoints(NUGGET_POINTS)
            }
            hit && !find.rock
        }

        if (phaseTime >= RIDE_SECONDS) surfacing = true
        if (surfacing && y >= 0f) {
            y = 0f
            enter(VehiclePhase.UNLOADING)
            world.dust(x + BODY_LENGTH / 2f)
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        val underground = phase == VehiclePhase.RIDING || trail.isNotEmpty() || y < 0f
        return RodeoDrillUi(
            x = x,
            y = y,
            drillPhase = drillPhase,
            hatInHatch = phase == VehiclePhase.RIDING,
            earthVisible = underground,
            tunnel = trail.map { (trailX, trailY) -> RodeoTunnelUi(trailX, trailY) },
            finds = finds.map { RodeoDrillFindUi(x = it.x, y = it.y, rock = it.rock, seed = it.seed) },
            // Stuck in a rock: the drill shakes
            shake = if (surfacing && phase == VehiclePhase.RIDING) 0.3f * sin(drillPhase * 3f) else 0f,
            lassoHint = lassoHint(x + FULL_LENGTH / 2f, BODY_TOP + 8f),
        )
    }
}

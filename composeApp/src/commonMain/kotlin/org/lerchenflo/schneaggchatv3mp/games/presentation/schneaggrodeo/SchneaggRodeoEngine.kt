package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// All game physics run in "units" (u). One unit is the canvas height / WORLD_HEIGHT_UNITS, so the
// game plays identically on every screen size - only the visible width (how far ahead you see) changes.
internal const val WORLD_HEIGHT_UNITS = 75f
internal const val GROUND_OFFSET_UNITS = 10f // ground line distance from the canvas bottom

// One unit is also 10 cm in "horse scale", which is what the fence height labels show.
private const val CM_PER_UNIT = 10
internal const val MIN_FENCE_CM = 50
internal const val MAX_FENCE_CM = 170
// Fence height range at the start of a run; both ends grow linearly to their final values over
// FENCE_GROWTH_SECONDS of riding.
private const val START_MAX_FENCE_CM = 90
private const val FINAL_MIN_FENCE_CM = 100
private const val FENCE_GROWTH_SECONDS = 150f
private const val POLE_SPACING = 4f
internal const val POLE_THICKNESS = 1.1f

private const val GRAVITY = 330f          // u/s²
private const val JUMP_VELOCITY = 82f     // u/s
// Holding the jump input lowers gravity while rising, for at most MAX_HOLD_SECONDS - a quick tap
// clears ~80 cm, a full hold clears the 170 cm fences.
private const val HOLD_GRAVITY_FACTOR = 0.3f
private const val MAX_HOLD_SECONDS = 0.3f
private const val START_SPEED = 75f       // u/s
private const val MAX_SPEED = 180f        // u/s
// Slow ramp like the Chrome dino: roughly two minutes of riding until MAX_SPEED
private const val ACCELERATION = 0.9f     // u/s per second of riding
private const val OXER_MIN_SPEED = 110f   // wide jumps only show up once the pace is high enough
internal const val UNITS_PER_POINT = 10f

// Knocking a pole or running into a snail doesn't end the run - the horse stumbles for a moment
// and the chasing pack closes in. The run ends once the pack reaches the horse.
private const val STUMBLE_SECONDS = 0.8f
private const val STUMBLE_SPEED_FACTOR = 0.55f
private const val STUMBLE_MAX_PITCH = 24f  // degrees nose-down at the worst moment of a stumble
private const val DUST_SECONDS = 0.5f
private const val SPLASH_SECONDS = 0.35f
private const val CHASE_GAP_MAX = 45f     // pack distance behind the horse; starts here (off screen above ~26)
private const val CHASE_GAP_REGAIN = 1.5f // u/s won back while riding
private const val FENCE_CRASH_PENALTY = 12f
private const val RUNNER_CRASH_PENALTY = 10f
private const val CATCH_GAP_BONUS = 8f
private const val CATCH_POINTS = 25
private const val PACK_SIZE = 3
private const val PACK_SPACING = 7f

// Schneaggs (snails), drawn with the same sprite as the TowerStack game
internal const val SNAIL_SIZE = 10f       // sprite side length in units
private const val SNAIL_HALF_WIDTH = 4f   // collision half width around the sprite center
private const val SNAIL_BODY_HEIGHT = 4.5f
private const val CRAWLER_SPEED = 3f      // u/s towards the horse, relative to the ground
private const val RUNNER_SPEED = 30f      // u/s towards the horse, relative to the ground
private const val RUNNER_START_SECONDS = 30f
private const val SNAIL_ON_FENCE_CHANCE = 0.3f
private const val SNAIL_BETWEEN_FENCES_CHANCE = 0.45f

// Super jump: one charge per SNAILS_PER_SUPER_JUMP caught snails. A slow-motion leap over the next
// SUPER_JUMP_FENCES fences, preceded by a wind-up in which the hind legs grow.
private const val SNAILS_PER_SUPER_JUMP = 5
private const val SUPER_JUMP_FENCES = 5
private const val SUPER_JUMP_HEIGHT = 31f           // peak height; keeps the hat inside the canvas
private const val SUPER_JUMP_WINDUP_SECONDS = 0.6f  // real time
private const val SUPER_JUMP_WINDUP_TIME_SCALE = 0.25f
private const val SUPER_JUMP_TIME_SCALE = 0.6f      // world slow motion during the flight...
private const val SUPER_JUMP_MAX_SECONDS = 4f       // ...unless that would make the flight longer than this
private const val SUPER_JUMP_ROW_GAP_SECONDS = 0.4f // gap between the pre-placed fences, in riding time
private const val SUPER_JUMP_LANDING_MARGIN = 6f
private const val SUPER_JUMP_HIND_LEG_GROWTH = 0.6f // hind legs grow to 160 % during the wind-up
/** Exponent < 1 turns the sine arc into a steep takeoff, long float and steep landing. */
private const val SUPER_JUMP_ARC_SHAPE = 0.45f
// The wind-up rears the horse up on its pumped hind legs, front hooves pawing the air
private const val SUPER_JUMP_REAR_PITCH = 28f        // degrees nose-up at the end of the wind-up
private const val SUPER_JUMP_LANDING_PITCH = 16f     // degrees nose-down on the way down

// The galloping horse rocks gently: nose down as the front legs land, up as the hind legs push off
private const val GALLOP_ROCK_DEGREES = 2.5f

// Normal jumps tilt the horse: nose up on takeoff, level at the peak, nose down to land
private const val JUMP_MAX_PITCH = 16f
private const val JUMP_PITCH_BLEND_HEIGHT = 4f       // pitch fades in / out over this height above the ground

// Lucky horseshoes float between the fences. Jumping through one stores a lucky charm that
// absorbs the next crash (fence or runner) - no stumble, no penalty.
private const val HORSESHOE_CHANCE = 0.2f
private const val HORSESHOE_MIN_HEIGHT = 26f
private const val HORSESHOE_MAX_HEIGHT = 42f
// Collected when the shoe is between the horse's belly and the rider's hat
private const val HORSESHOE_REACH_BOTTOM = 12f
private const val HORSESHOE_REACH_TOP = 30f
private const val HORSESHOE_POINTS = 10
private const val MAX_LUCKY_CHARMS = 3
private const val SPARKLE_SECONDS = 0.45f

// Plane: every now and then a plane passes low with a rope ladder. Lasso the ladder and the cowboy
// climbs aboard and flies over a city skyline until he crashes into a building or the ground,
// then drops back into the saddle. The riderless horse gallops on below, hopping the fences.
private const val PLANE_FIRST_SECONDS = 40f          // game seconds until the first plane
private const val PLANE_INTERVAL_MIN = 45f
private const val PLANE_INTERVAL_RANDOM = 30f
private const val PLANE_PASS_SPEED = 35f             // u/s across the screen while passing by
private const val PLANE_APPROACH_HEIGHT = 36f        // underside of the fuselage
internal const val PLANE_LENGTH = 22f
internal const val PLANE_LADDER_X = 10f              // ladder relative to the plane's left edge
internal const val PLANE_LADDER_LENGTH = 12f
private const val PLANE_BOARD_SECONDS = 0.9f
private const val PLANE_FLY_X = 16f                  // left edge of the plane while flying (HORSE_X + 4)
private const val PLANE_CLIMB_ACCEL = 150f           // u/s² while steering up
private const val PLANE_DIVE_ACCEL = 170f            // u/s² while steering down
private const val PLANE_SINK_ACCEL = 45f             // u/s² without input: it slowly goes down
private const val PLANE_MAX_CLIMB = 35f              // u/s
private const val PLANE_MAX_SINK = 45f               // u/s
private const val PLANE_MAX_HEIGHT = 55f             // keeps the plane inside the canvas
private const val PLANE_HIT_LEFT = 2f                // hitbox relative to the plane's left edge / underside
private const val PLANE_HIT_RIGHT = 20f
private const val PLANE_HIT_TOP = 7f
internal const val PLANE_PILOT_X = 11f                // cowboy's feet in the cockpit
private const val PLANE_PILOT_Y = 1f
private const val PLANE_DROP_SECONDS = 0.8f
private const val PLANE_DROP_HOP = 8f
private const val PLANE_WRECK_FALL_SPEED = 30f
private const val PLANE_WRECK_SPIN = 220f            // degrees per second
private const val BUILDING_MIN_HEIGHT = 12f
private const val BUILDING_MAX_HEIGHT = 44f
private const val BUILDING_MIN_WIDTH = 14f
private const val BUILDING_MAX_WIDTH = 22f
private const val BUILDING_POINTS = 5                // per building flown past
private const val FLIGHT_HORSESHOE_CHANCE = 0.6f
// Storm clouds hang from the sky above the buildings, leaving a passage over the roof that gets
// narrower and more frequent the longer the flight lasts
private const val CLOUD_START_CHANCE = 0.45f
private const val CLOUD_CHANCE_PER_SECOND = 0.03f
private const val CLOUD_MAX_CHANCE = 0.85f
private const val CLOUD_START_GAP = 26f              // roof to cloud at the start of a flight
private const val CLOUD_MIN_GAP = 15f                // never narrower than this (plane is 7 high)
private const val CLOUD_GAP_SHRINK_PER_SECOND = 0.5f
private const val CLOUD_GAP_RANDOM = 5f
private const val CLOUD_OVERHANG = 3f                // clouds are a bit wider than the building below

// Lawn tractor: every now and then a red lawn tractor rolls by on the ground. Lasso it and the horse
// hops onto its deck, cowboy and all, for a short ride at an absurd speed. The world races by, but
// points keep coming in at the normal pace; everything in the way is mowed flat without a penalty.
private const val TRACTOR_FIRST_SECONDS = 70f        // game seconds until the first tractor
private const val TRACTOR_INTERVAL_MIN = 50f
private const val TRACTOR_INTERVAL_RANDOM = 30f
private const val TRACTOR_PASS_SPEED = 30f           // u/s across the screen while rolling by
internal const val TRACTOR_LENGTH = 40f
internal const val TRACTOR_DECK_HEIGHT = 7f          // the horse's hooves stand on this
internal const val TRACTOR_HITCH_X = 4f              // where the lasso grabs it, relative to its left edge
internal const val TRACTOR_HITCH_Y = 6f
private const val TRACTOR_BOARD_SECONDS = 0.6f
private const val TRACTOR_RIDE_SECONDS = 4f
private const val TRACTOR_UNLOAD_SECONDS = 0.6f
private const val TRACTOR_HOP = 6f                   // hop height onto / off the deck
private const val TRACTOR_SCROLL_SPEED = 1500f       // u/s the world races by while riding
private const val TRACTOR_LEAVE_SPEED = 400f         // u/s it speeds away after dropping the horse off
private const val TRACTOR_RUMBLE_DEGREES = 0.8f
private const val TRACTOR_SHAKE = 0.7f               // units the whole picture shakes while riding
// Every fence mowed rips off one part; the hit after the last part wrecks it. The ride spaces its
// fences so that happens right around the end of the ride.
internal const val TRACTOR_PARTS = 5
private const val TRACTOR_FIRST_FENCE_SECONDS = 0.3f
private const val TRACTOR_MIN_FENCE_SECONDS = 0.3f
private const val TRACTOR_LAST_HIT_MARGIN = 0.3f     // the wrecking fence arrives this long before the ride ends
private const val TRACTOR_WRECK_TILT = 25f           // degrees the wreck tips over
private const val TRACTOR_WRECK_TILT_SPEED = 120f    // degrees per second
private const val DEBRIS_GRAVITY = 150f
/** Bits of scrap the wreck scatters on top of the parts lost before. */
private const val WRECK_SCRAP_PIECES = 4
/** The inside gag: shown on the speedometer while riding the tractor. */
private const val TRACTOR_SPEED_KMH = 65_000
// One unit is 10 cm, so u/s * 0.36 = km/h (75 u/s start pace = 27 km/h, 180 u/s top pace = 65 km/h)
private const val KMH_PER_UNIT_PER_SECOND = 0.36f

// Lasso: thrown from the rider's hand, homes in on the first snail within range
private const val LASSO_RANGE = 32f
private const val LASSO_DURATION = 0.45f  // out and back
private const val LASSO_COOLDOWN = 0.8f
private const val LASSO_CATCH_TOLERANCE = 6f // extra reach at catch time, absorbs stumbles and speed changes

private const val RIDER_LEAN_RESPONSE = 12f // 1/s, how fast the rider follows the lean target
internal const val HORSE_X = 12f          // left edge of the horse in world units
private const val TRACTOR_RIDE_X = HORSE_X - 3f // tractor's left edge while riding: horse centered on the deck
internal const val HAND_X = 18f           // rider's rein hand relative to the horse
internal const val HAND_Y = 18f
private const val MAX_FRAME_SECONDS = 0.05f

// Hitbox of the horse relative to its left edge / hooves - a bit smaller than the drawing so
// near misses feel fair.
private const val HITBOX_LEFT = 8f
private const val HITBOX_RIGHT = 24f
private const val HITBOX_BOTTOM = 1f

/** Highscore markers stay visible this far past either edge, so their labels slide in and out. */
private const val MARKER_VISIBLE_MARGIN = 40f
/**
 * Bonus points move the markers closer without any distance ridden. They catch up at this speed
 * (u/s) instead of jumping, so a marker slides in from the edge rather than popping up mid-screen.
 */
private const val MARKER_CATCH_UP_SPEED = 300f

/** How long the "you're in the plane / on the tractor" banner stays up, in real seconds. */
private const val ANNOUNCEMENT_SECONDS = 1.8f

// Falling off: rarely, after a big normal jump while the pack is far away, the horse bucks the
// cowboy off. He has to lasso his horse and swing back into the saddle before the pack gets there.
private const val FALL_MIN_JUMP_PEAK = 14f      // jumps peaking at 140 cm and more can throw him
private const val FALL_CHANCE = 0.25f
private const val FALL_MIN_CHASE_GAP = 32f      // only while the pack is well off screen
private const val FALL_MIN_ELAPSED = 20f
private const val FALL_COOLDOWN = 45f           // game seconds between two falls
private const val FALL_PACK_APPROACH = 4f       // u/s the pack closes in while he is on foot (~8 s from the closest allowed gap)
private const val BUCK_SECONDS = 0.5f
private const val BUCK_MAX_PITCH = 22f          // hindquarters up, nose down
private const val COWBOY_SEAT_X = 12f           // saddle relative to the horse's left edge
private const val COWBOY_SEAT_Y = 16f
private const val COWBOY_THROW_VX = 40f           // lands ~30 u ahead: the pacing horse drifts in and out of lasso range
private const val COWBOY_THROW_VY = 35f
private const val COWBOY_GRAVITY = 150f         // floaty, so the flip over the neck is readable
private const val COWBOY_DOWN_SECONDS = 0.8f    // lying in the dirt before he gets up
private const val COWBOY_GET_UP_SECONDS = 0.35f // last part of lying down: rotating back upright
private const val COWBOY_HAND_Y = 10f
private const val COWBOY_LEG_LENGTH = 6f        // feet to torso of the standing cowboy
// The riderless horse paces nervously behind him, in and out of lasso range
private const val HORSE_WANDER_CENTER = -4f
private const val HORSE_WANDER_AMPLITUDE = 8f
private const val HORSE_WANDER_SPEED = 1.1f     // rad/s
private const val HORSE_WANDER_BLEND_SECONDS = 1f
private const val REMOUNT_LASSO_RANGE = 32f
private const val REMOUNT_SECONDS = 0.6f
private const val REMOUNT_HOP = 5f
private const val REMOUNT_SPEED_FACTOR = 0.8f

private enum class FallPhase { RIDING, THROWN, DOWN, ON_FOOT, REMOUNT }

private enum class SuperJumpPhase { NONE, WINDUP, FLIGHT }

private enum class PlanePhase {
    NONE,
    /** Passing by with the ladder down, waiting to be lassoed. */
    APPROACH,
    /** Cowboy climbs the ladder while the plane settles into its flying spot. */
    BOARDING,
    FLYING,
    /** Crashed: the wreck tumbles away and the cowboy drops back into the saddle. */
    CRASHING,
}

private enum class TractorPhase {
    NONE,
    /** Rolling by on the ground, waiting to be lassoed. */
    APPROACH,
    /** Horse and cowboy hop onto the deck while the tractor slides under them. */
    BOARDING,
    RIDING,
    /** The horse hops back down and the tractor speeds off. */
    UNLOADING,
}

/**
 * A part torn off the tractor, flying in screen space. [part] is the index of the lost part (see
 * RodeoTractorUi.partsLost), or [TRACTOR_PARTS] for a bit of scrap from the wreck.
 */
private class Debris(
    var x: Float,
    var y: Float,
    val vx: Float,
    var vy: Float,
    val spin: Float,
    val part: Int,
) {
    var rotation = 0f
}

/** A building of the skyline under the plane; [x] is its left edge. */
private class Building(
    var x: Float,
    val width: Float,
    val height: Float,
    val seed: Int,
    /** Underside of the storm cloud hanging above this building, or null for open sky. */
    val cloudBottom: Float?,
) {
    var passed = false
}

private class Fence(var x: Float, val width: Float, val heightCm: Int, val colorOffset: Int) {
    val top: Float = heightCm / CM_PER_UNIT.toFloat()
    var knocked = false

    // Top pole at the fence height, further poles below it while there is room
    val poleHeights: List<Float> = generateSequence(top) { it - POLE_SPACING }
        .takeWhile { it >= 2f }
        .toList()
}

private enum class SnailKind {
    /** Crawls slowly or sits on a fence - worth points when lassoed. */
    CRAWLER,
    /** Sprints towards the horse and hops over fences - jump it or lasso it. */
    RUNNER
}

private enum class SnailState { ACTIVE, LASSOED, KNOCKED }

/** A lucky horseshoe floating at [height] above the ground; [x] is its center. */
private class Horseshoe(var x: Float, val height: Float) {
    val phase = Random.nextFloat() * 2f * PI.toFloat()
}

/** [x] is the sprite center, [height] its underside above the ground, both in units. */
private class Snail(var x: Float, val kind: SnailKind, var onFence: Fence? = null) {
    var height = 0f
    var state = SnailState.ACTIVE
    var verticalVelocity = 0f
    var spin = 0f
    val phase = Random.nextFloat() * 2f * PI.toFloat()
}

/** Overshoots slightly past 1 before settling - gives the growing legs a springy pop. */
private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val t = x - 1f
    return 1f + (c1 + 1f) * t * t * t + c1 * t * t
}

/** Where [snail] will be after [seconds], with the world scrolling at [worldSpeed]. */
private fun predictedSnailX(snail: Snail, worldSpeed: Float, seconds: Float): Float {
    val ownSpeed = when {
        snail.onFence != null -> 0f
        snail.kind == SnailKind.RUNNER -> RUNNER_SPEED
        else -> CRAWLER_SPEED
    }
    return snail.x - (worldSpeed + ownSpeed) * seconds
}

/** Height a snail at [x] hops to so it arcs over any fence it is passing. */
private fun hopHeight(x: Float, fences: List<Fence>): Float {
    var height = 0f
    fences.forEach { fence ->
        val halfSpan = fence.width / 2f + 8f
        val distance = abs(x - (fence.x + fence.width / 2f))
        if (distance < halfSpan) {
            val relative = distance / halfSpan
            height = max(height, (fence.top + 2f) * (1f - relative * relative))
        }
    }
    return height
}

/**
 * The Schneagg Rodeo simulation: a cowboy on a horse jumping show-jumping fences while a pack of
 * schneaggs chases him. Deliberately mutable and allocation-light - it is stepped every frame by
 * the ViewModel, which publishes an immutable [SchneaggRodeoFrame] built by [toFrame] afterwards.
 */
internal class SchneaggRodeoEngine {

    private val fences = mutableListOf<Fence>()
    private val snails = mutableListOf<Snail>()
    private val horseshoes = mutableListOf<Horseshoe>()
    private val buildings = mutableListOf<Building>()
    private val debris = mutableListOf<Debris>()

    /** Visible world width in units; follows the canvas size. */
    var worldWidth = 0f

    private var horseHeight = 0f
    private var verticalVelocity = 0f
    var speed = START_SPEED
        private set
    var distance = 0f
        private set
    private var nextFenceIn = 0f
    private var nextRunnerIn = 0f
    private var gaitPhase = 0f
    /** Game time ridden (slows down with the super jump); drives fence growth and runner start. */
    var elapsed = 0f
        private set
    /** Real (frame) time of the run, excluding pauses - the time that is submitted with the score. */
    var runTimeSeconds = 0f
        private set
    private var riderLean = 0f // 0 = upright, 1 = fully leaning forward over the neck
    private var jumpHeld = false
    private var airTime = 0f
    var fenceCount = 0
        private set
    private var stumble = 0f
    var chaseGap = CHASE_GAP_MAX
        private set
    private var packPhase = 0f
    var bonusPoints = 0
        private set
    private var dustTime = 0f   // counts down while a dust cloud is shown
    private var dustX = 0f
    private var splashTime = 0f // counts down while the landing splash is shown

    private var superJumpPhase = SuperJumpPhase.NONE
    private var superJumpQueued = false
    private var superJumpWindup = 0f     // real seconds into the wind-up
    private var superJumpDistance = 0f   // world distance of the whole flight
    private var superJumpTravelled = 0f
    private var superJumpTimeScale = 1f
    private var superJumpLastFence: Fence? = null

    private var fallPhase = FallPhase.RIDING
    private var fallClock = 0f       // seconds since the fall started
    private var fallPhaseTime = 0f   // seconds in the current fall phase
    private var lastFallAt = -FALL_COOLDOWN
    private var jumpPeak = 0f
    private var cowboyX = 0f
    private var cowboyY = 0f
    private var cowboyVx = 0f
    private var cowboyVy = 0f
    private var cowboyRotation = 0f
    private var cowboySpin = 0f
    private var horseWanderPhase = 0f
    private var remountStartX = 0f
    private var remountStartOffset = 0f

    private var lassoTime = -1f // < 0 while no lasso is out
    private var lassoCooldown = 0f
    private var lassoResolved = false
    private var lassoTarget: Snail? = null
    private var lassoAimX = 0f
    private var lassoAimY = 0f
    private var lassoTipX = 0f
    private var lassoTipY = 0f

    var snailsCaught = 0
        private set
    /** Super jumps cost snails: every full [SNAILS_PER_SUPER_JUMP] caught snails are one charge. */
    val superJumpCharges: Int get() = snailsCaught / SNAILS_PER_SUPER_JUMP
    /** Collected lucky horseshoes; each one absorbs one crash. */
    var luckyCharms = 0
        private set
    private var sparkleTime = 0f // counts down while a sparkle is shown
    private var sparkleX = 0f
    private var sparkleY = 0f

    private var planePhase = PlanePhase.NONE
    private var nextPlaneIn = PLANE_FIRST_SECONDS
    private var planeX = 0f          // left edge of the fuselage
    private var planeY = 0f          // underside of the fuselage
    private var planeVy = 0f
    private var planeRotation = 0f   // degrees clockwise, only while the wreck tumbles
    private var planePhaseTime = 0f
    private var planeBoardStartX = 0f
    private var planeClimb = false
    private var planeDive = false
    private var propellerPhase = 0f
    private var nextBuildingIn = 0f
    private var lassoAtPlane = false
    private var dropStartX = 0f
    private var dropStartY = 0f
    /** Height of the riderless horse hopping the fences on its own while the cowboy flies. */
    private var riderlessHop = 0f

    private var tractorPhase = TractorPhase.NONE
    private var nextTractorIn = TRACTOR_FIRST_SECONDS
    private var tractorX = 0f           // left edge of the tractor
    private var tractorPhaseTime = 0f
    private var tractorBoardStartX = 0f
    private var tractorBoardStartHeight = 0f
    /** Height of the horse's hooves while it stands on (or hops onto / off) the tractor. */
    private var tractorLift = 0f
    private var tractorWheelPhase = 0f
    private var tractorPartsLost = 0
    private var tractorWrecked = false
    private var tractorRotation = 0f
    private var tractorFenceIn = 0f
    /** The wreck is still sliding off screen after the ride is over. */
    private var tractorWreckLeaving = false
    private var lassoAtTractor = false
    /** Bonus points already applied to the highscore markers, in units; trails [bonusPoints]. */
    private var markerBonus = 0f
    private var announcementKind = RodeoAnnouncement.CRASH_PILOT
    private var announcementTime = 0f

    /** Banner currently shown over the track, if any. */
    val announcement: RodeoAnnouncement? get() = announcementKind.takeIf { announcementTime > 0f }
    /** How far the ground has scrolled; runs away from [distance] while the tractor races. */
    private var groundScroll = 0f

    /** The cowboy is in (or on his way into / out of) the plane: the horse runs on without him. */
    val isFlying: Boolean get() = planePhase == PlanePhase.BOARDING || planePhase == PlanePhase.FLYING || planePhase == PlanePhase.CRASHING

    /** Horse and cowboy are on (or hopping onto / off) the lawn tractor. */
    private val isOnTractor: Boolean
        get() = tractorPhase == TractorPhase.BOARDING || tractorPhase == TractorPhase.RIDING || tractorPhase == TractorPhase.UNLOADING

    /** What the speedometer shows: the horse's real pace, or the tractor's (slightly exaggerated) one. */
    val speedKmh: Int
        get() = when {
            tractorPhase == TractorPhase.RIDING -> TRACTOR_SPEED_KMH
            fallPhase != FallPhase.RIDING -> 0
            else -> (speed * (if (stumble > 0f) STUMBLE_SPEED_FACTOR else 1f) * KMH_PER_UNIT_PER_SECOND).roundToInt()
        }

    val score: Int get() = (distance / UNITS_PER_POINT).toInt() + bonusPoints

    /** The cowboy lies in the dirt or stands next to his horse - the lasso is his way back up. */
    val isOnFoot: Boolean get() = fallPhase == FallPhase.THROWN || fallPhase == FallPhase.DOWN || fallPhase == FallPhase.ON_FOOT

    /** True once the chasing pack reached the horse. */
    val isCaught: Boolean get() = chaseGap <= 0f

    /** Starts a fresh run. */
    fun reset() {
        fences.clear()
        snails.clear()
        horseshoes.clear()
        buildings.clear()
        horseHeight = 0f
        verticalVelocity = 0f
        speed = START_SPEED
        distance = 0f
        // Placed on the first frame, once the world width is known for sure
        nextFenceIn = -1f
        nextRunnerIn = 0f
        gaitPhase = 0f
        elapsed = 0f
        runTimeSeconds = 0f
        riderLean = 0f
        jumpHeld = false
        airTime = 0f
        fenceCount = 0
        stumble = 0f
        chaseGap = CHASE_GAP_MAX
        packPhase = 0f
        bonusPoints = 0
        dustTime = 0f
        splashTime = 0f
        superJumpPhase = SuperJumpPhase.NONE
        superJumpQueued = false
        superJumpLastFence = null
        lassoTime = -1f
        lassoCooldown = 0f
        lassoTarget = null
        fallPhase = FallPhase.RIDING
        lastFallAt = -FALL_COOLDOWN
        jumpPeak = 0f
        snailsCaught = 0
        luckyCharms = 0
        sparkleTime = 0f
        planePhase = PlanePhase.NONE
        nextPlaneIn = PLANE_FIRST_SECONDS
        planeClimb = false
        planeDive = false
        lassoAtPlane = false
        riderlessHop = 0f
        tractorPhase = TractorPhase.NONE
        nextTractorIn = TRACTOR_FIRST_SECONDS
        tractorLift = 0f
        tractorPartsLost = 0
        tractorWrecked = false
        tractorRotation = 0f
        tractorWreckLeaving = false
        lassoAtTractor = false
        groundScroll = 0f
        debris.clear()
        markerBonus = 0f
        announcementTime = 0f
    }

    /**
     * Continues a saved run: progress and pace come back, the track ahead starts empty again (like
     * at the start of a run), since fences and snails are not persisted.
     */
    fun restore(snapshot: SchneaggRodeoSnapshot) {
        reset()
        speed = snapshot.speed
        distance = snapshot.distance
        elapsed = snapshot.elapsedSeconds
        runTimeSeconds = snapshot.runTimeSeconds
        chaseGap = snapshot.chaseGap
        bonusPoints = snapshot.bonusPoints
        markerBonus = bonusPoints * UNITS_PER_POINT
        fenceCount = snapshot.fenceCount
        snailsCaught = snapshot.snailsCaught
        luckyCharms = snapshot.luckyCharms
        nextRunnerIn = snapshot.nextRunnerIn
    }

    fun toSnapshot(): SchneaggRodeoSnapshot = SchneaggRodeoSnapshot(
        speed = speed,
        distance = distance,
        elapsedSeconds = elapsed,
        runTimeSeconds = runTimeSeconds,
        chaseGap = chaseGap,
        bonusPoints = bonusPoints,
        fenceCount = fenceCount,
        snailsCaught = snailsCaught,
        superJumpCharges = superJumpCharges,
        luckyCharms = luckyCharms,
        nextRunnerIn = nextRunnerIn,
    )

    /** Places a fence at [x]; [gapAfter] is the free space to the next fence (for the crawler placement). */
    private fun addFence(x: Float, gapAfter: Float): Fence {
        val oxer = speed >= OXER_MIN_SPEED && Random.nextFloat() < 0.3f
        val progress = min(1f, elapsed / FENCE_GROWTH_SECONDS)
        val minCm = MIN_FENCE_CM + ((FINAL_MIN_FENCE_CM - MIN_FENCE_CM) * progress).toInt()
        val maxCm = START_MAX_FENCE_CM + ((MAX_FENCE_CM - START_MAX_FENCE_CM) * progress).toInt()
        val heightCm = Random.nextInt(minCm / 10, maxCm / 10 + 1) * 10
        val fence = Fence(
            x = x,
            width = if (oxer) 18f else 10f,
            heightCm = heightCm,
            colorOffset = fenceCount++
        )
        fences.add(fence)

        // Crawlers either sit on the new fence or halfway to the next one, so they never overlap poles
        val roll = Random.nextFloat()
        if (roll < SNAIL_ON_FENCE_CHANCE) {
            snails.add(Snail(x = fence.x + fence.width / 2f, kind = SnailKind.CRAWLER, onFence = fence))
        } else if (roll < SNAIL_ON_FENCE_CHANCE + SNAIL_BETWEEN_FENCES_CHANCE) {
            snails.add(Snail(x = fence.x + fence.width + gapAfter / 2f, kind = SnailKind.CRAWLER))
        }

        if (Random.nextFloat() < HORSESHOE_CHANCE) {
            val height = HORSESHOE_MIN_HEIGHT + Random.nextFloat() * (HORSESHOE_MAX_HEIGHT - HORSESHOE_MIN_HEIGHT)
            horseshoes.add(Horseshoe(x = fence.x + fence.width + gapAfter / 2f, height = height))
        }
        return fence
    }

    // Gap scales with speed so the reaction time stays roughly constant.
    private fun randomFenceGap() = speed * (0.9f + Random.nextFloat() * 0.9f)

    private fun spawnFence() {
        val gap = randomFenceGap()
        val fence = addFence(x = worldWidth, gapAfter = gap)
        nextFenceIn = fence.width + gap
    }

    /**
     * Makes sure exactly the right fences lie ahead for the super jump: everything not yet on screen
     * is replaced by a tight row, so the leap always clears [SUPER_JUMP_FENCES] fences and lands in
     * a normal gap after the last one. Returns the last fence of the row.
     */
    private fun prepareSuperJumpRow(): Fence? {
        val offscreen = fences.filter { it.x > worldWidth }.toSet()
        fences.removeAll(offscreen)
        snails.removeAll { snail ->
            snail.onFence?.let { it in offscreen } == true || (snail.kind == SnailKind.CRAWLER && snail.x > worldWidth)
        }
        horseshoes.removeAll { it.x > worldWidth }

        val hitLeft = HORSE_X + HITBOX_LEFT
        val ahead = fences.filter { it.x + it.width > hitLeft }.sortedBy { it.x }
        if (ahead.size >= SUPER_JUMP_FENCES) return ahead[SUPER_JUMP_FENCES - 1]

        val rowGap = speed * SUPER_JUMP_ROW_GAP_SECONDS
        var last = ahead.lastOrNull()
        repeat(SUPER_JUMP_FENCES - ahead.size) {
            // Never pop a fence into view - the row continues right of the visible area
            val x = max(worldWidth, last?.let { it.x + it.width + rowGap } ?: worldWidth)
            last = addFence(x = x, gapAfter = rowGap)
        }
        // Normal spawning resumes with a regular gap after the row
        val rowEnd = last?.let { it.x + it.width } ?: worldWidth
        nextFenceIn = rowEnd + randomFenceGap() - worldWidth
        return last
    }

    private fun startSuperJumpWindup() {
        superJumpQueued = false
        superJumpPhase = SuperJumpPhase.WINDUP
        superJumpWindup = 0f
        stumble = 0f
        verticalVelocity = 0f
        superJumpLastFence = prepareSuperJumpRow()
        snailsCaught -= SNAILS_PER_SUPER_JUMP
    }

    private fun launchSuperJump() {
        val hitLeft = HORSE_X + HITBOX_LEFT
        val lastFence = superJumpLastFence
        val landingX = lastFence?.let { it.x + it.width } ?: (hitLeft + speed)
        superJumpDistance = max(speed, landingX - hitLeft + SUPER_JUMP_LANDING_MARGIN)
        superJumpTravelled = 0f
        // Slow motion, but never so slow that the flight drags on
        superJumpTimeScale = max(
            SUPER_JUMP_TIME_SCALE,
            superJumpDistance / (speed * SUPER_JUMP_MAX_SECONDS)
        )
        superJumpPhase = SuperJumpPhase.FLIGHT
    }

    fun superJumpPressed() {
        if (superJumpCharges <= 0 || fallPhase != FallPhase.RIDING || isFlying || isOnTractor) return
        if (superJumpPhase != SuperJumpPhase.NONE || superJumpQueued) return
        // In the air it fires on landing, on the ground right away
        if (horseHeight > 0f) superJumpQueued = true else startSuperJumpWindup()
    }

    private fun crash(penalty: Float) {
        if (luckyCharms > 0) {
            // The lucky charm takes the hit: the poles still fall, but the horse keeps its stride
            luckyCharms--
            sparkle(HORSE_X + 16f, horseHeight + 14f)
            return
        }
        stumble = STUMBLE_SECONDS
        chaseGap -= penalty
        dustTime = DUST_SECONDS
        dustX = HORSE_X + 20f // front hooves
    }

    private fun sparkle(x: Float, y: Float) {
        sparkleTime = SPARKLE_SECONDS
        sparkleX = x
        sparkleY = y
    }

    fun jumpPressed() {
        if (jumpHeld) return // key repeat while holding
        jumpHeld = true
        if (isFlying) {
            planeClimb = true
            return
        }
        if (fallPhase == FallPhase.RIDING && horseHeight <= 0f && superJumpPhase == SuperJumpPhase.NONE && !isOnTractor) {
            verticalVelocity = JUMP_VELOCITY
            airTime = 0f
            jumpPeak = 0f
        }
    }

    fun jumpReleased() {
        jumpHeld = false
        planeClimb = false
    }

    /** Steers the plane down while held; does nothing on the horse. */
    fun divePressed() {
        if (isFlying) planeDive = true
    }

    fun diveReleased() {
        planeDive = false
    }

    fun lassoPressed() {
        if (lassoTime >= 0f || lassoCooldown > 0f || isFlying || isOnTractor) return
        if (fallPhase != FallPhase.RIDING) {
            // On foot the lasso is for the horse only, and only once he is back on his feet
            if (fallPhase == FallPhase.ON_FOOT) {
                lassoTime = 0f
                lassoCooldown = LASSO_COOLDOWN
                lassoResolved = false
            }
            return
        }
        val handX = HORSE_X + HAND_X
        val effectiveSpeed = speed * (if (stumble > 0f) STUMBLE_SPEED_FACTOR else 1f)
        val timeToCatch = LASSO_DURATION / 2f
        lassoTime = 0f
        lassoCooldown = LASSO_COOLDOWN
        lassoResolved = false
        lassoAtTractor = false
        // A passing plane beats any snail
        lassoAtPlane = planePhase == PlanePhase.APPROACH &&
                (planeX + PLANE_LADDER_X - PLANE_PASS_SPEED * timeToCatch) in (handX - 2f)..(handX + LASSO_RANGE)
        if (lassoAtPlane) {
            lassoTarget = null
            return
        }
        // So does a passing tractor
        lassoAtTractor = tractorPhase == TractorPhase.APPROACH && superJumpPhase == SuperJumpPhase.NONE &&
                (tractorX + TRACTOR_HITCH_X - TRACTOR_PASS_SPEED * timeToCatch) in (handX - 2f)..(handX + LASSO_RANGE)
        if (lassoAtTractor) {
            lassoTarget = null
            return
        }
        // Targets are picked by where they will be when the loop arrives - at full speed a snail
        // scrolls ~40 u during the throw, so its current position would already be behind the hand.
        lassoTarget = snails
            .filter { it.state == SnailState.ACTIVE }
            .map { it to predictedSnailX(it, effectiveSpeed, timeToCatch) }
            .filter { (_, x) -> x > handX - 2f && x < handX + LASSO_RANGE }
            .minByOrNull { (_, x) -> x }
            ?.first
    }

    /** Advances the world by one frame of [frameSeconds] real time. */
    fun step(frameSeconds: Float) {
        if (worldWidth <= 0f || isCaught) return
        val realDt = min(frameSeconds, MAX_FRAME_SECONDS)
        runTimeSeconds += realDt
        // Everything in the world runs on dt, which slows down during the super jump
        val timeScale = when (superJumpPhase) {
            SuperJumpPhase.NONE -> 1f
            SuperJumpPhase.WINDUP -> SUPER_JUMP_WINDUP_TIME_SCALE
            SuperJumpPhase.FLIGHT -> superJumpTimeScale
        }
        val dt = realDt * timeScale
        dustTime = max(0f, dustTime - realDt)
        splashTime = max(0f, splashTime - realDt)
        sparkleTime = max(0f, sparkleTime - realDt)
        announcementTime = max(0f, announcementTime - realDt)

        // Off the horse the world stands still - only the cowboy, his horse and the snails move
        if (fallPhase != FallPhase.RIDING) {
            stepFall(realDt)
            return
        }

        elapsed += dt
        speed = min(MAX_SPEED, speed + ACCELERATION * dt)
        stumble = max(0f, stumble - dt)
        val effectiveSpeed = speed * (if (stumble > 0f) STUMBLE_SPEED_FACTOR else 1f)
        val step = effectiveSpeed * dt
        distance += step
        // On the tractor the world races by, but the points keep coming in at the horse's pace
        val tractorRiding = tractorPhase == TractorPhase.RIDING
        val scroll = if (tractorRiding) TRACTOR_SCROLL_SPEED * dt else step
        groundScroll += scroll
        // Standing on the tractor the horse keeps its legs still
        if (!isOnTractor) gaitPhase += step * 0.12f
        packPhase += dt * 10f

        // Vertical movement
        when (superJumpPhase) {
            SuperJumpPhase.WINDUP -> {
                superJumpWindup += realDt
                if (superJumpWindup >= SUPER_JUMP_WINDUP_SECONDS) launchSuperJump()
            }
            SuperJumpPhase.FLIGHT -> {
                // Height follows the planned arc over the distance, so the landing is always
                // right behind the last fence of the row - no matter the speed.
                superJumpTravelled += step
                val progress = min(1f, superJumpTravelled / superJumpDistance)
                horseHeight = SUPER_JUMP_HEIGHT * sin(PI.toFloat() * progress).coerceAtLeast(0f).pow(SUPER_JUMP_ARC_SHAPE)
                if (progress >= 1f) {
                    horseHeight = 0f
                    superJumpPhase = SuperJumpPhase.NONE
                    superJumpLastFence = null
                    dustTime = DUST_SECONDS
                    dustX = HORSE_X + 14f
                    splashTime = SPLASH_SECONDS
                }
            }
            SuperJumpPhase.NONE -> if (horseHeight > 0f || verticalVelocity > 0f) {
                val boosted = !isFlying && jumpHeld && verticalVelocity > 0f && airTime < MAX_HOLD_SECONDS
                airTime += dt
                verticalVelocity -= GRAVITY * (if (boosted) HOLD_GRAVITY_FACTOR else 1f) * dt
                horseHeight += verticalVelocity * dt
                jumpPeak = max(jumpPeak, horseHeight)
                if (horseHeight <= 0f) {
                    horseHeight = 0f
                    verticalVelocity = 0f
                    splashTime = SPLASH_SECONDS
                    if (!isFlying && shouldThrowCowboy()) {
                        startFall()
                        return
                    }
                }
            }
        }
        if (superJumpQueued && horseHeight <= 0f && superJumpPhase == SuperJumpPhase.NONE) {
            startSuperJumpWindup()
        }
        riderlessHop = if (isFlying && planePhase != PlanePhase.BOARDING) riderlessHopHeight() else 0f

        // Rider eases into the forward seat on takeoff and back upright after landing
        val leanTarget = if (horseHeight > 0f || tractorRiding) 1f else 0f
        riderLean += (leanTarget - riderLean) * min(1f, dt * RIDER_LEAN_RESPONSE)

        // Fences. The first one of a run (or of a restored run, whose track starts empty) comes in
        // at 60 % of the visible width.
        if (nextFenceIn < 0f && fences.isEmpty()) nextFenceIn = worldWidth * 0.6f
        fences.forEach { it.x -= scroll }
        fences.removeAll { it.x + it.width < 0f }
        // No new fences while the cowboy flies (the skyline takes over) or rides the tractor (it
        // places its own)
        if (!isFlying && !isOnTractor) {
            nextFenceIn -= step
            if (nextFenceIn <= 0f) spawnFence()
        }

        // Runners only join once the rider had some time to warm up
        if (elapsed >= RUNNER_START_SECONDS) {
            nextRunnerIn -= dt
            if (nextRunnerIn <= 0f) {
                snails.add(Snail(x = worldWidth + SNAIL_SIZE, kind = SnailKind.RUNNER))
                nextRunnerIn = 5f + Random.nextFloat() * 7f
            }
        }

        val hitLeft = HORSE_X + HITBOX_LEFT
        val hitRight = HORSE_X + HITBOX_RIGHT
        val hitBottom = horseHeight + HITBOX_BOTTOM
        // The super jump sails over everything, including the fences it passes low at takeoff. The
        // riderless horse under the plane hops everything on its own.
        val invulnerable = superJumpPhase != SuperJumpPhase.NONE || isFlying || isOnTractor

        fences.forEach { fence ->
            if (!invulnerable && !fence.knocked && fence.x < hitRight && fence.x + fence.width > hitLeft && hitBottom < fence.top) {
                fence.knocked = true
                crash(FENCE_CRASH_PENALTY)
            }
        }

        // Snails
        snails.forEach { snail ->
            when (snail.state) {
                SnailState.ACTIVE -> {
                    val fence = snail.onFence
                    if (fence != null && fence.knocked) snail.onFence = null // falls down with the poles
                    val seat = snail.onFence
                    if (seat != null) {
                        snail.x = seat.x + seat.width / 2f
                        snail.height = seat.top
                    } else {
                        val ownSpeed = if (snail.kind == SnailKind.RUNNER) RUNNER_SPEED else CRAWLER_SPEED
                        snail.x -= scroll + ownSpeed * dt
                        snail.height = if (snail.kind == SnailKind.RUNNER) hopHeight(snail.x, fences) else 0f
                    }

                    val runnerHit = !invulnerable && snail.kind == SnailKind.RUNNER &&
                            snail.x - SNAIL_HALF_WIDTH < hitRight &&
                            snail.x + SNAIL_HALF_WIDTH > hitLeft &&
                            hitBottom < snail.height + SNAIL_BODY_HEIGHT
                    if (runnerHit) {
                        snail.state = SnailState.KNOCKED
                        snail.verticalVelocity = 45f
                        crash(RUNNER_CRASH_PENALTY)
                    }
                }
                SnailState.KNOCKED -> {
                    snail.x -= scroll * 0.5f
                    snail.verticalVelocity -= GRAVITY * dt
                    snail.height += snail.verticalVelocity * dt
                    snail.spin += 720f * dt
                }
                SnailState.LASSOED -> Unit // follows the lasso tip below
            }
        }
        snails.removeAll { it.x < -SNAIL_SIZE || it.height < -WORLD_HEIGHT_UNITS }

        // Lucky horseshoes scroll with the ground and are picked up by the horse passing through
        horseshoes.forEach { it.x -= scroll }
        val bodyHeight = horseHeight + tractorLift
        horseshoes.removeAll { shoe ->
            val collected = shoe.x > hitLeft && shoe.x < hitRight + 4f &&
                    shoe.height in (bodyHeight + HORSESHOE_REACH_BOTTOM)..(bodyHeight + HORSESHOE_REACH_TOP)
            if (collected) {
                luckyCharms = min(MAX_LUCKY_CHARMS, luckyCharms + 1)
                bonusPoints += HORSESHOE_POINTS
                sparkle(shoe.x, shoe.height)
            }
            collected || shoe.x < -SNAIL_SIZE
        }

        stepTractor(dt, realDt, scroll)
        stepPlane(dt, realDt, scroll)

        // Lasso: extends to the target (or straight ahead) and back over LASSO_DURATION
        lassoCooldown = max(0f, lassoCooldown - dt)
        if (lassoTime >= 0f) {
            lassoTime += dt
            val progress = min(1f, lassoTime / LASSO_DURATION)
            val handX = HORSE_X + HAND_X
            val handY = horseHeight + HAND_Y
            val target = lassoTarget

            if (!lassoResolved && lassoAtTractor) {
                lassoAimX = tractorX + TRACTOR_HITCH_X
                lassoAimY = TRACTOR_HITCH_Y
                if (progress >= 0.5f) {
                    lassoResolved = true
                    lassoAtTractor = false
                    val hitchX = tractorX + TRACTOR_HITCH_X
                    if (tractorPhase == TractorPhase.APPROACH &&
                        hitchX in (handX - LASSO_CATCH_TOLERANCE)..(handX + LASSO_RANGE + LASSO_CATCH_TOLERANCE)
                    ) {
                        startTractorBoarding()
                    }
                }
            } else if (!lassoResolved && lassoAtPlane) {
                lassoAimX = planeX + PLANE_LADDER_X
                lassoAimY = planeY - PLANE_LADDER_LENGTH
                if (progress >= 0.5f) {
                    lassoResolved = true
                    lassoAtPlane = false
                    val ladderX = planeX + PLANE_LADDER_X
                    if (planePhase == PlanePhase.APPROACH &&
                        ladderX in (handX - LASSO_CATCH_TOLERANCE)..(handX + LASSO_RANGE + LASSO_CATCH_TOLERANCE)
                    ) {
                        startBoarding()
                    }
                }
            } else if (!lassoResolved) {
                if (target != null && target.state == SnailState.ACTIVE) {
                    lassoAimX = target.x
                    lassoAimY = target.height + SNAIL_BODY_HEIGHT / 2f
                } else {
                    lassoAimX = handX + LASSO_RANGE
                    lassoAimY = max(2f, handY - 12f)
                }
                if (progress >= 0.5f) {
                    lassoResolved = true
                    if (target != null && target.state == SnailState.ACTIVE &&
                        target.x in (handX - LASSO_CATCH_TOLERANCE)..(handX + LASSO_RANGE + LASSO_CATCH_TOLERANCE)
                    ) {
                        target.state = SnailState.LASSOED
                        target.onFence = null
                        snailsCaught++
                        bonusPoints += CATCH_POINTS
                        chaseGap = min(CHASE_GAP_MAX, chaseGap + CATCH_GAP_BONUS)
                    } else {
                        lassoTarget = null
                    }
                }
            }

            val extension = sin(PI.toFloat() * progress)
            lassoTipX = handX + (lassoAimX - handX) * extension
            lassoTipY = handY + (lassoAimY - handY) * extension
            lassoTarget?.takeIf { it.state == SnailState.LASSOED }?.let { caught ->
                caught.x = lassoTipX
                caught.height = lassoTipY - SNAIL_BODY_HEIGHT / 2f
            }

            if (progress >= 1f || isFlying || isOnTractor) {
                lassoTarget?.let { caught -> if (caught.state == SnailState.LASSOED) snails.remove(caught) }
                lassoTarget = null
                lassoTime = -1f
            }
        }

        chaseGap = min(CHASE_GAP_MAX, chaseGap + CHASE_GAP_REGAIN * dt)
        if (chaseGap <= 0f) chaseGap = 0f
        markerBonus = min(bonusPoints * UNITS_PER_POINT, markerBonus + MARKER_CATCH_UP_SPEED * dt)
    }

    private fun announce(kind: RodeoAnnouncement) {
        announcementKind = kind
        announcementTime = ANNOUNCEMENT_SECONDS
    }

    /** The riderless horse arcs over whatever fence it is passing, like the running snails do. */
    private fun riderlessHopHeight(): Float {
        val center = HORSE_X + 14f
        var height = 0f
        fences.forEach { fence ->
            val halfSpan = fence.width / 2f + 14f
            val distance = abs(center - (fence.x + fence.width / 2f))
            if (distance < halfSpan) {
                val relative = distance / halfSpan
                height = max(height, (fence.top + 3f) * (1f - relative * relative))
            }
        }
        return height
    }

    private fun startBoarding() {
        announce(RodeoAnnouncement.CRASH_PILOT)
        planePhase = PlanePhase.BOARDING
        planePhaseTime = 0f
        planeBoardStartX = planeX
        planeVy = 0f
        planeClimb = false
        planeDive = false
        jumpHeld = false
        lassoTime = -1f
        // Buildings start right of the screen; the fences on screen scroll away under the horse
        nextBuildingIn = 0f
    }

    private fun crashPlane() {
        planePhase = PlanePhase.CRASHING
        planePhaseTime = 0f
        planeRotation = 0f
        planeClimb = false
        planeDive = false
        dropStartX = planeX + PLANE_PILOT_X
        dropStartY = planeY + PLANE_PILOT_Y
        sparkle(planeX + PLANE_HIT_RIGHT, planeY + PLANE_HIT_TOP / 2f)
    }

    private fun finishPlaneRide() {
        planePhase = PlanePhase.NONE
        nextPlaneIn = PLANE_INTERVAL_MIN + Random.nextFloat() * PLANE_INTERVAL_RANDOM
        riderlessHop = 0f
        jumpPeak = 0f
        splashTime = SPLASH_SECONDS
        // Fences come back with a normal gap after the skyline
        nextFenceIn = max(nextFenceIn, randomFenceGap())
    }

    private fun spawnBuilding() {
        val width = BUILDING_MIN_WIDTH + Random.nextFloat() * (BUILDING_MAX_WIDTH - BUILDING_MIN_WIDTH)
        val height = BUILDING_MIN_HEIGHT + Random.nextFloat() * (BUILDING_MAX_HEIGHT - BUILDING_MIN_HEIGHT)
        val cloudChance = min(CLOUD_MAX_CHANCE, CLOUD_START_CHANCE + CLOUD_CHANCE_PER_SECOND * planePhaseTime)
        val cloudGap = max(CLOUD_MIN_GAP, CLOUD_START_GAP - CLOUD_GAP_SHRINK_PER_SECOND * planePhaseTime) +
                Random.nextFloat() * CLOUD_GAP_RANDOM
        // Only where the cloud reaches into the flyable sky
        val cloudBottom = (height + cloudGap).takeIf {
            Random.nextFloat() < cloudChance && it < PLANE_MAX_HEIGHT + PLANE_HIT_TOP - 2f
        }
        buildings.add(
            Building(x = worldWidth, width = width, height = height, seed = Random.nextInt(1000), cloudBottom = cloudBottom)
        )
        val gap = speed * (0.35f + Random.nextFloat() * 0.35f)
        nextBuildingIn = width + gap
        // Horseshoes float in the gaps, some above the rooftops, some low between the houses
        if (Random.nextFloat() < FLIGHT_HORSESHOE_CHANCE) {
            val shoeHeight = 14f + Random.nextFloat() * (PLANE_MAX_HEIGHT - 14f)
            horseshoes.add(Horseshoe(x = worldWidth + width + gap / 2f, height = shoeHeight))
        }
    }

    /** Plane spawning, passing, boarding, flying and crashing. [step] is the world scroll of this frame. */
    private fun stepPlane(dt: Float, realDt: Float, step: Float) {
        propellerPhase += realDt * 40f

        // The skyline scrolls with the ground and outlives the flight until it leaves the screen
        buildings.forEach { it.x -= step }
        buildings.removeAll { it.x + it.width < 0f }

        when (planePhase) {
            PlanePhase.NONE -> {
                if (superJumpPhase == SuperJumpPhase.NONE && tractorPhase == TractorPhase.NONE && !tractorWreckLeaving && elapsed > 0f) {
                    nextPlaneIn -= dt
                    if (nextPlaneIn <= 0f) {
                        planePhase = PlanePhase.APPROACH
                        planeX = worldWidth + 5f
                        planeY = PLANE_APPROACH_HEIGHT
                        planeRotation = 0f
                    }
                }
            }
            PlanePhase.APPROACH -> {
                planeX -= PLANE_PASS_SPEED * dt
                if (planeX + PLANE_LENGTH < 0f) {
                    planePhase = PlanePhase.NONE
                    nextPlaneIn = PLANE_INTERVAL_MIN + Random.nextFloat() * PLANE_INTERVAL_RANDOM
                }
            }
            PlanePhase.BOARDING -> {
                planePhaseTime += dt
                val progress = min(1f, planePhaseTime / PLANE_BOARD_SECONDS)
                val eased = progress * progress * (3f - 2f * progress)
                planeX = planeBoardStartX + (PLANE_FLY_X - planeBoardStartX) * eased
                if (progress >= 1f) {
                    planePhase = PlanePhase.FLYING
                    planePhaseTime = 0f
                }
            }
            PlanePhase.FLYING -> {
                planePhaseTime += dt
                val accel = when {
                    planeClimb && !planeDive -> PLANE_CLIMB_ACCEL
                    planeDive && !planeClimb -> -PLANE_DIVE_ACCEL
                    else -> -PLANE_SINK_ACCEL
                }
                planeVy = (planeVy + accel * dt).coerceIn(-PLANE_MAX_SINK, PLANE_MAX_CLIMB)
                planeY += planeVy * dt
                if (planeY >= PLANE_MAX_HEIGHT) {
                    planeY = PLANE_MAX_HEIGHT
                    planeVy = min(0f, planeVy)
                }

                nextBuildingIn -= step
                if (nextBuildingIn <= 0f) spawnBuilding()

                val hitLeft = planeX + PLANE_HIT_LEFT
                val hitRight = planeX + PLANE_HIT_RIGHT
                buildings.forEach { building ->
                    if (!building.passed && building.x + building.width < hitLeft) {
                        building.passed = true
                        bonusPoints += BUILDING_POINTS
                    }
                }
                horseshoes.removeAll { shoe ->
                    val collected = shoe.x > hitLeft - 2f && shoe.x < hitRight + 2f &&
                            shoe.height > planeY - 2f && shoe.height < planeY + PLANE_HIT_TOP + 2f
                    if (collected) {
                        luckyCharms = min(MAX_LUCKY_CHARMS, luckyCharms + 1)
                        bonusPoints += HORSESHOE_POINTS
                        sparkle(shoe.x, shoe.height)
                    }
                    collected
                }

                val hitBuilding = buildings.any { building ->
                    val overlapsBuilding = building.x < hitRight && building.x + building.width > hitLeft
                    val overlapsCloud = building.x - CLOUD_OVERHANG < hitRight &&
                            building.x + building.width + CLOUD_OVERHANG > hitLeft
                    (overlapsBuilding && planeY < building.height) ||
                            (overlapsCloud && building.cloudBottom != null && planeY + PLANE_HIT_TOP > building.cloudBottom)
                }
                if (hitBuilding || planeY <= 0f) {
                    planeY = max(0f, planeY)
                    crashPlane()
                }
            }
            PlanePhase.CRASHING -> {
                planePhaseTime += dt
                // The wreck tumbles down and away behind the horse
                planeX -= step * 0.6f
                planeY -= PLANE_WRECK_FALL_SPEED * dt
                planeRotation += PLANE_WRECK_SPIN * dt
                if (planePhaseTime >= PLANE_DROP_SECONDS) finishPlaneRide()
            }
        }
    }

    private fun startTractorBoarding() {
        announce(RodeoAnnouncement.LAWN_TRACTOR)
        tractorPhase = TractorPhase.BOARDING
        tractorPhaseTime = 0f
        tractorBoardStartX = tractorX
        // A jump in progress ends on the deck instead of the ground
        tractorBoardStartHeight = horseHeight
        horseHeight = 0f
        verticalVelocity = 0f
        stumble = 0f
        superJumpQueued = false
        jumpHeld = false
        lassoTime = -1f
    }

    private fun startTractorUnloading() {
        if (!tractorWrecked) wreckTractor()
        tractorPhase = TractorPhase.UNLOADING
        tractorPhaseTime = 0f
        // Fences come back with a normal gap after the ride
        nextFenceIn = max(nextFenceIn, randomFenceGap())
    }

    private fun finishTractorRide() {
        tractorPhase = TractorPhase.NONE
        tractorLift = 0f
        tractorWreckLeaving = true
        nextTractorIn = TRACTOR_INTERVAL_MIN + Random.nextFloat() * TRACTOR_INTERVAL_RANDOM
        jumpPeak = 0f
        splashTime = SPLASH_SECONDS
    }

    /** Where each part sits on the tractor, relative to its left edge / the ground (see drawTractor). */
    private fun tractorPartAnchor(part: Int): Pair<Float, Float> = when (part) {
        0 -> 36f to 12f   // exhaust pipe
        1 -> 27f to 12f   // steering wheel
        2 -> 34f to 8f    // hood
        3 -> 20f to 1.5f  // mower deck
        4 -> 33f to 2.8f  // front wheel
        else -> 15f to 6f // scrap from the chassis
    }

    /** Flings [part] off the tractor, up and backwards over the screen. */
    private fun throwDebris(part: Int) {
        val (anchorX, anchorY) = tractorPartAnchor(part)
        debris.add(
            Debris(
                x = tractorX + anchorX,
                y = anchorY,
                vx = -(50f + Random.nextFloat() * 70f),
                vy = 40f + Random.nextFloat() * 35f,
                spin = (if (Random.nextBoolean()) 1f else -1f) * (300f + Random.nextFloat() * 500f),
                part = part,
            )
        )
    }

    /** A fence was mowed: one more part flies off, or the tractor is wrecked once none are left. */
    private fun hitTractor(fenceX: Float) {
        dustTime = DUST_SECONDS
        dustX = fenceX
        if (tractorPartsLost < TRACTOR_PARTS) {
            throwDebris(tractorPartsLost)
            tractorPartsLost++
        } else {
            wreckTractor()
        }
    }

    private fun wreckTractor() {
        // Whatever was still attached comes off at once
        while (tractorPartsLost < TRACTOR_PARTS) throwDebris(tractorPartsLost++)
        repeat(WRECK_SCRAP_PIECES) { throwDebris(TRACTOR_PARTS) }
        tractorWrecked = true
        sparkle(tractorX + TRACTOR_LENGTH - 4f, TRACTOR_DECK_HEIGHT)
    }

    /**
     * Spaces the ride's fences so the hit that wrecks the tractor lands shortly before the ride is
     * over. Fences already on their way count as hits too.
     */
    private fun nextTractorFenceDelay(): Float {
        val pending = fences.count { !it.knocked && it.x + it.width > tractorX }
        val hitsLeft = TRACTOR_PARTS + 1 - tractorPartsLost - pending
        if (hitsLeft <= 0) return TRACTOR_RIDE_SECONDS // enough on the way; nothing more this ride
        val timeLeft = TRACTOR_RIDE_SECONDS - TRACTOR_LAST_HIT_MARGIN - tractorPhaseTime
        return max(TRACTOR_MIN_FENCE_SECONDS, timeLeft / hitsLeft)
    }

    /** Tractor spawning, passing, boarding, the ride and dropping the horse off. [scroll] is the world scroll of this frame. */
    private fun stepTractor(dt: Float, realDt: Float, scroll: Float) {
        debris.forEach { piece ->
            piece.vy -= DEBRIS_GRAVITY * realDt
            piece.x += piece.vx * realDt
            piece.y += piece.vy * realDt
            piece.rotation += piece.spin * realDt
        }
        debris.removeAll { it.y < -GROUND_OFFSET_UNITS || it.x < -SNAIL_SIZE }
        tractorWheelPhase += scroll * 0.25f

        when (tractorPhase) {
            TractorPhase.NONE -> {
                if (tractorWreckLeaving) {
                    tractorX -= scroll
                    if (tractorX + TRACTOR_LENGTH < 0f) tractorWreckLeaving = false
                } else if (planePhase == PlanePhase.NONE && superJumpPhase == SuperJumpPhase.NONE && elapsed > 0f) {
                    nextTractorIn -= dt
                    if (nextTractorIn <= 0f) {
                        tractorPhase = TractorPhase.APPROACH
                        tractorX = worldWidth + 5f
                        tractorPartsLost = 0
                        tractorWrecked = false
                        tractorRotation = 0f
                    }
                }
            }
            TractorPhase.APPROACH -> {
                tractorX -= TRACTOR_PASS_SPEED * dt
                if (tractorX + TRACTOR_LENGTH < 0f) {
                    tractorPhase = TractorPhase.NONE
                    nextTractorIn = TRACTOR_INTERVAL_MIN + Random.nextFloat() * TRACTOR_INTERVAL_RANDOM
                }
            }
            TractorPhase.BOARDING -> {
                tractorPhaseTime += dt
                val progress = min(1f, tractorPhaseTime / TRACTOR_BOARD_SECONDS)
                val eased = progress * progress * (3f - 2f * progress)
                tractorX = tractorBoardStartX + (TRACTOR_RIDE_X - tractorBoardStartX) * eased
                tractorLift = tractorBoardStartHeight + (TRACTOR_DECK_HEIGHT - tractorBoardStartHeight) * eased +
                        TRACTOR_HOP * sin(PI.toFloat() * progress)
                if (progress >= 1f) {
                    tractorPhase = TractorPhase.RIDING
                    tractorPhaseTime = 0f
                    tractorLift = TRACTOR_DECK_HEIGHT
                    tractorFenceIn = TRACTOR_FIRST_FENCE_SECONDS
                }
            }
            TractorPhase.RIDING -> {
                tractorPhaseTime += dt
                tractorFenceIn -= dt
                if (tractorFenceIn <= 0f) {
                    addFence(x = worldWidth, gapAfter = TRACTOR_LENGTH)
                    tractorFenceIn = nextTractorFenceDelay()
                }
                // Everything in front of the mower deck is mowed flat; each fence costs a part
                val front = tractorX + TRACTOR_LENGTH
                fences.forEach { fence ->
                    if (!fence.knocked && fence.x < front && fence.x + fence.width > tractorX) {
                        fence.knocked = true
                        if (!tractorWrecked) hitTractor(fence.x)
                    }
                }
                snails.forEach { snail ->
                    if (snail.state == SnailState.ACTIVE && snail.x < front && snail.x > tractorX) {
                        snail.state = SnailState.KNOCKED
                        snail.onFence = null
                        snail.verticalVelocity = 45f
                    }
                }
                if (tractorWrecked || tractorPhaseTime >= TRACTOR_RIDE_SECONDS) startTractorUnloading()
            }
            TractorPhase.UNLOADING -> {
                tractorPhaseTime += dt
                val progress = min(1f, tractorPhaseTime / TRACTOR_UNLOAD_SECONDS)
                val eased = progress * progress * (3f - 2f * progress)
                tractorLift = TRACTOR_DECK_HEIGHT * (1f - eased) + TRACTOR_HOP * sin(PI.toFloat() * progress)
                // The wreck tips over and stays behind on the track
                tractorX -= scroll
                tractorRotation = min(TRACTOR_WRECK_TILT, tractorRotation + TRACTOR_WRECK_TILT_SPEED * dt)
                if (progress >= 1f) finishTractorRide()
            }
        }
    }

    private fun shouldThrowCowboy(): Boolean =
        planePhase == PlanePhase.NONE &&
                !isOnTractor &&
                jumpPeak >= FALL_MIN_JUMP_PEAK &&
                chaseGap >= FALL_MIN_CHASE_GAP &&
                elapsed >= FALL_MIN_ELAPSED &&
                elapsed - lastFallAt >= FALL_COOLDOWN &&
                !superJumpQueued &&
                Random.nextFloat() < FALL_CHANCE

    private fun startFall() {
        fallPhase = FallPhase.THROWN
        fallClock = 0f
        fallPhaseTime = 0f
        lastFallAt = elapsed
        stumble = 0f
        riderLean = 0f
        // A snail dangling in the lasso gets away
        lassoTarget?.takeIf { it.state == SnailState.LASSOED }?.let { snails.remove(it) }
        lassoTarget = null
        lassoTime = -1f
        lassoCooldown = 0f

        cowboyX = HORSE_X + COWBOY_SEAT_X
        cowboyY = COWBOY_SEAT_Y
        cowboyVx = COWBOY_THROW_VX
        cowboyVy = COWBOY_THROW_VY
        cowboyRotation = 0f
        // One and a quarter forward flips over the flight, so he lands exactly on his back
        val flightSeconds = (COWBOY_THROW_VY + sqrt(COWBOY_THROW_VY * COWBOY_THROW_VY + 2f * COWBOY_GRAVITY * COWBOY_SEAT_Y)) / COWBOY_GRAVITY
        cowboySpin = 450f / flightSeconds
        // Wander starts at the horse's riding spot (sin = 0.5 -> offset 0) and in lasso range
        horseWanderPhase = PI.toFloat() / 6f
    }

    /** Offset of the riderless horse from its riding spot. */
    private fun horseOffset(): Float = when (fallPhase) {
        FallPhase.RIDING -> 0f
        FallPhase.REMOUNT -> remountStartOffset * (1f - min(1f, fallPhaseTime / REMOUNT_SECONDS))
        else -> {
            val blend = min(1f, fallClock / HORSE_WANDER_BLEND_SECONDS)
            blend * (HORSE_WANDER_CENTER + HORSE_WANDER_AMPLITUDE * sin(horseWanderPhase))
        }
    }

    private fun cowboyHand(): Pair<Float, Float> = (cowboyX - 2f) to (cowboyY + COWBOY_HAND_Y)

    private fun saddle(): Pair<Float, Float> = (HORSE_X + horseOffset() + COWBOY_SEAT_X) to COWBOY_SEAT_Y

    private fun stepFall(dt: Float) {
        fallClock += dt
        fallPhaseTime += dt
        packPhase += dt * 10f
        gaitPhase += dt * 4f // nervous pacing
        horseWanderPhase += dt * HORSE_WANDER_SPEED
        lassoCooldown = max(0f, lassoCooldown - dt)
        chaseGap = max(0f, chaseGap - FALL_PACK_APPROACH * dt)

        // Snails keep crawling / running on their own; nobody collides while the world stands still
        snails.forEach { snail ->
            when (snail.state) {
                SnailState.ACTIVE -> if (snail.onFence == null) {
                    val ownSpeed = if (snail.kind == SnailKind.RUNNER) RUNNER_SPEED else CRAWLER_SPEED
                    snail.x -= ownSpeed * dt
                    if (snail.kind == SnailKind.RUNNER) snail.height = hopHeight(snail.x, fences)
                }
                SnailState.KNOCKED -> {
                    snail.verticalVelocity -= GRAVITY * dt
                    snail.height += snail.verticalVelocity * dt
                    snail.spin += 720f * dt
                }
                SnailState.LASSOED -> Unit
            }
        }
        snails.removeAll { it.x < -SNAIL_SIZE || it.height < -WORLD_HEIGHT_UNITS }

        when (fallPhase) {
            FallPhase.THROWN -> {
                cowboyVy -= COWBOY_GRAVITY * dt
                cowboyX += cowboyVx * dt
                cowboyY += cowboyVy * dt
                cowboyRotation += cowboySpin * dt
                if (cowboyY <= 0f) {
                    cowboyY = 0f
                    cowboyRotation = 90f // flat on his back
                    fallPhase = FallPhase.DOWN
                    fallPhaseTime = 0f
                    dustTime = DUST_SECONDS
                    dustX = cowboyX
                }
            }
            FallPhase.DOWN -> {
                val getUpStart = COWBOY_DOWN_SECONDS - COWBOY_GET_UP_SECONDS
                val getUp = ((fallPhaseTime - getUpStart) / COWBOY_GET_UP_SECONDS).coerceIn(0f, 1f)
                cowboyRotation = 90f * (1f - getUp * getUp * (3f - 2f * getUp))
                if (fallPhaseTime >= COWBOY_DOWN_SECONDS) {
                    cowboyRotation = 0f
                    fallPhase = FallPhase.ON_FOOT
                    fallPhaseTime = 0f
                }
            }
            FallPhase.ON_FOOT -> stepRemountLasso(dt)
            FallPhase.REMOUNT -> {
                val progress = min(1f, fallPhaseTime / REMOUNT_SECONDS)
                val (seatX, seatY) = saddle()
                cowboyX = remountStartX + (seatX - remountStartX) * progress
                // Ends with his torso on the saddle, where the seated rider takes over
                cowboyY = (seatY - COWBOY_LEG_LENGTH) * progress + REMOUNT_HOP * sin(PI.toFloat() * progress)
                if (progress >= 1f) finishRemount()
            }
            FallPhase.RIDING -> Unit
        }
    }

    /** The lasso thrown back at the horse: catches the saddle if the horse is in range. */
    private fun stepRemountLasso(dt: Float) {
        if (lassoTime < 0f) return
        lassoTime += dt
        val progress = min(1f, lassoTime / LASSO_DURATION)
        val (handX, handY) = cowboyHand()
        val (seatX, seatY) = saddle()
        val inRange = handX - seatX <= REMOUNT_LASSO_RANGE

        if (!lassoResolved) {
            if (inRange) {
                lassoAimX = seatX
                lassoAimY = seatY
            } else {
                // Falls short, straight towards the horse
                lassoAimX = handX - REMOUNT_LASSO_RANGE
                lassoAimY = seatY
            }
            if (progress >= 0.5f) {
                lassoResolved = true
                if (inRange) {
                    lassoTime = -1f
                    remountStartX = cowboyX
                    remountStartOffset = horseOffset()
                    fallPhase = FallPhase.REMOUNT
                    fallPhaseTime = 0f
                    return
                }
            }
        }

        val extension = sin(PI.toFloat() * progress)
        lassoTipX = handX + (lassoAimX - handX) * extension
        lassoTipY = handY + (lassoAimY - handY) * extension
        if (progress >= 1f) lassoTime = -1f
    }

    private fun finishRemount() {
        fallPhase = FallPhase.RIDING
        cowboyRotation = 0f
        lassoTime = -1f
        lassoCooldown = 0f
        jumpPeak = 0f
        speed = max(START_SPEED, speed * REMOUNT_SPEED_FACTOR)
        splashTime = SPLASH_SECONDS
    }

    /**
     * Immutable render model of the current world. [ghosts] are the all-time highscores, sorted by
     * score; each one becomes a marker post that reaches the horse exactly when the run's score
     * reaches the entry's score.
     */
    fun toFrame(ghosts: List<RodeoGhostUi>): SchneaggRodeoFrame {
        val fenceUis = fences.map { fence ->
            RodeoFenceUi(
                x = fence.x,
                width = fence.width,
                heightCm = fence.heightCm,
                top = fence.top,
                colorOffset = fence.colorOffset,
                knocked = fence.knocked,
                poleHeights = fence.poleHeights,
            )
        }

        val snailUis = snails.mapNotNull { snail ->
            when (snail.state) {
                SnailState.ACTIVE -> {
                    val wobble = if (snail.kind == SnailKind.RUNNER) {
                        sin(packPhase + snail.phase) * 8f
                    } else {
                        sin(packPhase * 0.3f + snail.phase) * 3f
                    }
                    RodeoSnailUi(snail.x, snail.height, facingLeft = true, tiltDeg = wobble)
                }
                SnailState.KNOCKED -> RodeoSnailUi(snail.x, snail.height, facingLeft = true, tiltDeg = snail.spin)
                SnailState.LASSOED -> null // drawn on top of the rider together with the lasso
            }
        }

        // Chasing pack, hopping along behind the horse (off screen while the gap is large)
        val pack = (0 until PACK_SIZE).mapNotNull { index ->
            val x = HORSE_X + HITBOX_LEFT - SNAIL_HALF_WIDTH - chaseGap - index * PACK_SPACING
            if (x > -SNAIL_SIZE) {
                val hop = abs(sin(packPhase + index * 1.3f)) * 1.5f
                val height = max(hop, hopHeight(x, fences))
                RodeoSnailUi(x, height, facingLeft = false, tiltDeg = sin(packPhase + index) * 6f)
            } else {
                null
            }
        }

        val onFootLasso = when {
            fallPhase == FallPhase.REMOUNT -> {
                // Rope stays taut between his hand and the saddle while he swings up
                val (handX, handY) = cowboyHand()
                val (seatX, seatY) = saddle()
                RodeoLassoUi(handX = handX, handY = handY, tipX = seatX, tipY = seatY, caught = null)
            }
            fallPhase != FallPhase.RIDING && lassoTime >= 0f -> {
                val (handX, handY) = cowboyHand()
                RodeoLassoUi(handX = handX, handY = handY, tipX = lassoTipX, tipY = lassoTipY, caught = null)
            }
            else -> null
        }

        val lasso = if (fallPhase != FallPhase.RIDING) {
            onFootLasso
        } else if (lassoTime >= 0f) {
            RodeoLassoUi(
                handX = HORSE_X + HAND_X,
                handY = horseHeight + HAND_Y,
                tipX = lassoTipX,
                tipY = lassoTipY,
                caught = lassoTarget?.takeIf { it.state == SnailState.LASSOED }?.let { caught ->
                    RodeoSnailUi(caught.x, caught.height, facingLeft = true, tiltDeg = -20f)
                },
            )
        } else {
            null
        }

        // A marker's post stands where the horse's nose will be once the score reaches the entry.
        // Bonus points add score without distance, so markers move closer by the bonus (smoothly).
        val effectiveDistance = distance + markerBonus
        val markers = ghosts.mapIndexedNotNull { index, ghost ->
            val x = HORSE_X + HITBOX_RIGHT + ghost.score * UNITS_PER_POINT - effectiveDistance
            if (x > -MARKER_VISIBLE_MARGIN && x < worldWidth + MARKER_VISIBLE_MARGIN) {
                RodeoMarkerUi(
                    x = x,
                    username = ghost.username,
                    score = ghost.score,
                    isOwn = ghost.isOwn,
                    // Neighbouring highscores alternate between a tall and a short post
                    staggered = index % 2 == 1,
                )
            } else {
                null
            }
        }

        val shaking = tractorPhase == TractorPhase.RIDING
        return SchneaggRodeoFrame(
            distance = groundScroll,
            fences = fenceUis,
            snails = snailUis,
            pack = pack,
            horse = horsePose(),
            lasso = lasso,
            dust = if (dustTime > 0f) RodeoDustUi(x = dustX, progress = 1f - dustTime / DUST_SECONDS) else null,
            splashProgress = if (splashTime > 0f) 1f - splashTime / SPLASH_SECONDS else null,
            markers = markers,
            snailsCaught = snailsCaught,
            cowboy = cowboyOnFoot() ?: cowboyAtPlane(),
            horseshoes = horseshoes.map { shoe ->
                RodeoHorseshoeUi(
                    x = shoe.x,
                    height = shoe.height + sin(packPhase * 0.4f + shoe.phase) * 1.2f,
                    tiltDeg = sin(packPhase * 0.25f + shoe.phase) * 12f,
                )
            },
            luckyCharms = luckyCharms,
            plane = planeUi(),
            buildings = buildings.map { RodeoBuildingUi(
                    x = it.x,
                    width = it.width,
                    height = it.height,
                    seed = it.seed,
                    cloudBottom = it.cloudBottom,
                    cloudOverhang = CLOUD_OVERHANG,
                ) },
            sparkle = if (sparkleTime > 0f) {
                RodeoSparkleUi(x = sparkleX, y = sparkleY, progress = 1f - sparkleTime / SPARKLE_SECONDS)
            } else null,
            tractor = tractorUi(),
            debris = debris.map { RodeoDebrisUi(x = it.x, y = it.y, rotation = it.rotation, part = it.part) },
            speedBlur = shaking,
            shakeX = if (shaking) TRACTOR_SHAKE * sin(runTimeSeconds * 97f) else 0f,
            shakeY = if (shaking) TRACTOR_SHAKE * cos(runTimeSeconds * 131f) else 0f,
        )
    }

    private fun tractorUi(): RodeoTractorUi? {
        if (tractorPhase == TractorPhase.NONE && !tractorWreckLeaving) return null
        return RodeoTractorUi(
            x = tractorX,
            rotation = tractorRotation,
            wheelPhase = tractorWheelPhase,
            partsLost = tractorPartsLost,
            wrecked = tractorWrecked,
            exhaust = tractorPhase == TractorPhase.RIDING,
        )
    }

    private fun planeUi(): RodeoPlaneUi? {
        if (planePhase == PlanePhase.NONE) return null
        // The wreck is gone once it fell out of the picture
        if (planePhase == PlanePhase.CRASHING && planeY < -PLANE_LENGTH) return null
        return RodeoPlaneUi(
            x = planeX,
            y = planeY,
            rotation = planeRotation,
            propellerPhase = propellerPhase,
            hasPilot = planePhase == PlanePhase.FLYING,
            ladderDown = planePhase == PlanePhase.APPROACH || planePhase == PlanePhase.BOARDING,
        )
    }

    /** The cowboy climbing the ladder, or dropping back into the saddle after a crash. */
    private fun cowboyAtPlane(): RodeoCowboyUi? = when (planePhase) {
        PlanePhase.BOARDING -> {
            val progress = min(1f, planePhaseTime / PLANE_BOARD_SECONDS)
            val startX = HORSE_X + COWBOY_SEAT_X
            val startY = horseHeight + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH
            val endX = planeX + PLANE_PILOT_X
            val endY = planeY + PLANE_PILOT_Y
            RodeoCowboyUi(
                x = startX + (endX - startX) * progress,
                height = startY + (endY - startY) * progress,
                rotation = 0f,
                facingLeft = false,
                hatLift = 0f,
            )
        }
        PlanePhase.CRASHING -> {
            val progress = min(1f, planePhaseTime / PLANE_DROP_SECONDS)
            val endX = HORSE_X + COWBOY_SEAT_X
            val endY = riderlessHop + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH
            RodeoCowboyUi(
                x = dropStartX + (endX - dropStartX) * progress,
                height = dropStartY + (endY - dropStartY) * progress + PLANE_DROP_HOP * sin(PI.toFloat() * progress),
                rotation = 360f * progress, // a full flip on the way down
                facingLeft = false,
                hatLift = 2f * (1f - progress),
            )
        }
        else -> null
    }

    /** The cowboy while he is off the horse; null while riding. */
    private fun cowboyOnFoot(): RodeoCowboyUi? = when (fallPhase) {
        FallPhase.RIDING -> null
        FallPhase.THROWN -> RodeoCowboyUi(cowboyX, cowboyY, cowboyRotation, facingLeft = false, hatLift = 2f)
        FallPhase.DOWN -> RodeoCowboyUi(cowboyX, cowboyY, cowboyRotation, facingLeft = false, hatLift = 0f)
        // Facing his horse while he throws, turning to face forward half way up into the saddle
        FallPhase.ON_FOOT -> RodeoCowboyUi(cowboyX, cowboyY, 0f, facingLeft = true, hatLift = 0f)
        FallPhase.REMOUNT -> RodeoCowboyUi(
            x = cowboyX,
            height = cowboyY,
            rotation = 0f,
            facingLeft = fallPhaseTime < REMOUNT_SECONDS / 2f,
            hatLift = 0f
        )
    }

    /** Pitch (degrees, positive = nose down) around a pivot in horse grid coordinates, plus leg/hat pose. */
    private fun horsePose(): RodeoHorsePose {
        var pitch = 0f
        var pivotX = 12f
        var pivotY = 12f
        var hindLegScale = 1f
        var glow = 0f
        var frontLegFold = 0f
        var frontLegRaise = 0f
        var hatLift = 0f
        var lean = riderLean
        when (superJumpPhase) {
            SuperJumpPhase.WINDUP -> {
                // Rears up: the hind legs pump up and lift the whole horse, which tips back around
                // its hind hooves with the front legs pawing the air - a loaded spring.
                val progress = min(1f, superJumpWindup / SUPER_JUMP_WINDUP_SECONDS)
                hindLegScale = 1f + SUPER_JUMP_HIND_LEG_GROWTH * easeOutBack(progress)
                pitch = -SUPER_JUMP_REAR_PITCH * easeOutBack(progress)
                pivotX = 7f
                pivotY = 9f - 9f * hindLegScale // the hind hooves stay planted
                frontLegRaise = min(1f, progress * 2f)
                lean = max(lean, progress) // rider leans into the mane to stay on
                glow = progress * (0.75f + 0.25f * sin(superJumpWindup * 40f))
            }
            SuperJumpPhase.FLIGHT -> {
                val progress = min(1f, superJumpTravelled / superJumpDistance)
                // Legs shrink back over the first quarter of the flight
                hindLegScale = 1f + SUPER_JUMP_HIND_LEG_GROWTH * max(0f, 1f - progress / 0.25f)
                glow = max(0f, 1f - progress / 0.25f)
                // Continues from the rear: nose up while rising, down to land
                val amplitude = if (progress < 0.5f) SUPER_JUMP_REAR_PITCH else SUPER_JUMP_LANDING_PITCH
                pitch = -amplitude * cos(PI.toFloat() * progress)
                pivotX = 7f
                pivotY = 9f - 9f * hindLegScale
            }
            SuperJumpPhase.NONE -> if (tractorPhase == TractorPhase.RIDING) {
                // Rattling along on the deck, hat flapping in the wind
                pitch = TRACTOR_RUMBLE_DEGREES * sin(runTimeSeconds * 70f)
                hatLift = 1f + 0.8f * sin(runTimeSeconds * 45f)
            } else if (fallPhase == FallPhase.THROWN && fallClock < BUCK_SECONDS) {
                // Bucks: hindquarters kick up around the front hooves and launch the cowboy
                pitch = BUCK_MAX_PITCH * sin(PI.toFloat() * fallClock / BUCK_SECONDS)
                pivotX = 19f
                pivotY = 0f
            } else if (stumble > 0f) {
                // Nose dives around the hind hooves, front legs buckle, then it catches itself
                val progress = 1f - stumble / STUMBLE_SECONDS
                val dip = sin(PI.toFloat() * progress)
                val wobble = sin(3f * PI.toFloat() * progress) * (1f - progress) * 0.25f
                pitch = STUMBLE_MAX_PITCH * (dip + wobble)
                pivotX = 7f
                pivotY = 0f
                frontLegFold = dip
                hatLift = 3f * dip
            } else if (horseHeight > 0f) {
                // Normal jump: tilts with the vertical speed, faded in and out near the ground so
                // takeoff and landing don't snap
                val tilt = (verticalVelocity / JUMP_VELOCITY).coerceIn(-1f, 1f)
                val blend = min(1f, horseHeight / JUMP_PITCH_BLEND_HEIGHT)
                pitch = -JUMP_MAX_PITCH * tilt * blend
            } else if (riderlessHop > 0f) {
                // Riderless hop over a fence under the plane: level
                pitch = 0f
            } else {
                pitch = GALLOP_ROCK_DEGREES * sin(gaitPhase + 0.8f)
            }
        }
        return RodeoHorsePose(
            // Pumped hind legs lift the horse, so they still reach the ground
            height = horseHeight + riderlessHop + tractorLift + 9f * (hindLegScale - 1f),
            gaitPhase = gaitPhase,
            airborne = horseHeight > 0f || riderlessHop > 0f ||
                    tractorPhase == TractorPhase.BOARDING || tractorPhase == TractorPhase.UNLOADING,
            riderLean = max(lean, frontLegFold * 1.4f),
            pitchDegrees = pitch,
            pivotX = pivotX,
            pivotY = pivotY,
            hindLegScale = hindLegScale,
            frontLegFold = frontLegFold,
            frontLegRaise = frontLegRaise,
            hatLift = hatLift,
            glow = glow,
            offsetX = horseOffset(),
            hasRider = fallPhase == FallPhase.RIDING && !isFlying,
        )
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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

// Lasso: thrown from the rider's hand, homes in on the first snail within range
private const val LASSO_RANGE = 32f
private const val LASSO_DURATION = 0.45f  // out and back
private const val LASSO_COOLDOWN = 0.8f
private const val LASSO_CATCH_TOLERANCE = 6f // extra reach at catch time, absorbs stumbles and speed changes

private const val RIDER_LEAN_RESPONSE = 12f // 1/s, how fast the rider follows the lean target
internal const val HORSE_X = 12f          // left edge of the horse in world units
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
    var superJumpCharges = 0
        private set

    val score: Int get() = (distance / UNITS_PER_POINT).toInt() + bonusPoints

    /** The cowboy lies in the dirt or stands next to his horse - the lasso is his way back up. */
    val isOnFoot: Boolean get() = fallPhase == FallPhase.THROWN || fallPhase == FallPhase.DOWN || fallPhase == FallPhase.ON_FOOT

    /** True once the chasing pack reached the horse. */
    val isCaught: Boolean get() = chaseGap <= 0f

    /** Starts a fresh run. */
    fun reset() {
        fences.clear()
        snails.clear()
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
        superJumpCharges = 0
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
        fenceCount = snapshot.fenceCount
        snailsCaught = snapshot.snailsCaught
        superJumpCharges = snapshot.superJumpCharges
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
        superJumpCharges--
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
        if (superJumpCharges <= 0 || fallPhase != FallPhase.RIDING) return
        if (superJumpPhase != SuperJumpPhase.NONE || superJumpQueued) return
        // In the air it fires on landing, on the ground right away
        if (horseHeight > 0f) superJumpQueued = true else startSuperJumpWindup()
    }

    private fun crash(penalty: Float) {
        stumble = STUMBLE_SECONDS
        chaseGap -= penalty
        dustTime = DUST_SECONDS
        dustX = HORSE_X + 20f // front hooves
    }

    fun jumpPressed() {
        if (jumpHeld) return // key repeat while holding
        jumpHeld = true
        if (fallPhase == FallPhase.RIDING && horseHeight <= 0f && superJumpPhase == SuperJumpPhase.NONE) {
            verticalVelocity = JUMP_VELOCITY
            airTime = 0f
            jumpPeak = 0f
        }
    }

    fun jumpReleased() {
        jumpHeld = false
    }

    fun lassoPressed() {
        if (lassoTime >= 0f || lassoCooldown > 0f) return
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
        gaitPhase += step * 0.12f
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
                val boosted = jumpHeld && verticalVelocity > 0f && airTime < MAX_HOLD_SECONDS
                airTime += dt
                verticalVelocity -= GRAVITY * (if (boosted) HOLD_GRAVITY_FACTOR else 1f) * dt
                horseHeight += verticalVelocity * dt
                jumpPeak = max(jumpPeak, horseHeight)
                if (horseHeight <= 0f) {
                    horseHeight = 0f
                    verticalVelocity = 0f
                    splashTime = SPLASH_SECONDS
                    if (shouldThrowCowboy()) {
                        startFall()
                        return
                    }
                }
            }
        }
        if (superJumpQueued && horseHeight <= 0f && superJumpPhase == SuperJumpPhase.NONE) {
            startSuperJumpWindup()
        }

        // Rider eases into the forward seat on takeoff and back upright after landing
        val leanTarget = if (horseHeight > 0f) 1f else 0f
        riderLean += (leanTarget - riderLean) * min(1f, dt * RIDER_LEAN_RESPONSE)

        // Fences. The first one of a run (or of a restored run, whose track starts empty) comes in
        // at 60 % of the visible width.
        if (nextFenceIn < 0f && fences.isEmpty()) nextFenceIn = worldWidth * 0.6f
        fences.forEach { it.x -= step }
        fences.removeAll { it.x + it.width < 0f }
        nextFenceIn -= step
        if (nextFenceIn <= 0f) spawnFence()

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
        // The super jump sails over everything, including the fences it passes low at takeoff
        val invulnerable = superJumpPhase != SuperJumpPhase.NONE

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
                        snail.x -= step + ownSpeed * dt
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
                    snail.x -= step * 0.5f
                    snail.verticalVelocity -= GRAVITY * dt
                    snail.height += snail.verticalVelocity * dt
                    snail.spin += 720f * dt
                }
                SnailState.LASSOED -> Unit // follows the lasso tip below
            }
        }
        snails.removeAll { it.x < -SNAIL_SIZE || it.height < -WORLD_HEIGHT_UNITS }

        // Lasso: extends to the target (or straight ahead) and back over LASSO_DURATION
        lassoCooldown = max(0f, lassoCooldown - dt)
        if (lassoTime >= 0f) {
            lassoTime += dt
            val progress = min(1f, lassoTime / LASSO_DURATION)
            val handX = HORSE_X + HAND_X
            val handY = horseHeight + HAND_Y
            val target = lassoTarget

            if (!lassoResolved) {
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
                        if (snailsCaught % SNAILS_PER_SUPER_JUMP == 0) superJumpCharges++
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

            if (progress >= 1f) {
                lassoTarget?.let { caught -> if (caught.state == SnailState.LASSOED) snails.remove(caught) }
                lassoTarget = null
                lassoTime = -1f
            }
        }

        chaseGap = min(CHASE_GAP_MAX, chaseGap + CHASE_GAP_REGAIN * dt)
        if (chaseGap <= 0f) chaseGap = 0f
    }

    private fun shouldThrowCowboy(): Boolean =
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
        // Caught snails add points without distance, so markers jump closer by the catch bonus.
        val effectiveDistance = distance + bonusPoints * UNITS_PER_POINT
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

        return SchneaggRodeoFrame(
            distance = distance,
            fences = fenceUis,
            snails = snailUis,
            pack = pack,
            horse = horsePose(),
            lasso = lasso,
            dust = if (dustTime > 0f) RodeoDustUi(x = dustX, progress = 1f - dustTime / DUST_SECONDS) else null,
            splashProgress = if (splashTime > 0f) 1f - splashTime / SPLASH_SECONDS else null,
            markers = markers,
            snailsCaught = snailsCaught,
            cowboy = cowboyOnFoot(),
        )
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
        var hatLift = 0f
        when (superJumpPhase) {
            SuperJumpPhase.WINDUP -> {
                // Hind legs pump up; the longer legs lift the hindquarters, tipping the horse
                // forward over its front hooves like a loaded spring.
                val progress = min(1f, superJumpWindup / SUPER_JUMP_WINDUP_SECONDS)
                hindLegScale = 1f + SUPER_JUMP_HIND_LEG_GROWTH * easeOutBack(progress)
                pitch = atan(9f * (hindLegScale - 1f) / 11f) * 180f / PI.toFloat()
                pivotX = 19f
                pivotY = 0f
                glow = progress * (0.75f + 0.25f * sin(superJumpWindup * 40f))
            }
            SuperJumpPhase.FLIGHT -> {
                val progress = min(1f, superJumpTravelled / superJumpDistance)
                // Legs shrink back over the first quarter of the flight
                hindLegScale = 1f + SUPER_JUMP_HIND_LEG_GROWTH * max(0f, 1f - progress / 0.25f)
                glow = max(0f, 1f - progress / 0.25f)
                pitch = -16f * cos(PI.toFloat() * progress) // nose up while rising, down to land
            }
            SuperJumpPhase.NONE -> if (fallPhase == FallPhase.THROWN && fallClock < BUCK_SECONDS) {
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
            }
        }
        return RodeoHorsePose(
            height = horseHeight,
            gaitPhase = gaitPhase,
            airborne = horseHeight > 0f,
            riderLean = max(riderLean, frontLegFold * 1.4f),
            pitchDegrees = pitch,
            pivotX = pivotX,
            pivotY = pivotY,
            hindLegScale = hindLegScale,
            frontLegFold = frontLegFold,
            hatLift = hatLift,
            glow = glow,
            offsetX = horseOffset(),
            hasRider = fallPhase == FallPhase.RIDING,
        )
    }
}

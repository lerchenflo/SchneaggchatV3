package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.horse_game_best
import schneaggchatv3mp.composeapp.generated.resources.horse_game_fence_height
import schneaggchatv3mp.composeapp.generated.resources.horse_game_game_over
import schneaggchatv3mp.composeapp.generated.resources.horse_game_lasso
import schneaggchatv3mp.composeapp.generated.resources.horse_game_score
import schneaggchatv3mp.composeapp.generated.resources.horse_game_snails
import schneaggchatv3mp.composeapp.generated.resources.horse_game_start
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative
import schneaggchatv3mp.composeapp.generated.resources.under_construction_description
import schneaggchatv3mp.composeapp.generated.resources.under_construction_title
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// All game physics run in "units" (u). One unit is GAME_HEIGHT / WORLD_HEIGHT_UNITS, so the game
// plays identically on every screen size - only the visible width (how far ahead you see) changes.
private val GAME_HEIGHT = 220.dp
private const val WORLD_HEIGHT_UNITS = 75f
private const val GROUND_OFFSET_UNITS = 10f // ground line distance from the canvas bottom

// One unit is also 10 cm in "horse scale", which is what the fence height labels show.
private const val CM_PER_UNIT = 10
private const val MIN_FENCE_CM = 50
private const val MAX_FENCE_CM = 170
// Fence height range at the start of a run; both ends grow linearly to their final values over
// FENCE_GROWTH_SECONDS of riding.
private const val START_MAX_FENCE_CM = 90
private const val FINAL_MIN_FENCE_CM = 100
private const val FENCE_GROWTH_SECONDS = 150f
private const val POLE_SPACING = 4f
private const val POLE_THICKNESS = 1.1f

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
private const val UNITS_PER_POINT = 10f

// Knocking a pole or running into a snail doesn't end the run - the horse stumbles for a moment
// and the chasing pack closes in. The run ends once the pack reaches the horse.
private const val STUMBLE_SECONDS = 0.6f
private const val STUMBLE_SPEED_FACTOR = 0.55f
private const val CHASE_GAP_MAX = 30f     // pack distance behind the horse; starts here
private const val CHASE_GAP_REGAIN = 1.5f // u/s won back while riding
private const val FENCE_CRASH_PENALTY = 12f
private const val RUNNER_CRASH_PENALTY = 10f
private const val CATCH_GAP_BONUS = 8f
private const val CATCH_POINTS = 25
private const val PACK_SIZE = 3
private const val PACK_SPACING = 7f

// Schneaggs (snails), drawn with the same sprite as the TowerStack game
private const val SNAIL_SIZE = 10f        // sprite side length in units
private const val SNAIL_HALF_WIDTH = 4f   // collision half width around the sprite center
private const val SNAIL_BODY_HEIGHT = 4.5f
/** The silhouette sits vertically centered in its square image; its underside is this far down. */
private const val SNAIL_FOOT_FRACTION = 0.785f
/** The background-colored halo that separates the schneagg from poles behind it. */
private const val SNAIL_OUTLINE_SCALE = 1.1f
private const val CRAWLER_SPEED = 3f      // u/s towards the horse, relative to the ground
private const val RUNNER_SPEED = 30f      // u/s towards the horse, relative to the ground
private const val RUNNER_START_SECONDS = 30f
private const val SNAIL_ON_FENCE_CHANCE = 0.3f
private const val SNAIL_BETWEEN_FENCES_CHANCE = 0.45f

// Lasso: thrown from the rider's hand, homes in on the first snail within range
private const val LASSO_RANGE = 32f
private const val LASSO_DURATION = 0.45f  // out and back
private const val LASSO_COOLDOWN = 0.8f
private const val LASSO_CATCH_TOLERANCE = 6f // extra reach at catch time, absorbs stumbles and speed changes

private const val RIDER_MAX_LEAN_DEGREES = 32f
private const val RIDER_LEAN_RESPONSE = 12f // 1/s, how fast the rider follows the lean target
private const val HORSE_X = 12f           // left edge of the horse in world units
private const val HAND_X = 18f            // rider's rein hand relative to the horse
private const val HAND_Y = 18f
private const val MAX_FRAME_SECONDS = 0.05f

// Hitbox of the horse relative to its left edge / hooves - a bit smaller than the drawing so
// near misses feel fair.
private const val HITBOX_LEFT = 8f
private const val HITBOX_RIGHT = 24f
private const val HITBOX_BOTTOM = 1f

// Same rainbow palette as the TowerStack game (explicitly requested for the poles)
private val POLE_COLORS = listOf(
    Color(0xFFFF0000), // Red
    Color(0xFFFF7F00), // Orange
    Color(0xFF00FF00), // Green
    Color(0xFF0000FF)  // Blue
)

// Cowboy hat, explicitly requested as a fixed orange
private val HAT_COLOR = Color(0xFFFF8C00)

private enum class RunState { READY, RUNNING, GAME_OVER }

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
 * Placeholder for features that are not done yet: a small endless runner where a cowboy on a
 * horse jumps show-jumping fences while a pack of schneaggs chases him. Tap (or space / arrow up
 * on desktop) to jump, hold to jump higher, throw the lasso (button or L / arrow down) to catch
 * snails.
 */
@Composable
fun UnderConstructionScreen(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    var runState by remember { mutableStateOf(RunState.READY) }
    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(0) }
    var snailsCaught by remember { mutableIntStateOf(0) }
    // Bumped every frame; only read in the draw phase so a frame redraws the canvas without recomposing.
    var frameTick by remember { mutableLongStateOf(0L) }

    // Mutable simulation state, deliberately not snapshot state - frameTick triggers the redraw.
    val fences = remember { mutableListOf<Fence>() }
    val snails = remember { mutableListOf<Snail>() }
    val sim = remember {
        object {
            var worldWidth = 0f
            var horseHeight = 0f
            var verticalVelocity = 0f
            var speed = START_SPEED
            var distance = 0f
            var nextFenceIn = 0f
            var nextRunnerIn = 0f
            var gaitPhase = 0f
            var elapsed = 0f
            var riderLean = 0f // 0 = upright, 1 = fully leaning forward over the neck
            var jumpHeld = false
            var airTime = 0f
            var fenceCount = 0
            var stumble = 0f
            var chaseGap = CHASE_GAP_MAX
            var packPhase = 0f
            var bonusPoints = 0

            var lassoTime = -1f // < 0 while no lasso is out
            var lassoCooldown = 0f
            var lassoResolved = false
            var lassoTarget: Snail? = null
            var lassoAimX = 0f
            var lassoAimY = 0f
            var lassoTipX = 0f
            var lassoTipY = 0f
        }
    }

    fun resetRun() {
        fences.clear()
        snails.clear()
        sim.horseHeight = 0f
        sim.verticalVelocity = 0f
        sim.speed = START_SPEED
        sim.distance = 0f
        sim.nextFenceIn = sim.worldWidth * 0.6f
        sim.nextRunnerIn = 0f
        sim.gaitPhase = 0f
        sim.elapsed = 0f
        sim.riderLean = 0f
        sim.airTime = 0f
        sim.stumble = 0f
        sim.chaseGap = CHASE_GAP_MAX
        sim.bonusPoints = 0
        sim.lassoTime = -1f
        sim.lassoCooldown = 0f
        sim.lassoTarget = null
        score = 0
        snailsCaught = 0
    }

    fun spawnFence() {
        val oxer = sim.speed >= OXER_MIN_SPEED && Random.nextFloat() < 0.3f
        val progress = min(1f, sim.elapsed / FENCE_GROWTH_SECONDS)
        val minCm = MIN_FENCE_CM + ((FINAL_MIN_FENCE_CM - MIN_FENCE_CM) * progress).toInt()
        val maxCm = START_MAX_FENCE_CM + ((MAX_FENCE_CM - START_MAX_FENCE_CM) * progress).toInt()
        val heightCm = Random.nextInt(minCm / 10, maxCm / 10 + 1) * 10
        val fence = Fence(
            x = sim.worldWidth,
            width = if (oxer) 18f else 10f,
            heightCm = heightCm,
            colorOffset = sim.fenceCount++
        )
        fences.add(fence)
        // Gap scales with speed so the reaction time stays roughly constant.
        sim.nextFenceIn = fence.width + sim.speed * (0.9f + Random.nextFloat() * 0.9f)

        // Crawlers either sit on the new fence or halfway to the next one, so they never overlap poles
        val roll = Random.nextFloat()
        if (roll < SNAIL_ON_FENCE_CHANCE) {
            snails.add(Snail(x = fence.x + fence.width / 2f, kind = SnailKind.CRAWLER, onFence = fence))
        } else if (roll < SNAIL_ON_FENCE_CHANCE + SNAIL_BETWEEN_FENCES_CHANCE) {
            val gapToNext = sim.nextFenceIn - fence.width
            snails.add(Snail(x = fence.x + fence.width + gapToNext / 2f, kind = SnailKind.CRAWLER))
        }
    }

    fun crash(penalty: Float) {
        sim.stumble = STUMBLE_SECONDS
        sim.chaseGap -= penalty
    }

    fun onJumpPressed() {
        if (sim.jumpHeld) return // key repeat while holding
        sim.jumpHeld = true
        when (runState) {
            RunState.READY -> {
                resetRun()
                runState = RunState.RUNNING
                sim.verticalVelocity = JUMP_VELOCITY
            }
            RunState.RUNNING -> if (sim.horseHeight <= 0f) {
                sim.verticalVelocity = JUMP_VELOCITY
                sim.airTime = 0f
            }
            RunState.GAME_OVER -> {
                resetRun()
                runState = RunState.RUNNING
            }
        }
    }

    fun onJumpReleased() {
        sim.jumpHeld = false
    }

    fun onLassoPressed() {
        if (runState != RunState.RUNNING || sim.lassoTime >= 0f || sim.lassoCooldown > 0f) return
        val handX = HORSE_X + HAND_X
        val effectiveSpeed = sim.speed * (if (sim.stumble > 0f) STUMBLE_SPEED_FACTOR else 1f)
        val timeToCatch = LASSO_DURATION / 2f
        sim.lassoTime = 0f
        sim.lassoCooldown = LASSO_COOLDOWN
        sim.lassoResolved = false
        // Targets are picked by where they will be when the loop arrives - at full speed a snail
        // scrolls ~40 u during the throw, so its current position would already be behind the hand.
        sim.lassoTarget = snails
            .filter { it.state == SnailState.ACTIVE }
            .map { it to predictedSnailX(it, effectiveSpeed, timeToCatch) }
            .filter { (_, x) -> x > handX - 2f && x < handX + LASSO_RANGE }
            .minByOrNull { (_, x) -> x }
            ?.first
    }

    LaunchedEffect(runState) {
        if (runState != RunState.RUNNING) return@LaunchedEffect
        var lastFrame = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = min((now - lastFrame) / 1_000_000_000f, MAX_FRAME_SECONDS)
            lastFrame = now

            sim.elapsed += dt
            sim.speed = min(MAX_SPEED, sim.speed + ACCELERATION * dt)
            sim.stumble = max(0f, sim.stumble - dt)
            val effectiveSpeed = sim.speed * (if (sim.stumble > 0f) STUMBLE_SPEED_FACTOR else 1f)
            val step = effectiveSpeed * dt
            sim.distance += step
            sim.gaitPhase += step * 0.12f
            sim.packPhase += dt * 10f

            // Vertical movement
            if (sim.horseHeight > 0f || sim.verticalVelocity > 0f) {
                val boosted = sim.jumpHeld && sim.verticalVelocity > 0f && sim.airTime < MAX_HOLD_SECONDS
                sim.airTime += dt
                sim.verticalVelocity -= GRAVITY * (if (boosted) HOLD_GRAVITY_FACTOR else 1f) * dt
                sim.horseHeight += sim.verticalVelocity * dt
                if (sim.horseHeight <= 0f) {
                    sim.horseHeight = 0f
                    sim.verticalVelocity = 0f
                }
            }

            // Rider eases into the forward seat on takeoff and back upright after landing
            val leanTarget = if (sim.horseHeight > 0f) 1f else 0f
            sim.riderLean += (leanTarget - sim.riderLean) * min(1f, dt * RIDER_LEAN_RESPONSE)

            // Fences
            fences.forEach { it.x -= step }
            fences.removeAll { it.x + it.width < 0f }
            sim.nextFenceIn -= step
            if (sim.nextFenceIn <= 0f) spawnFence()

            // Runners only join once the rider had some time to warm up
            if (sim.elapsed >= RUNNER_START_SECONDS) {
                sim.nextRunnerIn -= dt
                if (sim.nextRunnerIn <= 0f) {
                    snails.add(Snail(x = sim.worldWidth + SNAIL_SIZE, kind = SnailKind.RUNNER))
                    sim.nextRunnerIn = 5f + Random.nextFloat() * 7f
                }
            }

            val hitLeft = HORSE_X + HITBOX_LEFT
            val hitRight = HORSE_X + HITBOX_RIGHT
            val hitBottom = sim.horseHeight + HITBOX_BOTTOM

            fences.forEach { fence ->
                if (!fence.knocked && fence.x < hitRight && fence.x + fence.width > hitLeft && hitBottom < fence.top) {
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

                        val runnerHit = snail.kind == SnailKind.RUNNER &&
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
            sim.lassoCooldown = max(0f, sim.lassoCooldown - dt)
            if (sim.lassoTime >= 0f) {
                sim.lassoTime += dt
                val progress = min(1f, sim.lassoTime / LASSO_DURATION)
                val handX = HORSE_X + HAND_X
                val handY = sim.horseHeight + HAND_Y
                val target = sim.lassoTarget

                if (!sim.lassoResolved) {
                    if (target != null && target.state == SnailState.ACTIVE) {
                        sim.lassoAimX = target.x
                        sim.lassoAimY = target.height + SNAIL_BODY_HEIGHT / 2f
                    } else {
                        sim.lassoAimX = handX + LASSO_RANGE
                        sim.lassoAimY = max(2f, handY - 12f)
                    }
                    if (progress >= 0.5f) {
                        sim.lassoResolved = true
                        if (target != null && target.state == SnailState.ACTIVE &&
                            target.x in (handX - LASSO_CATCH_TOLERANCE)..(handX + LASSO_RANGE + LASSO_CATCH_TOLERANCE)
                        ) {
                            target.state = SnailState.LASSOED
                            target.onFence = null
                            snailsCaught++
                            sim.bonusPoints += CATCH_POINTS
                            sim.chaseGap = min(CHASE_GAP_MAX, sim.chaseGap + CATCH_GAP_BONUS)
                        } else {
                            sim.lassoTarget = null
                        }
                    }
                }

                val extension = sin(PI.toFloat() * progress)
                sim.lassoTipX = handX + (sim.lassoAimX - handX) * extension
                sim.lassoTipY = handY + (sim.lassoAimY - handY) * extension
                sim.lassoTarget?.takeIf { it.state == SnailState.LASSOED }?.let { caught ->
                    caught.x = sim.lassoTipX
                    caught.height = sim.lassoTipY - SNAIL_BODY_HEIGHT / 2f
                }

                if (progress >= 1f) {
                    sim.lassoTarget?.let { caught -> if (caught.state == SnailState.LASSOED) snails.remove(caught) }
                    sim.lassoTarget = null
                    sim.lassoTime = -1f
                }
            }

            sim.chaseGap = min(CHASE_GAP_MAX, sim.chaseGap + CHASE_GAP_REGAIN * dt)
            score = (sim.distance / UNITS_PER_POINT).toInt() + sim.bonusPoints
            frameTick++

            if (sim.chaseGap <= 0f) {
                sim.chaseGap = 0f
                if (score > bestScore) bestScore = score
                runState = RunState.GAME_OVER
                break
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    // Only 13 possible heights - resolved in composition since the canvas draws outside of it
    val heightLabels = (MIN_FENCE_CM..MAX_FENCE_CM step 10).associateWith {
        stringResource(Res.string.horse_game_fence_height, it)
    }
    val heightLabelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val bodyLabelStyle = MaterialTheme.typography.labelMedium.copy(color = colors.surface, fontWeight = FontWeight.Bold)
    val snailImage = imageResource(Res.drawable.icon_schneagg_alternative)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Construction,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = stringResource(Res.string.under_construction_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stringResource(Res.string.under_construction_description),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(top = 4.dp, bottom = 24.dp)
        )

        Row(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.horse_game_score, score),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(Res.string.horse_game_snails, snailsCaught),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(Res.string.horse_game_best, bestScore),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainer,
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .height(GAME_HEIGHT)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        val unitPx = it.height / WORLD_HEIGHT_UNITS
                        if (unitPx > 0f) sim.worldWidth = it.width / unitPx
                    }
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        val isJumpKey = event.key == Key.Spacebar || event.key == Key.DirectionUp || event.key == Key.W
                        val isLassoKey = event.key == Key.L || event.key == Key.DirectionDown || event.key == Key.S
                        when {
                            isLassoKey && event.type == KeyEventType.KeyDown -> { onLassoPressed(); true }
                            !isJumpKey -> false
                            event.type == KeyEventType.KeyDown -> { onJumpPressed(); true }
                            event.type == KeyEventType.KeyUp -> { onJumpReleased(); true }
                            else -> false
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            onJumpPressed()
                            tryAwaitRelease()
                            onJumpReleased()
                        })
                    }
            ) {
                frameTick // read so every simulation frame redraws the canvas
                val unit = size.height / WORLD_HEIGHT_UNITS
                val groundY = size.height - GROUND_OFFSET_UNITS * unit
                val snailOutline = colors.surfaceContainer

                fun drawSnailAt(x: Float, height: Float, facingLeft: Boolean, tiltDeg: Float) = drawSnail(
                    image = snailImage,
                    centerX = x * unit,
                    footY = groundY - height * unit,
                    size = SNAIL_SIZE * unit,
                    facingLeft = facingLeft,
                    tiltDeg = tiltDeg,
                    outline = snailOutline
                )

                drawGround(groundY, unit, sim.distance, colors.onSurfaceVariant)
                fences.forEach { fence ->
                    drawFence(fence, groundY, unit, colors.onSurface, heightLabels.getValue(fence.heightCm), heightLabelStyle, textMeasurer)
                }

                snails.forEach { snail ->
                    when (snail.state) {
                        SnailState.ACTIVE -> {
                            val wobble = if (snail.kind == SnailKind.RUNNER) {
                                sin(sim.packPhase + snail.phase) * 8f
                            } else {
                                sin(sim.packPhase * 0.3f + snail.phase) * 3f
                            }
                            drawSnailAt(snail.x, snail.height, facingLeft = true, tiltDeg = wobble)
                        }
                        SnailState.KNOCKED -> drawSnailAt(snail.x, snail.height, facingLeft = true, tiltDeg = snail.spin)
                        SnailState.LASSOED -> Unit // drawn on top of the rider together with the lasso
                    }
                }

                // Chasing pack, hopping along behind the horse (off screen while the gap is large)
                repeat(PACK_SIZE) { index ->
                    val x = HORSE_X + HITBOX_LEFT - SNAIL_HALF_WIDTH - sim.chaseGap - index * PACK_SPACING
                    if (x > -SNAIL_SIZE) {
                        val hop = abs(sin(sim.packPhase + index * 1.3f)) * 1.5f
                        val height = max(hop, hopHeight(x, fences))
                        drawSnailAt(x, height, facingLeft = false, tiltDeg = sin(sim.packPhase + index) * 6f)
                    }
                }

                drawHorseAndRider(
                    left = HORSE_X * unit,
                    groundY = groundY - sim.horseHeight * unit,
                    unit = unit,
                    gaitPhase = sim.gaitPhase,
                    airborne = sim.horseHeight > 0f,
                    riderLean = sim.riderLean,
                    color = colors.onSurface,
                    shirtColor = colors.primary,
                    bodyLabel = if (snailsCaught > 0) textMeasurer.measure(snailsCaught.toString(), bodyLabelStyle) else null
                )

                if (sim.lassoTime >= 0f) {
                    val hand = Offset((HORSE_X + HAND_X) * unit, groundY - (sim.horseHeight + HAND_Y) * unit)
                    val tip = Offset(sim.lassoTipX * unit, groundY - sim.lassoTipY * unit)
                    drawLasso(hand, tip, unit, colors.tertiary)
                    sim.lassoTarget?.takeIf { it.state == SnailState.LASSOED }?.let { caught ->
                        drawSnailAt(caught.x, caught.height, facingLeft = true, tiltDeg = -20f)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hint = when (runState) {
                RunState.READY -> stringResource(Res.string.horse_game_start)
                RunState.GAME_OVER -> stringResource(Res.string.horse_game_game_over)
                RunState.RUNNING -> ""
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            FilledTonalButton(
                onClick = { onLassoPressed() },
                enabled = runState == RunState.RUNNING,
                // Keeps keyboard focus on the canvas so space / L keep working after a click
                modifier = Modifier.focusProperties { canFocus = false }
            ) {
                Text(stringResource(Res.string.horse_game_lasso))
            }
        }
    }
}

private fun DrawScope.drawGround(groundY: Float, unit: Float, distance: Float, color: Color) {
    drawLine(color, Offset(0f, groundY), Offset(size.width, groundY), strokeWidth = unit * 0.6f)

    // Small pebbles scrolling with the ground, so the speed is visible between fences
    val spacing = 9f * unit
    val shift = (distance * unit) % spacing
    var x = -shift
    var index = (distance * unit / spacing).toInt()
    while (x < size.width) {
        val depth = 1.5f + (index * 7 % 5) * 0.8f
        val length = (1f + index * 5 % 3) * unit
        drawLine(
            color = color.copy(alpha = 0.5f),
            start = Offset(x, groundY + depth * unit),
            end = Offset(x + length, groundY + depth * unit),
            strokeWidth = unit * 0.5f,
            cap = StrokeCap.Round
        )
        x += spacing
        index++
    }
}

private fun DrawScope.drawFence(
    fence: Fence,
    groundY: Float,
    unit: Float,
    woodColor: Color,
    heightLabel: String,
    heightLabelStyle: TextStyle,
    textMeasurer: TextMeasurer
) {
    val left = fence.x * unit
    val right = (fence.x + fence.width) * unit
    val postWidth = 1.2f * unit
    val postHeight = (fence.top + 2.5f) * unit

    // Wooden standards with little feet
    listOf(left, right - postWidth).forEach { postX ->
        drawRect(woodColor, Offset(postX, groundY - postHeight), Size(postWidth, postHeight))
        drawRect(
            woodColor,
            Offset(postX - unit, groundY - 0.8f * unit),
            Size(postWidth + 2f * unit, 0.8f * unit)
        )
    }

    // Striped poles resting in the cups, every pole in its own color. Knocked poles lie stacked on
    // the ground in front of the standards.
    val poleThickness = POLE_THICKNESS * unit
    val stripeWidth = 2f * unit
    fence.poleHeights.forEachIndexed { index, height ->
        val poleColor = POLE_COLORS[(fence.colorOffset + index) % POLE_COLORS.size]
        val poleBottom = if (fence.knocked) index * POLE_THICKNESS else height - POLE_THICKNESS
        val poleY = groundY - (poleBottom + POLE_THICKNESS) * unit
        var x = left
        var stripe = 0
        while (x < right) {
            val width = min(stripeWidth, right - x)
            drawRect(
                color = if (stripe % 2 == 0) poleColor else poleColor.copy(alpha = 0.45f),
                topLeft = Offset(x, poleY),
                size = Size(width, poleThickness)
            )
            x += stripeWidth
            stripe++
        }
    }

    // Height label below the ground, centered under the fence
    val label = textMeasurer.measure(heightLabel, heightLabelStyle)
    val labelX = (left + right) / 2f - label.size.width / 2f
    if (labelX + label.size.width > 0f && labelX < size.width) {
        drawText(label, topLeft = Offset(labelX, groundY + 2f * unit))
    }
}

/**
 * Draws the schneagg sprite (which faces right) with its underside at [footY], centered on
 * [centerX], with a halo in [outline]. [facingLeft] mirrors it; [tiltDeg] tilts it around its foot.
 */
private fun DrawScope.drawSnail(
    image: ImageBitmap,
    centerX: Float,
    footY: Float,
    size: Float,
    facingLeft: Boolean,
    tiltDeg: Float,
    outline: Color,
) {
    val pivot = Offset(size / 2f, size * SNAIL_FOOT_FRACTION)
    val outlineSize = size * SNAIL_OUTLINE_SCALE
    val outlineInset = ((size - outlineSize) / 2f).roundToInt()
    withTransform({
        translate(centerX - size / 2f, footY - size * SNAIL_FOOT_FRACTION)
        rotate(tiltDeg, pivot = pivot)
        if (facingLeft) scale(-1f, 1f, pivot = pivot)
    }) {
        drawImage(
            image = image,
            dstOffset = IntOffset(outlineInset, outlineInset),
            dstSize = IntSize(outlineSize.roundToInt(), outlineSize.roundToInt()),
            colorFilter = ColorFilter.tint(outline),
            filterQuality = FilterQuality.Medium,
        )
        drawImage(
            image = image,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.roundToInt(), size.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}

/** Rope from the rider's [hand] sagging slightly towards the loop at [tip]. */
private fun DrawScope.drawLasso(hand: Offset, tip: Offset, unit: Float, color: Color) {
    val control = Offset((hand.x + tip.x) / 2f, max(hand.y, tip.y) + 2f * unit)
    drawPath(
        path = Path().apply {
            moveTo(hand.x, hand.y)
            quadraticTo(control.x, control.y, tip.x, tip.y)
        },
        color = color,
        style = Stroke(width = 0.5f * unit, cap = StrokeCap.Round)
    )
    drawCircle(color, radius = 1.8f * unit, center = tip, style = Stroke(width = 0.5f * unit))
}

/**
 * Side view of the horse (facing right) with the cowboy on top, drawn on a 30 x 28 unit grid.
 * Grid coordinates are y-up from the hooves; [p] converts them to canvas pixels.
 */
private fun DrawScope.drawHorseAndRider(
    left: Float,
    groundY: Float,
    unit: Float,
    gaitPhase: Float,
    airborne: Boolean,
    riderLean: Float,
    color: Color,
    shirtColor: Color,
    bodyLabel: TextLayoutResult?
) {
    fun p(x: Float, y: Float) = Offset(left + x * unit, groundY - y * unit)
    fun polygon(vararg points: Pair<Float, Float>) = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(p(x, y).x, p(x, y).y) else lineTo(p(x, y).x, p(x, y).y) }
        close()
    }

    val legStroke = 1.6f * unit
    val bounce = if (airborne) 0f else sin(gaitPhase * 2f) * 0.4f

    // Legs
    if (airborne) {
        // Front legs tucked, hind legs stretched back - the classic jumping pose
        listOf(17f, 19f).forEach { x ->
            drawLine(color, p(x, 9f), p(x + 2.5f, 5f), legStroke, StrokeCap.Round)
            drawLine(color, p(x + 2.5f, 5f), p(x + 0.5f, 3f), legStroke, StrokeCap.Round)
        }
        listOf(6f, 8f).forEach { x ->
            drawLine(color, p(x, 9f), p(x - 4f, 2f), legStroke, StrokeCap.Round)
        }
    } else {
        val swing = sin(gaitPhase) * 2.2f
        drawLine(color, p(17f, 9f), p(17f + swing, 0f), legStroke, StrokeCap.Round)
        drawLine(color, p(19f, 9f), p(19f - swing, 0f), legStroke, StrokeCap.Round)
        drawLine(color, p(6f, 9f), p(6f - swing, 0f), legStroke, StrokeCap.Round)
        drawLine(color, p(8f, 9f), p(8f + swing, 0f), legStroke, StrokeCap.Round)
    }

    // Body
    drawRoundRect(
        color = color,
        topLeft = p(4f, 15f + bounce),
        size = Size(16f * unit, 7f * unit),
        cornerRadius = CornerRadius(3f * unit)
    )

    // Caught snails, painted on the horse's side like a race number
    if (bodyLabel != null) {
        val center = p(8f, 11.5f + bounce) // rear half, clear of the rider's leg
        drawText(bodyLabel, topLeft = center - Offset(bodyLabel.size.width / 2f, bodyLabel.size.height / 2f))
    }

    // Tail
    val tailSwing = if (airborne) 2f else sin(gaitPhase + PI.toFloat()) * 1f
    drawPath(
        path = Path().apply {
            val start = p(4.5f, 14f + bounce)
            val control = p(1f, 14f + tailSwing)
            val end = p(0.5f, 7f + tailSwing)
            moveTo(start.x, start.y)
            quadraticTo(control.x, control.y, end.x, end.y)
        },
        color = color,
        style = Stroke(width = 1.4f * unit, cap = StrokeCap.Round)
    )

    // Neck, head and ear
    drawPath(polygon(15f to 15f + bounce, 20f to 12f + bounce, 24f to 21f, 20f to 22.5f), color)
    drawPath(polygon(20f to 22.5f, 24f to 23f, 29f to 19f, 28f to 17f, 23f to 18.5f), color)
    drawPath(polygon(21f to 22.5f, 22f to 25f, 23f to 22.8f), color)

    // Cowboy: leg stays at the horse's side, the upper body pivots forward at the seat in jumps
    val riderY = bounce * 1.5f
    drawLine(color, p(12f, 16f + riderY), p(13.5f, 12f + riderY), 1.3f * unit, StrokeCap.Round)
    rotate(degrees = RIDER_MAX_LEAN_DEGREES * riderLean, pivot = p(12f, 16f + riderY)) {
        drawRoundRect(
            color = shirtColor,
            topLeft = p(10.5f, 21.5f + riderY),
            size = Size(3f * unit, 6f * unit),
            cornerRadius = CornerRadius(1f * unit)
        )
        drawLine(shirtColor, p(13f, 20.5f + riderY), p(18f, 17.5f + riderY), 1f * unit, StrokeCap.Round)
        drawCircle(color, radius = 1.8f * unit, center = p(12.2f, 23.6f + riderY))
        drawRoundRect(
            color = HAT_COLOR,
            topLeft = p(9f, 25.6f + riderY),
            size = Size(6.4f * unit, 0.7f * unit),
            cornerRadius = CornerRadius(0.35f * unit)
        )
        drawRoundRect(
            color = HAT_COLOR,
            topLeft = p(10.4f, 28f + riderY),
            size = Size(3.6f * unit, 2.6f * unit),
            cornerRadius = CornerRadius(0.8f * unit)
        )
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

// Everything that lies on the track: fences, schneaggs (snails), lucky horseshoes, carrots, magic
// mushrooms and mud.
// Plain mutable objects, moved around by RodeoCourse and knocked over by the horse and the vehicles.

internal const val MIN_FENCE_CM = 50
internal const val MAX_FENCE_CM = 170
/** The highest fence of any level (the harder level goes above MAX_FENCE_CM, see RodeoLevel). */
internal const val HIGHEST_FENCE_CM = 190
private const val POLE_SPACING = 4f
internal const val POLE_THICKNESS = 1.1f

// Schneaggs (snails), drawn with the same sprite as the TowerStack game
/** Sprite side length. */
internal const val SNAIL_SIZE = 10f
/** Collision half width around the sprite center. */
internal const val SNAIL_HALF_WIDTH = 4f
internal const val SNAIL_BODY_HEIGHT = 4.5f
/** u/s towards the horse, relative to the ground. */
private const val CRAWLER_SPEED = 3f
private const val RUNNER_SPEED = 30f
/** Upwards speed of a snail sent flying. */
private const val KNOCK_VELOCITY = 45f

/** Something lying on the track: the ground carries it along to the left. */
internal interface OnTrack {
    var x: Float
}

/** Moves everything [scroll] units to the left with the ground and drops what [isGone] (out of the picture). */
internal fun <T : OnTrack> MutableList<T>.scrollAlong(scroll: Float, isGone: (T) -> Boolean) {
    forEach { it.x -= scroll }
    removeAll(isGone)
}

/** [x] is the left edge, [colorOffset] rotates the pole colors so neighbouring fences differ. */
internal class Fence(override var x: Float, val width: Float, val heightCm: Int, val colorOffset: Int) : OnTrack {
    val top: Float = heightCm / CM_PER_UNIT.toFloat()
    var knocked = false

    /** Top pole at the fence height, further poles below it while there is room. */
    val poleHeights: List<Float> = generateSequence(top) { it - POLE_SPACING }
        .takeWhile { it >= 2f }
        .toList()

    /** Overlaps the stretch [from]..[to] of the track. */
    fun overlaps(from: Float, to: Float) = x < to && x + width > from
}

internal enum class SnailKind {
    /** Crawls slowly or sits on a fence - worth points when lassoed. */
    CRAWLER,
    /** Sprints towards the horse and hops over fences - lasso it, or the horse just kicks it away. */
    RUNNER
}

internal enum class SnailState { ACTIVE, LASSOED, KNOCKED }

/** [x] is the sprite center, [height] its underside above the ground. */
internal class Snail(var x: Float, val kind: SnailKind, var onFence: Fence? = null) {
    var height = 0f
    var state = SnailState.ACTIVE
    var verticalVelocity = 0f
    var spin = 0f
    /** Offsets its wobble so a group of snails doesn't move in lockstep. */
    val phase = Random.nextFloat() * 2f * PI.toFloat()

    /** Its own pace towards the horse, on top of the scrolling ground. */
    val ownSpeed: Float
        get() = when {
            onFence != null -> 0f
            kind == SnailKind.RUNNER -> RUNNER_SPEED
            else -> CRAWLER_SPEED
        }

    /** Where it will be after [seconds], with the ground scrolling at [groundSpeed]. */
    fun predictedX(groundSpeed: Float, seconds: Float) = x - (groundSpeed + ownSpeed) * seconds

    /** Sent flying by a crash or something big (a vehicle); it tumbles off the screen. */
    fun knock() {
        state = SnailState.KNOCKED
        onFence = null
        verticalVelocity = KNOCK_VELOCITY
    }
}

/** A lucky horseshoe floating at [height] above the ground; [x] is its center. */
internal class Horseshoe(override var x: Float, val height: Float) : OnTrack {
    val phase = Random.nextFloat() * 2f * PI.toFloat()
}

/** A carrot floating at [height] above the ground; [x] is its center. Feeds the horse one heart. */
internal class Carrot(override var x: Float, val height: Float) : OnTrack {
    val phase = Random.nextFloat() * 2f * PI.toFloat()
}

/** A magic mushroom on the ground at [x] (its center); see RodeoMushrooms. */
internal class Mushroom(override var x: Float) : OnTrack {
    val seed = Random.nextInt(1000)
}

/** A crystal floating in the cave at [height]; [x] is its center, [hue] picks its color. */
internal class Gem(override var x: Float, val height: Float) : OnTrack {
    val phase = Random.nextFloat() * 2f * PI.toFloat()
    val hue = Random.nextInt(2)
}

/** A mud puddle on the ground from [x] (left edge) over [width]; slows the horse wading through. */
internal class MudPatch(override var x: Float, val width: Float) : OnTrack {
    /** Picks the shape of the splotches. */
    val seed = Random.nextInt(1000)

    fun overlaps(from: Float, to: Float) = x < to && x + width > from
}

/**
 * Height something at [x] hops to so it arcs over any fence it is passing: starts [reach] units
 * before a fence and clears its top by [clearance]. Used by the running snails, the pack and the
 * riderless horse.
 */
internal fun hopOverFences(x: Float, fences: List<Fence>, reach: Float, clearance: Float): Float {
    var height = 0f
    fences.forEach { fence ->
        val halfSpan = fence.width / 2f + reach
        val distance = abs(x - (fence.x + fence.width / 2f))
        if (distance < halfSpan) {
            val relative = distance / halfSpan
            height = max(height, (fence.top + clearance) * (1f - relative * relative))
        }
    }
    return height
}


/** A gap in the rainbow track from [x] over [width]: hooves on the ground over it drop the horse through. */
internal class Gap(override var x: Float, val width: Float) : OnTrack

/**
 * A dirt mound in the cave with a shovel stuck in it; [x] its center. Lasso the shovel and the horse
 * digs down to the fossil layer (see SchneaggRodeoEngine.startDigging).
 */
internal class DigMound(override var x: Float) : OnTrack {
    val seed = Random.nextInt(1000)
    var hasShovel = true
    /** The shovel dangling from the lasso on its way back, if it is; null otherwise. */
    var shovelX: Float? = null
    var shovelY = 0f
}

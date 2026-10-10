package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCarrotUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoHorseshoeUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMoundUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMudUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMushroomUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSnailUi
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Crawlers either sit on a new fence or halfway to the next one, so they never overlap poles
private const val SNAIL_ON_FENCE_CHANCE = 0.3f
private const val SNAIL_BETWEEN_FENCES_CHANCE = 0.45f

// Lucky horseshoes float between the fences
private const val HORSESHOE_CHANCE = 0.2f
private const val HORSESHOE_MIN_HEIGHT = 26f
private const val HORSESHOE_MAX_HEIGHT = 42f

// Carrots feed the horse; they float low enough to be picked up at a gallop, some need a hop
private const val CARROT_CHANCE = 0.14f
private const val CARROT_MIN_HEIGHT = 16f
private const val CARROT_MAX_HEIGHT = 30f

// Mud puddles lie in the gaps once the warm-up is over; jump them or wade through slowly
private const val MUD_START_SECONDS = 15f
private const val MUD_CHANCE = 0.22f
private const val MUD_MIN_WIDTH = 16f
private const val MUD_MAX_WIDTH = 32f

// Magic mushrooms: plenty in the forest, rare elsewhere (see RodeoMushrooms)
internal const val FOREST_MUSHROOM_CHANCE = 0.4f
internal const val MUSHROOM_CHANCE = 0.03f

// Crystals float in the cave, one or two per gap (see RodeoMapSwitch)
private const val GEM_CHANCE = 0.7f
private const val GEM_MIN_HEIGHT = 12f
private const val GEM_MAX_HEIGHT = 34f
/** Mushrooms grow in the damp cave too. */
private const val CAVE_MUSHROOM_CHANCE = 0.2f
/** Dirt mounds with a shovel to lasso, where no mushroom grows; rare, they are the way down. */
private const val MOUND_CHANCE = 0.15f

// The sea: pearls float in the water, oil slicks slow the swimming horse
private const val PEARL_CHANCE = 0.55f
private const val OIL_CHANCE = 0.3f

// The rainbow: gaps in place of the fences, as wide as the horse covers in this many seconds (a jump
// lasts about half a second), and stars floating over them, caught mid-jump
private const val GAP_SECONDS = 0.22f
private const val GAP_RANDOM_SECONDS = 0.06f
private const val GAP_MIN_WIDTH = 10f
private const val GAP_MAX_WIDTH = 30f
private const val STAR_CHANCE = 0.6f
/** Riding seconds of solid rainbow after arriving up there, so the first gap never comes while the map's name still shows. */
private const val RAINBOW_RUN_IN_SECONDS = 3f
private const val STAR_HEIGHT = 16f
// The fossil layer: ammonites in the rock, like the crystals in the cave
private const val AMMONITE_CHANCE = 0.55f

private const val OXER_WIDTH = 18f
private const val FENCE_WIDTH = 10f
/** Snails sent flying fall like the horse does. */
private const val KNOCKED_SNAIL_GRAVITY = 330f
private const val KNOCKED_SNAIL_SPIN = 720f // degrees per second

/**
 * The track ahead of the horse: fences, snails and horseshoes, and when the next ones come. It only
 * places and moves things - what happens when the horse runs into them is up to the engine.
 */
internal class RodeoCourse {
    val fences = mutableListOf<Fence>()
    val snails = mutableListOf<Snail>()
    val horseshoes = mutableListOf<Horseshoe>()
    val carrots = mutableListOf<Carrot>()
    val mud = mutableListOf<MudPatch>()
    val mushrooms = mutableListOf<Mushroom>()
    val gems = mutableListOf<Gem>()
    val gaps = mutableListOf<Gap>()
    val mounds = mutableListOf<DigMound>()
    /** Chance of a mushroom per fence placed; the engine raises it in the forest. */
    var mushroomChance = MUSHROOM_CHANCE
    /** Multiplies the chance of a mud puddle; the run's weather sets it (see RodeoRunFlavor). */
    var mudFactor = 1f
    /** The map the track runs through: each map has its own pickups. */
    var map = RodeoMap.SURFACE

    /** Fences placed this run; also rotates their pole colors. */
    var fenceCount = 0
        private set
    /** Seconds until the next runner is sent in. */
    var nextRunnerIn = 0f
        private set
    /** Distance still to ride until the next fence is placed at the right edge. */
    private var nextFenceIn = 0f

    fun reset() {
        fences.clear()
        snails.clear()
        horseshoes.clear()
        carrots.clear()
        mud.clear()
        mushrooms.clear()
        gems.clear()
        mounds.clear()
        gaps.clear()
        mushroomChance = MUSHROOM_CHANCE
        mudFactor = 1f
        map = RodeoMap.SURFACE
        fenceCount = 0
        nextRunnerIn = 0f
        // Placed on the first frame, once the world width is known for sure
        nextFenceIn = -1f
    }

    fun restore(fenceCount: Int, nextRunnerIn: Float) {
        this.fenceCount = fenceCount
        this.nextRunnerIn = nextRunnerIn
    }

    /**
     * Places a fence at [x], maybe with a crawler, a horseshoe, a carrot and a mud puddle; [gapAfter]
     * is the free space to the next fence, used to put things in between.
     */
    fun addFence(x: Float, gapAfter: Float, speed: Float, elapsed: Float): Fence {
        val oxer = RodeoDifficulty.rollOxer(speed, elapsed)
        val fence = Fence(
            x = x,
            width = if (oxer) OXER_WIDTH else FENCE_WIDTH,
            heightCm = RodeoDifficulty.randomFenceHeightCm(elapsed),
            colorOffset = fenceCount++
        )
        fences.add(fence)

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

        // A carrot at a third of the gap, a puddle past the middle - never on top of each other
        if (Random.nextFloat() < CARROT_CHANCE) {
            val height = CARROT_MIN_HEIGHT + Random.nextFloat() * (CARROT_MAX_HEIGHT - CARROT_MIN_HEIGHT)
            carrots.add(Carrot(x = fence.x + fence.width + gapAfter / 3f, height = height))
        }
        val gapStart = fence.x + fence.width
        when (map) {
            RodeoMap.CAVE -> {
                if (Random.nextFloat() < GEM_CHANCE) {
                    val height = GEM_MIN_HEIGHT + Random.nextFloat() * (GEM_MAX_HEIGHT - GEM_MIN_HEIGHT)
                    gems.add(Gem(x = gapStart + gapAfter * 0.45f, height = height))
                }
                if (Random.nextFloat() < CAVE_MUSHROOM_CHANCE) {
                    mushrooms.add(Mushroom(x = gapStart + gapAfter * 0.75f))
                } else if (Random.nextFloat() < MOUND_CHANCE) {
                    mounds.add(DigMound(x = gapStart + gapAfter * 0.7f))
                }
            }
            RodeoMap.SEA -> {
                // Pearls count like crystals; oil slicks work like mud
                if (Random.nextFloat() < PEARL_CHANCE) {
                    val height = GEM_MIN_HEIGHT + Random.nextFloat() * (GEM_MAX_HEIGHT - GEM_MIN_HEIGHT)
                    gems.add(Gem(x = gapStart + gapAfter * 0.45f, height = height))
                }
                if (Random.nextFloat() < OIL_CHANCE) addPuddle(gapStart, gapAfter)
            }
            RodeoMap.FOSSIL -> if (Random.nextFloat() < AMMONITE_CHANCE) {
                val height = GEM_MIN_HEIGHT + Random.nextFloat() * (GEM_MAX_HEIGHT - GEM_MIN_HEIGHT)
                gems.add(Gem(x = gapStart + gapAfter * 0.45f, height = height))
            }
            RodeoMap.RAINBOW -> Unit
            RodeoMap.SURFACE -> if (elapsed >= MUD_START_SECONDS && Random.nextFloat() < MUD_CHANCE * mudFactor) {
                addPuddle(gapStart, gapAfter)
            } else if (Random.nextFloat() < mushroomChance) {
                mushrooms.add(Mushroom(x = gapStart + gapAfter * 0.7f))
            }
        }
        return fence
    }

    private fun addPuddle(gapStart: Float, gapAfter: Float) {
        val width = min(gapAfter * 0.4f, MUD_MIN_WIDTH + Random.nextFloat() * (MUD_MAX_WIDTH - MUD_MIN_WIDTH))
        mud.add(MudPatch(x = gapStart + gapAfter * 0.55f, width = width))
    }

    /**
     * Another map begins: everything on the track is gone (snails in the lasso stay with it), and
     * the first fence comes after a normal gap (on the rainbow after a solid run-in first).
     */
    fun clearForNewMap(speed: Float, elapsed: Float) {
        fences.clear()
        snails.removeAll { it.state != SnailState.LASSOED }
        horseshoes.clear()
        carrots.clear()
        mud.clear()
        mushrooms.clear()
        gems.clear()
        mounds.clear()
        gaps.clear()
        nextFenceIn = RodeoDifficulty.randomFenceGap(speed, elapsed)
        if (map == RodeoMap.RAINBOW) nextFenceIn += speed * RAINBOW_RUN_IN_SECONDS
    }

    /** After a vehicle ride: the next fence comes no earlier than a normal gap. */
    fun resumeFences(speed: Float, elapsed: Float) {
        nextFenceIn = max(nextFenceIn, RodeoDifficulty.randomFenceGap(speed, elapsed))
    }

    /**
     * Moves the fences by [scroll] and places new ones at the right edge as [ridden] distance
     * passes - unless [spawnFences] is off because a vehicle brings its own obstacles.
     */
    fun scrollFences(scroll: Float, ridden: Float, worldWidth: Float, speed: Float, elapsed: Float, spawnFences: Boolean) {
        // The first fence of a run (or of a restored run, whose track starts empty) comes in at 60 %
        // of the visible width
        if (nextFenceIn < 0f && fences.isEmpty()) nextFenceIn = worldWidth * 0.6f
        fences.scrollAlong(scroll) { it.x + it.width < 0f }
        gaps.scrollAlong(scroll) { it.x + it.width < 0f }
        if (!spawnFences) return
        nextFenceIn -= ridden
        if (nextFenceIn <= 0f) {
            val gap = RodeoDifficulty.randomFenceGap(speed, elapsed)
            // On the rainbow a gap to jump takes the fence's place
            val width = if (map == RodeoMap.RAINBOW) addGap(worldWidth, gap, speed) else addFence(x = worldWidth, gapAfter = gap, speed = speed, elapsed = elapsed).width
            nextFenceIn = width + gap
        }
    }

    /** Places a gap in the rainbow at [x], maybe with a star over it; returns its width. */
    private fun addGap(x: Float, gapAfter: Float, speed: Float): Float {
        val width = (speed * (GAP_SECONDS + Random.nextFloat() * GAP_RANDOM_SECONDS)).coerceIn(GAP_MIN_WIDTH, GAP_MAX_WIDTH)
        gaps.add(Gap(x = x, width = width))
        if (Random.nextFloat() < STAR_CHANCE) gems.add(Gem(x = x + width / 2f, height = STAR_HEIGHT))
        if (Random.nextFloat() < CARROT_CHANCE) {
            carrots.add(Carrot(x = x + width + gapAfter / 2f, height = CARROT_MIN_HEIGHT))
        }
        return width
    }

    /** Sends a runner in from the right edge every now and then, once the warm-up is over. */
    fun sendRunners(dt: Float, worldWidth: Float, elapsed: Float) {
        if (elapsed < RUNNER_START_SECONDS) return
        nextRunnerIn -= dt
        if (nextRunnerIn <= 0f) {
            snails.add(Snail(x = worldWidth + SNAIL_SIZE, kind = SnailKind.RUNNER))
            nextRunnerIn = RodeoDifficulty.randomRunnerInterval(elapsed)
        }
    }

    /**
     * Snails move with the ground ([scroll], 0 while the world stands still) plus their own pace;
     * runners hop the fences, knocked ones tumble away. Lassoed ones are moved by the lasso.
     */
    fun moveSnails(scroll: Float, dt: Float) {
        snails.forEach { snail ->
            when (snail.state) {
                SnailState.ACTIVE -> {
                    if (snail.onFence?.knocked == true) snail.onFence = null // falls down with the poles
                    val seat = snail.onFence
                    if (seat != null) {
                        snail.x = seat.x + seat.width / 2f
                        snail.height = seat.top
                    } else {
                        snail.x -= scroll + snail.ownSpeed * dt
                        snail.height = if (snail.kind == SnailKind.RUNNER) hopOverFences(snail.x, fences, reach = 8f, clearance = 2f) else 0f
                    }
                }
                SnailState.KNOCKED -> {
                    snail.x -= scroll * 0.5f
                    snail.verticalVelocity -= KNOCKED_SNAIL_GRAVITY * dt
                    snail.height += snail.verticalVelocity * dt
                    snail.spin += KNOCKED_SNAIL_SPIN * dt
                }
                SnailState.LASSOED -> Unit
            }
        }
        snails.removeAll { it.x < -SNAIL_SIZE || it.height < -WORLD_HEIGHT_UNITS }
    }

    /** Horseshoes, carrots, mushrooms and puddles lie still on the track: they only move with the ground. */
    fun scrollPickups(scroll: Float) {
        horseshoes.scrollAlong(scroll) { it.x < -SNAIL_SIZE }
        carrots.scrollAlong(scroll) { it.x < -SNAIL_SIZE }
        mud.scrollAlong(scroll) { it.x + it.width < 0f }
        mushrooms.scrollAlong(scroll) { it.x < -SNAIL_SIZE }
        gems.scrollAlong(scroll) { it.x < -SNAIL_SIZE }
        mounds.scrollAlong(scroll) { it.x < -SNAIL_SIZE }
    }

    /**
     * Makes sure exactly the right fences lie ahead for the super jump: everything not yet on screen
     * is replaced by a tight row, so the leap always clears [count] fences and lands in a normal gap
     * after the last one. Returns the last fence of the row.
     */
    fun prepareFenceRow(count: Int, gapSeconds: Float, worldWidth: Float, speed: Float, elapsed: Float): Fence? {
        val offscreen = fences.filter { it.x > worldWidth }.toSet()
        fences.removeAll(offscreen)
        snails.removeAll { snail ->
            snail.onFence?.let { it in offscreen } == true || (snail.kind == SnailKind.CRAWLER && snail.x > worldWidth)
        }
        horseshoes.removeAll { it.x > worldWidth }
        carrots.removeAll { it.x > worldWidth }
        mud.removeAll { it.x > worldWidth }
        mushrooms.removeAll { it.x > worldWidth }
        gems.removeAll { it.x > worldWidth }
        mounds.removeAll { it.x > worldWidth }

        val hitLeft = HORSE_X + HITBOX_LEFT
        val ahead = fences.filter { it.x + it.width > hitLeft }.sortedBy { it.x }
        if (ahead.size >= count) return ahead[count - 1]

        val rowGap = speed * gapSeconds
        var last = ahead.lastOrNull()
        repeat(count - ahead.size) {
            // Never pop a fence into view - the row continues right of the visible area
            val x = max(worldWidth, last?.let { it.x + it.width + rowGap } ?: worldWidth)
            last = addFence(x = x, gapAfter = rowGap, speed = speed, elapsed = elapsed)
        }
        // Normal spawning resumes with a regular gap after the row
        val rowEnd = last?.let { it.x + it.width } ?: worldWidth
        nextFenceIn = rowEnd + RodeoDifficulty.randomFenceGap(speed, elapsed) - worldWidth
        return last
    }

    fun fenceUis(): List<RodeoFenceUi> = fences.map { fence ->
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

    /** [clock] drives the wobbling (see RodeoPack.clock). Lassoed snails are drawn with the lasso. */
    fun snailUis(clock: Float): List<RodeoSnailUi> = snails.mapNotNull { snail ->
        when (snail.state) {
            SnailState.ACTIVE -> {
                val wobble = if (snail.kind == SnailKind.RUNNER) {
                    sin(clock + snail.phase) * 8f
                } else {
                    sin(clock * 0.3f + snail.phase) * 3f
                }
                RodeoSnailUi(snail.x, snail.height, facingLeft = true, tiltDeg = wobble, runner = snail.kind == SnailKind.RUNNER)
            }
            SnailState.KNOCKED -> RodeoSnailUi(snail.x, snail.height, facingLeft = true, tiltDeg = snail.spin, runner = snail.kind == SnailKind.RUNNER)
            SnailState.LASSOED -> null
        }
    }

    /** Horseshoes bob and sway gently, driven by [clock]. */
    fun horseshoeUis(clock: Float): List<RodeoHorseshoeUi> = horseshoes.map { shoe ->
        RodeoHorseshoeUi(
            x = shoe.x,
            height = shoe.height + sin(clock * 0.4f + shoe.phase) * 1.2f,
            tiltDeg = sin(clock * 0.25f + shoe.phase) * 12f,
        )
    }


    /** Carrots bob a little, like the horseshoes, driven by [clock]. */
    fun carrotUis(clock: Float): List<RodeoCarrotUi> = carrots.map { carrot ->
        RodeoCarrotUi(
            x = carrot.x,
            height = carrot.height + sin(clock * 0.4f + carrot.phase) * 1f,
            tiltDeg = 30f + sin(clock * 0.3f + carrot.phase) * 15f,
        )
    }

    /** Crystals bob and turn slowly, driven by [clock]. */
    fun gemUis(clock: Float): List<RodeoGemUi> = gems.map { gem ->
        RodeoGemUi(
            x = gem.x,
            height = gem.height + sin(clock * 0.35f + gem.phase) * 1.2f,
            tiltDeg = sin(clock * 0.2f + gem.phase) * 10f,
            hue = gem.hue,
        )
    }

    fun gapUis(): List<RodeoGapUi> = gaps.map { RodeoGapUi(x = it.x, width = it.width) }

    fun mushroomUis(): List<RodeoMushroomUi> = mushrooms.map { RodeoMushroomUi(x = it.x, seed = it.seed) }

    fun moundUis(): List<RodeoMoundUi> = mounds.map {
        RodeoMoundUi(x = it.x, seed = it.seed, hasShovel = it.hasShovel, shovelX = it.shovelX, shovelY = it.shovelY)
    }

    fun mudUis(): List<RodeoMudUi> = mud.map { RodeoMudUi(x = it.x, width = it.width, seed = it.seed) }
}

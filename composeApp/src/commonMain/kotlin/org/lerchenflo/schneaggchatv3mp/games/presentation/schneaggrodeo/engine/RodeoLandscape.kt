package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSectionUi
import kotlin.random.Random

// Stretches of landscape the track runs through now and then, taking turns:
//  - a mountain: the track runs up and down it. Uphill the horse slows and tires three times as
//    fast, downhill it races. A cable car picks horse and rider up before it (see
//    vehicles/cablecar) - miss it and it's the slog over the top.
//  - a forest: trees in the background and magic mushrooms on the ground.
// Sections lie on the track like fences: they scroll with the ground, [x] is where they begin.

/** Game seconds until the first section, and between the end of one and the next. */
private const val SECTION_FIRST_SECONDS = 55f
private const val SECTION_INTERVAL_MIN = 45f
private const val SECTION_INTERVAL_RANDOM = 30f

// Mountain shape (units along the track and up): the silhouette behind the track, and the cable of
// the cable car from the valley station before it over a mast on the peak to the one after it
internal const val MOUNTAIN_UP = 450f
internal const val MOUNTAIN_DOWN = 300f
internal const val MOUNTAIN_WIDTH = MOUNTAIN_UP + MOUNTAIN_DOWN
internal const val MOUNTAIN_HEIGHT = 58f
internal const val CABLE_STATION_MARGIN = 30f
internal const val CABLE_FOOT_HEIGHT = 44f
internal const val CABLE_PEAK_HEIGHT = 82f
/** Units ahead of the valley station the mountain is placed, so the cable car passes first. */
private const val MOUNTAIN_LEAD_MARGIN = 40f

internal const val FOREST_WIDTH = 1500f

/** The horse's pace on the mountain, and how much faster it tires uphill. */
internal const val UPHILL_SPEED_FACTOR = 0.7f
internal const val DOWNHILL_SPEED_FACTOR = 1.25f
internal const val UPHILL_TIRE_FACTOR = 3f

internal enum class SectionKind { MOUNTAIN, FOREST }

/** Where the track runs under the horse. */
internal enum class Slope { FLAT, UPHILL, DOWNHILL }

internal class LandscapeSection(val kind: SectionKind, var x: Float, val width: Float) {
    val seed = Random.nextInt(1000)
}

/** Height of the cable car's cable at track position [x], for a mountain beginning at [mountainX]. */
internal fun cableHeightAt(x: Float, mountainX: Float): Float {
    val start = mountainX - CABLE_STATION_MARGIN
    val peak = mountainX + MOUNTAIN_UP
    val end = mountainX + MOUNTAIN_WIDTH + CABLE_STATION_MARGIN
    return when {
        x <= start || x >= end -> CABLE_FOOT_HEIGHT
        x <= peak -> lerp(CABLE_FOOT_HEIGHT, CABLE_PEAK_HEIGHT, (x - start) / (peak - start))
        else -> lerp(CABLE_PEAK_HEIGHT, CABLE_FOOT_HEIGHT, (x - peak) / (end - peak))
    }
}

internal class RodeoLandscape {
    val sections = mutableListOf<LandscapeSection>()
    private var nextIn = SECTION_FIRST_SECONDS
    private var nextKind = SectionKind.entries.random()

    /** Nothing of a section is ahead or in the picture. */
    val isClear: Boolean get() = sections.isEmpty()

    val mountain: LandscapeSection? get() = sections.firstOrNull { it.kind == SectionKind.MOUNTAIN }
    val forest: LandscapeSection? get() = sections.firstOrNull { it.kind == SectionKind.FOREST }

    fun reset() {
        sections.clear()
        nextIn = SECTION_FIRST_SECONDS
        nextKind = SectionKind.entries.random()
    }

    fun scroll(scroll: Float, worldWidth: Float) {
        sections.forEach { it.x -= scroll }
        // Kept until it is well out of the picture on the left, so the parallax backdrop can slide out
        sections.removeAll { it.x + it.width < -worldWidth }
    }

    /**
     * Counts down while [allowed] and nothing is in view; returns the kind of the section it just
     * placed. A mountain is placed so that a cable car starting at the right edge now at [passSpeed]
     * u/s has passed the horse (riding at [pace]) before the valley station arrives.
     */
    fun tick(dt: Float, worldWidth: Float, pace: Float, passSpeed: Float, allowed: Boolean): SectionKind? {
        if (!allowed || !isClear) return null
        nextIn -= dt
        if (nextIn > 0f) return null
        nextIn = SECTION_INTERVAL_MIN + Random.nextFloat() * SECTION_INTERVAL_RANDOM
        val kind = nextKind
        nextKind = if (kind == SectionKind.MOUNTAIN) SectionKind.FOREST else SectionKind.MOUNTAIN
        place(kind, worldWidth, pace, passSpeed)
        return kind
    }

    /** Places a section of [kind] now (see [tick]). */
    private fun place(kind: SectionKind, worldWidth: Float, pace: Float, passSpeed: Float) {
        when (kind) {
            SectionKind.MOUNTAIN -> {
                val passSeconds = (worldWidth - HORSE_X) / passSpeed
                val stationX = HORSE_X + pace * passSeconds + MOUNTAIN_LEAD_MARGIN
                sections.add(LandscapeSection(kind, x = stationX + CABLE_STATION_MARGIN, width = MOUNTAIN_WIDTH))
            }
            SectionKind.FOREST -> sections.add(LandscapeSection(kind, x = worldWidth, width = FOREST_WIDTH))
        }
    }

    /** Up, down or flat at track position [x]. */
    fun slopeAt(x: Float): Slope {
        val mountain = mountain ?: return Slope.FLAT
        return when (x - mountain.x) {
            in 0f..MOUNTAIN_UP -> Slope.UPHILL
            in MOUNTAIN_UP..MOUNTAIN_WIDTH -> Slope.DOWNHILL
            else -> Slope.FLAT
        }
    }

    /** The stretch from [from] to [to] lies (partly) in a forest. */
    fun inForest(from: Float, to: Float): Boolean {
        val forest = forest ?: return false
        return forest.x < to && forest.x + forest.width > from
    }

    fun ui(): List<RodeoSectionUi> = sections.map {
        RodeoSectionUi(isMountain = it.kind == SectionKind.MOUNTAIN, x = it.x, width = it.width, seed = it.seed)
    }
}

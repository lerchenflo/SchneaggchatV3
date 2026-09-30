package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPortalUi
import kotlin.math.abs
import kotlin.random.Random

// The other maps: once in a long while the track leads somewhere else, each map with its own look,
// obstacles, pickups and vehicles:
//  - the cave ("Stollen"): stalagmites instead of fences, crystals, more mushrooms
//  - the mine: rock piles, gold nuggets, dirt mounds to dig through, a mine cart and a drill
//  - the open sea: the horse swims; buoys instead of fences, sharks, oil slicks, pearls, and boats,
//    a U-boat, an oil tanker, a pirate ship and a flamingo to ride
// The cave and the mine are reached through a shaft at the foot of a hill: gallop into it (hooves
// on the ground) to go down, jump over it to stay up on the main track. The sea begins at the end
// of a beach. After a while a ramp leads back up (out of the shaft, or onto a harbour pier). The
// ground is shaped for each of them (see RodeoTerrain's TerrainFeature). Which map comes next is
// shuffled per run, so every run goes somewhere else first.
//
// Switching maps fades to black; at the darkest moment the track is cleared and the other map
// begins (see SchneaggRodeoEngine.switchMap).

/** Game seconds until the first way down, and between two (counted on the surface only). */
private const val ENTRANCE_FIRST_SECONDS = 90f
private const val ENTRANCE_INTERVAL_MIN = 150f
private const val ENTRANCE_INTERVAL_RANDOM = 90f
/** Game seconds on another map until the way out shows up; the big sea lasts longer. */
private const val UNDERGROUND_SECONDS = 30f
private const val SEA_SECONDS = 60f
/** Width of the hole in the track, and of the shaft of light. */
internal const val PORTAL_WIDTH = 24f
/** Height of the water surface in the underground sea: the horse swims with its legs below it. */
internal const val SEA_WATER_LINE = 5f
/** Height of the seabed of the open sea, far below the surface the horse swims on. */
internal const val SEABED_Y = -30f
/** Real seconds of the fade to black and back. */
private const val FADE_SECONDS = 1f

enum class RodeoMap {
    SURFACE, CAVE, SEA, MINE;

    val isUnderground: Boolean get() = this != SURFACE

    /** Dark with rock all around (the open sea has the sky above it). */
    val isEnclosed: Boolean get() = this == CAVE || this == MINE
}

/** A way to the other map: an entrance on the surface, the light shaft back up; [x] its left edge. */
internal class Portal(var x: Float, val destination: RodeoMap) {
    /** The horse went through; it only works once. */
    var used = false
}

internal class RodeoUnderground {
    var map = RodeoMap.SURFACE
        private set
    val portals = mutableListOf<Portal>()
    private var nextEntranceIn = ENTRANCE_FIRST_SECONDS
    private var undergroundTime = 0f
    /** Underground maps still to come, in random order; refilled once all were visited. */
    private val upcoming = mutableListOf<RodeoMap>()
    private var lastUnderground: RodeoMap? = null
    /** Seconds into the fade, or < 0 while none is running. */
    private var fadeTime = -1f
    private var switched = false
    private var switchTo = RodeoMap.SURFACE

    /** 0..1 how dark the picture is. */
    val fade: Float
        get() = if (fadeTime < 0f) 0f else 1f - abs(2f * fadeTime / FADE_SECONDS - 1f)

    val isUnderground: Boolean get() = map.isUnderground

    /** No portal is ahead and no fade is running: the map may bring other things. */
    val isClear: Boolean get() = portals.isEmpty() && fadeTime < 0f

    /** A way down (or back up) is due: nothing new comes along until it opened, so it can't be crowded out. */
    val isDue: Boolean
        get() = isClear && if (map == RodeoMap.SURFACE) nextEntranceIn <= 0f else undergroundTime >= visitSeconds

    private val visitSeconds: Float get() = if (map == RodeoMap.SEA) SEA_SECONDS else UNDERGROUND_SECONDS

    fun reset() {
        map = RodeoMap.SURFACE
        portals.clear()
        nextEntranceIn = ENTRANCE_FIRST_SECONDS
        undergroundTime = 0f
        upcoming.clear()
        lastUnderground = null
        fadeTime = -1f
    }

    /** Test mode: the run is on [map] right away, with no way in or out. */
    fun startOn(map: RodeoMap) {
        this.map = map
    }

    fun scroll(scroll: Float) {
        portals.forEach { it.x -= scroll }
        portals.removeAll { it.x + PORTAL_WIDTH < 0f }
    }

    /**
     * Counts down to the next way to another map (on the surface) or back (on the other map); once
     * due, it is laid out ahead as soon as the track is clear ([allowed]). [shapeGround] shapes the
     * ground for it and returns where it lies.
     */
    fun tick(dt: Float, allowed: Boolean, shapeGround: (TerrainFeature) -> Float) {
        if (fadeTime >= 0f || portals.isNotEmpty()) return
        if (map == RodeoMap.SURFACE) {
            nextEntranceIn -= dt
            if (nextEntranceIn <= 0f && allowed) {
                nextEntranceIn = ENTRANCE_INTERVAL_MIN + Random.nextFloat() * ENTRANCE_INTERVAL_RANDOM
                val destination = nextUnderground()
                val feature = if (destination == RodeoMap.SEA) TerrainFeature.BEACH else TerrainFeature.SHAFT
                portals.add(Portal(x = shapeGround(feature), destination = destination))
            }
        } else {
            undergroundTime += dt
            if (undergroundTime >= visitSeconds && allowed) {
                // The ramp ends at the way out
                portals.add(Portal(x = shapeGround(TerrainFeature.RAMP_UP) - PORTAL_WIDTH, destination = RodeoMap.SURFACE))
            }
        }
    }

    /** The next underground map: every one comes once in random order, never the same twice in a row. */
    private fun nextUnderground(): RodeoMap {
        if (upcoming.isEmpty()) {
            upcoming += listOf(RodeoMap.CAVE, RodeoMap.SEA, RodeoMap.MINE).shuffled()
            if (upcoming.first() == lastUnderground) upcoming.add(upcoming.removeAt(0))
        }
        return upcoming.removeAt(0).also { lastUnderground = it }
    }

    /**
     * Starts the fade once the horse's hitbox ([from]..[to]) is over a portal: into a shaft only
     * with the hooves [onGround] (jumping over it stays up), into the sea and out any way.
     */
    fun checkHorse(from: Float, to: Float, onGround: Boolean) {
        if (fadeTime >= 0f) return
        portals.forEach { portal ->
            val over = portal.x < to && portal.x + PORTAL_WIDTH > from
            if (!portal.used && over && (onGround || map.isUnderground || portal.destination == RodeoMap.SEA)) {
                portal.used = true
                fadeTime = 0f
                switched = false
                switchTo = portal.destination
            }
        }
    }

    /** Moves the fade on by [realDt]; [onSwitch] fires once at its darkest with the new map. */
    fun stepFade(realDt: Float, onSwitch: (RodeoMap) -> Unit) {
        if (fadeTime < 0f) return
        fadeTime += realDt
        if (!switched && fadeTime >= FADE_SECONDS / 2f) {
            switched = true
            map = switchTo
            portals.clear()
            undergroundTime = 0f
            onSwitch(map)
        }
        if (fadeTime >= FADE_SECONDS) fadeTime = -1f
    }

    fun ui(): List<RodeoPortalUi> = portals.map {
        RodeoPortalUi(x = it.x, width = PORTAL_WIDTH, exit = map.isUnderground, destination = it.destination, origin = map)
    }
}

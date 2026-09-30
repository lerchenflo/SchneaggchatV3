package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMapWayUi
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
private const val VISIT_SECONDS = 30f
private const val SEA_SECONDS = 60f
/** Width of a way to another map: the shaft in the track, the end of the beach or the ramp. */
internal const val MAP_WAY_WIDTH = 24f
/** Height of the water surface in the sea: the horse swims with its legs below it. */
internal const val SEA_WATER_LINE = 5f
/** Height of the seabed of the open sea, far below the surface the horse swims on. */
internal const val SEABED_Y = -30f
/** Real seconds of the fade to black and back. */
private const val FADE_SECONDS = 1f

enum class RodeoMap {
    SURFACE, CAVE, SEA, MINE;

    /** Away from the surface, on one of the other maps. */
    val isAway: Boolean get() = this != SURFACE

    /** Dark with rock all around (the open sea has the sky above it). */
    val isEnclosed: Boolean get() = this == CAVE || this == MINE
}

/** A way to another map: a shaft or the beach on the surface, the ramp back up; [x] its left edge. */
internal class MapWay(override var x: Float, val destination: RodeoMap) : OnTrack {
    /** The horse went through; it only works once. */
    var used = false
}

internal class RodeoMapSwitch {
    var map = RodeoMap.SURFACE
        private set
    val mapWays = mutableListOf<MapWay>()
    private var nextEntranceIn = ENTRANCE_FIRST_SECONDS
    private var awayTime = 0f
    /** Maps still to come, in random order; refilled once all were visited. */
    private val upcoming = mutableListOf<RodeoMap>()
    private var lastVisited: RodeoMap? = null
    /** Seconds into the fade, or < 0 while none is running. */
    private var fadeTime = -1f
    private var switched = false
    private var switchTo = RodeoMap.SURFACE

    /** 0..1 how dark the picture is. */
    val fade: Float
        get() = if (fadeTime < 0f) 0f else 1f - abs(2f * fadeTime / FADE_SECONDS - 1f)

    val isAway: Boolean get() = map.isAway

    /** No way to another map is ahead and no fade is running: the map may bring other things. */
    val isClear: Boolean get() = mapWays.isEmpty() && fadeTime < 0f

    /** A way down (or back up) is due: nothing new comes along until it opened, so it can't be crowded out. */
    val isDue: Boolean
        get() = isClear && if (map == RodeoMap.SURFACE) nextEntranceIn <= 0f else awayTime >= visitSeconds

    private val visitSeconds: Float get() = if (map == RodeoMap.SEA) SEA_SECONDS else VISIT_SECONDS

    fun reset() {
        map = RodeoMap.SURFACE
        mapWays.clear()
        nextEntranceIn = ENTRANCE_FIRST_SECONDS
        awayTime = 0f
        upcoming.clear()
        lastVisited = null
        fadeTime = -1f
    }

    /** Test mode: the run is on [map] right away, with no way in or out. */
    fun startOn(map: RodeoMap) {
        this.map = map
    }

    fun scroll(scroll: Float) {
        mapWays.scrollAlong(scroll) { it.x + MAP_WAY_WIDTH < 0f }
    }

    /**
     * Counts down to the next way to another map (on the surface) or back (on the other map); once
     * due, it is laid out ahead as soon as the track is clear ([allowed]). [shapeGround] shapes the
     * ground for it and returns where it lies.
     */
    fun tick(dt: Float, allowed: Boolean, shapeGround: (TerrainFeature) -> Float) {
        if (fadeTime >= 0f || mapWays.isNotEmpty()) return
        if (map == RodeoMap.SURFACE) {
            nextEntranceIn -= dt
            if (nextEntranceIn <= 0f && allowed) {
                nextEntranceIn = ENTRANCE_INTERVAL_MIN + Random.nextFloat() * ENTRANCE_INTERVAL_RANDOM
                val destination = nextDestination()
                val feature = if (destination == RodeoMap.SEA) TerrainFeature.BEACH else TerrainFeature.SHAFT
                mapWays.add(MapWay(x = shapeGround(feature), destination = destination))
            }
        } else {
            awayTime += dt
            if (awayTime >= visitSeconds && allowed) {
                // The ramp ends at the way out
                mapWays.add(MapWay(x = shapeGround(TerrainFeature.RAMP_UP) - MAP_WAY_WIDTH, destination = RodeoMap.SURFACE))
            }
        }
    }

    /** The next map to visit: every one comes once in random order, never the same twice in a row. */
    private fun nextDestination(): RodeoMap {
        if (upcoming.isEmpty()) {
            upcoming += listOf(RodeoMap.CAVE, RodeoMap.SEA, RodeoMap.MINE).shuffled()
            if (upcoming.first() == lastVisited) upcoming.add(upcoming.removeAt(0))
        }
        return upcoming.removeAt(0).also { lastVisited = it }
    }

    /**
     * Starts the fade once the horse's hitbox ([from]..[to]) is over a way: into a shaft only
     * with the hooves [onGround] (jumping over it stays up), into the sea and out any way.
     */
    fun checkHorse(from: Float, to: Float, onGround: Boolean) {
        if (fadeTime >= 0f) return
        mapWays.forEach { way ->
            val over = way.x < to && way.x + MAP_WAY_WIDTH > from
            if (!way.used && over && (onGround || map.isAway || way.destination == RodeoMap.SEA)) {
                way.used = true
                fadeTime = 0f
                switched = false
                switchTo = way.destination
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
            mapWays.clear()
            awayTime = 0f
            onSwitch(map)
        }
        if (fadeTime >= FADE_SECONDS) fadeTime = -1f
    }

    fun ui(): List<RodeoMapWayUi> = mapWays.map {
        RodeoMapWayUi(x = it.x, width = MAP_WAY_WIDTH, exit = map.isAway, destination = it.destination, origin = map)
    }
}

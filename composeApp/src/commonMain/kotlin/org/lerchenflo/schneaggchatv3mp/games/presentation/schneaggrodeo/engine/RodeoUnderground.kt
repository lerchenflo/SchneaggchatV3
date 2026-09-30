package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPortalUi
import kotlin.math.abs
import kotlin.random.Random

// The underground: once in a long while a way down opens up in the track. Gallop over it (hooves on
// the ground) and horse and rider drop into one of the underground maps, each with its own look,
// obstacles, pickups and vehicles:
//  - the cave ("Stollen"): stalagmites instead of fences, crystals, more mushrooms
//  - the underground sea: the horse swims; buoys instead of fences, sharks, oil slicks, pearls,
//    and boats, a U-boat, an oil tanker and a flamingo float to ride
//  - the mine: rock piles, gold nuggets, dirt mounds to dig through, and a mine cart
// Which one comes next is shuffled per run, so every run goes down somewhere else first. After a
// while a shaft of light shows the way back up. Jumping over the entrance leaves it be.
//
// Switching maps fades to black; at the darkest moment the track is cleared and the other map
// begins (see SchneaggRodeoEngine.switchMap).

/** Game seconds until the first way down, and between two (counted on the surface only). */
private const val ENTRANCE_FIRST_SECONDS = 90f
private const val ENTRANCE_INTERVAL_MIN = 150f
private const val ENTRANCE_INTERVAL_RANDOM = 90f
/** Game seconds underground until the way out shows up. */
private const val UNDERGROUND_SECONDS = 30f
/** Width of the hole in the track, and of the shaft of light. */
internal const val PORTAL_WIDTH = 24f
/** Height of the water surface in the underground sea: the horse swims with its legs below it. */
internal const val SEA_WATER_LINE = 5f
/** Real seconds of the fade to black and back. */
private const val FADE_SECONDS = 1f

enum class RodeoMap {
    SURFACE, CAVE, SEA, MINE;

    val isUnderground: Boolean get() = this != SURFACE
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
        get() = isClear && if (map == RodeoMap.SURFACE) nextEntranceIn <= 0f else undergroundTime >= UNDERGROUND_SECONDS

    fun reset() {
        map = RodeoMap.SURFACE
        portals.clear()
        nextEntranceIn = ENTRANCE_FIRST_SECONDS
        undergroundTime = 0f
        upcoming.clear()
        lastUnderground = null
        fadeTime = -1f
    }

    fun scroll(scroll: Float) {
        portals.forEach { it.x -= scroll }
        portals.removeAll { it.x + PORTAL_WIDTH < 0f }
    }

    /**
     * Counts down to the next way down (on the surface) or back up (underground); once due, it
     * opens at the right edge as soon as the track is clear ([allowed]).
     */
    fun tick(dt: Float, worldWidth: Float, allowed: Boolean) {
        if (fadeTime >= 0f || portals.isNotEmpty()) return
        if (map == RodeoMap.SURFACE) {
            nextEntranceIn -= dt
            if (nextEntranceIn <= 0f && allowed) {
                nextEntranceIn = ENTRANCE_INTERVAL_MIN + Random.nextFloat() * ENTRANCE_INTERVAL_RANDOM
                portals.add(Portal(x = worldWidth + 5f, destination = nextUnderground()))
            }
        } else {
            undergroundTime += dt
            if (undergroundTime >= UNDERGROUND_SECONDS && allowed) {
                portals.add(Portal(x = worldWidth + 5f, destination = RodeoMap.SURFACE))
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
     * Starts the fade once the horse's hitbox ([from]..[to]) is over a portal: the way down only
     * with the hooves [onGround], the light shaft any way.
     */
    fun checkHorse(from: Float, to: Float, onGround: Boolean) {
        if (fadeTime >= 0f) return
        portals.forEach { portal ->
            val over = portal.x < to && portal.x + PORTAL_WIDTH > from
            if (!portal.used && over && (onGround || map.isUnderground)) {
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
        RodeoPortalUi(x = it.x, width = PORTAL_WIDTH, exit = map.isUnderground, destination = it.destination)
    }
}

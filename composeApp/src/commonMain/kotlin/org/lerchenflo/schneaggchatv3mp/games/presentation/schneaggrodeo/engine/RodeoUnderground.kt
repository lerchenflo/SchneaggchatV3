package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPortalUi
import kotlin.math.abs
import kotlin.random.Random

// The underground: once in a long while a mine shaft opens up in the track. Gallop over it (hooves
// on the ground) and horse and rider drop into the "Stollen", a cave with its own look: stalagmites
// instead of fences, crystals to collect, more mushrooms, no vehicles. After a while a shaft of
// light shows the way back up. Jumping over the shaft leaves it be.
//
// Switching maps fades to black; at the darkest moment the track is cleared and the other map
// begins (see SchneaggRodeoEngine.switchMap).

/** Game seconds until the first mine shaft, and between two (counted on the surface only). */
private const val ENTRANCE_FIRST_SECONDS = 90f
private const val ENTRANCE_INTERVAL_MIN = 150f
private const val ENTRANCE_INTERVAL_RANDOM = 90f
/** Game seconds in the cave until the way out shows up. */
private const val CAVE_SECONDS = 30f
/** Width of the hole in the track, and of the shaft of light. */
internal const val PORTAL_WIDTH = 24f
/** Real seconds of the fade to black and back. */
private const val FADE_SECONDS = 1f

internal enum class RodeoMap { SURFACE, CAVE }

/** A way to the other map: the mine shaft on the surface, the light shaft in the cave; [x] its left edge. */
internal class Portal(var x: Float) {
    /** The horse went through; it only works once. */
    var used = false
}

internal class RodeoUnderground {
    var map = RodeoMap.SURFACE
        private set
    val portals = mutableListOf<Portal>()
    private var nextEntranceIn = ENTRANCE_FIRST_SECONDS
    private var caveTime = 0f
    /** Seconds into the fade, or < 0 while none is running. */
    private var fadeTime = -1f
    private var switched = false

    /** 0..1 how dark the picture is. */
    val fade: Float
        get() = if (fadeTime < 0f) 0f else 1f - abs(2f * fadeTime / FADE_SECONDS - 1f)

    val isInCave: Boolean get() = map == RodeoMap.CAVE

    /** No portal is ahead and no fade is running: the surface may bring other things. */
    val isClear: Boolean get() = portals.isEmpty() && fadeTime < 0f

    fun reset() {
        map = RodeoMap.SURFACE
        portals.clear()
        nextEntranceIn = ENTRANCE_FIRST_SECONDS
        caveTime = 0f
        fadeTime = -1f
    }

    fun scroll(scroll: Float) {
        portals.forEach { it.x -= scroll }
        portals.removeAll { it.x + PORTAL_WIDTH < 0f }
    }

    /**
     * Opens a mine shaft at the right edge once it is due and [allowed] (on the surface), shows the
     * way out once the cave time is over.
     */
    fun tick(dt: Float, worldWidth: Float, allowed: Boolean) {
        if (fadeTime >= 0f || portals.isNotEmpty()) return
        when (map) {
            RodeoMap.SURFACE -> {
                if (!allowed) return
                nextEntranceIn -= dt
                if (nextEntranceIn <= 0f) {
                    nextEntranceIn = ENTRANCE_INTERVAL_MIN + Random.nextFloat() * ENTRANCE_INTERVAL_RANDOM
                    portals.add(Portal(x = worldWidth + 5f))
                }
            }
            RodeoMap.CAVE -> {
                caveTime += dt
                if (caveTime >= CAVE_SECONDS) portals.add(Portal(x = worldWidth + 5f))
            }
        }
    }

    /**
     * Starts the fade once the horse's hitbox ([from]..[to]) is over a portal: the mine shaft only
     * with the hooves [onGround], the light shaft any way.
     */
    fun checkHorse(from: Float, to: Float, onGround: Boolean) {
        if (fadeTime >= 0f) return
        portals.forEach { portal ->
            val over = portal.x < to && portal.x + PORTAL_WIDTH > from
            if (!portal.used && over && (onGround || map == RodeoMap.CAVE)) {
                portal.used = true
                fadeTime = 0f
                switched = false
            }
        }
    }

    /** Moves the fade on by [realDt]; [onSwitch] fires once at its darkest with the new map. */
    fun stepFade(realDt: Float, onSwitch: (RodeoMap) -> Unit) {
        if (fadeTime < 0f) return
        fadeTime += realDt
        if (!switched && fadeTime >= FADE_SECONDS / 2f) {
            switched = true
            map = if (map == RodeoMap.SURFACE) RodeoMap.CAVE else RodeoMap.SURFACE
            portals.clear()
            caveTime = 0f
            onSwitch(map)
        }
        if (fadeTime >= FADE_SECONDS) fadeTime = -1f
    }

    fun ui(): List<RodeoPortalUi> = portals.map { RodeoPortalUi(x = it.x, width = PORTAL_WIDTH, exit = map == RodeoMap.CAVE) }
}

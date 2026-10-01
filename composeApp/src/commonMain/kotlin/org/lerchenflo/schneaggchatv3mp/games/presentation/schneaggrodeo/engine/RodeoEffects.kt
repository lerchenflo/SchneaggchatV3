package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyPart
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoDustUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoLostPartUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSparkleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind

private const val DUST_SECONDS = 0.5f
private const val SPLASH_SECONDS = 0.35f
private const val SPARKLE_SECONDS = 0.45f
private const val LOST_PART_SECONDS = 1.2f
/** How long the banner naming a boarded vehicle stays up, in real seconds. */
private const val ANNOUNCEMENT_SECONDS = 1.8f

/**
 * Short-lived eye candy without any effect on the game: one dust cloud, one landing splash, one
 * sparkle and one banner at a time - a new one simply replaces the old one.
 */
internal class RodeoEffects {
    private var dustTime = 0f     // each counts down while its effect is shown
    private var dustX = 0f
    private var splashTime = 0f
    private var sparkleTime = 0f
    private var sparkleX = 0f
    private var sparkleY = 0f
    private var announcementKind = RodeoVehicleKind.PLANE
    private var announcementTime = 0f
    private var lostPart = RodeoCowboyPart.HAT
    private var lostPartTime = 0f
    private var lostPartX = 0f
    private var lostPartY = 0f

    /** Banner currently shown over the track, if any. */
    val announcement: RodeoVehicleKind? get() = announcementKind.takeIf { announcementTime > 0f }

    fun reset() {
        dustTime = 0f
        splashTime = 0f
        sparkleTime = 0f
        announcementTime = 0f
        lostPartTime = 0f
    }

    fun tick(dt: Float) {
        dustTime = countDown(dustTime, dt)
        splashTime = countDown(splashTime, dt)
        sparkleTime = countDown(sparkleTime, dt)
        announcementTime = countDown(announcementTime, dt)
        lostPartTime = countDown(lostPartTime, dt)
    }

    /** Puffs of dust rolling out on the ground at [x] (crashes, landings, rammed fences). */
    fun dust(x: Float) {
        dustTime = DUST_SECONDS
        dustX = x
    }

    /** Dirt spraying from the horse's hooves as it lands. */
    fun splash() {
        splashTime = SPLASH_SECONDS
    }

    /** Sparks at [x] / [y] (bonus points, a lucky charm taking a hit). */
    fun sparkle(x: Float, y: Float) {
        sparkleTime = SPARKLE_SECONDS
        sparkleX = x
        sparkleY = y
    }

    /** A [part] the ravens pecked off the cowboy flies away from [x] / [y]. */
    fun losePart(part: RodeoCowboyPart, x: Float, y: Float) {
        lostPart = part
        lostPartTime = LOST_PART_SECONDS
        lostPartX = x
        lostPartY = y
    }

    fun announce(kind: RodeoVehicleKind) {
        announcementKind = kind
        announcementTime = ANNOUNCEMENT_SECONDS
    }

    fun dustUi(): RodeoDustUi? =
        if (dustTime > 0f) RodeoDustUi(x = dustX, progress = 1f - dustTime / DUST_SECONDS) else null

    fun splashProgress(): Float? = if (splashTime > 0f) 1f - splashTime / SPLASH_SECONDS else null

    fun lostPartUi(): RodeoLostPartUi? =
        if (lostPartTime > 0f) RodeoLostPartUi(lostPart, lostPartX, lostPartY, progress = 1f - lostPartTime / LOST_PART_SECONDS) else null

    fun sparkleUi(): RodeoSparkleUi? =
        if (sparkleTime > 0f) RodeoSparkleUi(x = sparkleX, y = sparkleY, progress = 1f - sparkleTime / SPARKLE_SECONDS) else null
}

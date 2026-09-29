package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.tractor

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.GROUND_OFFSET_UNITS
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SNAIL_SIZE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoDeckVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Lawn tractor: a red lawn tractor rolls by on the ground. Lasso it and the horse hops onto its
// deck, cowboy and all, for a short ride at an absurd speed. The world races by, but points keep
// coming in at the normal pace; everything in the way is mowed flat without a penalty - but every
// fence rips a part off. It is the special one of the rotation: it only comes once every other
// vehicle came by.

// Shape, shared with the drawing (grid: x from the tractor's left edge, y up from the ground)
internal const val TRACTOR_DECK_HEIGHT = 7f          // the horse's hooves stand on this
// Every fence mowed rips off one part; the hit after the last part wrecks it. The ride spaces its
// fences so that happens right around the end of the ride.
internal const val TRACTOR_PARTS = 10
// The parts in the order they come off
internal const val PART_HEADLIGHT = 0
internal const val PART_EXHAUST = 1
internal const val PART_STEERING_WHEEL = 2
internal const val PART_SEAT = 3
internal const val PART_GRILLE = 4
internal const val PART_FENDER = 5
internal const val PART_HOOD = 6
internal const val PART_MOWER_DECK = 7
internal const val PART_FRONT_WHEEL = 8
internal const val PART_REAR_WHEEL = 9

private const val TRACTOR_LENGTH = 40f
private const val TRACTOR_PASS_SPEED = 30f           // u/s across the screen while rolling by
private const val TRACTOR_HITCH_X = 4f               // where the lasso grabs it
private const val TRACTOR_HITCH_Y = 6f
private const val TRACTOR_RIDE_SECONDS = 7f
private const val TRACTOR_SCROLL_SPEED = 1500f       // u/s the world races by while riding
private const val TRACTOR_RIDE_X = HORSE_X - 3f      // horse centered on the deck
private const val TRACTOR_RUMBLE_DEGREES = 0.8f
private const val TRACTOR_FIRST_FENCE_SECONDS = 0.3f
private const val TRACTOR_MIN_FENCE_SECONDS = 0.25f
private const val TRACTOR_LAST_HIT_MARGIN = 0.3f     // the wrecking fence arrives this long before the ride ends
private const val TRACTOR_WRECK_TILT = 25f           // degrees the wreck tips over
private const val TRACTOR_WRECK_TILT_SPEED = 120f    // degrees per second
private const val DEBRIS_GRAVITY = 150f
/** Bits of scrap the wreck scatters on top of the parts lost before. */
private const val WRECK_SCRAP_PIECES = 4
/** The inside gag: shown on the speedometer while riding the tractor. */
private const val TRACTOR_SPEED_KMH = 65_000

/**
 * A part torn off the tractor, flying in screen space. [part] is the index of the lost part (see
 * the PART_ constants), or [TRACTOR_PARTS] for a bit of scrap from the wreck.
 */
private class Debris(
    var x: Float,
    var y: Float,
    val vx: Float,
    var vy: Float,
    val spin: Float,
    val part: Int,
) {
    var rotation = 0f
}

internal class RodeoLawnTractor : RodeoDeckVehicle(RodeoVehicleKind.LAWN_TRACTOR) {

    private val debris = mutableListOf<Debris>()
    private var partsLost = 0
    private var wrecked = false
    private var rotation = 0f
    private var fenceIn = 0f

    override val special = true
    override val length = TRACTOR_LENGTH
    override val passSpeed = TRACTOR_PASS_SPEED
    override val deckHeight = TRACTOR_DECK_HEIGHT
    override val rideX = TRACTOR_RIDE_X
    override val rideSeconds = TRACTOR_RIDE_SECONDS
    override val wheelTurn = 0.25f
    override val riderLeans: Boolean get() = isRiding
    override val shaking: Boolean get() = isRiding

    override fun hitch() = (x + TRACTOR_HITCH_X) to TRACTOR_HITCH_Y

    override fun reset() {
        super.reset()
        debris.clear()
    }

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        partsLost = 0
        wrecked = false
        rotation = 0f
    }

    override fun worldScroll(world: RodeoWorld, step: Float, dt: Float): Float =
        if (isRiding) TRACTOR_SCROLL_SPEED * dt else step

    override fun speedKmh(world: RodeoWorld): Int? = if (isRiding) TRACTOR_SPEED_KMH else null

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? = if (isRiding) {
        // Rattling along on the deck, hat flapping in the wind
        RodeoRidePose(
            pitch = TRACTOR_RUMBLE_DEGREES * sin(runTimeSeconds * 70f),
            hatLift = 1f + 0.8f * sin(runTimeSeconds * 45f),
        )
    } else null

    override fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) {
        debris.forEach { piece ->
            piece.vy -= DEBRIS_GRAVITY * dt
            piece.x += piece.vx * dt
            piece.y += piece.vy * dt
            piece.rotation += piece.spin * dt
        }
        debris.removeAll { it.y < -GROUND_OFFSET_UNITS || it.x < -SNAIL_SIZE }
    }

    override fun onRideStart() {
        fenceIn = TRACTOR_FIRST_FENCE_SECONDS
    }

    override fun ride(world: RodeoWorld, dt: Float) {
        fenceIn -= dt
        if (fenceIn <= 0f) {
            world.addFence(x = world.worldWidth, gapAfter = TRACTOR_LENGTH)
            fenceIn = nextFenceDelay(world)
        }
        // Everything in front of the mower deck is mowed flat; each fence costs a part
        world.clearTrack(x, x + TRACTOR_LENGTH) { fence -> if (!wrecked) hit(world, fence.x) }
        if (wrecked || rideIsOver) {
            if (!wrecked) wreck(world)
            getOff(world)
        }
    }

    /** The wreck tips over and stays behind on the track. */
    override fun whileUnloading(dt: Float, scroll: Float) {
        x -= scroll
        rotation = min(TRACTOR_WRECK_TILT, rotation + TRACTOR_WRECK_TILT_SPEED * dt)
    }

    override fun leave(world: RodeoWorld, dt: Float, scroll: Float) {
        x -= scroll
        if (x + TRACTOR_LENGTH < 0f && debris.isEmpty()) enter(VehiclePhase.IDLE)
    }

    /**
     * Spaces the ride's fences so the hit that wrecks the tractor lands shortly before the ride is
     * over. Fences already on their way count as hits too.
     */
    private fun nextFenceDelay(world: RodeoWorld): Float {
        val pending = world.fences.count { !it.knocked && it.x + it.width > x }
        val hitsLeft = TRACTOR_PARTS + 1 - partsLost - pending
        if (hitsLeft <= 0) return TRACTOR_RIDE_SECONDS // enough on the way; nothing more this ride
        val timeLeft = TRACTOR_RIDE_SECONDS - TRACTOR_LAST_HIT_MARGIN - phaseTime
        return max(TRACTOR_MIN_FENCE_SECONDS, timeLeft / hitsLeft)
    }

    /** Where each part sits on the tractor, relative to its left edge / the ground (see drawTractor). */
    private fun partAnchor(part: Int): Pair<Float, Float> = when (part) {
        PART_HEADLIGHT -> 38.8f to 9.3f
        PART_EXHAUST -> 36f to 12f
        PART_STEERING_WHEEL -> 27f to 12f
        PART_SEAT -> 1.5f to 10f
        PART_GRILLE -> 40.2f to 7f
        PART_FENDER -> 7f to 9f
        PART_HOOD -> 34f to 8f
        PART_MOWER_DECK -> 20f to 1.5f
        PART_FRONT_WHEEL -> 33f to 2.8f
        PART_REAR_WHEEL -> 7f to 4.5f
        else -> 15f to 6f // scrap from the chassis
    }

    /** Flings [part] off the tractor, up and backwards over the screen. */
    private fun throwDebris(part: Int) {
        val (anchorX, anchorY) = partAnchor(part)
        debris.add(
            Debris(
                x = x + anchorX,
                y = anchorY,
                vx = -(50f + Random.nextFloat() * 70f),
                vy = 40f + Random.nextFloat() * 35f,
                spin = (if (Random.nextBoolean()) 1f else -1f) * (300f + Random.nextFloat() * 500f),
                part = part,
            )
        )
    }

    /** A fence was mowed: one more part flies off, or the tractor is wrecked once none are left. */
    private fun hit(world: RodeoWorld, fenceX: Float) {
        world.dust(fenceX)
        if (partsLost < TRACTOR_PARTS) {
            throwDebris(partsLost)
            partsLost++
        } else {
            wreck(world)
        }
    }

    private fun wreck(world: RodeoWorld) {
        // Whatever was still attached comes off at once
        while (partsLost < TRACTOR_PARTS) throwDebris(partsLost++)
        repeat(WRECK_SCRAP_PIECES) { throwDebris(TRACTOR_PARTS) }
        wrecked = true
        world.sparkle(x + TRACTOR_LENGTH - 4f, TRACTOR_DECK_HEIGHT)
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoTractorUi(
            x = x,
            rotation = rotation,
            wheelPhase = wheelPhase,
            partsLost = partsLost,
            wrecked = wrecked,
            exhaust = isRiding,
            debris = debris.map { RodeoDebrisUi(x = it.x, y = it.y, rotation = it.rotation, part = it.part) },
            lassoHint = lassoHint(x + TRACTOR_LENGTH / 2f, 22f),
        )
    }
}

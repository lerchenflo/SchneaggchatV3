package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pirateship

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HAND_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HITBOX_LEFT
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HITBOX_RIGHT
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SnailState
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoDeckVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Pirate ship (underground sea): a wooden ship with a black skull flag sails by. Lasso it and the
// horse hops up onto its deck. Its bow cannon fires on its own at every shark and snail ahead
// (points for each hit), and treasure chests come floating by - the horse grabs them passing
// through. No input needed; it pushes the buoys aside.

// Shape, shared with the drawing (grid: x from the stern, y up from the ground under the water)
internal const val SHIP_LENGTH = 64f
internal const val SHIP_DECK = 10f
/** The raised stern castle. */
internal const val STERN_CASTLE_WIDTH = 12f
internal const val STERN_CASTLE_TOP = 15f
internal const val FORE_MAST_X = 46f
internal const val MAIN_MAST_X = 26f
internal const val MAST_TOP = 40f
/** The bow cannon's muzzle. */
internal const val CANNON_X = SHIP_LENGTH - 5f
internal const val CANNON_Y = SHIP_DECK + 2f

private const val SHIP_PASS_SPEED = 20f
private const val SHIP_HITCH_Y = 8f
private const val SHIP_HITCH_AIM = HORSE_X + HAND_X + 16f
private const val LASSO_LEAD = 0.225f
/** Stern while riding: the horse stands on the deck in front of the stern castle. */
private const val SHIP_RIDE_X = HORSE_X - 14f
private const val SHIP_LEAVE_SPEED = 30f
private const val SHIP_ROLL_DEGREES = 1.2f
private const val FIRE_INTERVAL = 0.9f
/** How far ahead of the bow the cannon reaches. */
private const val CANNON_RANGE = 90f
private const val SHOT_SECONDS = 0.45f
private const val SHOT_ARC = 5f
private const val SHOT_POINTS = 15
private const val CHEST_POINTS = 25
private const val CHEST_INTERVAL_MIN = 1.6f
private const val CHEST_INTERVAL_RANDOM = 1.4f
/** Chests float right where horse and rider pass. */
private const val CHEST_HEIGHT = SHIP_DECK + 8f
private const val SMOKE_SECONDS = 0.5f

/** A cannonball on its way from the muzzle to [toX] (moving with the ground). */
private class Shot(val fromX: Float, var toX: Float, val toY: Float) {
    var time = 0f
    val progress: Float get() = time / SHOT_SECONDS
}

/** A treasure chest bobbing ahead; moves with the ground. */
private class Chest(var x: Float, val seed: Int)

internal class RodeoPirateShip : RodeoDeckVehicle(RodeoVehicleKind.PIRATE_SHIP) {

    private val shots = mutableListOf<Shot>()
    private val chests = mutableListOf<Chest>()
    private var fireIn = 0f
    private var chestIn = 0f
    /** Seconds since the cannon last fired, for the muzzle smoke. */
    private var smokeTime = SMOKE_SECONDS
    private var time = 0f

    override val length = SHIP_LENGTH
    override val passSpeed = SHIP_PASS_SPEED
    override val deckHeight = SHIP_DECK
    override val rideX = SHIP_RIDE_X
    override val rideSeconds = 10f
    override val boardSeconds = 0.7f
    override val hop = 8f

    /** The whole ship is a target. */
    override fun hitch() = (SHIP_HITCH_AIM + SHIP_PASS_SPEED * LASSO_LEAD).coerceIn(x + 2f, x + SHIP_LENGTH - 2f) to SHIP_HITCH_Y

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? =
        if (isRiding) RodeoRidePose(pitch = SHIP_ROLL_DEGREES * sin(runTimeSeconds * 1.5f)) else null

    override fun reset() {
        super.reset()
        shots.clear()
        chests.clear()
    }

    override fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) {
        time += dt
        smokeTime += dt
        chests.forEach { it.x -= scroll }
        chests.removeAll { it.x < -5f }
        shots.forEach {
            it.time += dt
            it.toX -= scroll
        }
        shots.removeAll { it.progress >= 1f }
    }

    override fun whileBoarding(world: RodeoWorld) = plow(world)

    override fun onRideStart() {
        fireIn = 0.3f
        chestIn = 0.8f
    }

    override fun ride(world: RodeoWorld, dt: Float) {
        plow(world)
        fireIn -= dt
        if (fireIn <= 0f) fire(world)
        chestIn -= dt
        if (chestIn <= 0f && !rideIsOverSoon) {
            chestIn = CHEST_INTERVAL_MIN + Random.nextFloat() * CHEST_INTERVAL_RANDOM
            chests += Chest(x = world.worldWidth + 3f, seed = Random.nextInt(100))
        }
        // Horse and rider grab the chests passing through
        chests.removeAll { chest ->
            val grabbed = chest.x > HORSE_X + HITBOX_LEFT && chest.x < HORSE_X + HITBOX_RIGHT + 4f
            if (grabbed) {
                world.addBonusPoints(CHEST_POINTS)
                world.sparkle(chest.x, CHEST_HEIGHT)
            }
            grabbed
        }
        if (rideIsOver) getOff(world)
    }

    /** No more chests in the last seconds, so none floats by out of reach after the ride. */
    private val rideIsOverSoon: Boolean get() = phaseTime > rideSeconds - 2f

    /** The bow cannon fires at the closest shark or snail in range; it is knocked away when the ball lands. */
    private fun fire(world: RodeoWorld) {
        val bow = x + SHIP_LENGTH
        val target = world.snails
            .filter { it.state == SnailState.ACTIVE && it.x > bow - 10f && it.x < bow + CANNON_RANGE }
            .minByOrNull { it.x }
        fireIn = if (target == null) 0.2f else FIRE_INTERVAL
        target ?: return
        smokeTime = 0f
        shots += Shot(fromX = x + CANNON_X, toX = target.x, toY = target.height + 2f)
        target.knock()
        world.addBonusPoints(SHOT_POINTS)
    }

    override fun leave(world: RodeoWorld, dt: Float, scroll: Float) {
        x += SHIP_LEAVE_SPEED * dt
        if (x > world.worldWidth && chests.isEmpty() && shots.isEmpty()) enter(VehiclePhase.IDLE)
    }

    /** Buoys in front of the bow are pushed aside. */
    private fun plow(world: RodeoWorld) {
        world.clearTrack(x, x + SHIP_LENGTH) { fence -> world.dust(fence.x) }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoPirateShipUi(
            x = x,
            time = time,
            muzzleSmoke = if (smokeTime < SMOKE_SECONDS) smokeTime / SMOKE_SECONDS else null,
            cannonballs = shots.map { shot ->
                val progress = shot.progress
                RodeoCannonballUi(
                    x = lerp(shot.fromX, shot.toX, progress),
                    y = lerp(CANNON_Y, shot.toY, progress) + SHOT_ARC * sin(PI.toFloat() * progress),
                )
            },
            chests = chests.map { RodeoTreasureChestUi(x = it.x, y = CHEST_HEIGHT + 0.8f * sin(time * 3f + it.seed), seed = it.seed) },
            lassoHint = lassoHint(x + SHIP_LENGTH / 2f, MAST_TOP + 4f),
        )
    }
}

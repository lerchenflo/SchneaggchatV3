package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.flamingo

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HAND_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.Horseshoe
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoDeckVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoRidePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

// Flamingo float (underground sea): a big pink inflatable flamingo bobs by. Lasso it and the horse
// sits down on its back. Every tap bounces horse and float high out of the water - grab the
// horseshoes floating up there. The float drifts over the buoys.

// Shape, shared with the drawing (grid: x from the tail, y up from the ground under the water)
internal const val FLAMINGO_LENGTH = 24f
/** The float's back, where the horse's hooves rest. */
internal const val FLAMINGO_BACK = 7f

private const val FLAMINGO_PASS_SPEED = 20f
private const val FLAMINGO_HITCH_Y = 6f
private const val FLAMINGO_HITCH_AIM = HORSE_X + HAND_X + 16f
private const val LASSO_LEAD = 0.225f
private const val FLAMINGO_RIDE_X = HORSE_X - 2f
private const val FLAMINGO_LEAVE_SPEED = 30f
private const val BOUNCE_VELOCITY = 75f
private const val BOUNCE_GRAVITY = 150f
private const val BOUNCE_POINTS = 2
private const val SHOE_INTERVAL = 0.8f
private const val SHOE_MIN_HEIGHT = 30f
private const val SHOE_MAX_HEIGHT = 48f

internal class RodeoFlamingo : RodeoDeckVehicle(RodeoVehicleKind.FLAMINGO) {

    /** Height of the bounce above the water, and its vertical speed. */
    private var bounce = 0f
    private var bounceSpeed = 0f
    private var bob = 0f
    private var shoeIn = 0f
    /** A bounce started since the last frame: it gives points. */
    private var bounced = false

    override val length = FLAMINGO_LENGTH
    override val passSpeed = FLAMINGO_PASS_SPEED
    override val deckHeight = FLAMINGO_BACK
    override val rideX = FLAMINGO_RIDE_X
    override val rideSeconds = 9f
    override val hop = 4f

    /** Horse and float bounce together. */
    override val horseLift: Float get() = super.horseLift + if (carriesRider) bounce else 0f

    /** The whole float is a target. */
    override fun hitch() = (FLAMINGO_HITCH_AIM + FLAMINGO_PASS_SPEED * LASSO_LEAD).coerceIn(x + 2f, x + FLAMINGO_LENGTH - 2f) to FLAMINGO_HITCH_Y

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        bounce = 0f
        bounceSpeed = 0f
    }

    override fun onJump(pressed: Boolean) {
        // Only from the water: a bounce can't be pushed on in mid-air
        if (pressed && isRiding && bounce <= 0.5f) {
            bounceSpeed = BOUNCE_VELOCITY
            bounced = true
        }
    }

    override fun ridePose(runTimeSeconds: Float): RodeoRidePose? =
        if (isRiding) RodeoRidePose(pitch = if (bounce > 0f) -6f else 2f * sin(bob * 2f)) else null

    override fun updateAlways(world: RodeoWorld, dt: Float, scroll: Float) {
        bob += dt
        if (bounce > 0f || bounceSpeed > 0f) {
            bounceSpeed -= BOUNCE_GRAVITY * dt
            bounce = max(0f, bounce + bounceSpeed * dt)
            if (bounce <= 0f) {
                if (bounceSpeed < -20f) world.splash()
                bounceSpeed = 0f
            }
        }
    }

    override fun whileBoarding(world: RodeoWorld) = drift(world)

    override fun onRideStart() {
        shoeIn = 0.4f
    }

    override fun ride(world: RodeoWorld, dt: Float) {
        drift(world)
        shoeIn -= dt
        if (shoeIn <= 0f) {
            shoeIn = SHOE_INTERVAL
            world.horseshoes.add(Horseshoe(x = world.worldWidth + 2f, height = SHOE_MIN_HEIGHT + Random.nextFloat() * (SHOE_MAX_HEIGHT - SHOE_MIN_HEIGHT)))
        }
        if (bounced) {
            bounced = false
            world.addBonusPoints(BOUNCE_POINTS)
        }
        if (rideIsOver && bounce <= 0f) getOff(world)
    }

    override fun leave(world: RodeoWorld, dt: Float, scroll: Float) {
        // Left behind, bobbing away with the water
        x -= scroll - FLAMINGO_LEAVE_SPEED * 0.3f * dt
        if (x + FLAMINGO_LENGTH < 0f) enter(VehiclePhase.IDLE)
    }

    /** Buoys and snails in the way are pushed aside. */
    private fun drift(world: RodeoWorld) {
        world.clearTrack(x, x + FLAMINGO_LENGTH) { fence -> world.dust(fence.x) }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoFlamingoUi(
            x = x,
            lift = if (carriesRider) bounce else 0f,
            bob = bob,
            lassoHint = lassoHint(x + FLAMINGO_LENGTH / 2f, FLAMINGO_BACK + 16f),
        )
    }
}

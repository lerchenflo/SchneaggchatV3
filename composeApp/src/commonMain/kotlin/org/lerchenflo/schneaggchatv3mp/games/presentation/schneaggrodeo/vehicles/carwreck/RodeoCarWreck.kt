package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.carwreck

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_LEG_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HAND_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.clearTrack
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

// Car wreck: a wrecked red Ford Escort Mk3 cabrio without wheels slides by on its belly, sparks
// flying. The lasso grabs whatever part of it is in reach. Lasso it and the
// cowboy jumps into the driver's seat while his horse shoves the wreck ahead of it on its belly,
// sparks flying, right through every fence in the way. At the end the cowboy jumps back into the
// saddle and the horse kicks the wreck off the track.

// Shape, shared with the drawing (grid: x from the rear bumper, y up from the ground)
internal const val CAR_LENGTH = 38f
internal const val CAR_SEAT_X = 20f         // driver's hips, between roll bar and windshield
internal const val CAR_SEAT_Y = 4f

private const val CAR_PASS_SPEED = 30f      // u/s across the screen while sliding by, slow enough to lasso
private const val CAR_HITCH_Y = 5f
/** Where the loop lands on the car: the middle of the lasso's reach, ahead of the hand. */
private const val CAR_HITCH_AIM = HORSE_X + HAND_X + 16f
/** Rear bumper against the horse's chest while shoving. */
private const val CAR_RIDE_X = HORSE_X + 29f
/** Seconds the loop takes to reach the car (half the lasso's throw), aimed at where it will be. */
private const val LASSO_LEAD = 0.225f
private const val CAR_BOARD_SECONDS = 0.7f
private const val CAR_UNLOAD_SECONDS = 0.7f
private const val CAR_RIDE_SECONDS = 6f
private const val CAR_JUMP_HOP = 7f
private const val CAR_FIRST_FENCE_SECONDS = 0.4f
private const val CAR_FENCE_SECONDS = 0.8f
private const val CAR_FENCE_POINTS = 8
private const val CAR_RUMBLE_DEGREES = 1.2f
// Kicked off the track: flies forward, tumbling
private const val KICK_VX = 110f
private const val KICK_VY = 45f
private const val KICK_GRAVITY = 150f
private const val KICK_SPIN = 260f

internal class RodeoCarWreck : RodeoVehicle(RodeoVehicleKind.CAR_WRECK) {

    private var y = 0f
    private var vx = 0f
    private var vy = 0f
    private var rotation = 0f
    private var boardStartX = 0f
    private var fenceIn = 0f
    /** Drives the sparks and the rattling. */
    private var scrape = 0f

    override val length = CAR_LENGTH
    override val passSpeed = CAR_PASS_SPEED
    override val carriesHorse = false
    override val horseHops = false

    /** The cowboy sits in the car, not on the horse, from the jump over until he is back. */
    override val riderOnHorse: Boolean get() = !carriesRider

    /**
     * The whole car is a target: the loop grabs the part closest to the middle of the lasso's
     * reach, so any throw while some of it passes in front of the horse catches it.
     */
    override fun hitch() = (CAR_HITCH_AIM + CAR_PASS_SPEED * LASSO_LEAD).coerceIn(x + 2f, x + CAR_LENGTH - 2f) to CAR_HITCH_Y

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        y = 0f
        rotation = 0f
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        // A jump in progress ends right here; the horse stays on the ground behind the car
        world.takeHorseOffTheGround()
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> {
                scrape += dt
                passBy(dt)
            }
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, CAR_BOARD_SECONDS)
                x = lerp(boardStartX, CAR_RIDE_X, smoothstep(progress))
                if (progress >= 1f) {
                    enter(VehiclePhase.RIDING)
                    fenceIn = CAR_FIRST_FENCE_SECONDS
                }
            }
            VehiclePhase.RIDING -> ride(world, dt)
            VehiclePhase.UNLOADING -> {
                scrape += dt
                if (phaseTime >= CAR_UNLOAD_SECONDS) kickAway(world)
            }
            VehiclePhase.LEAVING -> {
                vy -= KICK_GRAVITY * dt
                x += vx * dt
                y = max(0f, y + vy * dt)
                rotation += KICK_SPIN * dt
                world.clearTrack(x, x + CAR_LENGTH)
                // Bounces along on its belly until it is out of the picture
                if (y <= 0f && vy < 0f) vy = KICK_VY * 0.5f
                if (x > world.worldWidth + 10f) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun ride(world: RodeoWorld, dt: Float) {
        scrape += dt
        fenceIn -= dt
        if (fenceIn <= 0f) {
            world.addFence(x = world.worldWidth, gapAfter = CAR_LENGTH)
            fenceIn = CAR_FENCE_SECONDS
        }
        // Everything in front of the horse is shoved away by the wreck
        world.clearTrack(x, x + CAR_LENGTH) { fence ->
            world.addBonusPoints(CAR_FENCE_POINTS)
            world.dust(fence.x)
            world.sparkle(x + CAR_LENGTH, 5f)
        }
        if (phaseTime >= CAR_RIDE_SECONDS) {
            enter(VehiclePhase.UNLOADING)
            world.resumeFences()
        }
    }

    /** The cowboy is back in the saddle: the horse kicks the wreck off the track. */
    private fun kickAway(world: RodeoWorld) {
        enter(VehiclePhase.LEAVING)
        vx = KICK_VX
        vy = KICK_VY
        world.splash()
        world.dust(x)
    }

    override fun cowboy(world: RodeoWorld): RodeoCowboyUi? {
        val saddleX = HORSE_X + COWBOY_SEAT_X
        val saddleY = world.horseHeight + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH
        return when (phase) {
            // Jumps over the horse's head into the driver's seat...
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, CAR_BOARD_SECONDS)
                RodeoCowboyUi(
                    x = lerp(saddleX, x + CAR_SEAT_X, progress),
                    height = lerp(saddleY, CAR_SEAT_Y, progress) + CAR_JUMP_HOP * sin(PI.toFloat() * progress),
                    rotation = 0f,
                    facingLeft = false,
                    hatLift = 1f,
                )
            }
            // ...and back into the saddle at the end
            VehiclePhase.UNLOADING -> {
                val progress = progressOf(phaseTime, CAR_UNLOAD_SECONDS)
                RodeoCowboyUi(
                    x = lerp(x + CAR_SEAT_X, saddleX, progress),
                    height = lerp(CAR_SEAT_Y, saddleY, progress) + CAR_JUMP_HOP * sin(PI.toFloat() * progress),
                    rotation = -360f * progress, // a backflip
                    facingLeft = false,
                    hatLift = 1f,
                )
            }
            else -> null
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        val shoving = phase == VehiclePhase.RIDING
        return RodeoCarWreckUi(
            x = x,
            y = y,
            rotation = if (shoving) CAR_RUMBLE_DEGREES * sin(scrape * 60f) else rotation,
            hasDriver = shoving,
            sparks = if (shoving || phase == VehiclePhase.APPROACH) scrape else null,
            lassoHint = lassoHint(x + CAR_LENGTH / 2f, 16f),
        )
    }
}

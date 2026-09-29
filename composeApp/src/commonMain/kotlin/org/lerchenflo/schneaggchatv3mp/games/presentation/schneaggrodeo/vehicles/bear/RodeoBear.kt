package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.bear

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_LEG_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HAND_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.Horseshoe
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SnailState
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.countDown
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
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Bear: lumbers by in the forest (see engine/RodeoLandscape). Lasso it and the cowboy jumps onto its
// back while the horse runs on riderless behind. On the ground the bear smashes every fence and
// eats every snail in its way (they count as caught). Holding anywhere (space) climbs it up the
// trees for the horseshoes hanging high up; letting go drops it back down.

// Shape, shared with the drawing (grid: x from the bear's rear, y up from its feet)
internal const val BEAR_LENGTH = 28f
internal const val BEAR_SEAT_X = 11f
internal const val BEAR_SEAT_Y = 16f
/** Highest the bear climbs; its tree is drawn up to here and beyond. */
internal const val BEAR_MAX_CLIMB = 32f

private const val BEAR_PASS_SPEED = 28f
private const val BEAR_HITCH_Y = 12f
/** Where the loop lands on the bear: the middle of the lasso's reach, ahead of the hand. */
private const val BEAR_HITCH_AIM = HORSE_X + HAND_X + 16f
/** Seconds the loop takes to reach the bear (half the lasso's throw), aimed at where it will be. */
private const val LASSO_LEAD = 0.225f
private const val BEAR_RIDE_X = HORSE_X + 34f
private const val BEAR_BOARD_SECONDS = 0.7f
private const val BEAR_UNLOAD_SECONDS = 0.8f
private const val BEAR_JUMP_HOP = 8f
private const val BEAR_RIDE_SECONDS = 9f
private const val BEAR_CLIMB_SPEED = 38f
private const val BEAR_FALL_GRAVITY = 140f
/** Below this height the bear is back on the ground, smashing and eating. */
private const val BEAR_GROUND_HEIGHT = 2f
private const val BEAR_FENCE_POINTS = 5
private const val BEAR_FENCE_SECONDS = 1.2f
private const val BEAR_HORSESHOE_SECONDS = 1.4f
private const val BEAR_RUN_OFF_SPEED = 55f
private const val BEAR_GAIT_SPEED = 11f
/** How long the jaws stay open after eating a snail. */
private const val BEAR_CHOMP_SECONDS = 0.35f

internal class RodeoBear : RodeoVehicle(RodeoVehicleKind.BEAR) {

    private var gaitPhase = 0f
    private var boardStartX = 0f
    private var height = 0f
    private var fallSpeed = 0f
    private var climbing = false
    private var fenceIn = 0f
    private var horseshoeIn = 0f
    private var chomp = 0f
    private var dropStartX = 0f
    private var dropStartY = 0f

    override val length = BEAR_LENGTH
    override val passSpeed = BEAR_PASS_SPEED
    override val carriesHorse = false
    override val horseHops = false

    override val horseRunsRiderless: Boolean
        get() = phase == VehiclePhase.RIDING || phase == VehiclePhase.UNLOADING

    override val riderOnHorse: Boolean get() = !carriesRider

    override fun hitch() =
        (BEAR_HITCH_AIM + BEAR_PASS_SPEED * LASSO_LEAD).coerceIn(x + 2f, x + BEAR_LENGTH - 2f) to BEAR_HITCH_Y

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        height = 0f
        fallSpeed = 0f
        climbing = false
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        world.takeHorseOffTheGround()
    }

    override fun onJump(pressed: Boolean) {
        climbing = pressed && phase == VehiclePhase.RIDING
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        gaitPhase += dt * BEAR_GAIT_SPEED
        chomp = countDown(chomp, dt)
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> passBy(dt)
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, BEAR_BOARD_SECONDS)
                x = lerp(boardStartX, BEAR_RIDE_X, smoothstep(progress))
                if (progress >= 1f) {
                    enter(VehiclePhase.RIDING)
                    fenceIn = BEAR_FENCE_SECONDS
                    horseshoeIn = BEAR_HORSESHOE_SECONDS / 2f
                }
            }
            VehiclePhase.RIDING -> ride(world, dt)
            VehiclePhase.UNLOADING -> {
                climbDown(dt)
                x += BEAR_RUN_OFF_SPEED * dt
                if (phaseTime >= BEAR_UNLOAD_SECONDS) enter(VehiclePhase.LEAVING)
            }
            VehiclePhase.LEAVING -> {
                climbDown(dt)
                x += BEAR_RUN_OFF_SPEED * dt
                if (x > world.worldWidth + 5f) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun ride(world: RodeoWorld, dt: Float) {
        if (climbing) {
            height = min(BEAR_MAX_CLIMB, height + BEAR_CLIMB_SPEED * dt)
            fallSpeed = 0f
        } else {
            climbDown(dt)
        }

        fenceIn -= dt
        if (fenceIn <= 0f) {
            world.addFence(x = world.worldWidth, gapAfter = BEAR_LENGTH * 2f)
            fenceIn = BEAR_FENCE_SECONDS
        }
        // Horseshoes hang high up in the trees - only a climbing bear gets them
        horseshoeIn -= dt
        if (horseshoeIn <= 0f) {
            horseshoeIn = BEAR_HORSESHOE_SECONDS
            world.horseshoes.add(Horseshoe(x = world.worldWidth, height = 26f + Random.nextFloat() * 18f))
        }

        if (height < BEAR_GROUND_HEIGHT) {
            // Down on the ground: snails in the way are eaten, fences smashed
            world.snails
                .filter { it.state == SnailState.ACTIVE && it.x > x && it.x < x + BEAR_LENGTH + 4f && it.height < 12f }
                .forEach { snail ->
                    world.eatSnail(snail)
                    chomp = BEAR_CHOMP_SECONDS
                }
            world.clearTrack(x, x + BEAR_LENGTH) { fence ->
                world.addBonusPoints(BEAR_FENCE_POINTS)
                world.dust(fence.x)
            }
        }
        world.horseshoes.removeAll { shoe ->
            val collected = shoe.x > x && shoe.x < x + BEAR_LENGTH + 4f &&
                    shoe.height > height + 4f && shoe.height < height + BEAR_SEAT_Y + 14f
            if (collected) world.collectHorseshoe(shoe)
            collected
        }

        if (phaseTime >= BEAR_RIDE_SECONDS) {
            climbing = false
            enter(VehiclePhase.UNLOADING)
            dropStartX = x + BEAR_SEAT_X
            dropStartY = height + BEAR_SEAT_Y - COWBOY_LEG_LENGTH
            world.resumeFences()
        }
    }

    private fun climbDown(dt: Float) {
        if (height <= 0f) return
        fallSpeed += BEAR_FALL_GRAVITY * dt
        height = max(0f, height - fallSpeed * dt)
    }

    override fun cowboy(world: RodeoWorld): RodeoCowboyUi? = when (phase) {
        // Jumps over from the saddle onto the bear's back...
        VehiclePhase.BOARDING -> {
            val progress = progressOf(phaseTime, BEAR_BOARD_SECONDS)
            RodeoCowboyUi(
                x = lerp(HORSE_X + COWBOY_SEAT_X, x + BEAR_SEAT_X, progress),
                height = lerp(world.horseHeight + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, BEAR_SEAT_Y - COWBOY_LEG_LENGTH, progress) +
                        BEAR_JUMP_HOP * sin(PI.toFloat() * progress),
                rotation = 0f,
                facingLeft = false,
                hatLift = 1f,
            )
        }
        // ...and back into the saddle once the bear has had enough
        VehiclePhase.UNLOADING -> {
            val progress = progressOf(phaseTime, BEAR_UNLOAD_SECONDS)
            RodeoCowboyUi(
                x = lerp(dropStartX, HORSE_X + COWBOY_SEAT_X, progress),
                height = lerp(dropStartY, world.riderlessHop + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, progress) +
                        BEAR_JUMP_HOP * sin(PI.toFloat() * progress),
                rotation = -360f * progress,
                facingLeft = false,
                hatLift = 1f,
            )
        }
        else -> null
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoBearUi(
            x = x,
            height = height,
            gaitPhase = gaitPhase,
            rider = phase == VehiclePhase.RIDING,
            chomping = chomp > 0f,
            lassoHint = lassoHint(x + BEAR_LENGTH / 2f, 24f),
        )
    }
}

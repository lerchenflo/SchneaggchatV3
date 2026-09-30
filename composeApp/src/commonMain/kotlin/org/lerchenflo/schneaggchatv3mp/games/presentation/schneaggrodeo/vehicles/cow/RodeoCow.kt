package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cow

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
import kotlin.math.sin

// Cow ride: a brown Vorarlberg cow ambles by, a big bell round her neck. Lasso her and the cowboy
// swings over onto her back while the horse gallops on riderless. She trots through every fence,
// and every time her bell rings the chasing snails get scared and fall back a bit - the last ring
// sends them all the way back. Then the cowboy jumps back into the saddle and she wanders off.

// Shape, shared with the drawing (grid: x from the cow's tail end, y up from the ground)
internal const val COW_LENGTH = 22f
/** The cowboy's hips on her back. */
internal const val COW_SEAT_X = 9f
internal const val COW_SEAT_Y = 11.5f
/** The bell under her neck. */
internal const val COW_BELL_X = 17.5f
internal const val COW_BELL_Y = 6f

private const val COW_PASS_SPEED = 20f
private const val COW_HITCH_Y = 8f
private const val COW_HITCH_AIM = HORSE_X + HAND_X + 16f
private const val LASSO_LEAD = 0.225f
/** Ahead of the horse, which gallops on riderless behind her. */
private const val COW_RIDE_X = HORSE_X + 32f
private const val COW_BOARD_SECONDS = 0.7f
private const val COW_RIDE_SECONDS = 7f
private const val COW_UNLOAD_SECONDS = 0.7f
private const val COW_JUMP_HOP = 7f
private const val BELL_INTERVAL = 1.4f
private const val BELL_RING_SECONDS = 0.6f
/** Pack gap won with every ring. */
private const val BELL_SCARE_GAP = 6f
private const val COW_POINTS = 40
private const val GAIT_SPEED = 9f
private const val WANDER_SPEED = 8f

internal class RodeoCow : RodeoVehicle(RodeoVehicleKind.COW) {

    private var boardStartX = 0f
    private var gait = 0f
    private var bellIn = 0f
    /** Seconds since the bell last rang, for the ringing waves. */
    private var ringTime = BELL_RING_SECONDS

    override val length = COW_LENGTH
    override val passSpeed = COW_PASS_SPEED
    override val carriesHorse = false
    override val horseHops = false

    override val horseRunsRiderless: Boolean
        get() = phase == VehiclePhase.RIDING || phase == VehiclePhase.UNLOADING

    override val riderOnHorse: Boolean get() = !carriesRider

    /** The whole cow is a target. */
    override fun hitch() = (COW_HITCH_AIM + COW_PASS_SPEED * LASSO_LEAD).coerceIn(x + 2f, x + COW_LENGTH - 2f) to COW_HITCH_Y

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        world.takeHorseOffTheGround()
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        ringTime += dt
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> {
                gait += dt * GAIT_SPEED * 0.5f
                passBy(dt)
            }
            VehiclePhase.BOARDING -> {
                gait += dt * GAIT_SPEED
                x = lerp(boardStartX, COW_RIDE_X, smoothstep(progressOf(phaseTime, COW_BOARD_SECONDS)))
                if (phaseTime >= COW_BOARD_SECONDS) {
                    enter(VehiclePhase.RIDING)
                    bellIn = BELL_INTERVAL
                }
            }
            VehiclePhase.RIDING -> {
                gait += dt * GAIT_SPEED
                world.clearTrack(x, x + COW_LENGTH) { fence -> world.dust(fence.x) }
                bellIn -= dt
                if (bellIn <= 0f) {
                    bellIn = BELL_INTERVAL
                    ringTime = 0f
                    world.scarePack(BELL_SCARE_GAP)
                    world.sparkle(x + COW_BELL_X, COW_BELL_Y)
                }
                if (phaseTime >= COW_RIDE_SECONDS) {
                    // The last ring sends the whole pack back
                    ringTime = 0f
                    world.escapePack()
                    world.addBonusPoints(COW_POINTS)
                    world.resumeFences()
                    enter(VehiclePhase.UNLOADING)
                }
            }
            VehiclePhase.UNLOADING -> {
                gait += dt * GAIT_SPEED * 0.5f
                x -= scroll * progressOf(phaseTime, COW_UNLOAD_SECONDS)
                if (phaseTime >= COW_UNLOAD_SECONDS) enter(VehiclePhase.LEAVING)
            }
            VehiclePhase.LEAVING -> {
                // Stays behind grazing, then wanders off with the ground
                gait += dt * GAIT_SPEED * 0.3f
                x -= scroll + WANDER_SPEED * dt
                if (x + COW_LENGTH < 0f) enter(VehiclePhase.IDLE)
            }
        }
    }

    override fun cowboy(world: RodeoWorld): RodeoCowboyUi? {
        val saddleX = HORSE_X + COWBOY_SEAT_X
        return when (phase) {
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, COW_BOARD_SECONDS)
                RodeoCowboyUi(
                    x = lerp(saddleX, x + COW_SEAT_X, progress),
                    height = lerp(world.horseHeight + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, COW_SEAT_Y - 2f, progress) + COW_JUMP_HOP * sin(PI.toFloat() * progress),
                    rotation = 0f,
                    facingLeft = false,
                    hatLift = 1f,
                )
            }
            VehiclePhase.UNLOADING -> {
                val progress = progressOf(phaseTime, COW_UNLOAD_SECONDS)
                RodeoCowboyUi(
                    x = lerp(x + COW_SEAT_X, saddleX, progress),
                    height = lerp(COW_SEAT_Y - 2f, world.riderlessHop + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, progress) + COW_JUMP_HOP * sin(PI.toFloat() * progress),
                    rotation = -360f * progress,
                    facingLeft = false,
                    hatLift = 1f,
                )
            }
            else -> null
        }
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoCowUi(
            x = x,
            gait = gait,
            hasRider = phase == VehiclePhase.RIDING,
            ring = if (ringTime < BELL_RING_SECONDS) ringTime / BELL_RING_SECONDS else null,
            lassoHint = lassoHint(x + COW_LENGTH / 2f, 18f),
        )
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.bull

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_LEG_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_Y
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Bull ride: a bull charges by on the track. Lasso it and the cowboy jumps over onto its back - the
// real rodeo. The bull bucks and throws the rider forward and back; holding the left half (space)
// leans back, the right half (S) leans forward to keep the balance. Staying on for BULL_RIDE_SECONDS
// pays a big bonus; tipping over too far throws him back into the saddle of his horse, which gallops
// on riderless behind the bull meanwhile. The bull smashes every fence in its way.

// Shape, shared with the drawing (grid: x from the bull's rear, y up from the ground)
internal const val BULL_LENGTH = 30f
internal const val BULL_SEAT_X = 13f          // rider's hips on the bull's back
internal const val BULL_SEAT_Y = 19f

private const val BULL_PASS_SPEED = 32f       // u/s across the screen while charging by
private const val BULL_HITCH_Y = 16f
/** Where the loop lands on the bull: the middle of the lasso's reach, ahead of the hand. */
/** Seconds the loop takes to reach the bull (half the lasso's throw), aimed at where it will be. */
private const val BULL_RIDE_X = HORSE_X + 34f  // runs ahead of the riderless horse
private const val BULL_BOARD_SECONDS = 0.7f
private const val BULL_UNLOAD_SECONDS = 0.8f
private const val BULL_JUMP_HOP = 8f
internal const val BULL_RIDE_SECONDS = 6f
private const val BULL_RIDE_BONUS = 150
private const val BULL_FENCE_POINTS = 5
private const val BULL_FENCE_SECONDS = 1.1f
private const val BULL_RUN_OFF_SPEED = 60f
private const val BULL_GAIT_SPEED = 16f       // gait radians per second

// Balance: the rider's lean runs from -1 (flat back) to 1 (over the horns); at either end he's off.
// The bull's bucks push it one way, the rider's input the other; the further he leans, the more he
// tips (like any inverted pendulum), and his own sway dampens it a bit.
// Tuned forgiving: gentle bucks that ramp in slowly, strong control, and the rider finds back to
// the middle on his own (LEAN_RECENTER) - the input only has to help against the bucks.
private const val BUCK_START_STRENGTH = 1.2f
private const val BUCK_END_STRENGTH = 2.6f
private const val BUCK_WARMUP_SECONDS = 2f    // the first bucks ramp in, time to find the balance
private const val BUCK_MIN_SECONDS = 0.6f     // a buck lasts this long plus a random share
private const val BUCK_RANDOM_SECONDS = 0.5f
private const val LEAN_CONTROL = 11f
private const val LEAN_INSTABILITY = 0.4f
private const val LEAN_DAMPING = 4f
/** Pulls the rider back upright by himself. */
private const val LEAN_RECENTER = 2.2f
/** Degrees the bull pitches with a buck: rear up (positive) or rearing up at the front (negative). */
private const val BUCK_PITCH = 14f
private const val BUCK_PITCH_RESPONSE = 10f

internal class RodeoBull : RodeoVehicle(RodeoVehicleKind.BULL) {

    private var gaitPhase = 0f
    private var boardStartX = 0f
    private var lean = 0f
    private var leanSpeed = 0f
    /** Direction of the current buck: 1 throws the rider forward, -1 back. */
    private var buckDirection = 1f
    private var buckIn = 0f
    private var pitch = 0f
    private var fenceIn = 0f
    private var leanBack = false
    private var leanForward = false
    /** The ride ended by the rider falling off, not by making it to the bell. */
    private var thrown = false
    private var dropStartX = 0f
    private var dropStartY = 0f

    override val length = BULL_LENGTH
    override val passSpeed = BULL_PASS_SPEED
    override val carriesHorse = false
    override val horseHops = false

    override val horseRunsRiderless: Boolean
        get() = phase == VehiclePhase.RIDING || phase == VehiclePhase.UNLOADING

    override val riderOnHorse: Boolean get() = !carriesRider

    /**
     * The whole bull is a target: the loop grabs the part closest to the middle of the lasso's
     * reach, so any throw while some of it charges past in front of the horse catches it.
     */
    override fun hitch() =
        hitchX() to BULL_HITCH_Y

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        pitch = 0f
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        world.takeHorseOffTheGround()
        lean = 0f
        leanSpeed = 0f
        thrown = false
        releaseInput()
    }

    override fun onJump(pressed: Boolean) {
        leanBack = pressed && phase == VehiclePhase.RIDING
    }

    override fun onDive(pressed: Boolean) {
        leanForward = pressed && phase == VehiclePhase.RIDING
    }

    private fun releaseInput() {
        leanBack = false
        leanForward = false
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        gaitPhase += dt * BULL_GAIT_SPEED
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> passBy(dt)
            VehiclePhase.BOARDING -> {
                val progress = progressOf(phaseTime, BULL_BOARD_SECONDS)
                x = lerp(boardStartX, BULL_RIDE_X, smoothstep(progress))
                if (progress >= 1f) {
                    enter(VehiclePhase.RIDING)
                    fenceIn = BULL_FENCE_SECONDS
                    buckIn = 0f
                }
            }
            VehiclePhase.RIDING -> ride(world, dt)
            VehiclePhase.UNLOADING -> {
                x += BULL_RUN_OFF_SPEED * dt
                pitch += (BUCK_PITCH - pitch) * min(1f, dt * BUCK_PITCH_RESPONSE)
                if (phaseTime >= BULL_UNLOAD_SECONDS) {
                    enter(VehiclePhase.LEAVING)
                    world.splash()
                }
            }
            VehiclePhase.LEAVING -> {
                x += BULL_RUN_OFF_SPEED * dt
                pitch = BUCK_PITCH * 0.5f * (1f + sin(gaitPhase * 0.5f))
                world.clearTrack(x, x + BULL_LENGTH)
                if (x > world.worldWidth + 5f) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun ride(world: RodeoWorld, dt: Float) {
        // A new buck now and then, harder the longer the ride lasts
        buckIn -= dt
        if (buckIn <= 0f) {
            buckDirection = if (Random.nextBoolean()) 1f else -1f
            buckIn = BUCK_MIN_SECONDS + Random.nextFloat() * BUCK_RANDOM_SECONDS
        }
        val progress = phaseTime / BULL_RIDE_SECONDS
        val strength = lerp(BUCK_START_STRENGTH, BUCK_END_STRENGTH, progress) * min(1f, phaseTime / BUCK_WARMUP_SECONDS)
        val input = (if (leanForward) 1f else 0f) - (if (leanBack) 1f else 0f)
        val accel = buckDirection * strength + (LEAN_INSTABILITY - LEAN_RECENTER) * lean + input * LEAN_CONTROL - LEAN_DAMPING * leanSpeed
        leanSpeed += accel * dt
        lean += leanSpeed * dt
        pitch += (buckDirection * BUCK_PITCH - pitch) * min(1f, dt * BUCK_PITCH_RESPONSE)

        // The bull charges through the fences
        fenceIn -= dt
        if (fenceIn <= 0f) {
            world.addFence(x = world.worldWidth, gapAfter = BULL_LENGTH * 2f)
            fenceIn = BULL_FENCE_SECONDS
        }
        world.clearTrack(x, x + BULL_LENGTH) { fence ->
            world.addBonusPoints(BULL_FENCE_POINTS)
            world.dust(fence.x)
        }

        when {
            abs(lean) >= 1f -> getOff(world, thrownOff = true)
            phaseTime >= BULL_RIDE_SECONDS -> {
                // Made it to the bell
                world.addBonusPoints(BULL_RIDE_BONUS)
                world.sparkle(x + BULL_SEAT_X, BULL_SEAT_Y + 10f)
                getOff(world, thrownOff = false)
            }
        }
    }

    private fun getOff(world: RodeoWorld, thrownOff: Boolean) {
        thrown = thrownOff
        lean = lean.coerceIn(-1f, 1f)
        releaseInput()
        enter(VehiclePhase.UNLOADING)
        dropStartX = x + BULL_SEAT_X
        dropStartY = BULL_SEAT_Y - COWBOY_LEG_LENGTH
        world.resumeFences()
        if (thrownOff) world.dust(x + BULL_SEAT_X)
    }

    override fun cowboy(world: RodeoWorld): RodeoCowboyUi? = when (phase) {
        // Jumps over from the saddle onto the bull's back...
        VehiclePhase.BOARDING -> {
            val progress = progressOf(phaseTime, BULL_BOARD_SECONDS)
            RodeoCowboyUi(
                x = lerp(HORSE_X + COWBOY_SEAT_X, x + BULL_SEAT_X, progress),
                height = lerp(world.horseHeight + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, BULL_SEAT_Y - COWBOY_LEG_LENGTH, progress) +
                        BULL_JUMP_HOP * sin(PI.toFloat() * progress),
                rotation = 0f,
                facingLeft = false,
                hatLift = 1f,
            )
        }
        // ...and back into the saddle: a proud hop after the bell, a wild tumble when thrown
        VehiclePhase.UNLOADING -> {
            val progress = progressOf(phaseTime, BULL_UNLOAD_SECONDS)
            RodeoCowboyUi(
                x = lerp(dropStartX, HORSE_X + COWBOY_SEAT_X, progress),
                height = lerp(dropStartY, world.riderlessHop + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, progress) +
                        BULL_JUMP_HOP * sin(PI.toFloat() * progress),
                rotation = if (thrown) -540f * progress else -360f * progress,
                facingLeft = false,
                hatLift = if (thrown) 3f * (1f - progress) else 1f,
            )
        }
        else -> null
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        val riding = phase == VehiclePhase.RIDING
        return RodeoBullUi(
            x = x,
            gaitPhase = gaitPhase,
            pitch = pitch,
            rider = riding,
            lean = lean,
            rideProgress = if (riding) progressOf(phaseTime, BULL_RIDE_SECONDS) else null,
            lassoHint = lassoHint(x + BULL_LENGTH / 2f, 26f),
        )
    }
}

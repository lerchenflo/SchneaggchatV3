package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.balloon

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_LEG_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.COWBOY_SEAT_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.Horseshoe
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoSteering
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

// Hot-air balloon: drifts by low with a rope ladder hanging from its basket. Lasso the ladder and
// the cowboy climbs into the basket while the horse gallops on riderless below. Hold to fire the
// burner and rise, let go to sink; grab the horseshoes floating up high. After a while it lands
// the cowboy back in the saddle and floats away.

// Shape, shared with the drawing (grid: x from the basket's left edge, y up from the basket's floor)
internal const val BASKET_WIDTH = 6f
internal const val BASKET_HEIGHT = 3.5f
/** The envelope's middle above the basket floor, and its radii. */
internal const val ENVELOPE_CENTER_Y = 19f
internal const val ENVELOPE_RADIUS_X = 9f
internal const val ENVELOPE_RADIUS_Y = 11f
internal const val BALLOON_LADDER_LENGTH = 12f

private const val BALLOON_PASS_SPEED = 24f
private const val BALLOON_APPROACH_HEIGHT = 26f   // basket floor while drifting by
private const val BALLOON_FLY_X = HORSE_X + 8f
private const val BALLOON_BOARD_SECONDS = 1f
private const val BALLOON_RIDE_SECONDS = 10f
private const val BALLOON_DROP_SECONDS = 0.8f
private const val BALLOON_DROP_HOP = 6f
private const val BALLOON_MIN_HEIGHT = 8f
private const val BALLOON_MAX_HEIGHT = 30f       // the envelope stays inside the canvas
private const val BALLOON_FLOAT_AWAY_SPEED = 14f
private const val SHOE_INTERVAL = 0.7f
private const val SHOE_MIN_HEIGHT = 14f
private const val SHOE_MAX_HEIGHT = 36f

internal class RodeoBalloon : RodeoVehicle(RodeoVehicleKind.BALLOON) {

    private val steering = RodeoSteering(
        climbAccel = 55f,
        diveAccel = 0f,
        sinkAccel = 22f,
        maxClimb = 16f,
        maxSink = 12f,
    )
    /** Basket floor above the ground. */
    private var y = 0f
    private var boardStartX = 0f
    private var shoeIn = 0f
    private var sway = 0f
    private var dropStartX = 0f
    private var dropStartY = 0f

    override val length = BASKET_WIDTH + ENVELOPE_RADIUS_X
    override val passSpeed = BALLOON_PASS_SPEED
    override val carriesHorse = false
    override val horseHops = false

    override val horseRunsRiderless: Boolean
        get() = phase == VehiclePhase.RIDING || phase == VehiclePhase.UNLOADING

    override val riderOnHorse: Boolean get() = !carriesRider

    override fun hitch() = (x + BASKET_WIDTH / 2f) to (y - BALLOON_LADDER_LENGTH)

    override fun reset() {
        super.reset()
        steering.release()
    }

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        y = BALLOON_APPROACH_HEIGHT
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        steering.stop()
        steering.release()
    }

    override fun onJump(pressed: Boolean) {
        steering.climb = pressed && phase == VehiclePhase.RIDING
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        sway += dt
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> passBy(dt)
            VehiclePhase.BOARDING -> {
                x = lerp(boardStartX, BALLOON_FLY_X, smoothstep(progressOf(phaseTime, BALLOON_BOARD_SECONDS)))
                if (phaseTime >= BALLOON_BOARD_SECONDS) {
                    enter(VehiclePhase.RIDING)
                    shoeIn = 0f
                }
            }
            VehiclePhase.RIDING -> fly(world, dt)
            VehiclePhase.UNLOADING -> if (phaseTime >= BALLOON_DROP_SECONDS) {
                enter(VehiclePhase.LEAVING)
                world.splash()
                world.resumeFences()
            }
            VehiclePhase.LEAVING -> {
                // Floats away, up and back with the wind
                y += BALLOON_FLOAT_AWAY_SPEED * dt
                x -= scroll * 0.4f
                if (y > 80f || x + length < 0f) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun fly(world: RodeoWorld, dt: Float) {
        y = max(BALLOON_MIN_HEIGHT, steering.fly(y, dt, ceiling = BALLOON_MAX_HEIGHT))
        // Horseshoes float by at all heights
        shoeIn -= dt
        if (shoeIn <= 0f) {
            shoeIn = SHOE_INTERVAL
            world.horseshoes.add(Horseshoe(x = world.worldWidth + 2f, height = SHOE_MIN_HEIGHT + Random.nextFloat() * (SHOE_MAX_HEIGHT - SHOE_MIN_HEIGHT)))
        }
        // The basket (and the cowboy in it) picks them up
        world.horseshoes.removeAll { shoe ->
            val collected = shoe.x > x - 2f && shoe.x < x + BASKET_WIDTH + 2f && shoe.height > y - 2f && shoe.height < y + BASKET_HEIGHT + 8f
            if (collected) world.collectHorseshoe(shoe)
            collected
        }
        if (phaseTime >= BALLOON_RIDE_SECONDS) {
            enter(VehiclePhase.UNLOADING)
            steering.release()
            dropStartX = x + BASKET_WIDTH / 2f
            dropStartY = y + 1f
        }
    }

    override fun cowboy(world: RodeoWorld): RodeoCowboyUi? = when (phase) {
        // Climbing the rope ladder into the basket
        VehiclePhase.BOARDING -> {
            val progress = progressOf(phaseTime, BALLOON_BOARD_SECONDS)
            RodeoCowboyUi(
                x = lerp(HORSE_X + COWBOY_SEAT_X, x + BASKET_WIDTH / 2f, progress),
                height = lerp(world.horseHeight + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, y + 1f, progress),
                rotation = 0f,
                facingLeft = false,
                hatLift = 0f,
            )
        }
        // Jumping back into the saddle
        VehiclePhase.UNLOADING -> {
            val progress = progressOf(phaseTime, BALLOON_DROP_SECONDS)
            RodeoCowboyUi(
                x = lerp(dropStartX, HORSE_X + COWBOY_SEAT_X, progress),
                height = lerp(dropStartY, world.riderlessHop + COWBOY_SEAT_Y - COWBOY_LEG_LENGTH, progress) + BALLOON_DROP_HOP * sin(PI.toFloat() * progress),
                rotation = 360f * progress,
                facingLeft = false,
                hatLift = 0f,
            )
        }
        else -> null
    }

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoBalloonUi(
            x = x,
            y = y,
            sway = sway,
            burning = steering.climb,
            hasPilot = phase == VehiclePhase.RIDING,
            ladderDown = phase == VehiclePhase.APPROACH || phase == VehiclePhase.BOARDING,
            lassoHint = lassoHint(x + BASKET_WIDTH / 2f, y + ENVELOPE_CENTER_Y + ENVELOPE_RADIUS_Y + 4f),
        )
    }
}

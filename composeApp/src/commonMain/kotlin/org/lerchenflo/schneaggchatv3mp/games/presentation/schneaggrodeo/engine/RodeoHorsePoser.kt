package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoHorsePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// How the horse looks in every situation. Pitch is in degrees (positive = nose down) around a
// pivot in the horse's drawing grid (see drawHorseAndRider); the first matching situation wins.

// The galloping horse rocks gently: nose down as the front legs land, up as the hind legs push off
private const val GALLOP_ROCK_DEGREES = 2.5f
// Normal jumps tilt the horse: nose up on takeoff, level at the peak, nose down to land
private const val JUMP_MAX_PITCH = 16f
private const val JUMP_PITCH_BLEND_HEIGHT = 4f       // pitch fades in / out over this height above the ground
private const val STUMBLE_MAX_PITCH = 24f            // nose down at the worst moment of a stumble
private const val BUCK_MAX_PITCH = 22f               // hindquarters up, nose down
// The super jump wind-up rears the horse up on its pumped hind legs, front hooves pawing the air
private const val SUPER_JUMP_HIND_LEG_GROWTH = 0.6f  // hind legs grow to 160 %
private const val SUPER_JUMP_REAR_PITCH = 28f        // nose up at the end of the wind-up
private const val SUPER_JUMP_LANDING_PITCH = 16f     // nose down on the way down
/** The pumped hind legs are this long in the drawing grid; they lift the whole horse. */
private const val HIND_LEG_LENGTH = 9f

/**
 * Builds the pose of horse and rider for this frame. [base] is the height of the hooves (vehicle
 * included), [ride] the vehicle carrying them, if any, [passenger] a friend riding along, [scale]
 * the size of a mushroom-grown (or shrunk) horse.
 */
internal fun poseHorse(
    horse: RodeoHorse,
    superJump: RodeoSuperJump,
    fall: RodeoFall,
    ride: RodeoVehicle?,
    base: Float,
    riderlessHop: Float,
    runTimeSeconds: Float,
    passenger: RodeoFriend?,
    scale: Float,
): RodeoHorsePose {
    var pitch = 0f
    var pivotX = 12f
    var pivotY = 12f
    var hindLegScale = 1f
    var glow = 0f
    var frontLegFold = 0f
    var frontLegRaise = 0f
    var hatLift = 0f
    var lean = horse.riderLean
    val ridePose = ride?.ridePose(runTimeSeconds)
    val hasRider = fall.isInSaddle && ride?.riderOnHorse != false
    when (superJump.phase) {
        SuperJumpPhase.WINDUP -> {
            // Rears up: the hind legs pump up and lift the whole horse, which tips back around its
            // hind hooves with the front legs pawing the air - a loaded spring.
            val progress = superJump.windupProgress
            hindLegScale = 1f + SUPER_JUMP_HIND_LEG_GROWTH * easeOutBack(progress)
            pitch = -SUPER_JUMP_REAR_PITCH * easeOutBack(progress)
            pivotX = 7f
            pivotY = HIND_LEG_LENGTH - HIND_LEG_LENGTH * hindLegScale // the hind hooves stay planted
            frontLegRaise = min(1f, progress * 2f)
            lean = max(lean, progress) // rider leans into the mane to stay on
            glow = progress * (0.75f + 0.25f * sin(superJump.windup * 40f))
        }
        SuperJumpPhase.FLIGHT -> {
            val progress = superJump.flightProgress
            // Legs shrink back over the first quarter of the flight
            hindLegScale = 1f + SUPER_JUMP_HIND_LEG_GROWTH * max(0f, 1f - progress / 0.25f)
            glow = max(0f, 1f - progress / 0.25f)
            // Continues from the rear: nose up while rising, down to land
            val amplitude = if (progress < 0.5f) SUPER_JUMP_REAR_PITCH else SUPER_JUMP_LANDING_PITCH
            pitch = -amplitude * cos(PI.toFloat() * progress)
            pivotX = 7f
            pivotY = HIND_LEG_LENGTH - HIND_LEG_LENGTH * hindLegScale
        }
        SuperJumpPhase.NONE -> if (ridePose != null) {
            // The vehicle rocks and rattles horse and rider
            pitch = ridePose.pitch
            hatLift = ridePose.hatLift
        } else if (fall.phase == FallPhase.THROWN && fall.clock < BUCK_SECONDS) {
            // Bucks: hindquarters kick up around the front hooves and launch the cowboy
            pitch = BUCK_MAX_PITCH * sin(PI.toFloat() * fall.clock / BUCK_SECONDS)
            pivotX = 19f
            pivotY = 0f
        } else if (horse.stumble > 0f) {
            // Nose dives around the hind hooves, front legs buckle, then it catches itself
            val progress = 1f - horse.stumble / STUMBLE_SECONDS
            val dip = sin(PI.toFloat() * progress)
            val wobble = sin(3f * PI.toFloat() * progress) * (1f - progress) * 0.25f
            pitch = STUMBLE_MAX_PITCH * (dip + wobble)
            pivotX = 7f
            pivotY = 0f
            frontLegFold = dip
            hatLift = 3f * dip
        } else if (horse.height > 0f) {
            // Normal jump: tilts with the vertical speed, faded in and out near the ground so
            // takeoff and landing don't snap
            val tilt = (horse.verticalVelocity / JUMP_VELOCITY).coerceIn(-1f, 1f)
            val blend = min(1f, horse.height / JUMP_PITCH_BLEND_HEIGHT)
            pitch = -JUMP_MAX_PITCH * tilt * blend
        } else if (riderlessHop > 0f) {
            // Riderless hop over a fence under the plane: level
            pitch = 0f
        } else {
            pitch = GALLOP_ROCK_DEGREES * sin(horse.gaitPhase + 0.8f)
        }
    }
    return RodeoHorsePose(
        // Pumped hind legs lift the horse, so they still reach the ground
        height = base + riderlessHop + HIND_LEG_LENGTH * (hindLegScale - 1f),
        gaitPhase = horse.gaitPhase,
        airborne = horse.height > 0f || riderlessHop > 0f || ride?.horseHops == true,
        riderLean = max(lean, frontLegFold * 1.4f),
        pitchDegrees = pitch,
        pivotX = pivotX,
        pivotY = pivotY,
        hindLegScale = hindLegScale,
        frontLegFold = frontLegFold,
        frontLegRaise = frontLegRaise,
        hatLift = hatLift,
        glow = glow,
        offsetX = fall.horseOffset(),
        hasRider = hasRider,
        duck = ridePose?.duck ?: 0f,
        parachute = ridePose?.parachute == true,
        visible = ride?.hidesHorse != true,
        coat = horse.coat,
        level = horse.level,
        lives = horse.lives,
        maxLives = horse.maxLives,
        passengerId = passenger?.userId?.takeIf { hasRider },
        scale = scale,
    )
}

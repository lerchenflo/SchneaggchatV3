package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import kotlin.math.max
import kotlin.math.min

private const val GRAVITY = 330f          // u/s²
internal const val JUMP_VELOCITY = 82f    // u/s
// Holding the jump input lowers gravity while rising, for at most MAX_HOLD_SECONDS - a quick tap
// clears ~80 cm, a full hold clears the 170 cm fences.
private const val HOLD_GRAVITY_FACTOR = 0.3f
private const val MAX_HOLD_SECONDS = 0.3f

// Knocking a pole or running into a snail doesn't end the run - the horse stumbles for a moment
// (and the chasing pack closes in, see RodeoPack).
internal const val STUMBLE_SECONDS = 0.8f
/** Hearts a crash costs. */
private const val CRASH_LIVES = 0.5f
private const val STUMBLE_SPEED_FACTOR = 0.55f

private const val RIDER_LEAN_RESPONSE = 12f   // 1/s, how fast the rider follows the lean target
private const val GALLOP_STRIDE = 0.12f       // gait radians per unit ridden
private const val REMOUNT_SPEED_FACTOR = 0.8f // pace lost while the cowboy was off the horse
// Momentum on the hills: how fast a slope changes the pace, how fast it settles back on the flat,
// and how slow / fast it can get
private const val HILL_ACCELERATION = 1.3f
private const val HILL_SETTLE = 1.4f
private const val HILL_MIN_FACTOR = 0.65f
private const val HILL_MAX_FACTOR = 1.4f
/** Wading through mud: share of the pace that is left. */
private const val MUD_SPEED_FACTOR = 0.6f

/**
 * The horse's own motion: its pace, its (normal) jump and stumbles, plus the rider's lean. Heights
 * are its hooves above the ground, without any vehicle lift. Also its strength: level, hearts and
 * coat (see RodeoHorseStats) - a new horse keeps the pace, only its strength changes.
 */
internal class RodeoHorse {
    var speed = START_SPEED
        private set
    var height = 0f
    var verticalVelocity = 0f
        private set
    /** Counts down while the horse stumbles. */
    var stumble = 0f
        private set
    /** Animates the legs; advances with the distance galloped. */
    var gaitPhase = 0f
    /** 0 = upright, 1 = fully leaning forward over the neck. */
    var riderLean = 0f
        private set
    /** The jump input is held down: boosts a jump while rising. */
    var jumpHeld = false
    /** Highest point of the current / last normal jump; big ones can throw the cowboy off. */
    var jumpPeak = 0f
    private var airTime = 0f
    /** Hooves in a mud puddle this frame; set by the engine. */
    var inMud = false
    /** Up or down a mountain this frame; set by the engine. */
    var slope = Slope.FLAT
    /** Momentum from the hills: > 1 after running downhill, < 1 after uphill. */
    var hillFactor = 1f
        private set
    /** Extra jump strength of a mushroom (tiny horse); set by the engine. */
    var jumpBoost = 1f

    var level = MIN_HORSE_LEVEL
        private set
    /** Hearts left; fractions drain away while riding. */
    var lives = 0f
        private set
    var coat = DEFAULT_COAT
        private set
    val maxLives: Int get() = RodeoHorseStats.maxLivesOf(level)
    /** No hearts left: the horse throws the cowboy off and runs away. */
    val isExhausted: Boolean get() = lives <= 0f
    val stats: RodeoHorseStats get() = RodeoHorseStats(level, lives, coat)

    /** Pace including a stumble, mud and the mountain. */
    val pace: Float
        get() = speed * hillFactor * (if (stumble > 0f) STUMBLE_SPEED_FACTOR else 1f) * (if (inMud) MUD_SPEED_FACTOR else 1f) *
                when (slope) {
                    Slope.FLAT -> 1f
                    Slope.UPHILL -> UPHILL_SPEED_FACTOR
                    Slope.DOWNHILL -> DOWNHILL_SPEED_FACTOR
                }

    val isOnGround: Boolean get() = height <= 0f

    fun reset() {
        speed = START_SPEED
        height = 0f
        verticalVelocity = 0f
        stumble = 0f
        gaitPhase = 0f
        riderLean = 0f
        jumpHeld = false
        jumpPeak = 0f
        airTime = 0f
        inMud = false
        slope = Slope.FLAT
        hillFactor = 1f
        jumpBoost = 1f
        fullSeconds = 0f
        takeOver(RodeoHorseStats.START)
    }

    fun restore(speed: Float, stats: RodeoHorseStats) {
        this.speed = speed
        takeOver(stats)
    }

    /** The cowboy is on another horse now: same pace, its strength. */
    fun takeOver(stats: RodeoHorseStats) {
        level = stats.level.coerceIn(MIN_HORSE_LEVEL, MAX_HORSE_LEVEL)
        lives = stats.lives.coerceIn(0f, maxLives.toFloat())
        coat = stats.coat
    }

    /** Riding tires the horse: one heart per secondsPerLifeOf(level), three times as fast uphill. */
    fun tire(dt: Float) {
        if (fullSeconds > 0f) {
            fullSeconds = maxOf(0f, fullSeconds - dt)
            return
        }
        val factor = if (slope == Slope.UPHILL) UPHILL_TIRE_FACTOR else 1f
        lives = maxOf(0f, lives - factor * dt / secondsPerLifeOf(level))
    }

    /** A crash costs half a heart. */
    fun hurt() {
        lives = maxOf(0f, lives - CRASH_LIVES)
    }

    /** A carrot brings back a heart. */
    /** Seconds the horse stays full after a bowl of Käsknöpfle: no hearts drain meanwhile. */
    var fullSeconds = 0f

    /** A bowl of Käsknöpfle from the kiosk: full for a while, and a heart back. */
    fun eatKaesknoepfle(seconds: Float) {
        fullSeconds = seconds
        feed()
    }

    /** A pizza from the oven: all hearts back and one level stronger. */
    fun eatPizza() {
        level = min(MAX_HORSE_LEVEL, level + 1)
        lives = maxLives.toFloat()
    }

    fun feed() {
        lives = minOf(maxLives.toFloat(), lives + 1f)
    }

    /**
     * The ground under the hooves rises by [rise] per unit: uphill the horse slows down, downhill it
     * speeds up, and on the flat it settles back to its own pace. In the air it keeps its momentum.
     */
    fun rideHills(dt: Float, rise: Float) {
        if (isOnGround) hillFactor -= rise * HILL_ACCELERATION * dt
        hillFactor = (hillFactor - (hillFactor - 1f) * HILL_SETTLE * dt).coerceIn(HILL_MIN_FACTOR, HILL_MAX_FACTOR)
    }

    /** Speeds up towards [topSpeed] and recovers from a stumble. */
    fun accelerate(dt: Float, topSpeed: Float) {
        speed = min(topSpeed, speed + RodeoDifficulty.acceleration * dt)
        stumble = countDown(stumble, dt)
    }

    fun gallop(distance: Float) {
        gaitPhase += distance * GALLOP_STRIDE
    }

    fun jump() {
        // Stronger horses push off harder and jump higher
        verticalVelocity = JUMP_VELOCITY * jumpBoostOf(level) * jumpBoost
        airTime = 0f
        jumpPeak = 0f
    }

    /** Is mid jump (or just took off). */
    val isJumping: Boolean get() = height > 0f || verticalVelocity > 0f

    /**
     * Moves a normal jump on by [dt]; the held jump input boosts it while [boostAllowed]. Returns
     * true in the frame the hooves touch down again.
     */
    fun stepJump(dt: Float, boostAllowed: Boolean): Boolean {
        if (!isJumping) return false
        val boosted = boostAllowed && jumpHeld && verticalVelocity > 0f && airTime < MAX_HOLD_SECONDS
        airTime += dt
        verticalVelocity -= GRAVITY * (if (boosted) HOLD_GRAVITY_FACTOR else 1f) * dt
        height += verticalVelocity * dt
        jumpPeak = max(jumpPeak, height)
        if (height > 0f) return false
        height = 0f
        verticalVelocity = 0f
        return true
    }

    /** Stops any vertical motion (super jump wind-up, boarding a vehicle). */
    fun stopJumping() {
        verticalVelocity = 0f
        stumble = 0f
    }

    fun stumble() {
        stumble = STUMBLE_SECONDS
    }

    /** The rider eases into the forward seat ([leaning]) and back upright. */
    fun lean(dt: Float, leaning: Boolean) {
        val target = if (leaning) 1f else 0f
        riderLean += (target - riderLean) * min(1f, dt * RIDER_LEAN_RESPONSE)
    }

    /** Thrown off: the rider sits upright again once he is back, the stumble is over. */
    fun loseRider() {
        stumble = 0f
        riderLean = 0f
    }

    /** Back in the saddle: the horse lost some pace meanwhile. */
    fun remounted() {
        jumpPeak = 0f
        speed = max(START_SPEED, speed * REMOUNT_SPEED_FACTOR)
    }
}

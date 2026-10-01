package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoHorsePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoWildHorseUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.LassoGrab
import kotlin.math.max
import kotlin.random.Random

// Other horses on the track, galloping a bit slower than the rider so they drift into lasso range:
//  - wild horses, stronger than the ridden one - lasso one and the cowboy switches horses; the old
//    one stays behind
//  - friends on their own horse, showing up shortly before their highscore no matter what else is
//    going on; they gallop next to the rider until the run passes their highscore, then fall back.
//    Lasso them and they ride along behind the cowboy (see the engine's passenger)
//  - on night runs, now and then the ghost horse instead of a wild one: see-through and glowing.
//    Lassoed, it vanishes and makes horse and rider ghostly for a while (see RodeoSkyWonders)
// Horses let go (the old horse after a switch, a friend's horse) stop and fall back with the ground.

/** Game seconds until the first wild horse, and between two. */
private const val WILD_FIRST_SECONDS = 40f
private const val WILD_INTERVAL_MIN = 30f
private const val WILD_INTERVAL_RANDOM = 25f
/** u/s a wild horse drifts towards the rider across the screen. */
private const val WILD_DRIFT = 22f
/** A friend drifts in so that he is in lasso range when the run reaches his highscore. */
private const val FRIEND_ARRIVE_X = HORSE_X + 20f
private const val FRIEND_MIN_DRIFT = 10f
/** Fast enough to arrive in time, slow enough to be lassoed. */
private const val FRIEND_MAX_DRIFT = 35f
/** u/s a let-go horse trots back on its own, on top of falling back with the ground. */
private const val RELEASED_DRIFT = 10f
private const val WILD_GAIT_SPEED = 14f   // gait radians per second
/** Chance on a night run that the ghost horse comes instead of a wild one. */
private const val GHOST_CHANCE = 0.4f

/** A friend of the player, known by the id their profile picture is stored under. */
internal data class RodeoFriend(val userId: String, val username: String)

internal enum class WildHorseState {
    /** Galloping along, waiting for the lasso. */
    RUNNING,
    /** In the loop, pulled to the rider. */
    LASSOED,
    /** Let go: no longer a lasso target, falls back and leaves the picture. */
    RELEASED,
}

/** [x] is the left edge, like HORSE_X for the ridden horse; [drift] its screen speed to the left. */
internal class WildHorse(
    var x: Float,
    val stats: RodeoHorseStats,
    var friend: RodeoFriend?,
    /** Screen speed to the left while running; the lasso aims ahead by it. */
    val drift: Float,
    /** A friend keeps pace next to the rider until the run's score reaches this (their highscore). */
    var holdUntilScore: Long? = null,
    /** The ghost horse: lassoing it does not switch horses. */
    val ghost: Boolean = false,
) {
    var state = WildHorseState.RUNNING
    var height = 0f
    var gaitPhase = Random.nextFloat() * 6f

    fun step(dt: Float, scroll: Float, fences: List<Fence>) {
        gaitPhase += dt * WILD_GAIT_SPEED
        when (state) {
            WildHorseState.RUNNING -> {
                x -= drift * dt
                if (holdUntilScore != null) x = max(x, FRIEND_ARRIVE_X)
            }
            WildHorseState.LASSOED -> Unit // the lasso moves it
            WildHorseState.RELEASED -> x -= scroll + RELEASED_DRIFT * dt
        }
        height = hopOverFences(x + 14f, fences, reach = 14f, clearance = 3f)
    }
}

internal class RodeoWildHorses {
    val horses = mutableListOf<WildHorse>()
    private var nextWildIn = WILD_FIRST_SECONDS
    /** Friends already shown this run; each one comes by once. */
    private val shownFriends = mutableSetOf<String>()

    /** No wild horse is waiting for the lasso; vehicles may come. Friends don't hold anything up. */
    val isClear: Boolean get() = horses.none { it.state != WildHorseState.RELEASED && it.friend == null }

    fun reset() {
        horses.clear()
        shownFriends.clear()
        nextWildIn = WILD_FIRST_SECONDS
    }

    /** Moves all horses on; friends stop keeping pace once the run's [score] passed their highscore. */
    fun step(dt: Float, scroll: Float, fences: List<Fence>, score: Long) {
        horses.forEach { horse ->
            if (horse.holdUntilScore?.let { score >= it } == true) horse.holdUntilScore = null
            horse.step(dt, scroll, fences)
        }
        horses.removeAll { it.x + 32f < 0f }
    }

    /**
     * Sends a wild horse stronger than [currentLevel] in from the right now and then, while [allowed];
     * on a [night] run sometimes the ghost horse instead.
     */
    fun sendWild(dt: Float, worldWidth: Float, currentLevel: Int, allowed: Boolean, night: Boolean) {
        if (!allowed || !isClear) return
        nextWildIn -= dt
        if (nextWildIn > 0f) return
        nextWildIn = WILD_INTERVAL_MIN + Random.nextFloat() * WILD_INTERVAL_RANDOM
        if (night && Random.nextFloat() < GHOST_CHANCE) {
            horses.add(WildHorse(x = worldWidth + 5f, stats = RodeoHorseStats.wild(currentLevel), friend = null, drift = WILD_DRIFT, ghost = true))
            return
        }
        // At the top level there is nothing stronger to switch to
        if (currentLevel >= MAX_HORSE_LEVEL) return
        horses.add(WildHorse(x = worldWidth + 5f, stats = RodeoHorseStats.wild(currentLevel), friend = null, drift = WILD_DRIFT))
    }


    fun wasShown(friend: RodeoFriend): Boolean = friend.userId in shownFriends

    /**
     * Sends [friend] in on a horse of their own, reaching lasso range in [seconds]; they keep pace
     * there until the run's score reaches their [highscore].
     */
    fun sendFriend(friend: RodeoFriend, seconds: Float, worldWidth: Float, highscore: Long) {
        shownFriends += friend.userId
        val startX = worldWidth + 5f
        val drift = ((startX - FRIEND_ARRIVE_X) / seconds).coerceIn(FRIEND_MIN_DRIFT, FRIEND_MAX_DRIFT)
        val stats = RodeoHorseStats.wild(Random.nextInt(MIN_HORSE_LEVEL, MAX_HORSE_LEVEL))
        horses.add(WildHorse(x = startX, stats = stats, friend = friend, drift = drift, holdUntilScore = highscore))
    }

    /** Leaves [horse] behind; its rider, if any, rides along. */
    fun letGo(horse: WildHorse) {
        horse.state = WildHorseState.RELEASED
    }

    /** The ridden horse was swapped for another one: it stays behind with [stats] (and [rider]). */
    fun leaveBehind(stats: RodeoHorseStats, rider: RodeoFriend?) {
        horses.add(WildHorse(x = HORSE_X, stats = stats, friend = rider, drift = 0f).apply { state = WildHorseState.RELEASED })
    }

    /**
     * The closest horse whose saddle will be in [reach] when the loop arrives in [timeToCatch];
     * [onCaught] gets it once the loop brought it to the rider.
     */
    fun lassoGrab(reach: ClosedFloatingPointRange<Float>, timeToCatch: Float, onCaught: (WildHorse) -> Unit): LassoGrab? {
        val horse = horses
            .filter { it.state == WildHorseState.RUNNING }
            .filter { it.x + COWBOY_SEAT_X - it.drift * timeToCatch in reach || it.x + COWBOY_SEAT_X in reach }
            .minByOrNull { it.x }
            ?: return null
        return object : LassoGrab {
            override val x: Float get() = horse.x + COWBOY_SEAT_X
            override val y: Float get() = horse.height + COWBOY_SEAT_Y
            override fun catch(): Boolean {
                if (horse.state != WildHorseState.RUNNING) return false
                horse.state = WildHorseState.LASSOED
                return true
            }
            override fun follow(tipX: Float, tipY: Float) {
                horse.x = tipX - COWBOY_SEAT_X
            }
            override fun release() = onCaught(horse)
        }
    }

    fun ui(): List<RodeoWildHorseUi> = horses.map { horse ->
        RodeoWildHorseUi(
            x = horse.x,
            pose = RodeoHorsePose(
                height = horse.height,
                gaitPhase = horse.gaitPhase,
                airborne = horse.height > 0f,
                riderLean = if (horse.height > 0f) 1f else 0f,
                pitchDegrees = if (horse.height > 0f) 0f else gallopRock(horse.gaitPhase),
                pivotX = 12f,
                pivotY = 12f,
                hindLegScale = 1f,
                frontLegFold = 0f,
                hatLift = 0f,
                glow = 0f,
                hasRider = horse.friend != null,
                coat = horse.stats.coat,
                level = horse.stats.level,
                lives = horse.stats.lives,
                maxLives = horse.stats.maxLives,
                riderId = horse.friend?.userId,
                ghost = horse.ghost,
            ),
            friendName = horse.friend?.username,
            lassoable = horse.state == WildHorseState.RUNNING,
        )
    }
}

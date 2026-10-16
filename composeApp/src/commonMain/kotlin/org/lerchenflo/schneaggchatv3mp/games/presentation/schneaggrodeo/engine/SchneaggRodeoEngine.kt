package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyPart
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGhostUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.SchneaggRodeoFrame
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.SchneaggRodeoSnapshot
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.LassoGrab
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoTraffic
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cablecar.CABLE_CAR_PASS_SPEED
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.carriage.SNAILS_PER_CARRIAGE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rocket.SNAILS_PER_ROCKET
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Schneagg Rodeo - how the game is put together
//
//  SchneaggRodeoViewModel  steps this engine once per display frame and publishes toFrame() to
//                          the screen (render/, ui/).
//  SchneaggRodeoEngine     (this file) owns the parts below and decides the order things happen
//                          in each frame (see step). It holds no mechanics of its own beyond the
//                          rules that connect the parts: collisions, rewards, penalties.
//
//  engine/   RodeoScale        units, fixed positions of horse, hand and saddle, hitbox
//            RodeoDifficulty   pace and pressure over time - the place to tune the difficulty
//            RodeoCourse       fences, snails and horseshoes: spawning and moving them
//            RodeoHorse        pace, jump, stumble, rider lean
//            RodeoSuperJump    the super jump's wind-up and flight
//            RodeoLasso        throwing, homing in, catching
//            RodeoFall         thrown off: the cowboy on foot and back into the saddle
//            RodeoPack         the chasing ravens; the run ends once they catch up
//            RodeoEffects      dust, splash, sparkle, vehicle banner
//            RodeoMarkers      highscore posts along the track
//            RodeoHorseStats   level, hearts and coat of a horse; RodeoHorse holds the ridden one's
//            RodeoWildHorses   other horses on the track: wild ones to switch to, friends to pick up
//            RodeoLandscape    mountains and forests the track runs through; their vehicles come along
//            RodeoMushrooms    magic mushrooms: giant, tiny or slow motion for a few seconds
//            RodeoMapSwitch    the rare way to another map (cave, sea, rainbow, fossil layer) and back
//            RodeoTerrain      the hills of the ground under the track
//            RodeoTest         developer toggles to try out one vehicle or map on its own
//            RodeoDeepSea      fish, whales and wrecks deep down in the open sea
//            RodeoRunFlavor    the run's weather and time of day
//            RodeoSkyWonders   rainbow after the rain, fireflies and the ghost horse's spell at night
//            RodeoPizzaOvens   stops by the roadside: pizza oven and Käsknöpfle kiosk
//            RodeoStanislausRunner  the rare Stanislaus running alongside
//            RodeoHorsePoser   how horse and rider look in every situation
//  vehicles/ RodeoTraffic      all vehicles and which one is around; see RodeoVehicle on adding one
//  render/   drawing of the track (RodeoTrackCanvas) and everything on it
//  ui/       buttons, hints, banner and speedometer around the track
//
// A new mechanic usually gets its own class here with reset() and a step function, plus one line
// in step() below; if it shows up on screen, a field in SchneaggRodeoFrame and a draw call in
// RodeoTrackCanvas.

/** Longer frames (a hiccup, a debugger pause) are cut so nothing tunnels through a fence. */
private const val MAX_FRAME_SECONDS = 0.05f

// Rules: what running into things costs and what catching things gives
private const val FENCE_CRASH_PENALTY = 12f   // pack gap lost
private const val CATCH_POINTS = 25           // per lassoed snail
private const val CATCH_GAP_BONUS = 8f        // pack gap won per lassoed snail
// Lucky horseshoes: jumping through one stores a lucky charm that absorbs the next crash (fence or
// bridge) - no stumble, no penalty. Collected between the horse's belly and the rider's hat.
private const val HORSESHOE_REACH_BOTTOM = 12f
private const val HORSESHOE_REACH_TOP = 30f
private const val HORSESHOE_POINTS = 10
private const val MAX_LUCKY_CHARMS = 3
private const val CARROT_POINTS = 5
private const val PIZZA_POINTS = 30
private const val GEM_POINTS = 20
private const val FIREFLY_POINTS = 5
/** Where the horse's hooves stand, from HORSE_X: the ground there carries it (and the camera). */
// Spending saved-up snails pays out on top of what catching them gave, so saving them up is worth it
private const val SUPER_JUMP_POINTS = 150
private const val ROCKET_POINTS = 1000
private const val CARRIAGE_POINTS = 2500
/** Seconds a bowl of Käsknöpfle keeps the horse full (no hearts drain). */
private const val KAESKNOEPFLE_SECONDS = 20f
private const val KAESKNOEPFLE_POINTS = 20
// Digging through a dirt mound in the cave turns up a find
// Digging down from the cave: the horse paws the ground with its front hooves, dirt flying, sinks
// in and the fade takes them down to the fossil layer
private const val DIG_SECONDS = 1.6f
private const val DIG_PAW_SPEED = 9f
private const val DIG_DIRT_INTERVAL = 0.18f
private const val DIG_SINK_START = 0.9f
private const val DIG_SINK_SPEED = 6f
/** Where the loop grabs the shovel: its grip above the mound. */
private const val SHOVEL_GRIP_X = 0.5f
/** Where the mine cart comes out, from the gold mine way's left edge: in front of the tunnel mouth. */
private const val MINE_CART_OFFSET = MAP_WAY_WIDTH - 8f
/** Pawing the ground: front legs half lifted, nose down a little. */
private const val DIG_LEG_RAISE = 0.6f
private const val DIG_PITCH = 6f
private const val SHOVEL_GRIP_Y = 7f
// The pack keeps its distance around a new horse, so getting back up is never an instant loss
/** At least this far back when an exhausted horse runs off: time to lasso the new one. */
private const val EXHAUSTED_PACK_GAP = 22f
/** At least this far back once the cowboy sits on a new horse. */
private const val NEW_HORSE_PACK_GAP = 30f
/** At least this far back after getting back on after any fall. */
private const val REMOUNT_PACK_GAP = 15f
/** u/s the pack gains while the horse wades through mud. */
private const val MUD_PACK_GAIN = 3f
/** The cowboy's head above his feet on foot, and on the horse (horse grid, see drawHorseAndRider). */
private const val COWBOY_HEAD_Y = 14f
private const val RIDER_HEAD_X = 12.2f
private const val RIDER_HEAD_Y = 23.6f
/** Only a run with at least this many points gets the long ending; shorter ones end right away. */
private const val LONG_ENDING_MIN_SCORE = 2500
/** His head above the middle of his body, where he tumbles around. */
private const val HEAD_ABOVE_MIDDLE = 5f
/** The horse shies nervously before it gallops off at the end. */
private const val WAITING_GAIT_SPEED = 4f
/** How fast the rider twirls the lasso over his head (radians per second). */
private const val LASSO_TWIRL_SPEED = 11f
/**
 * A friend shows up this many seconds (at the current pace) before the run reaches their highscore,
 * whatever else is going on; they keep pace next to the rider until the run passes it.
 */
private const val FRIEND_LEAD_SECONDS = 5f
/** A friend needs at least this long to ride into lasso range. */
private const val FRIEND_MIN_ARRIVE_SECONDS = 3f

/** Horse top (hat included) above its hooves; the camera keeps this inside the canvas. */
private const val CAMERA_HORSE_TOP = 34f
private const val CAMERA_TOP_MARGIN = 2f
/** Units the whole picture shakes while a vehicle rattles along. */
private const val VEHICLE_SHAKE = 0.7f
/** Crossing a gorge the view looks down to the river: how far, around which stretch, how fast. */
private const val GORGE_CAMERA_DOWN = -12f
private const val GORGE_LOOK_BEHIND = 40f
private const val GORGE_LOOK_AHEAD = 90f
private const val GORGE_LOOK_RESPONSE = 1.5f
/** Nothing new is placed this close to a bridge (units). */
private const val BRIDGE_CLEARANCE = 20f
/** Degrees the picture tilts on the mountain, and how fast it follows. */
private const val UPHILL_TILT = -8f
private const val DOWNHILL_TILT = 6f
private const val TILT_RESPONSE = 4f
/** Hooves this far inside a rainbow gap's edges drop the horse through; it falls this fast (u/s²). */
private const val GAP_EDGE = 2f
private const val GAP_DROP_GRAVITY = 260f

/** What the ViewModel has to act on, collected during a frame (see [SchneaggRodeoEngine.drainEvents]). */
internal sealed interface RodeoEvent {
    /** A lassoed friend joined the ride. */
    data class FriendJoined(val friend: RodeoFriend) : RodeoEvent

    /** The cowboy left the horse with a friend riding along: the run's [score] is now the friend's. */
    data class FriendLeft(val friend: RodeoFriend, val score: Long, val timeMillis: Long) : RodeoEvent
}

/**
 * The Schneagg Rodeo simulation: a cowboy on a horse jumping show-jumping fences while ravens
 * chase him. Deliberately mutable and allocation-light - it is stepped every frame by
 * the ViewModel, which publishes an immutable [SchneaggRodeoFrame] built by [toFrame] afterwards.
 */
internal class SchneaggRodeoEngine : RodeoWorld {

    // --- The parts (see the overview above)

    private val course = RodeoCourse()
    private val horse = RodeoHorse()
    private val pack = RodeoPack()
    private val death = RodeoDeath()
    /** Seconds into digging down from the cave with the lassoed shovel; negative while not digging. */
    private var digTime = -1f
    private var digDirtIn = 0f
    private var digTravelled = false
    /** Digging or the long ending: the buttons do nothing meanwhile. */
    private val inputLocked: Boolean get() = death.isActive || digTime >= 0f
    /** A short run ends right away when the ravens get him: no long ending. */
    private var caughtAtOnce = false
    private val lasso = RodeoLasso(course.snails)
    private val fall = RodeoFall()
    private val superJump = RodeoSuperJump()
    private val effects = RodeoEffects()
    private val markers = RodeoMarkers()
    private val traffic = RodeoTraffic()
    private val wildHorses = RodeoWildHorses()
    private val landscape = RodeoLandscape()
    private val trip = RodeoMushroomTrip()
    private val mapSwitch = RodeoMapSwitch()
    private val pizzaOvens = RodeoPizzaOvens()
    private val flavor = RodeoRunFlavor()
    private val stanislaus = RodeoStanislausRunner()
    private val wonders = RodeoSkyWonders()
    private val terrain = RodeoTerrain()
    private val deepSea = RodeoDeepSea()

    // --- Progress of the run

    /** Visible world width in units; follows the canvas size. */
    override var worldWidth = 0f
    /** Distance ridden at the horse's own pace; the score comes from it. */
    var distance = 0f
        private set
    /** Game time ridden; drives fence growth, runners and the pressure (see RodeoDifficulty). */
    var elapsed = 0f
        private set
    /** Real (frame) time of the run, excluding pauses - the time that is submitted with the score. */
    var runTimeSeconds = 0f
        private set
    var bonusPoints = 0
        private set
    var snailsCaught = 0
        private set
    /** Collected lucky horseshoes; each one absorbs one crash. */
    var luckyCharms = 0
        private set
    /** Hits the cowboy took from the ravens: hat, arm, leg (see RodeoCowboyPart), the next one ends the run. */
    private var cowboyWounds = 0
    /** How far the ground has scrolled; runs away from [distance] while a vehicle races. */
    private var groundScroll = 0f
    /** Height of the riderless horse hopping the fences on its own while the cowboy flies. */
    override var riderlessHop = 0f
        private set
    /** A lassoed friend sitting behind the cowboy; the run's score becomes theirs once he leaves the horse. */
    var passenger: RodeoFriend? = null
        private set
    /**
     * All-time highscores sorted by score; each one is a marker post along the track, and friends
     * come riding in shortly before theirs. Set by the ViewModel once loaded.
     */
    var ghosts: List<RodeoGhostUi> = emptyList()
    private val events = mutableListOf<RodeoEvent>()
    /** Degrees the picture is tilted on the mountain, easing towards the slope. */
    private var tilt = 0f
    /** How far the view looks down into a gorge the horse crosses (negative), easing in and out. */
    private var gorgeLook = 0f
    /** Test mode: seconds until the tested vehicle comes (again). */
    private var testVehicleIn = 0f
    /** A vehicle (or a missed gap) is taking the run to another map; the ground starts over there. */
    private var travelling = false
    /** Missed a gap in the rainbow: horse and rider fall through, down to the surface. */
    private var dropping = false
    private var dropVelocity = 0f

    // --- Read by the ViewModel

    /** Distance points (more per unit on a harder level, see RodeoLevel.pointsFactor) plus bonus points. */
    val score: Int get() = (distance * level.pointsFactor / UNITS_PER_POINT).toInt() + bonusPoints

    /** Super jumps cost snails: every full [SNAILS_PER_SUPER_JUMP] caught snails are one charge. */
    val superJumpCharges: Int get() = snailsCaught / SNAILS_PER_SUPER_JUMP

    /** The cowboy lies in the dirt or stands next to his horse - the lasso is his way back up. */
    val isOnFoot: Boolean get() = fall.isOnFoot

    /** The running cowboy is close enough to his horse to lasso it. */
    val canCatchHorse: Boolean get() = fall.horseInReach

    /**
     * True once the run is over: the ravens pecked the cowboy one more time than he had parts to
     * lose, and feasted on him until he was gone (see RodeoDeath).
     */
    val isCaught: Boolean get() = death.isOver || caughtAtOnce

    /** The ravens are taking the cowboy away (the long ending of a good run). */
    val isEnding: Boolean get() = death.isActive && !death.isOver

    /** Banner currently shown over the track, if any. */
    val announcement: RodeoVehicleKind? get() = effects.announcement

    /** The map whose name is shown big right after arriving there, if any. */
    val mapTitle: RodeoMap? get() = mapSwitch.title

    /** Kind of the vehicle being ridden; the controls turn into a hint on how to ride it. */
    val rideKind: RodeoVehicleKind? get() = ride?.kind

    /** What the speedometer shows: the horse's real pace, or the vehicle's. */
    val speedKmh: Int
        get() = ride?.speedKmh(this) ?: ((if (fall.isInSaddle) horse.pace else fall.groundSpeed) * KMH_PER_UNIT_PER_SECOND).roundToInt()

    /** Enough snails saved up for the rocket, and the track is clear for it. */
    val rocketReady: Boolean
        get() = snailsCaught >= SNAILS_PER_ROCKET && fall.isInSaddle && !superJump.isBusy && traffic.isClear

    /** Enough snails saved up for the golden carriage, and the track is clear for it. */
    val carriageReady: Boolean
        get() = snailsCaught >= SNAILS_PER_CARRIAGE && fall.isInSaddle && !superJump.isBusy && traffic.isClear

    /** The vehicle carrying the rider (or getting him on / off). */
    private val ride: RodeoVehicle? get() = traffic.ride

    /** Height of the horse's hooves above the ground, vehicle included. */
    private val horseBase: Float get() = horse.height + (ride?.horseLift ?: 0f)

    // --- Run lifecycle

    /** The level of the current run (see RodeoLevel). */
    val level: RodeoLevel get() = RodeoDifficulty.level

    /** Starts a fresh run on [level]. */
    fun reset(level: RodeoLevel = RodeoDifficulty.level) {
        RodeoDifficulty.level = level
        course.reset()
        horse.reset()
        pack.reset()
        death.reset()
        caughtAtOnce = false
        digTime = -1f
        lasso.reset()
        fall.reset()
        superJump.reset()
        effects.reset()
        markers.reset()
        traffic.reset()
        wildHorses.reset()
        landscape.reset()
        trip.reset()
        mapSwitch.reset()
        pizzaOvens.reset()
        stanislaus.reset()
        wonders.reset()
        terrain.reset()
        deepSea.reset()
        testVehicleIn = 0f
        travelling = false
        dropping = false
        dropVelocity = 0f
        flavor.roll()
        course.mudFactor = flavor.mudFactor
        tilt = 0f
        gorgeLook = 0f
        passenger = null
        events.clear()
        distance = 0f
        elapsed = 0f
        runTimeSeconds = 0f
        bonusPoints = 0
        snailsCaught = 0
        luckyCharms = 0
        cowboyWounds = 0
        groundScroll = 0f
        riderlessHop = 0f
        // Test mode starts right on the tested map
        val testMap = RodeoTest.vehicle?.let { traffic.mapOf(it) } ?: RodeoTest.map
        if (testMap != null && testMap != RodeoMap.SURFACE) {
            mapSwitch.startOn(testMap)
            switchMap(testMap)
        }
    }

    /**
     * Continues a saved run: progress and pace come back, the track ahead starts empty again (like
     * at the start of a run), since fences and snails are not persisted.
     */
    fun restore(snapshot: SchneaggRodeoSnapshot) {
        reset(RodeoLevel.of(snapshot.level))
        horse.restore(snapshot.speed, RodeoHorseStats(snapshot.horseLevel, snapshot.horseLives, snapshot.horseCoat))
        passenger = snapshot.passengerId?.let { RodeoFriend(userId = it, username = snapshot.passengerName.orEmpty()) }
        pack.restore(snapshot.chaseGap)
        course.restore(snapshot.fenceCount, snapshot.nextRunnerIn)
        distance = snapshot.distance
        elapsed = snapshot.elapsedSeconds
        runTimeSeconds = snapshot.runTimeSeconds
        bonusPoints = snapshot.bonusPoints
        markers.reset(bonusPoints)
        snailsCaught = snapshot.snailsCaught
        luckyCharms = snapshot.luckyCharms
        // Saved while the ravens were feasting: one last chance
        cowboyWounds = snapshot.cowboyWounds.coerceAtMost(RodeoCowboyPart.entries.size)
    }

    fun toSnapshot(): SchneaggRodeoSnapshot = SchneaggRodeoSnapshot(
        speed = horse.speed,
        distance = distance,
        elapsedSeconds = elapsed,
        runTimeSeconds = runTimeSeconds,
        chaseGap = pack.gap,
        bonusPoints = bonusPoints,
        fenceCount = course.fenceCount,
        snailsCaught = snailsCaught,
        superJumpCharges = superJumpCharges,
        luckyCharms = luckyCharms,
        nextRunnerIn = course.nextRunnerIn,
        horseLevel = horse.level,
        horseLives = horse.lives,
        horseCoat = horse.coat,
        passengerId = passenger?.userId,
        passengerName = passenger?.username,
        cowboyWounds = cowboyWounds,
        level = level.difficulty,
    )

    /** Events of the frames since the last call. */
    fun drainEvents(): List<RodeoEvent> = events.toList().also { events.clear() }

    /**
     * The cowboy is off the horse (thrown, switched horses, boarded the plane, run over): a friend
     * riding along gets off, and the run's score so far becomes theirs.
     */
    fun leaveHorse() {
        val friend = passenger ?: return
        passenger = null
        events += RodeoEvent.FriendLeft(friend, score.toLong(), (runTimeSeconds * 1000f).toLong())
    }

    // --- Input

    fun jumpPressed() {
        if (horse.jumpHeld || inputLocked) return // key repeat while holding
        horse.jumpHeld = true
        if (!fall.isInSaddle) {
            fall.jumpPressed()
            return
        }
        val ride = ride
        if (ride != null) {
            ride.onJump(true)
            return
        }
        if (fall.isInSaddle && horse.isOnGround && !superJump.isActive && !dropping) horse.jump()
    }

    fun jumpReleased() {
        horse.jumpHeld = false
        traffic.releaseJump()
    }

    /** Steers the plane or the rocket down while held; does nothing on the horse. */
    fun divePressed() {
        if (inputLocked) return
        ride?.onDive(true)
    }

    fun diveReleased() {
        traffic.releaseDive()
    }

    fun superJumpPressed() {
        // No super jump over the rainbow's gaps: it is made for fences
        if (inputLocked || superJumpCharges <= 0 || !fall.isInSaddle || ride != null || superJump.isBusy || mapSwitch.map == RodeoMap.RAINBOW) return
        // In the air it fires on landing, on the ground right away
        if (horse.isOnGround) startSuperJump() else superJump.queue()
    }

    /** Buys the rocket with saved-up snails. */
    fun rocketPressed() {
        if (!rocketReady || inputLocked) return
        snailsCaught -= SNAILS_PER_ROCKET
        bonusPoints += ROCKET_POINTS
        traffic.launchRocket(this)
    }

    /** Buys the golden carriage with saved-up snails. */
    fun carriagePressed() {
        if (!carriageReady || inputLocked) return
        snailsCaught -= SNAILS_PER_CARRIAGE
        bonusPoints += CARRIAGE_POINTS
        traffic.sendCarriage(this)
    }

    fun lassoPressed() {
        if (inputLocked) return
        val ride = ride
        if (ride != null && ride.onLassoButton(this)) return
        if (!lasso.isReady) return
        if (ride != null && !ride.allowsLasso) return
        if (!fall.isInSaddle) {
            // On foot the lasso is for the horse only; it catches it once he is close enough
            if (fall.canLasso) lasso.throwOnFoot()
            return
        }
        // How fast the ground (and everything on it) moves by, per second
        val groundSpeed = ride?.worldScroll(this, horse.pace, 1f) ?: horse.pace
        lasso.throwFromSaddle(groundSpeed) { reach, timeToCatch ->
            // A passing vehicle beats any snail; while riding one it may offer targets of its own.
            // Then other horses on the track, then snails.
            traffic.current?.takeIf { !superJump.isActive }?.lassoGrab(this, HORSE_X + HAND_X, reach, timeToCatch)
                ?: wildHorses.takeIf { ride == null && !superJump.isActive }?.lassoGrab(reach, timeToCatch, ::wildHorseCaught)
                ?: stanislaus.takeIf { ride == null && !superJump.isActive }?.lassoGrab(reach, timeToCatch, ::stanislausCaught)
                ?: pizzaOvens.takeIf { ride == null && !superJump.isActive }?.lassoGrab(reach, timeToCatch, groundSpeed, ::stopFood)
                ?: shovelGrab(reach, timeToCatch, groundSpeed).takeIf { ride == null && !superJump.isActive }
        }
    }

    // --- The frame

    /** Advances the world by one frame of [frameSeconds] real time. */
    fun step(frameSeconds: Float) {
        if (worldWidth <= 0f || isCaught) return
        val realDt = min(frameSeconds, MAX_FRAME_SECONDS)
        runTimeSeconds += realDt
        trip.tick(realDt)
        // A slow-motion mushroom slows down the whole world, not the clock of the run
        val dt = realDt * trip.timeFactor
        if (death.isActive) {
            stepDeath(dt)
            return
        }
        mapSwitch.stepFade(realDt, ::switchMap)
        if (digTime >= 0f) {
            stepDig(dt)
            return
        }
        effects.tick(dt)
        if (fall.isInSaddle) stepRiding(dt) else stepOffTheHorse(dt)
        if (pack.stepAttack(dt)) peckCowboy()
    }

    /** The ravens peck the next part off the cowboy; it flies away. Nothing left to lose: he is done for. */
    private fun peckCowboy() {
        cowboyWounds++
        val part = RodeoCowboyPart.entries.getOrNull(cowboyWounds - 1)
        if (part == null) {
            die()
            return
        }
        val (headX, headY) = cowboyHead()
        val drop = when (part) {
            RodeoCowboyPart.HAT -> -2f
            RodeoCowboyPart.ARM -> 4f
            RodeoCowboyPart.LEG -> 9f
        }
        effects.losePart(part, headX, headY - drop)
    }

    /**
     * The end of the run: the ravens grab him, drop him into the mud and carry him off (see
     * RodeoDeath), while the horse gallops off. Whatever carried him ends at once. A short run
     * skips all that and ends right away.
     */
    private fun die() {
        if (score < LONG_ENDING_MIN_SCORE) {
            caughtAtOnce = true
            return
        }
        val (headX, headY) = cowboyHead()
        traffic.endRide()
        lasso.reset()
        superJump.reset()
        horse.stopJumping()
        horse.height = 0f
        horse.inMud = false
        riderlessHop = 0f
        death.start(headX, headY - HEAD_ABOVE_MIDDLE)
        pack.takeAway()
    }

    /** The world stands still while the ravens take him away; the horse shies, then gallops off. */
    private fun stepDeath(dt: Float) {
        effects.tick(dt)
        pack.tick(dt)
        val ranBefore = death.horseRun
        death.step(dt) { x -> effects.dust(x) }
        val ran = death.horseRun - ranBefore
        if (ran > 0f) horse.gallop(ran) else horse.gaitPhase += dt * WAITING_GAIT_SPEED
    }

    /** Where the cowboy's head is: on the horse, on his own, or as a vehicle shows him. */
    private fun cowboyHead(): Pair<Float, Float> {
        val alone = fall.cowboyUi() ?: traffic.current?.cowboy(this)
        if (alone != null) return alone.x to (alone.height + COWBOY_HEAD_Y)
        return (HORSE_X + fall.horseOffset() + RIDER_HEAD_X) to (horseBase + RIDER_HEAD_Y)
    }

    private fun stepRiding(dt: Float) {
        elapsed += dt
        // Up or down the mountain unless something carries the horse over it
        horse.slope = if (ride == null && !superJump.isActive) landscape.slopeAt(HORSE_X + HITBOX_RIGHT) else Slope.FLAT
        horse.jumpBoost = trip.jumpBoost
        horse.accelerate(dt, RodeoDifficulty.topSpeed(elapsed))
        // Momentum from the hills, unless something carries the horse
        horse.rideHills(dt, if (ride == null && !superJump.isActive) terrain.slopeAt(HORSE_X + HOOVES_X) else 0f)
        val ridden = horse.pace * dt
        distance += ridden
        // On a vehicle the world may race by, but the points keep coming in at the horse's pace
        val scroll = ride?.worldScroll(this, ridden, dt) ?: ridden
        groundScroll += scroll
        // Standing on a vehicle the horse keeps its legs still
        if (ride?.carriesHorse != true) horse.gallop(ridden)
        pack.tick(dt)
        // Only galloping tires the horse, not standing on a vehicle
        if (ride == null) horse.tire(dt)

        if (!stepJumps(dt, ridden)) return // thrown off
        if (dropping) dropThroughGap(dt) else fallIntoGaps()
        riderlessHop = if (ride?.horseRunsRiderless == true) hopOverFences(HORSE_X + 14f, course.fences, reach = 14f, clearance = 3f) else 0f
        // Rider eases into the forward seat on takeoff and back upright after landing
        horse.lean(dt, leaning = horse.height > 0f || ride?.riderLeans == true)

        landscape.scroll(scroll, worldWidth)
        // Hills only up on the surface, and never under the fast vehicles or the mountain
        terrain.scroll(
            scroll, worldWidth, elapsed,
            keepFlat = traffic.needsFlatTrack || landscape.mountain != null || mapSwitch.isAway,
        )
        // The planks of a bridge bend under the horse standing (or galloping) on them
        terrain.bearBridges(dt, load = if (horseBase < 0.5f) HORSE_X + HOOVES_X else null)
        val gorgeTarget = if (terrain.bridgeWithin(HORSE_X - GORGE_LOOK_BEHIND, HORSE_X + GORGE_LOOK_AHEAD)) GORGE_CAMERA_DOWN else 0f
        gorgeLook += (gorgeTarget - gorgeLook) * min(1f, dt * GORGE_LOOK_RESPONSE)
        mapSwitch.scroll(scroll)
        pizzaOvens.scroll(scroll)
        course.map = mapSwitch.map
        deepSea.step(dt, scroll, worldWidth, active = mapSwitch.map == RodeoMap.SEA)
        // The rain runs out, and once back on the surface a rainbow comes along
        if (flavor.tick(dt)) course.mudFactor = flavor.mudFactor
        if (flavor.rainbowDue && !mapSwitch.isAway && mapSwitch.isClear) {
            flavor.rainbowSent()
            wonders.sendRainbow(worldWidth)
        }
        bonusPoints += wonders.step(
            dt, scroll, ridden * level.pointsFactor, worldWidth,
            firefliesAllowed = flavor.timeOfDay == RodeoTimeOfDay.NIGHT && !mapSwitch.isAway && mapSwitch.isClear,
        )
        val targetTilt = when (horse.slope) {
            Slope.FLAT -> 0f
            Slope.UPHILL -> UPHILL_TILT
            Slope.DOWNHILL -> DOWNHILL_TILT
        }
        tilt += (targetTilt - tilt) * min(1f, dt * TILT_RESPONSE)
        // Mushrooms grow thick in the forest
        course.mushroomChance = if (landscape.inForest(worldWidth, worldWidth + 1f)) FOREST_MUSHROOM_CHANCE else MUSHROOM_CHANCE

        // No new fences while a vehicle is ridden: it brings its own obstacles (or none). None on a
        // bridge either, and no mud or mushrooms on its planks.
        val bridgeComing = terrain.bridgeWithin(worldWidth - BRIDGE_CLEARANCE, worldWidth + BRIDGE_CLEARANCE)
        // The rainbow's last stretch down to its end has no gaps
        val spawnFences = traffic.current?.blocksFences != true && !bridgeComing && (mapSwitch.map != RodeoMap.RAINBOW || mapSwitch.isClear)
        course.scrollFences(scroll, ridden, worldWidth, horse.speed, elapsed, spawnFences)
        if (terrain.hasBridge) {
            course.mud.removeAll { it.x > worldWidth && terrain.bridgeWithin(it.x, it.x + it.width) }
            course.mushrooms.removeAll { it.x > worldWidth && terrain.bridgeWithin(it.x - 2f, it.x + 2f) }
        }
        // No runners on the rainbow: they would only fall through its gaps
        if (mapSwitch.map != RodeoMap.RAINBOW) course.sendRunners(dt, worldWidth, elapsed)
        crashIntoFences()
        course.moveSnails(scroll, dt)
        crashIntoRunners()
        course.scrollPickups(scroll)
        collectHorseshoes()
        collectCarrots()
        eatMushrooms()
        collectGems()
        catchFireflies()
        wadeThroughMud(dt)
        // Into the sea or up the ramp; the gold mine sends its cart along
        mapSwitch.checkHorse(HORSE_X + HITBOX_LEFT, HORSE_X + HITBOX_RIGHT)
        sendMineEntranceCart()

        // Other horses: wild ones now and then, friends shortly before their highscore
        wildHorses.step(dt, scroll, course.fences, score.toLong())
        // On the other maps there are no wild horses, landscapes or stops - friends still come, and each
        // other map has vehicles of its own. A due way there (or back) goes first.
        val mapIsClear = mapSwitch.isClear && !mapSwitch.isDue
        val onSurface = !mapSwitch.isAway && mapIsClear
        val horsesMayCome = ride == null && traffic.isClear && !superJump.isActive
        val nothingAround = horsesMayCome && wildHorses.isClear && landscape.isClear
        // A test run (see RodeoTest) only brings its vehicle
        val regular = !RodeoTest.isActive
        // The faster the run, the stronger the wild horses (see RodeoHorseStats.minLevelAt)
        val wildBase = max(horse.level, RodeoHorseStats.minLevelAt(horse.speed) - 1)
        wildHorses.sendWild(dt, worldWidth, wildBase, allowed = horsesMayCome && onSurface && regular, night = flavor.timeOfDay == RodeoTimeOfDay.NIGHT)
        // Friends come near their highscore no matter what else is going on
        if (regular) sendFriends()

        // A mountain (with its cable car) or a forest (with its magic mushrooms) now and then
        val section = landscape.tick(
            dt,
            worldWidth,
            pace = horse.pace,
            passSpeed = CABLE_CAR_PASS_SPEED,
            allowed = horsesMayCome && wildHorses.isClear && onSurface && regular && !terrain.hasBridge,
        )
        when (section) {
            SectionKind.MOUNTAIN -> traffic.sendCableCar(this)
            SectionKind.FOREST -> Unit
            null -> Unit
        }

        // Vehicles: one at a time, sent along while the rider is free
        traffic.step(
            this, dt, scroll,
            maySend = !superJump.isActive && elapsed > 0f && wildHorses.isClear && landscape.isClear && mapIsClear &&
                    !stanislaus.isAround && regular,
            map = mapSwitch.map,
            flatTrack = terrain.isFlat(HORSE_X, worldWidth),
        )
        sendTestVehicle(dt)

        // Rarely a shaft or the beach opens up, when nothing else is going on; away the way out comes
        if (regular) mapSwitch.tick(dt, allowed = nothingAround && !terrain.hasBridge, shapeGround = terrain::addFeature)
        // Now and then a stop by the roadside, and very rarely Stanislaus running along
        pizzaOvens.tick(dt, worldWidth, allowed = onSurface && regular && !bridgeComing)
        stanislaus.step(dt)
        stanislaus.tick(dt, worldWidth, allowed = nothingAround && onSurface && regular)
        // Boarding a vehicle ends a throw at once
        lasso.step(dt, horseBase, cutShort = ride?.allowsLasso == false, onSnailCaught = ::snailCaught)
        traffic.checkRider(
            onBoarded = { kind ->
                effects.announce(kind)
                horse.jumpHeld = false
                // Plane, car: the cowboy leaves his horse
                if (ride?.riderOnHorse == false) leaveHorse()
            },
            onLeft = {
                horse.jumpPeak = 0f
                effects.splash()
            },
        )

        pack.ride(dt, RodeoDifficulty.chaseGapRegain(elapsed))
        markers.catchUp(dt, bonusPoints)

        // Out of hearts: bucks the cowboy off and runs away, once nothing else is going on
        // (not up on the rainbow: there is no ground to run after a new horse on)
        if (horse.isExhausted && ride == null && traffic.isClear && !superJump.isActive && horse.isOnGround && mapSwitch.map != RodeoMap.RAINBOW) exhaustHorse()
    }

    /** Test mode: sends the tested vehicle right away, and again shortly after each ride. */
    private fun sendTestVehicle(dt: Float) {
        val kind = RodeoTest.vehicle ?: return
        if (!traffic.isClear || !fall.isInSaddle || superJump.isActive) return
        testVehicleIn -= dt
        if (testVehicleIn > 0f) return
        testVehicleIn = TEST_VEHICLE_AGAIN_SECONDS
        if (kind == RodeoVehicleKind.CABLE_CAR) {
            // The cable car comes with its mountain
            landscape.sections.clear()
            landscape.place(SectionKind.MOUNTAIN, worldWidth, horse.pace, CABLE_CAR_PASS_SPEED)
            traffic.sendCableCar(this)
        } else {
            traffic.sendTest(kind, this)
        }
    }

    /**
     * Off the horse the cowboy runs after it: the world scrolls by at his pace (not at all while he
     * lies in the dirt).
     */
    private fun stepOffTheHorse(dt: Float) {
        pack.tick(dt)
        // The pack waits while he is off the horse
        lasso.cool(dt)
        val remounted = fall.step(
            dt,
            lasso,
            worldWidth,
            onLanded = { x -> effects.dust(x) },
            onNewHorse = { horse.takeOver(RodeoHorseStats.replacement(horse.speed)) },
        )
        // Galloping or trotting ahead of him, or pacing nervously while it waits
        if (fall.horseStride > 0f) horse.gallop(fall.horseStride) else horse.gaitPhase += dt * 4f
        scrollOnFoot(fall.groundSpeed * dt, dt)
        if (remounted) {
            pack.keepAway(if (fall.hasNewHorse) NEW_HORSE_PACK_GAP else REMOUNT_PACK_GAP)
            lasso.reset()
            horse.remounted()
            riderlessHop = 0f
            effects.splash()
        }
    }

    /** The world moves by [scroll] under the running cowboy: the track, the landscape and what is on it. */
    private fun scrollOnFoot(scroll: Float, dt: Float) {
        distance += scroll
        groundScroll += scroll
        landscape.scroll(scroll, worldWidth)
        terrain.scroll(
            scroll, worldWidth, elapsed,
            keepFlat = traffic.needsFlatTrack || landscape.mountain != null || mapSwitch.isAway,
        )
        terrain.bearBridges(dt, load = fall.runnerX?.takeIf { fall.runnerHeight < 0.5f })
        mapSwitch.scroll(scroll)
        pizzaOvens.scroll(scroll)
        deepSea.step(dt, scroll, worldWidth, active = mapSwitch.map == RodeoMap.SEA)
        bonusPoints += wonders.step(dt, scroll, scroll * level.pointsFactor, worldWidth, firefliesAllowed = false)
        // Fences come at his pace, so they are as far apart in time as on the horse
        val bridgeComing = terrain.bridgeWithin(worldWidth - BRIDGE_CLEARANCE, worldWidth + BRIDGE_CLEARANCE)
        course.scrollFences(scroll, scroll, worldWidth, COWBOY_RUN_SPEED, elapsed, spawnFences = !bridgeComing)
        course.moveSnails(scroll, dt)
        course.scrollPickups(scroll)
        wildHorses.step(dt, scroll, course.fences, score.toLong())
        // The riderless horse hops the fences on its own
        riderlessHop = hopOverFences(HORSE_X + fall.horseOffset() + 14f, course.fences, reach = 14f, clearance = 3f)
    }

    /** Super jump or normal jump; returns false if the landing threw the cowboy off. */
    private fun stepJumps(dt: Float, ridden: Float): Boolean {
        if (superJump.isActive) {
            if (superJump.step(dt, ridden, horse, course.fences)) {
                effects.dust(HORSE_X + 14f)
                effects.splash()
            }
        } else if (horse.stepJump(dt, boostAllowed = ride == null)) {
            effects.splash()
            if (shouldThrowCowboy()) {
                throwCowboy()
                return false
            }
        }
        if (superJump.queued && horse.isOnGround && !superJump.isActive) startSuperJump()
        return true
    }

    private fun startSuperJump() {
        horse.stopJumping()
        superJump.start(course, worldWidth, horse.speed, elapsed)
        snailsCaught -= SNAILS_PER_SUPER_JUMP
        bonusPoints += SUPER_JUMP_POINTS
    }

    private fun shouldThrowCowboy(): Boolean =
        traffic.isClear && !superJump.queued && mapSwitch.map != RodeoMap.RAINBOW && fall.shouldThrow(horse.jumpPeak, pack.gap, elapsed)

    private fun throwCowboy() {
        fall.start(elapsed)
        loseRider()
    }

    /** No hearts left: the horse throws the cowboy off and runs away; another one comes along. */
    private fun exhaustHorse() {
        fall.start(elapsed, horseRunsOff = true, worldWidth = worldWidth)
        pack.keepAway(EXHAUSTED_PACK_GAP)
        loseRider()
    }

    private fun loseRider() {
        horse.loseRider()
        horse.inMud = false
        leaveHorse()
        // A snail dangling in the lasso gets away
        lasso.end()
        lasso.clearCooldown()
    }

    // --- Rules

    /**
     * The super jump sails over everything, including the fences it passes low at takeoff. On a
     * vehicle the vehicle deals with the track; the riderless horse under the plane hops everything
     * on its own.
     */
    private val invulnerable: Boolean get() = superJump.isActive || ride != null || wonders.isGhost || dropping

    private fun crashIntoFences() {
        if (invulnerable) return
        val hitBottom = horse.height + HITBOX_BOTTOM
        course.fences.forEach { fence ->
            if (!fence.knocked && fence.overlaps(HORSE_X + HITBOX_LEFT, HORSE_X + HITBOX_RIGHT) && hitBottom < fence.top) {
                fence.knocked = true
                // A giant horse tramples it flat without breaking its stride
                if (trip.tramples) effects.dust(fence.x) else crash(FENCE_CRASH_PENALTY)
            }
        }
    }

    /** Running into a snail on the ground is harmless: the horse just kicks it out of the way. */
    private fun crashIntoRunners() {
        if (invulnerable) return
        val hitBottom = horse.height + HITBOX_BOTTOM
        course.snails.forEach { snail ->
            val hit = snail.state == SnailState.ACTIVE && snail.kind == SnailKind.RUNNER &&
                    snail.x - SNAIL_HALF_WIDTH < HORSE_X + HITBOX_RIGHT &&
                    snail.x + SNAIL_HALF_WIDTH > HORSE_X + HITBOX_LEFT &&
                    hitBottom < snail.height + SNAIL_BODY_HEIGHT
            if (hit) {
                snail.knock()
                effects.dust(snail.x)
            }
        }
    }

    /** Lucky horseshoes are picked up by the horse (and rider) passing through. */
    private fun collectHorseshoes() {
        if (ride?.hidesHorse == true) return
        val base = horseBase
        course.horseshoes.removeAll { shoe ->
            val collected = shoe.x > HORSE_X + HITBOX_LEFT && shoe.x < HORSE_X + HITBOX_RIGHT + 4f &&
                    shoe.height in (base + HORSESHOE_REACH_BOTTOM)..(base + HORSESHOE_REACH_TOP)
            if (collected) collectHorseshoe(shoe)
            collected
        }
    }

    /** Carrots are eaten by the horse passing through, like the horseshoes; each one brings back a heart. */
    private fun collectCarrots() {
        if (ride != null) return
        val base = horseBase
        course.carrots.removeAll { carrot ->
            val eaten = carrot.x > HORSE_X + HITBOX_LEFT && carrot.x < HORSE_X + HITBOX_RIGHT + 6f &&
                    carrot.height in (base + HORSESHOE_REACH_BOTTOM)..(base + HORSESHOE_REACH_TOP)
            if (eaten) {
                horse.feed()
                bonusPoints += CARROT_POINTS
                effects.sparkle(carrot.x, carrot.height)
            }
            eaten
        }
    }


    /** Galloping over a magic mushroom eats it: a random effect for a few seconds (see RodeoMushrooms). */
    private fun eatMushrooms() {
        if (ride != null || !horse.isOnGround) return
        course.mushrooms.removeAll { mushroom ->
            val eaten = mushroom.x > HORSE_X + HITBOX_LEFT - MUSHROOM_HALF_WIDTH && mushroom.x < HORSE_X + HITBOX_RIGHT + MUSHROOM_HALF_WIDTH
            if (eaten) {
                trip.start()
                bonusPoints += MUSHROOM_POINTS
                effects.sparkle(mushroom.x, 4f)
            }
            eaten
        }
    }

    /** Crystals in the cave are picked up like the horseshoes. */
    private fun collectGems() {
        if (ride?.hidesHorse == true) return
        val base = horseBase
        course.gems.removeAll { gem ->
            val collected = gem.x > HORSE_X + HITBOX_LEFT && gem.x < HORSE_X + HITBOX_RIGHT + 4f &&
                    gem.height in (base + HORSESHOE_REACH_BOTTOM - 4f)..(base + HORSESHOE_REACH_TOP)
            if (collected) {
                bonusPoints += GEM_POINTS
                effects.sparkle(gem.x, gem.height)
            }
            collected
        }
    }

    /** Fireflies on a night run are caught by horse and rider passing through, like the horseshoes. */
    private fun catchFireflies() {
        if (ride?.hidesHorse == true) return
        val base = horseBase
        wonders.catchFireflies(
            left = HORSE_X + HITBOX_LEFT,
            right = HORSE_X + HITBOX_RIGHT + 4f,
            bottom = base + HORSESHOE_REACH_BOTTOM - 4f,
            top = base + HORSESHOE_REACH_TOP,
        ) { firefly ->
            bonusPoints += FIREFLY_POINTS
            effects.sparkle(firefly.x, firefly.height)
        }
    }

    /**
     * The fade to the other map is at its darkest: the track is cleared and [map] begins. The pack
     * and friends on their horses come along; everything else stays behind.
     */
    private fun switchMap(map: RodeoMap) {
        // A vehicle that took the run here (or a gap the horse fell through) stays behind; the ground
        // starts over flat
        traffic.endRide()
        lasso.end()
        digTime = -1f
        if (travelling) terrain.reset()
        travelling = false
        if (dropping || horse.height < 0f) effects.dust(HORSE_X + HOOVES_X)
        dropping = false
        horse.height = maxOf(0f, horse.height)
        horse.jumpPeak = 0f
        course.map = map
        course.clearForNewMap(horse.speed, elapsed)
        stanislaus.reset()
        wonders.clearForNewMap()
        landscape.sections.clear()
        pizzaOvens.ovens.clear()
        wildHorses.horses.removeAll { it.friend == null }
        horse.inMud = false
        tilt = 0f
    }

    /** Hooves on the ground over a gap in the rainbow: horse and rider drop through, down to the surface. */
    private fun fallIntoGaps() {
        if (mapSwitch.map != RodeoMap.RAINBOW || ride != null || superJump.isActive || !horse.isOnGround) return
        val hooves = HORSE_X + HOOVES_X
        if (course.gaps.none { hooves > it.x + GAP_EDGE && hooves < it.x + it.width - GAP_EDGE }) return
        dropping = true
        dropVelocity = 0f
        horse.stopJumping()
        travelTo(RodeoMap.SURFACE)
    }

    /** Falling through the rainbow until the fade to the surface is at its darkest. */
    private fun dropThroughGap(dt: Float) {
        dropVelocity -= GAP_DROP_GRAVITY * dt
        horse.height += dropVelocity * dt
    }

    /** Hooves on the ground in a puddle: the horse slows down and the pack gains on it. */
    private fun wadeThroughMud(dt: Float) {
        horse.inMud = ride == null && !superJump.isActive && horse.isOnGround &&
                course.mud.any { it.overlaps(HORSE_X + HITBOX_LEFT, HORSE_X + HITBOX_RIGHT) }
        if (horse.inMud) pack.closeIn(MUD_PACK_GAIN * RodeoDifficulty.penaltyFactor(elapsed) * dt)
    }

    /** Sends in a friend whose highscore the run reaches in about [FRIEND_LEAD_SECONDS]. */
    private fun sendFriends() {
        val pace = max(1f, horse.pace)
        val score = score
        for (ghost in ghosts) {
            if (!ghost.isFriend || ghost.isOwn || ghost.userId.isEmpty() || ghost.userId == passenger?.userId) continue
            val friend = RodeoFriend(userId = ghost.userId, username = ghost.username)
            if (wildHorses.wasShown(friend)) continue
            val seconds = (ghost.score - score) * UNITS_PER_POINT / level.pointsFactor / pace
            if (seconds in 0f..FRIEND_LEAD_SECONDS) {
                wildHorses.sendFriend(friend, max(seconds, FRIEND_MIN_ARRIVE_SECONDS), worldWidth, ghost.score)
                return
            }
        }
    }

    /**
     * The lasso brought another horse to the rider: a friend hops on behind the cowboy (their horse
     * stays behind), a wild horse is switched to (the old one stays behind, with a friend on it).
     */
    private fun wildHorseCaught(caught: WildHorse) {
        if (!fall.isInSaddle || ride != null) {
            wildHorses.letGo(caught)
            return
        }
        if (caught.ghost) {
            // The ghost horse vanishes into thin air and leaves its spell on horse and rider
            wildHorses.horses.remove(caught)
            wonders.startGhost()
            effects.sparkle(HORSE_X + COWBOY_SEAT_X, horseBase + COWBOY_SEAT_Y + 6f)
            return
        }
        val friend = caught.friend
        if (friend != null) {
            caught.friend = null
            wildHorses.letGo(caught)
            leaveHorse()
            passenger = friend
            events += RodeoEvent.FriendJoined(friend)
        } else {
            val rider = passenger
            leaveHorse()
            wildHorses.leaveBehind(horse.stats, rider)
            wildHorses.horses.remove(caught)
            horse.takeOver(caught.stats)
        }
        effects.sparkle(HORSE_X + COWBOY_SEAT_X, horseBase + COWBOY_SEAT_Y + 6f)
        effects.splash()
    }

    /**
     * The lasso brought something to eat from a stop: a pizza (full hearts and one level up) or a
     * bowl of Käsknöpfle (full for a while).
     */
    private fun stopFood(kind: RodeoStopKind) {
        when (kind) {
            RodeoStopKind.PIZZA_OVEN -> {
                horse.eatPizza()
                bonusPoints += PIZZA_POINTS
            }
            RodeoStopKind.KIOSK -> {
                horse.eatKaesknoepfle(KAESKNOEPFLE_SECONDS)
                bonusPoints += KAESKNOEPFLE_POINTS
            }
        }
        effects.sparkle(HORSE_X + HAND_X, horseBase + HAND_Y)
    }

    /** The lasso brought Stanislaus in: a big bonus, and the baffled pack falls all the way back. */
    private fun stanislausCaught() {
        bonusPoints += stanislaus.catchPoints
        pack.escape()
        effects.sparkle(HORSE_X + HAND_X, horseBase + HAND_Y)
        effects.splash()
    }

    /** The gold mine came up on the surface: its cart comes out to roll along beside the horse. */
    private fun sendMineEntranceCart() {
        if (mapSwitch.map != RodeoMap.SURFACE) return
        val way = mapSwitch.mapWays.firstOrNull { it.destination == RodeoMap.CAVE && !it.cartSent } ?: return
        // Another vehicle still around: tried again next frame
        way.cartSent = traffic.sendMineEntranceCart(this, x = way.x + MINE_CART_OFFSET)
    }

    /** The shovel stuck in a dirt mound in the cave, for the lasso; it starts the dig down once it is back. */
    private fun shovelGrab(reach: ClosedFloatingPointRange<Float>, timeToCatch: Float, groundSpeed: Float): LassoGrab? {
        if (mapSwitch.map != RodeoMap.CAVE) return null
        val mound = course.mounds
            .filter { it.hasShovel && it.x + SHOVEL_GRIP_X - groundSpeed * timeToCatch in reach }
            .minByOrNull { it.x }
            ?: return null
        return object : LassoGrab {
            override val x: Float get() = mound.x + SHOVEL_GRIP_X
            override val y: Float get() = SHOVEL_GRIP_Y
            override fun catch(): Boolean {
                if (!mound.hasShovel) return false
                mound.hasShovel = false
                mound.shovelX = x
                mound.shovelY = y
                return true
            }
            override fun follow(tipX: Float, tipY: Float) {
                mound.shovelX = tipX
                mound.shovelY = tipY
            }
            override fun release() {
                mound.shovelX = null
                startDigging()
            }
        }
    }

    /** The horse stops and digs down with its hooves, then horse and rider go down to the fossil layer. */
    private fun startDigging() {
        if (digTime >= 0f || death.isActive || mapSwitch.map != RodeoMap.CAVE || ride != null || !fall.isInSaddle) return
        horse.stopJumping()
        horse.height = 0f
        digTime = 0f
        digDirtIn = 0f
        digTravelled = false
        effects.sparkle(HORSE_X + HOOVES_X, 4f)
    }

    /** The world stands still while the horse digs; dirt flies off its hooves until the fade takes them down. */
    private fun stepDig(dt: Float) {
        digTime += dt
        effects.tick(dt)
        pack.tick(dt)
        horse.gaitPhase += dt * DIG_PAW_SPEED
        digDirtIn -= dt
        if (digDirtIn <= 0f) {
            digDirtIn = DIG_DIRT_INTERVAL
            effects.splash()
            effects.dust(HORSE_X + HOOVES_X + 4f)
        }
        if (digTime >= DIG_SECONDS && !digTravelled) {
            digTravelled = true
            travelTo(RodeoMap.FOSSIL)
        }
    }

    /** How far horse and rider sank into the hole they dig. */
    private val digSink: Float get() = if (digTime < 0f) 0f else max(0f, digTime - DIG_SINK_START) * DIG_SINK_SPEED

    private fun snailCaught() {
        snailsCaught++
        bonusPoints += CATCH_POINTS
        pack.fallBack(CATCH_GAP_BONUS)
    }

    // --- RodeoWorld: what vehicles may use

    override val fences: MutableList<Fence> get() = course.fences
    override val snails: MutableList<Snail> get() = course.snails
    override val horseshoes: MutableList<Horseshoe> get() = course.horseshoes
    override val speed: Float get() = horse.speed
    override val horseHeight: Float get() = horse.height
    override val mountainX: Float? get() = landscape.mountain?.x
    override val map: RodeoMap get() = mapSwitch.map

    override fun addFence(x: Float, gapAfter: Float): Fence = course.addFence(x, gapAfter, horse.speed, elapsed)

    override fun resumeFences() = course.resumeFences(horse.speed, elapsed)

    override fun addBonusPoints(points: Int) {
        bonusPoints += points
    }

    override fun eatSnail(snail: Snail) {
        course.snails.remove(snail)
        snailCaught()
        effects.sparkle(snail.x, snail.height + 3f)
    }

    override fun collectHorseshoe(shoe: Horseshoe) {
        luckyCharms = min(MAX_LUCKY_CHARMS, luckyCharms + 1)
        bonusPoints += HORSESHOE_POINTS
        effects.sparkle(shoe.x, shoe.height)
    }

    override fun sparkle(x: Float, y: Float) = effects.sparkle(x, y)

    override fun dust(x: Float) = effects.dust(x)

    override fun splash() = effects.splash()

    override fun crash(penalty: Float): Boolean {
        if (luckyCharms > 0) {
            // The lucky charm takes the hit: the poles still fall, but the horse keeps its stride
            luckyCharms--
            effects.sparkle(HORSE_X + HOOVES_X, horseBase + 14f)
            return false
        }
        horse.stumble()
        horse.hurt()
        pack.closeIn(penalty * RodeoDifficulty.penaltyFactor(elapsed))
        effects.dust(HORSE_X + 20f) // front hooves
        return true
    }

    override fun takeHorseOffTheGround(): Float {
        val height = horse.height
        horse.height = 0f
        horse.stopJumping()
        superJump.cancelQueue()
        return height
    }

    override fun escapePack() = pack.escape()

    override fun scarePack(gap: Float) = pack.fallBack(gap)

    override fun travelTo(map: RodeoMap) {
        travelling = true
        mapSwitch.travelTo(map)
    }

    // --- Render model

    /** Immutable render model of the current world. */
    fun toFrame(): SchneaggRodeoFrame {
        val vehicle = traffic.current
        val shaking = ride?.shaking == true
        val (headX, headY) = if (pack.isAttacking) cowboyHead() else (0f to 0f)
        val dying = death.isActive
        return SchneaggRodeoFrame(
            distance = groundScroll,
            fences = course.fenceUis(),
            snails = course.snailUis(pack.clock),
            ravens = if (dying) pack.flockUi(death.flockX, death.flockY, death.ravensHold) else pack.ui(course.fences, headX, headY),
            horse = poseHorse(horse, superJump, fall, ride, horseBase, riderlessHop, runTimeSeconds, passenger, trip.horseScale)
                .let { pose ->
                    pose.copy(
                        ghost = wonders.isGhost,
                        riderWounds = cowboyWounds,
                        hasRider = pose.hasRider && !dying,
                        offsetX = pose.offsetX + death.horseRun,
                        hasLasso = fall.isInSaddle && ride == null && !dying && digTime < 0f,
                        frontLegRaise = if (digTime >= 0f) DIG_LEG_RAISE else pose.frontLegRaise,
                        pitchDegrees = if (digTime >= 0f) pose.pitchDegrees + DIG_PITCH else pose.pitchDegrees,
                        height = pose.height - digSink,
                        lassoTwirl = runTimeSeconds * LASSO_TWIRL_SPEED,
                        lassoThrow = lasso.throwProgress ?: -1f,
                    )
                },
            lasso = if (fall.isInSaddle) lasso.uiFromSaddle(horseBase) else fall.lassoUi(lasso),
            dust = effects.dustUi(),
            splashProgress = effects.splashProgress(),
            markers = markers.ui(ghosts, distance, worldWidth, level.pointsFactor),
            cowboy = death.cowboyUi() ?: fall.cowboyUi() ?: vehicle?.cowboy(this),
            horseshoes = course.horseshoeUis(pack.clock),
            luckyCharms = luckyCharms,
            sparkle = effects.sparkleUi(),
            cowboyWounds = cowboyWounds,
            lostPart = effects.lostPartUi(),
            mudSplash = death.splashUi(),
            spotlight = death.spotlightUi(),
            vehicles = listOfNotNull(vehicle?.ui()),
            horseOnVehicle = ride?.carriesHorse == true,
            speedBlur = shaking,
            shakeX = if (shaking) VEHICLE_SHAKE * sin(runTimeSeconds * 97f) else 0f,
            shakeY = if (shaking) VEHICLE_SHAKE * cos(runTimeSeconds * 131f) else 0f,
            cameraY = cameraY(),
            space = vehicle?.space() ?: 0f,
            carrots = course.carrotUis(pack.clock),
            mud = death.puddleUi()?.let { course.mudUis() + it } ?: course.mudUis(),
            inMud = horse.inMud && fall.isInSaddle,
            wildHorses = wildHorses.ui(),
            sections = landscape.ui(),
            mushrooms = course.mushroomUis(),
            tiltDegrees = tilt,
            enclosed = mapSwitch.map.isEnclosed,
            map = mapSwitch.map,
            gaps = course.gapUis(),
            mounds = course.moundUis(),
            runnerMan = stanislaus.ui(),
            weather = flavor.weather,
            timeOfDay = flavor.timeOfDay,
            mapWays = mapSwitch.ui(),
            gems = course.gemUis(pack.clock),
            pizzaOvens = pizzaOvens.ui(),
            pizza = pizzaOvens.pizzaUi(),
            terrain = terrain.ui(),
            // The banks, not the planks: the camera stays put while the bridge bends
            terrainShift = terrain.baseHeightAt(HORSE_X + HOOVES_X),
            deepSea = deepSea.ui(),
            rainbowX = wonders.rainbowX,
            doublePointsSeconds = wonders.doublePointsSeconds,
            fireflies = wonders.fireflyUis(),
            fade = mapSwitch.fade,
            slowMotion = trip.timeFactor < 1f,
            tripStrength = trip.strength,
            tripClock = trip.clock,
        )
    }

    /** How far the view pans up so the horse (or the vehicle) stays in the picture. */
    private fun cameraY(): Float {
        death.camera()?.let { return it }
        traffic.current?.camera()?.let { return it }
        val visibleTop = WORLD_HEIGHT_UNITS - GROUND_OFFSET_UNITS - CAMERA_TOP_MARGIN
        // In the sea the view looks down into the deep water, unless the horse jumps high
        val lowest = (if (mapSwitch.map == RodeoMap.SEA) SEA_CAMERA_DOWN else 0f) + gorgeLook
        return max(lowest, horseBase + CAMERA_HORSE_TOP - visibleTop)
    }
}

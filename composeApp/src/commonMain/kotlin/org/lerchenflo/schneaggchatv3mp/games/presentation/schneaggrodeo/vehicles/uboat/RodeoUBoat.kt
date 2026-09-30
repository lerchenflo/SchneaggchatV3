package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.uboat

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.OnTrack
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.scrollAlong
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.circleTouchesBox
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEABED_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEA_CAMERA_DOWN
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEA_WATER_LINE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.progressOf
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoSteering
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoWorld
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.VehiclePhase
import kotlin.math.max
import kotlin.random.Random

// U-boat (open sea): a yellow submarine surfaces next to the track. Lasso it and horse and rider
// climb in through the hatch. Hold to rise, let go to dive all the way down to the seabed, the view
// following it: pearls float at all depths, treasure chests lie on the seabed, and sea mines hang on
// their chains - touching one blows the U-boat back up to the surface and ends the dive early.

// Shape, shared with the drawing (grid: x from the stern, y up from the hull's underside)
internal const val UBOAT_LENGTH = 32f
internal const val UBOAT_HEIGHT = 7f
/** The conning tower on top. */
internal const val TOWER_X = 17f
internal const val TOWER_WIDTH = 6f
internal const val TOWER_HEIGHT = 4f
internal const val PEARL_RADIUS = 1.2f
internal const val MINE_RADIUS = 2f

private const val UBOAT_PASS_SPEED = 22f
/** Underside while surfaced: only the tower and the top of the hull stick out of the water. */
private const val SURFACED_Y = SEA_WATER_LINE - UBOAT_HEIGHT + 1.5f
/** How deep it dives (the underside): just above the seabed. */
private const val DEEPEST_Y = SEABED_Y + 2f
/** How far the view goes down with it, at most (see RodeoVehicle.camera). */
internal const val UBOAT_CAMERA_DEEPEST = -30f
private const val UBOAT_RIDE_X = HORSE_X - 4f
private const val UBOAT_BOARD_SECONDS = 0.7f
private const val UBOAT_RIDE_SECONDS = 12f
private const val UBOAT_UNLOAD_SECONDS = 0.8f
private const val UBOAT_SPEED_FACTOR = 1.1f
private const val THING_INTERVAL = 0.45f
private const val MINE_SHARE = 0.3f
private const val TREASURE_SHARE = 0.15f
private const val PEARL_POINTS = 15
private const val TREASURE_POINTS = 40
/** Hitbox inset from the hull's ends. */
private const val HIT_INSET = 2f

private enum class ThingKind { PEARL, MINE, TREASURE }

/** A pearl or a sea mine in the water, or a treasure chest on the seabed; [x] / [y] its center. */
private class Thing(override var x: Float, val y: Float, val kind: ThingKind) : OnTrack

internal class RodeoUBoat : RodeoVehicle(RodeoVehicleKind.U_BOAT) {

    private val steering = RodeoSteering(climbAccel = 60f, diveAccel = 0f, sinkAccel = 40f, maxClimb = 22f, maxSink = 22f)
    private val things = mutableListOf<Thing>()
    /** Underside of the hull. */
    private var y = SURFACED_Y
    private var boardStartX = 0f
    private var propeller = 0f
    private var thingIn = 0f
    private var unloadStartY = 0f
    /** The dive ended on a mine: a flash at the hull. */
    private var boom = false

    override val length = UBOAT_LENGTH
    override val passSpeed = UBOAT_PASS_SPEED
    override val carriesHorse = true
    override val horseHops = false
    /** Horse and rider are inside while it dives. */
    override val hidesHorse: Boolean get() = phase == VehiclePhase.RIDING

    /** The whole U-boat is a target. */
    override fun hitch() = hitchX() to SEA_WATER_LINE

    override fun reset() {
        super.reset()
        things.clear()
        steering.release()
    }

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        y = SURFACED_Y
        boom = false
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        world.takeHorseOffTheGround()
        steering.stop()
        steering.release()
    }

    override fun onJump(pressed: Boolean) {
        steering.climb = pressed && phase == VehiclePhase.RIDING
    }

    /** The view dives along with it. */
    override fun camera(): Float? =
        if (phase == VehiclePhase.RIDING || phase == VehiclePhase.UNLOADING) (y - 4f).coerceIn(UBOAT_CAMERA_DEEPEST, SEA_CAMERA_DOWN) else null

    override val rideSpeedFactor = UBOAT_SPEED_FACTOR

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        propeller += dt * 20f
        things.scrollAlong(scroll) { it.x < -5f }
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> passBy(dt)
            VehiclePhase.BOARDING -> {
                x = lerp(boardStartX, UBOAT_RIDE_X, smoothstep(progressOf(phaseTime, UBOAT_BOARD_SECONDS)))
                if (phaseTime >= UBOAT_BOARD_SECONDS) {
                    enter(VehiclePhase.RIDING)
                    thingIn = 0.4f
                }
            }
            VehiclePhase.RIDING -> dive(world, dt)
            VehiclePhase.UNLOADING -> {
                // Back up to the surface, horse and rider climb out
                y = lerp(unloadStartY, SURFACED_Y, smoothstep(progressOf(phaseTime, UBOAT_UNLOAD_SECONDS)))
                if (phaseTime >= UBOAT_UNLOAD_SECONDS) {
                    enter(VehiclePhase.LEAVING)
                    world.splash()
                    world.resumeFences()
                }
            }
            VehiclePhase.LEAVING -> {
                // Dives away
                y -= 8f * dt
                x -= scroll * 0.5f
                if (y < DEEPEST_Y - 10f || x + UBOAT_LENGTH < 0f) enter(VehiclePhase.IDLE)
            }
        }
    }

    private fun dive(world: RodeoWorld, dt: Float) {
        y = max(DEEPEST_Y, steering.fly(y, dt, ceiling = SURFACED_Y))
        thingIn -= dt
        if (thingIn <= 0f) {
            thingIn = THING_INTERVAL
            val roll = Random.nextFloat()
            val kind = when {
                roll < MINE_SHARE -> ThingKind.MINE
                roll < MINE_SHARE + TREASURE_SHARE -> ThingKind.TREASURE
                else -> ThingKind.PEARL
            }
            val depth = if (kind == ThingKind.TREASURE) {
                SEABED_Y + 1.5f
            } else {
                DEEPEST_Y + 2f + Random.nextFloat() * (SURFACED_Y + UBOAT_HEIGHT - DEEPEST_Y - 3f)
            }
            things.add(Thing(x = world.worldWidth + 3f, y = depth, kind = kind))
        }
        var hitMine = false
        things.removeAll { thing ->
            val radius = if (thing.kind == ThingKind.MINE) MINE_RADIUS else PEARL_RADIUS * 1.5f
            val touching = circleTouchesBox(thing.x, thing.y, radius, left = x + HIT_INSET, right = x + UBOAT_LENGTH - HIT_INSET, bottom = y, top = y + UBOAT_HEIGHT)
            if (touching) {
                when (thing.kind) {
                    ThingKind.MINE -> hitMine = true
                    ThingKind.PEARL -> world.addBonusPoints(PEARL_POINTS)
                    ThingKind.TREASURE -> world.addBonusPoints(TREASURE_POINTS)
                }
                world.sparkle(thing.x, thing.y)
            }
            touching
        }
        if (hitMine || phaseTime >= UBOAT_RIDE_SECONDS) {
            boom = hitMine
            if (hitMine) world.splash()
            unloadStartY = y
            steering.release()
            enter(VehiclePhase.UNLOADING)
        }
    }

    override val horseLift: Float
        get() = if (phase == VehiclePhase.UNLOADING) max(0f, y + UBOAT_HEIGHT - 1f) else 0f

    override fun ui(): RodeoVehicleUi? {
        if (phase == VehiclePhase.IDLE) return null
        return RodeoUBoatUi(
            x = x,
            y = y,
            propeller = propeller,
            crewAboard = phase == VehiclePhase.RIDING,
            boom = if (boom && phase == VehiclePhase.UNLOADING) progressOf(phaseTime, UBOAT_UNLOAD_SECONDS) else null,
            pearls = things.filter { it.kind == ThingKind.PEARL }.map { RodeoSeaThingUi(it.x, it.y) },
            mines = things.filter { it.kind == ThingKind.MINE }.map { RodeoSeaThingUi(it.x, it.y) },
            treasures = things.filter { it.kind == ThingKind.TREASURE }.map { RodeoSeaThingUi(it.x, it.y) },
            lassoHint = lassoHint(x + UBOAT_LENGTH / 2f, SEA_WATER_LINE + 8f),
        )
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.fordescort

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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

// Ford Escort: a wrecked red Ford Escort Mk3 cabrio rolls by on bare golden rims, its paint dripping
// onto the road, a crate full of cash in the trunk. The lasso grabs whatever part of it is in reach.
// Lasso it and the cowboy jumps into the driver's seat while his horse shoves the wreck ahead of it,
// sparks flying, right through every fence in the way. The cash blows out of the trunk while it
// drives; once the last of the 12000 € is gone the wreck breaks down and the cowboy jumps back into
// the saddle. Then one of two endings (the horse gallops on either way):
//  - the scrapyard: the horse kicks the wreck onto the junk pile of the scrapyard coming up ahead
//  - the car lift: the empty wreck rolls on between the pillars of a car lift, is lifted up and
//    stripped down to the bare hull - door, windshield, seats, crate, rims and bumpers fly over onto
//    the scrapyard right behind it

// Shape, shared with the drawing (grid: x from the rear bumper, y up from the belly)
internal const val CAR_LENGTH = 38f
/** The belly rides this high on the bare rims. */
internal const val CAR_RIM_LIFT = 2.2f
internal const val CAR_SEAT_X = 20f         // driver's hips, between roll bar and windshield
private const val CAR_SEAT_Y = 4f + CAR_RIM_LIFT
/** The cash crate in the trunk: its center and top. */
internal const val CRATE_X = 5f
internal const val CRATE_TOP = 11.5f

// The scrapyard (grid: x from its left end, y up from the ground)
internal const val YARD_WIDTH = 70f
/** The top of the junk pile the wreck lands on. */
internal const val PILE_X = 30f
internal const val PILE_TOP = 12f

private const val CAR_PASS_SPEED = 30f      // u/s across the screen while rolling by, slow enough to lasso
private const val CAR_HITCH_Y = 5f
/** Where the loop lands on the car: the middle of the lasso's reach, ahead of the hand. */
private const val CAR_HITCH_AIM = HORSE_X + HAND_X + 16f
/** Rear bumper against the horse's chest while shoving. */
private const val CAR_RIDE_X = HORSE_X + 29f
/** Seconds the loop takes to reach the car (half the lasso's throw), aimed at where it will be. */
private const val LASSO_LEAD = 0.225f
private const val CAR_BOARD_SECONDS = 0.7f
private const val CAR_UNLOAD_SECONDS = 0.9f
/** Until the money is gone and the wreck breaks down. */
private const val CAR_RIDE_SECONDS = 7f
private const val CAR_JUMP_HOP = 7f
private const val CAR_FIRST_FENCE_SECONDS = 0.4f
private const val CAR_FENCE_SECONDS = 0.8f
private const val CAR_FENCE_POINTS = 8
private const val CAR_RUMBLE_DEGREES = 1.2f
private const val RIM_TURN = 0.45f          // radians per unit of ground rolled over

// The cash in the trunk, blowing out while it drives
internal const val START_MONEY = 12000
private const val BILL_INTERVAL = 0.07f
private const val BILL_GRAVITY = 22f        // bills flutter down slowly
private const val BILL_MIN_VX = -35f
private const val BILL_RANDOM_VX = 20f
private const val BILL_MIN_VY = 8f
private const val BILL_RANDOM_VY = 14f

// Paint dripping onto the road
private const val DRIP_INTERVAL = 0.12f

// Kicked onto the scrapyard: flies in an arc onto the junk pile, tumbling once
/** Where on screen the junk pile is when the wreck lands on it. */
private const val LANDING_X = HORSE_X + 60f
private const val THROW_SECONDS = 1.1f
private const val THROW_ARC = 18f
private const val LANDED_TILT = 14f

// The car lift (grid: x from its rear pillar, y up from the ground)
internal const val LIFT_SPAN = 48f
internal const val LIFT_PILLAR_WIDTH = 2.5f
internal const val LIFT_PILLAR_HEIGHT = 26f
internal const val RUNWAY_THICKNESS = 1.2f
/** Rear bumper on the runway, between the pillars. */
private const val LIFT_CAR_X = 5f
private const val LIFT_HEIGHT = 9f
/** The lift comes in this far inside the right edge, the scrapyard right behind it. */
private const val LIFT_ENTER_INSET = 12f
private const val YARD_AFTER_LIFT = 6f
private const val DRIVE_IN_RATE = 6f
private const val DRIVE_IN_MIN_SPEED = 40f
private const val DRIVE_IN_MAX_SECONDS = 1.2f
private const val RAISE_SECONDS = 0.5f
private const val STRIP_INTERVAL = 0.13f
private const val PART_THROW_SECONDS = 0.65f
private const val PART_THROW_ARC = 12f

/** A bill blown out of the trunk; [x] / [y] its center. */
private class Bill(var x: Float, var y: Float, val vx: Float, var vy: Float, val spin: Float) {
    var rotation = 0f
    var landed = false
}

/** A puddle of red paint on the road, [x] its center. */
private class Puddle(var x: Float, val size: Float)

private enum class Ending { SCRAPYARD, CAR_LIFT }

private enum class LiftStep { DRIVE_IN, RAISE, STRIP, DONE }

/**
 * A part torn off on the car lift, flying from where it sat on the car ([startX] / [startY]) onto
 * the junk pile, [pileOffset] from the scrapyard's left end.
 */
private class FlyingPart(val part: RodeoEscortPart, val startX: Float, val startY: Float, val pileOffset: Float, val spin: Float) {
    var time = 0f
    var x = startX
    var y = startY
    var rotation = 0f
    var landed = false
}

internal class RodeoFordEscort : RodeoVehicle(RodeoVehicleKind.FORD_ESCORT) {

    private var y = 0f
    private var rotation = 0f
    private var boardStartX = 0f
    private var fenceIn = 0f
    /** Drives the sparks, the drips and the rattling. */
    private var scrape = 0f
    private var rimPhase = 0f
    private var money = START_MONEY
    private val bills = mutableListOf<Bill>()
    private var billIn = 0f
    private val puddles = mutableListOf<Puddle>()
    private var dripIn = 0f
    /** Left end of the scrapyard, once it came up after the breakdown. */
    private var yardX: Float? = null
    private var launchX = 0f
    private var landed = false
    private var ending = Ending.SCRAPYARD
    /** Rear pillar of the car lift, once it came up (car lift ending). */
    private var liftX: Float? = null
    private var liftStep = LiftStep.DRIVE_IN
    private var liftTime = 0f
    private var armHeight = 0f
    private var stripIn = 0f
    /** Torn off the car on the lift, in this order. */
    private val partsGone = mutableListOf<RodeoEscortPart>()
    private val flyingParts = mutableListOf<FlyingPart>()

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

    override fun reset() {
        super.reset()
        clearLeftovers()
    }

    private fun clearLeftovers() {
        bills.clear()
        puddles.clear()
        yardX = null
        liftX = null
        partsGone.clear()
        flyingParts.clear()
    }

    override fun spawn(world: RodeoWorld) {
        super.spawn(world)
        y = 0f
        rotation = 0f
        money = START_MONEY
        clearLeftovers()
        landed = false
    }

    override fun board(world: RodeoWorld) {
        super.board(world)
        boardStartX = x
        // A jump in progress ends right here; the horse stays on the ground behind the car
        world.takeHorseOffTheGround()
    }

    override fun update(world: RodeoWorld, dt: Float, scroll: Float) {
        stepLeftovers(dt, scroll)
        val onTheRoad = phase != VehiclePhase.LEAVING
        if (onTheRoad) {
            rimPhase += scroll * RIM_TURN
            drip(dt)
        }
        when (phase) {
            VehiclePhase.IDLE -> Unit
            VehiclePhase.APPROACH -> {
                scrape += dt
                passBy(dt)
            }
            VehiclePhase.BOARDING -> {
                scrape += dt
                val progress = progressOf(phaseTime, CAR_BOARD_SECONDS)
                x = lerp(boardStartX, CAR_RIDE_X, smoothstep(progress))
                if (progress >= 1f) {
                    enter(VehiclePhase.RIDING)
                    fenceIn = CAR_FIRST_FENCE_SECONDS
                    billIn = 0f
                }
            }
            VehiclePhase.RIDING -> ride(world, dt)
            VehiclePhase.UNLOADING -> {
                // Broken down and smoking in front of the horse while the cowboy jumps back
                scrape += dt
                if (phaseTime >= CAR_UNLOAD_SECONDS) {
                    when (ending) {
                        Ending.SCRAPYARD -> kickOntoYard(world)
                        Ending.CAR_LIFT -> bringLift(world)
                    }
                }
            }
            VehiclePhase.LEAVING -> when (ending) {
                Ending.SCRAPYARD -> flyOntoYard()
                Ending.CAR_LIFT -> strip(dt)
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
        // The cash blows away, bill by bill
        val left = 1f - progressOf(phaseTime, CAR_RIDE_SECONDS)
        money = ((START_MONEY * left) / 10f).roundToInt() * 10
        billIn -= dt
        if (billIn <= 0f && money > 0) {
            billIn = BILL_INTERVAL
            bills.add(
                Bill(
                    x = x + CRATE_X,
                    y = CAR_RIM_LIFT + CRATE_TOP,
                    vx = BILL_MIN_VX + Random.nextFloat() * BILL_RANDOM_VX,
                    vy = BILL_MIN_VY + Random.nextFloat() * BILL_RANDOM_VY,
                    spin = (if (Random.nextBoolean()) 1f else -1f) * (150f + Random.nextFloat() * 250f),
                )
            )
        }
        if (money <= 0) {
            money = 0
            enter(VehiclePhase.UNLOADING)
            world.resumeFences()
            ending = if (Random.nextBoolean()) Ending.SCRAPYARD else Ending.CAR_LIFT
            if (ending == Ending.SCRAPYARD) {
                // Placed ahead so its pile comes by right where the wreck lands
                yardX = LANDING_X - PILE_X + world.speed * (CAR_UNLOAD_SECONDS + THROW_SECONDS)
            }
        }
    }

    /** The cowboy is back in the saddle: the horse kicks the wreck up onto the scrapyard's junk pile. */
    private fun kickOntoYard(world: RodeoWorld) {
        enter(VehiclePhase.LEAVING)
        launchX = x
        landed = false
        world.splash()
        world.dust(x)
    }

    private fun flyOntoYard() {
        val yard = yardX ?: return enter(VehiclePhase.IDLE)
        val targetX = yard + PILE_X - CAR_LENGTH / 2f
        if (!landed) {
            val progress = progressOf(phaseTime, THROW_SECONDS)
            x = lerp(launchX, targetX, progress)
            y = lerp(0f, PILE_TOP, progress) + THROW_ARC * sin(PI.toFloat() * progress)
            rotation = (360f + LANDED_TILT) * smoothstep(progress)
            if (progress >= 1f) landed = true
        } else {
            // Lies on the pile and scrolls away with the scrapyard
            x = targetX
            y = PILE_TOP
            rotation = LANDED_TILT
            if (yard + YARD_WIDTH < 0f && bills.isEmpty()) enter(VehiclePhase.IDLE)
        }
    }

    /** The cowboy is back in the saddle: the car lift comes in from the right, the scrapyard behind it. */
    private fun bringLift(world: RodeoWorld) {
        enter(VehiclePhase.LEAVING)
        val lift = world.worldWidth - LIFT_ENTER_INSET
        liftX = lift
        yardX = lift + LIFT_SPAN + YARD_AFTER_LIFT
        liftStep = LiftStep.DRIVE_IN
        liftTime = 0f
        armHeight = 0f
    }

    /** Rolls between the pillars, goes up, and loses everything but the hull, part by part. */
    private fun strip(dt: Float) {
        val lift = liftX ?: return enter(VehiclePhase.IDLE)
        val yard = yardX ?: return enter(VehiclePhase.IDLE)
        liftTime += dt
        val onRunway = lift + LIFT_CAR_X
        when (liftStep) {
            LiftStep.DRIVE_IN -> {
                val remaining = onRunway - x
                val speed = max(DRIVE_IN_MIN_SPEED, abs(remaining) * DRIVE_IN_RATE)
                val move = sign(remaining) * min(abs(remaining), speed * dt)
                x += move
                rimPhase += move * RIM_TURN
                if (abs(onRunway - x) < 0.3f || liftTime >= DRIVE_IN_MAX_SECONDS) nextLiftStep(LiftStep.RAISE)
            }
            LiftStep.RAISE -> {
                armHeight = LIFT_HEIGHT * smoothstep(progressOf(liftTime, RAISE_SECONDS))
                if (liftTime >= RAISE_SECONDS) {
                    nextLiftStep(LiftStep.STRIP)
                    stripIn = 0f
                }
            }
            LiftStep.STRIP -> {
                stripIn -= dt
                val next = RodeoEscortPart.entries.firstOrNull { it !in partsGone }
                if (next == null) {
                    nextLiftStep(LiftStep.DONE)
                } else if (stripIn <= 0f) {
                    stripIn = STRIP_INTERVAL
                    partsGone += next
                    flyingParts += FlyingPart(
                        part = next,
                        startX = x + next.carX,
                        startY = y + CAR_RIM_LIFT + next.carY,
                        pileOffset = PILE_X - 6f + Random.nextFloat() * 12f,
                        spin = (if (Random.nextBoolean()) 1f else -1f) * (300f + Random.nextFloat() * 300f),
                    )
                }
            }
            LiftStep.DONE -> if (yard + YARD_WIDTH < 0f && bills.isEmpty()) enter(VehiclePhase.IDLE)
        }
        if (liftStep != LiftStep.DRIVE_IN) x = onRunway
        // Standing on the runway: on its rims, or on its belly once they are gone
        val rimsGone = RodeoEscortPart.REAR_RIM in partsGone && RodeoEscortPart.FRONT_RIM in partsGone
        y = armHeight + RUNWAY_THICKNESS - if (rimsGone) CAR_RIM_LIFT else 0f
    }

    private fun nextLiftStep(step: LiftStep) {
        liftStep = step
        liftTime = 0f
    }

    /** Bills, puddles, torn-off parts, the lift and the scrapyard go on moving with the ground. */
    private fun stepLeftovers(dt: Float, scroll: Float) {
        yardX = yardX?.minus(scroll)
        liftX = liftX?.minus(scroll)
        yardX?.let { yard ->
            flyingParts.forEach { part ->
                val targetX = yard + part.pileOffset
                if (part.landed) {
                    part.x = targetX
                } else {
                    part.time += dt
                    val progress = progressOf(part.time, PART_THROW_SECONDS)
                    part.x = lerp(part.startX, targetX, progress)
                    part.y = lerp(part.startY, PILE_TOP + 1f, progress) + PART_THROW_ARC * sin(PI.toFloat() * progress)
                    part.rotation += part.spin * dt
                    if (progress >= 1f) {
                        part.landed = true
                        part.y = PILE_TOP + 1f
                    }
                }
            }
        }
        bills.forEach { bill ->
            if (bill.landed) {
                bill.x -= scroll
            } else {
                bill.vy -= BILL_GRAVITY * dt
                bill.x += bill.vx * dt
                bill.y += bill.vy * dt
                bill.rotation += bill.spin * dt
                if (bill.y <= 0.3f) {
                    bill.y = 0.3f
                    bill.landed = true
                    bill.rotation = 0f
                }
            }
        }
        bills.removeAll { it.x < -5f }
        puddles.forEach { it.x -= scroll }
        puddles.removeAll { it.x < -5f }
    }

    /** Red paint drips off the body and leaves puddles on the road. */
    private fun drip(dt: Float) {
        if (phase == VehiclePhase.IDLE) return
        dripIn -= dt
        if (dripIn > 0f) return
        dripIn = DRIP_INTERVAL
        puddles.add(Puddle(x = x + 2f + Random.nextFloat() * (CAR_LENGTH - 4f), size = 0.8f + Random.nextFloat() * 1.4f))
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
            // ...and back into the saddle once it broke down
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
        return RodeoFordEscortUi(
            x = x,
            y = y,
            rotation = if (shoving) CAR_RUMBLE_DEGREES * sin(scrape * 60f) else rotation,
            hasDriver = shoving,
            sparks = if (shoving || phase == VehiclePhase.APPROACH) scrape else null,
            time = scrape,
            rimPhase = rimPhase,
            money = money.takeIf { phase != VehiclePhase.LEAVING },
            smoking = phase == VehiclePhase.UNLOADING || (phase == VehiclePhase.LEAVING && ending == Ending.SCRAPYARD && !landed),
            dripping = phase != VehiclePhase.LEAVING,
            onPile = landed,
            yardX = yardX,
            lift = liftX?.let { RodeoCarLiftUi(x = it, armHeight = armHeight) },
            onLift = liftX != null && liftStep != LiftStep.DRIVE_IN,
            partsGone = partsGone.toSet(),
            flyingParts = flyingParts.map { RodeoFlyingPartUi(part = it.part, x = it.x, y = it.y, rotation = it.rotation, landed = it.landed) },
            bills = bills.map { RodeoBillUi(x = it.x, y = it.y, rotation = it.rotation) },
            puddles = puddles.map { RodeoPuddleUi(x = it.x, size = it.size) },
            lassoHint = lassoHint(x + CAR_LENGTH / 2f, 16f),
        )
    }
}

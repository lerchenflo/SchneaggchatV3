package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyPart
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoLostPartUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoHorsePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.FRONT_SUPPORT_PHASE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HAND_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HAND_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MAX_HORSE_LEVEL
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.lerp
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Horse, rider, the cowboy on foot and the parachute, plus friends with their profile picture as
// head. The pose itself is decided in the engine (RodeoHorsePoser); these functions only draw it.

private const val RIDER_MAX_LEAN_DEGREES = 32f
/** Ducking: the rider lies flat on the neck and the whole figure crouches by this share. */
private const val RIDER_DUCK_DEGREES = 45f
private const val DUCK_SQUASH = 0.42f
/** Thickness of the cowboy's legs and of his boots. */
private const val RIDER_LIMB = 1.3f
private const val BOOT_WIDTH = 1.4f
/** The outline round the rider's leg against the horse. */
private const val HALO_WIDTH = 0.5f
// The cowboy on foot: thigh and shin, upper arm and forearm
private const val THIGH = 3f
private const val SHIN = 3.2f
private const val UPPER_ARM = 2.3f
private const val FOREARM = 2.2f
private const val HIP_Y = THIGH + SHIN
/** Share of a lasso throw the arm takes to swing out, and to come back up. */
private const val LASSO_SWING = 0.18f
/** How far a galloping hoof reaches forward and back from under its hip, in horse grid units. */
private const val GALLOP_REACH = 3f
/** How high a galloping hoof snaps up after pushing off: the front ones fold up higher. */
private const val FRONT_LIFT = 3.4f
private const val HIND_LIFT = 2.4f
/** Share of a stride a galloping hoof is on the ground; the rest of it, it swings forward. */
private const val GALLOP_STANCE = 0.32f
/**
 * Where in the stride (0..1) each hoof touches down: hind left, hind right, front left, front
 * right. After the last one leaves the ground all four are in the air for a moment.
 */
private val HIND_TOUCHDOWN = floatArrayOf(0f, 0.09f)
private val FRONT_TOUCHDOWN = floatArrayOf(0.3f, 0.39f)
/** Hips of the two legs of a pair, in the horse grid. */
private val FRONT_HIPS = floatArrayOf(17f, 19f)
private val HIND_HIPS = floatArrayOf(6f, 8f)
/** Leg segments: hip to knee (or hock) and down to the hoof; a bit longer than the hip is high, so they never lock. */
private const val FRONT_UPPER = 4.6f
private const val FRONT_LOWER = 4.9f
private const val HIND_UPPER = 4.7f
private const val HIND_LOWER = 4.9f
/** Gait phase (radians) of the suspension, all four hooves off the ground: the body is highest. */
private const val SUSPENSION_PHASE = 5.3f
/** How far the head nods with every stride. */
private const val HEAD_NOD_DEGREES = 5f
private const val TWO_PI = 2f * PI.toFloat()
/** A friend's profile picture is drawn as a big bobble head, so it can be recognized. */
private const val FRIEND_HEAD_RADIUS = 2.6f
/** Tip of the ears: a horse of the top level is gold all the way up to here. */
private const val HORSE_TOP = 25f

/** Colors of horse and rider; [body] is also the coat of the horse the run starts on. */
internal class RodeoHorseColors(
    val body: Color,
    val shirt: Color,
    val glow: Color,
    val canopy: Color,
    /** Shirt of a friend, on their own horse or riding along. */
    val friendShirt: Color,
    /** Ring around a profile picture head. */
    val headRing: Color,
    /** The lasso twirled over the rider's head. */
    val rope: Color,
    /** The cowboy's trousers, so his leg stands out against the horse. */
    val pants: Color,
)

/** Coat of [coat] (index into HORSE_COATS), or [default] for the horse the run starts on. */
internal fun coatColor(coat: Int, default: Color): Color = HORSE_COATS.getOrNull(coat) ?: default

/**
 * Side view of the horse (facing right) with the cowboy on top, drawn on a 30 x 28 unit grid.
 * Grid coordinates are y-up from the hooves; [p] converts them to canvas pixels.
 *
 * Pose: the whole figure is rotated by [RodeoHorsePose.pitchDegrees] (positive = nose down) around
 * the pivot (grid coordinates). hindLegScale > 1 pumps up the hind legs for the super jump, glow
 * lights them up, frontLegRaise lifts the front legs while the horse rears up for it. frontLegFold
 * buckles the front legs at the knee and hatLift pops the hat off the head, both for stumbling.
 * duck crouches the figure under a bridge, parachute hangs it from one. The horse's level colors
 * it gold from the hooves up (the higher, the better), its hearts are painted on its front half; a
 * friend riding it or along has [pictures] of their head.
 */
internal fun DrawScope.drawHorseAndRider(
    left: Float,
    groundY: Float,
    unit: Float,
    pose: RodeoHorsePose,
    colors: RodeoHorseColors,
    pictures: Map<String, ImageBitmap> = emptyMap(),
) {
    val coat = coatColor(pose.coat, colors.body)
    val riderColor = colors.body
    val shirtColor = if (pose.riderId != null) colors.friendShirt else colors.shirt
    val gaitPhase = pose.gaitPhase
    val airborne = pose.airborne
    val hindLegScale = pose.hindLegScale
    val frontLegFold = pose.frontLegFold
    val hatLift = pose.hatLift
    val glow = pose.glow

    fun p(x: Float, y: Float) = Offset(left + x * unit, groundY - y * unit)
    fun polygon(vararg points: Pair<Float, Float>) = polygonPath(::p, *points)

    // Floating down after the rocket ride: the canopy hangs behind the rider
    if (pose.parachute) drawParachute(::p, unit, colors.canopy, shirtColor, coat)

    withTransform({
        // Grown or shrunk by a magic mushroom, standing on the same hooves
        if (pose.scale != 1f) scale(pose.scale, pose.scale, pivot = p(14f, 0f))
        rotate(degrees = pose.pitchDegrees, pivot = p(pose.pivotX, pose.pivotY))
        // Ducking under a bridge: the whole figure crouches down onto its hooves
        if (pose.duck > 0f) scale(1f, 1f - DUCK_SQUASH * pose.duck, pivot = p(14f, 0f))
    }) {
        val legStroke = 1.6f * unit
        // Grown hind legs also get thicker, so they read as pumped up and not just stretched
        val hindStroke = legStroke * (1f + (hindLegScale - 1f) * 1.5f)
        // The body rises in the moment of suspension, all four legs gathered under it, and sinks
        // while the front legs carry it
        val bounce = if (airborne) 0f else cos(gaitPhase - SUSPENSION_PHASE) * 0.6f
        // The head nods down as the front legs land and swings up again while they push off;
        // stretched forward over a jump, ducking a little more in a stumble
        val nod = if (airborne) 4f else HEAD_NOD_DEGREES * sin(gaitPhase - FRONT_SUPPORT_PHASE - 0.3f) + 10f * frontLegFold
        val stride = fract(gaitPhase / TWO_PI)

        if (glow > 0f) {
            drawCircle(
                color = colors.glow.copy(alpha = 0.35f * glow),
                radius = 7f * hindLegScale * unit,
                center = p(7f, 9f - 4.5f * hindLegScale)
            )
        }

        /** One leg from its hip at grid ([x], [hipY]) to [hoof], bent at the joint, with a hoof at the end. */
        fun drawLeg(color: Color, x: Float, hipY: Float, hoof: Offset, upper: Float, lower: Float, kneeForward: Boolean, stroke: Float) {
            val hip = Offset(x, hipY)
            val joint = legJoint(hip, hoof, upper, lower, kneeForward)
            drawLine(color, p(hip.x, hip.y), p(joint.x, joint.y), stroke, StrokeCap.Round)
            drawLine(color, p(joint.x, joint.y), p(hoof.x, hoof.y), stroke, StrokeCap.Round)
            drawCircle(color, radius = stroke * 0.58f, center = p(hoof.x, hoof.y))
        }

        // The horse itself; drawn a second time in gold from the hooves up, as high as its level
        fun drawHorseShape(color: Color) {
            // Legs
            val hindUpper = HIND_UPPER * hindLegScale
            val hindLower = HIND_LOWER * hindLegScale
            if (airborne) {
                // Front legs tucked, hind legs stretched back - the classic jumping pose
                FRONT_HIPS.forEach { x ->
                    drawLeg(color, x, 9f, Offset(x + 0.5f, 3f), FRONT_UPPER, FRONT_LOWER, kneeForward = true, legStroke)
                }
                HIND_HIPS.forEach { x ->
                    drawLeg(color, x, 9f, Offset(x - 4f * hindLegScale, 9f - 7f * hindLegScale), hindUpper, hindLower, kneeForward = false, hindStroke)
                }
            } else {
                // Four-beat gallop: hind left, hind right, front left, front right touch down one
                // after the other, then a moment with all four off the ground, gathered under the
                // belly. Each hoof pushes back on the ground, snaps up and reaches forward to land;
                // the knees of the front legs bend forward, the hocks of the hind legs backwards.
                val raise = pose.frontLegRaise
                FRONT_HIPS.forEachIndexed { index, x ->
                    val gallop = gallopHoof(fract(stride - FRONT_TOUCHDOWN[index]), GALLOP_REACH, FRONT_LIFT)
                    // Stumbling: the knee buckles forward and the hoof folds under
                    val stumbled = lerp(Offset(x + gallop.x, gallop.y), Offset(x - 1f, 3.5f), frontLegFold)
                    // Rearing up for the super jump: lifted and pawing the air
                    val paw = sin(gaitPhase * 6f + index * PI.toFloat()) * 1.2f
                    val hoof = lerp(stumbled, Offset(x + 1f, 4.5f + paw * 1.5f), raise)
                    drawLeg(color, x, 9f, hoof, FRONT_UPPER, FRONT_LOWER, kneeForward = true, legStroke)
                }
                HIND_HIPS.forEachIndexed { index, x ->
                    val gallop = gallopHoof(fract(stride - HIND_TOUCHDOWN[index]), GALLOP_REACH * hindLegScale, HIND_LIFT)
                    val hoof = Offset(x + gallop.x, 9f - 9f * hindLegScale + gallop.y)
                    drawLeg(color, x, 9f, hoof, hindUpper, hindLower, kneeForward = false, hindStroke)
                }
            }

            // Hindquarter muscle bulging with the legs
            if (hindLegScale > 1.02f) {
                drawCircle(color, radius = 3f * hindLegScale * unit, center = p(7f, 11.5f + bounce))
            }

            // Body
            drawRoundRect(
                color = color,
                topLeft = p(4f, 15f + bounce),
                size = Size(16f * unit, 7f * unit),
                cornerRadius = CornerRadius(3f * unit)
            )

            // Tail: carried up off the croup and streaming back, swaying a beat behind the body
            // with its tip whipping a little more; streaming out straighter over a jump. A second,
            // thinner strand makes it read as hair.
            val sway = if (airborne) 0f else sin(gaitPhase - 2.4f)
            val whip = if (airborne) 0f else sin(gaitPhase * 2f - 1f)
            val tipY = if (airborne) 12f else 8.5f + 1.6f * sway + 0.6f * whip
            val tipX = if (airborne) -2.5f else -1f + 0.4f * whip
            fun drawTailStrand(droop: Float, width: Float) = drawPath(
                path = Path().apply {
                    val start = p(4.5f, 14.5f + bounce)
                    val control1 = p(1.8f, 15.8f + bounce * 0.5f + 0.6f * sway)
                    val control2 = p(-0.5f - droop * 0.3f, 12.5f + 1.2f * sway - droop)
                    val end = p(tipX + droop * 0.4f, tipY - droop)
                    moveTo(start.x, start.y)
                    cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
                },
                color = color,
                style = Stroke(width = width * unit, cap = StrokeCap.Round)
            )
            drawTailStrand(droop = 0f, width = 1.5f)
            drawTailStrand(droop = 1.2f, width = 0.8f)

            // Neck, head and ear: ride along with the body and nod around the withers
            rotate(degrees = nod, pivot = p(17.5f, 14f + bounce)) {
                drawPath(polygon(15f to 15f + bounce, 20f to 12f + bounce, 24f to 21f + bounce, 20f to 22.5f + bounce), color)
                drawPath(polygon(20f to 22.5f + bounce, 24f to 23f + bounce, 29f to 19f + bounce, 28f to 17f + bounce, 23f to 18.5f + bounce), color)
                drawPath(polygon(21f to 22.5f + bounce, 22f to 25f + bounce, 23f to 22.8f + bounce), color)
            }
        }

        drawHorseShape(coat)
        if (pose.maxLives > 0) {
            val goldTop = p(0f, HORSE_TOP * pose.level / MAX_HORSE_LEVEL).y
            clipRect(left = p(-10f, 0f).x, top = goldTop, right = p(40f, 0f).x, bottom = p(0f, -20f).y) {
                drawHorseShape(LEVEL_GOLD_COLOR)
            }
        }

        // Hearts along the body, low enough to stay clear of the rider's leg
        if (pose.maxLives > 0) drawHorseHearts(::p, unit, bounce, pose)

        // Cowboy: leg stays at the horse's side, the upper body pivots forward at the seat in jumps.
        // Once he is thrown off he is drawn separately (drawCowboy).
        if (!pose.hasRider) return@withTransform
        val riderY = bounce * 1.5f
        val lean = RIDER_MAX_LEAN_DEGREES * pose.riderLean + RIDER_DUCK_DEGREES * pose.duck
        val wounds = pose.riderWounds
        // A friend riding along sits behind him, arms around his waist
        pose.passengerId?.let { friendId ->
            drawRiderLeg(::p, 8.5f, 16f + riderY, colors.pants, colors.headRing, unit)
            rotate(degrees = lean, pivot = p(8.5f, 16f + riderY)) {
                drawRoundRect(
                    color = colors.friendShirt,
                    topLeft = p(7f, 21f + riderY),
                    size = Size(3f * unit, 5.5f * unit),
                    cornerRadius = CornerRadius(1f * unit)
                )
                drawLine(colors.friendShirt, p(9.5f, 20f + riderY), p(12f, 18.5f + riderY), 1f * unit, StrokeCap.Round)
                drawFriendHead(::p, 8.7f, 23.4f + riderY, pictures[friendId], riderColor, colors.headRing, unit)
            }
        }
        // Leg down the horse's side with the boot in the stirrup - or a stump, once pecked off
        if (RodeoCowboyPart.LEG.isLostAt(wounds)) {
            drawLine(colors.pants, p(12.2f, 16f + riderY), p(13.4f, 14.6f + riderY), RIDER_LIMB * unit, StrokeCap.Round)
        } else {
            drawRiderLeg(::p, 12.2f, 16f + riderY, colors.pants, colors.headRing, unit)
        }
        rotate(degrees = lean, pivot = p(12f, 16f + riderY)) {
            // The far arm: twirls the lasso over his head, ready to throw, and swings forward after
            // the rope when he throws it. Without a lasso it holds the reins too.
            if (pose.hasLasso) {
                drawLassoArm(::p, 11.8f, 20.6f + riderY, pose, shirtColor.copy(alpha = 0.85f), riderColor, colors.rope, unit)
            } else {
                drawArm(::p, 11.8f, 20.6f + riderY, 13.2f, 18.4f + riderY, 16.8f, 18.4f + riderY, shirtColor.copy(alpha = 0.75f), riderColor, unit)
            }
            drawRoundRect(
                color = shirtColor,
                topLeft = p(10.5f, 21.5f + riderY),
                size = Size(3f * unit, 6f * unit),
                cornerRadius = CornerRadius(1f * unit)
            )
            drawLine(riderColor, p(10.6f, 16.5f + riderY), p(13.4f, 16.5f + riderY), 0.5f * unit)
            drawRect(HAT_COLOR, p(12.6f, 16.85f + riderY), Size(0.7f * unit, 0.7f * unit))
            // The near arm holds on to the horse: a fist in the mane - the first arm the ravens take
            if (RodeoCowboyPart.ARM.isLostAt(wounds)) {
                drawLine(shirtColor, p(12.8f, 20.6f + riderY), p(13.4f, 19.6f + riderY), RIDER_LIMB * unit, StrokeCap.Round)
            } else {
                drawArm(::p, 12.8f, 20.6f + riderY, 14.4f, 18f + riderY, 16.9f, 17.2f + riderY, shirtColor, riderColor, unit)
                drawCircle(riderColor, radius = 0.75f * unit, center = p(16.9f, 17.2f + riderY))
            }
            val friendId = pose.riderId
            if (friendId != null) {
                // A friend on their own horse: their profile picture is the head
                drawFriendHead(::p, 12.2f, 23.6f + riderY, pictures[friendId], riderColor, colors.headRing, unit)
            } else {
                drawCowboyHead(::p, 12.2f, 23.6f + riderY, riderColor, colors.canopy, unit)
                // Hat, popping off the head (and tilting back) when the horse stumbles
                if (!RodeoCowboyPart.HAT.isLostAt(wounds)) {
                    rotate(degrees = -25f * hatLift / 3f, pivot = p(12.2f, 25.6f + riderY + hatLift)) {
                        drawCowboyHat(::p, 9f, 25.6f + riderY + hatLift, unit)
                    }
                }
            }
        }
    }
}

/**
 * The rider's leg from the hip at grid ([hipX], [hipY]): thigh along the saddle, shin down the
 * horse's side and the boot, all with a thin [halo] so they stand out against a horse of any coat.
 */
private fun DrawScope.drawRiderLeg(p: (Float, Float) -> Offset, hipX: Float, hipY: Float, pants: Color, halo: Color, unit: Float) {
    val hip = p(hipX, hipY)
    val knee = p(hipX + 2.4f, hipY - 2.6f)
    val ankle = p(hipX + 1.5f, hipY - 6f)
    val heel = p(hipX + 1.1f, hipY - 7f)
    val toe = p(hipX + 3.3f, hipY - 7.1f)
    val outline = HALO_WIDTH * unit
    drawLine(halo, hip, knee, RIDER_LIMB * unit + outline, StrokeCap.Round)
    drawLine(halo, knee, ankle, RIDER_LIMB * 0.9f * unit + outline, StrokeCap.Round)
    drawLine(halo, ankle, heel, BOOT_WIDTH * unit + outline, StrokeCap.Round)
    drawLine(halo, heel, toe, BOOT_WIDTH * unit + outline, StrokeCap.Round)
    drawLine(pants, hip, knee, RIDER_LIMB * unit, StrokeCap.Round)
    drawLine(pants, knee, ankle, RIDER_LIMB * 0.9f * unit, StrokeCap.Round)
    // Boot: shaft and foot, the toe in the stirrup
    drawLine(HAT_COLOR, ankle, heel, BOOT_WIDTH * unit, StrokeCap.Round)
    drawLine(HAT_COLOR, heel, toe, BOOT_WIDTH * unit, StrokeCap.Round)
}

/** An arm from the shoulder over the elbow to the hand, all in grid coordinates: sleeve and a bare hand. */
private fun DrawScope.drawArm(
    p: (Float, Float) -> Offset,
    shoulderX: Float, shoulderY: Float,
    elbowX: Float, elbowY: Float,
    handX: Float, handY: Float,
    sleeve: Color,
    skin: Color,
    unit: Float,
) {
    drawLine(sleeve, p(shoulderX, shoulderY), p(elbowX, elbowY), 1f * unit, StrokeCap.Round)
    drawLine(sleeve, p(elbowX, elbowY), p(handX, handY), 0.9f * unit, StrokeCap.Round)
    drawCircle(skin, radius = 0.55f * unit, center = p(handX, handY))
}

/**
 * The rider's lasso arm from the shoulder at grid ([shoulderX], [shoulderY]). Ready: raised over his
 * head, circling the loop above his hat. Throwing ([RodeoHorsePose.lassoThrow] 0..1): swings forward
 * to where the rope leaves the hand (HAND_X / HAND_Y), stays stretched out while the rope is out
 * (the rope itself is drawn by the track) and goes back up as the loop comes home.
 */
private fun DrawScope.drawLassoArm(
    p: (Float, Float) -> Offset,
    shoulderX: Float,
    shoulderY: Float,
    pose: RodeoHorsePose,
    sleeve: Color,
    skin: Color,
    rope: Color,
    unit: Float,
) {
    val twirl = pose.lassoTwirl
    // The hand circles a little with the loop
    val raisedX = shoulderX + 1.2f + 0.5f * cos(twirl)
    val raisedY = shoulderY + 4.1f + 0.3f * sin(twirl)
    val throwing = pose.lassoThrow
    val out = when {
        throwing < 0f -> 0f
        throwing < LASSO_SWING -> smoothstep(throwing / LASSO_SWING)
        throwing > 1f - LASSO_SWING -> smoothstep((1f - throwing) / LASSO_SWING)
        else -> 1f
    }
    val shoulder = Offset(shoulderX, shoulderY)
    val hand = Offset(lerp(raisedX, HAND_X, out), lerp(raisedY, HAND_Y, out))
    val elbow = legJoint(shoulder, hand, UPPER_ARM, FOREARM, forward = out < 0.5f)
    drawArm(p, shoulderX, shoulderY, elbow.x, elbow.y, hand.x, hand.y, sleeve, skin, unit)
    if (throwing >= 0f) return
    // The loop spinning above his hat, seen from the side: an ellipse swinging round the hand
    val loopX = hand.x + 2.2f * cos(twirl)
    val loopY = hand.y + 3.2f + 0.5f * sin(twirl)
    val loopCenter = p(loopX, loopY)
    val radiusX = (2.2f + 0.8f * sin(twirl)) * unit
    val radiusY = 0.8f * unit
    drawLine(rope, p(hand.x, hand.y), p(loopX, loopY - 0.8f), 0.4f * unit, StrokeCap.Round)
    drawOval(
        color = rope,
        topLeft = Offset(loopCenter.x - radiusX, loopCenter.y - radiusY),
        size = Size(radiusX * 2f, radiusY * 2f),
        style = Stroke(width = 0.45f * unit),
    )
}

/** The cowboy's head at grid ([x], [y]), facing right: neck, nose and a neckerchief in [scarf]. */
private fun DrawScope.drawCowboyHead(p: (Float, Float) -> Offset, x: Float, y: Float, skin: Color, scarf: Color, unit: Float) {
    drawLine(skin, p(x, y - 1.6f), p(x, y - 2.4f), 0.9f * unit)
    drawCircle(skin, radius = 1.8f * unit, center = p(x, y))
    drawCircle(skin, radius = 0.5f * unit, center = p(x + 1.7f, y - 0.2f))
    drawPath(polygonPath(p, x - 1.3f to y - 2.1f, x + 1.3f to y - 2.1f, x + 0.4f to y - 3.3f), scarf)
}

/** [value] wrapped into 0..1. */
private fun fract(value: Float) = value - floor(value)

/**
 * A galloping hoof at [t] of its stride (0 = touchdown), relative to the ground under its hip: on
 * the ground it pushes back under the body, then it snaps up quickly and reaches forward low to land.
 */
private fun gallopHoof(t: Float, reach: Float, lift: Float): Offset {
    if (t < GALLOP_STANCE) return Offset(reach * (1f - 2f * t / GALLOP_STANCE), 0f)
    val swing = (t - GALLOP_STANCE) / (1f - GALLOP_STANCE)
    val rest = 1f - swing
    return Offset(-reach + 2f * reach * smoothstep(swing), lift * sin(PI.toFloat() * (1f - rest * rest)))
}

/**
 * The knee (or hock) of a leg from [hip] to [hoof] with segments [upper] and [lower], in grid
 * coordinates; bent to the front when [forward], else backwards. Stretched straight when the hoof
 * is out of reach.
 */
private fun legJoint(hip: Offset, hoof: Offset, upper: Float, lower: Float, forward: Boolean): Offset {
    val dx = hoof.x - hip.x
    val dy = hoof.y - hip.y
    val length = max(0.01f, sqrt(dx * dx + dy * dy))
    val distance = min(length, upper + lower)
    val along = (upper * upper - lower * lower + distance * distance) / (2f * distance)
    val side = sqrt(max(0f, upper * upper - along * along)) * (if (forward) 1f else -1f)
    val dirX = dx / length
    val dirY = dy / length
    // Perpendicular to the leg, pointing to the front for a leg hanging down
    return Offset(hip.x + dirX * along - dirY * side, hip.y + dirY * along + dirX * side)
}

/** The horse's hearts along its body; the last one fills up only partly while it drains away. */
private fun DrawScope.drawHorseHearts(p: (Float, Float) -> Offset, unit: Float, bounce: Float, pose: RodeoHorsePose) {
    val areaLeft = 6f
    val areaWidth = 13.5f
    val heartSpacing = min(2.7f, areaWidth / pose.maxLives)
    val heartsLeft = areaLeft + (areaWidth - heartSpacing * pose.maxLives) / 2f
    repeat(pose.maxLives) { index ->
        val fill = (pose.lives - index).coerceIn(0f, 1f)
        drawHeart(p(heartsLeft + heartSpacing * (index + 0.5f), 10.4f + bounce), 0.92f * heartSpacing * unit, fill)
    }
}

/** A heart [size] wide around [center]; only its left [fill] share is red, the rest a faint outline. */
private fun DrawScope.drawHeart(center: Offset, size: Float, fill: Float) {
    val half = size / 2f
    val path = Path().apply {
        moveTo(center.x, center.y + half * 0.9f)
        cubicTo(center.x - half * 1.3f, center.y, center.x - half * 0.7f, center.y - half * 1.1f, center.x, center.y - half * 0.35f)
        cubicTo(center.x + half * 0.7f, center.y - half * 1.1f, center.x + half * 1.3f, center.y, center.x, center.y + half * 0.9f)
        close()
    }
    drawPath(path, HEART_COLOR.copy(alpha = 0.3f))
    if (fill > 0f) {
        clipRect(left = center.x - half, top = center.y - size, right = center.x - half + size * fill, bottom = center.y + size) {
            drawPath(path, HEART_COLOR)
        }
    }
}

/** A friend's head at grid ([x], [y]): their profile picture as a bobble head, under a cowboy hat. */
private fun DrawScope.drawFriendHead(
    p: (Float, Float) -> Offset,
    x: Float,
    y: Float,
    picture: ImageBitmap?,
    fallback: Color,
    ring: Color,
    unit: Float,
) {
    drawProfilePicture(picture, p(x, y), FRIEND_HEAD_RADIUS * unit, fallback, ring)
    drawCowboyHat(p, x - 3.2f, y + FRIEND_HEAD_RADIUS + 0.6f, unit)
}

/**
 * A round profile picture of [radius] around [center] (center-cropped to a square) with a thin
 * [ring]; a plain [fallback] disc when there is no picture.
 */
internal fun DrawScope.drawProfilePicture(picture: ImageBitmap?, center: Offset, radius: Float, fallback: Color, ring: Color) {
    if (picture == null) {
        drawCircle(fallback, radius = radius, center = center)
    } else {
        val side = min(picture.width, picture.height)
        val clip = Path().apply { addOval(Rect(center, radius)) }
        clipPath(clip) {
            drawImage(
                image = picture,
                srcOffset = IntOffset((picture.width - side) / 2, (picture.height - side) / 2),
                srcSize = IntSize(side, side),
                dstOffset = IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt()),
                dstSize = IntSize((radius * 2f).roundToInt(), (radius * 2f).roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
    drawCircle(ring, radius = radius, center = center, style = Stroke(width = radius * 0.12f))
}

/**
 * The cowboy on foot, same proportions as the rider on the horse. Grid is y-up from his feet with
 * x = 0 at his center; he faces right unless mirrored, and rotates around his middle. Legs and arms
 * bend at knee and elbow; [wounds] leave out what the ravens pecked off (see RodeoCowboyPart).
 */
internal fun DrawScope.drawCowboy(
    cowboy: RodeoCowboyUi,
    groundY: Float,
    unit: Float,
    color: Color,
    shirtColor: Color,
    scarfColor: Color,
    pantsColor: Color,
    wounds: Int = 0,
) {
    val centerX = cowboy.x * unit
    val feetY = groundY - cowboy.height * unit
    fun p(x: Float, y: Float) = Offset(centerX + x * unit, feetY - y * unit)
    val middle = p(0f, 9f)
    val tuck = cowboy.tuck
    val runPhase = cowboy.runPhase

    /**
     * A leg from the hip at [hipX]: the thigh swung forward by [swing] radians, the knee bent back by
     * [bend]; pulled in for the salto. Just a stump when [stump].
     */
    fun drawSwingingLeg(hipX: Float, swing: Float, bend: Float, stump: Boolean) {
        val thighAngle = swing + (1.4f - swing) * tuck
        val shinAngle = thighAngle - (bend + (2.2f - bend) * tuck)
        val kneeX = hipX + THIGH * sin(thighAngle)
        val kneeY = HIP_Y - THIGH * cos(thighAngle)
        if (stump) {
            drawLine(pantsColor, p(hipX, HIP_Y), p(hipX + 1.2f * sin(thighAngle), HIP_Y - 1.2f * cos(thighAngle)), RIDER_LIMB * unit, StrokeCap.Round)
            return
        }
        val footX = kneeX + SHIN * sin(shinAngle)
        val footY = kneeY - SHIN * cos(shinAngle)
        drawLine(pantsColor, p(hipX, HIP_Y), p(kneeX, kneeY), RIDER_LIMB * unit, StrokeCap.Round)
        drawLine(pantsColor, p(kneeX, kneeY), p(footX, footY), RIDER_LIMB * 0.9f * unit, StrokeCap.Round)
        // The boot: shaft over the ankle, the foot pointing forward, square to the shin
        val shaftX = footX - 0.8f * sin(shinAngle)
        val shaftY = footY + 0.8f * cos(shinAngle)
        drawLine(HAT_COLOR, p(shaftX, shaftY), p(footX, footY), BOOT_WIDTH * unit, StrokeCap.Round)
        drawLine(HAT_COLOR, p(footX - 0.3f * cos(shinAngle), footY - 0.3f * sin(shinAngle)), p(footX + 1.4f * cos(shinAngle), footY + 1.4f * sin(shinAngle)), BOOT_WIDTH * unit, StrokeCap.Round)
    }

    /** An arm from the shoulder swung forward by [swing] radians, the elbow bent by [bend]; a stump when [stump]. */
    fun drawSwingingArm(swing: Float, bend: Float, sleeve: Color, stump: Boolean) {
        val upperAngle = swing + (1.2f - swing) * tuck
        val foreAngle = upperAngle + bend
        val shoulderX = 0.4f
        val shoulderY = 11.2f
        if (stump) {
            drawLine(sleeve, p(shoulderX, shoulderY), p(shoulderX + 1f * sin(upperAngle), shoulderY - 1f * cos(upperAngle)), 1f * unit, StrokeCap.Round)
            return
        }
        val elbowX = shoulderX + UPPER_ARM * sin(upperAngle)
        val elbowY = shoulderY - UPPER_ARM * cos(upperAngle)
        drawArm(::p, shoulderX, shoulderY, elbowX, elbowY, elbowX + FOREARM * sin(foreAngle), elbowY - FOREARM * cos(foreAngle), sleeve, color, unit)
    }

    // Fading away: the whole figure drawn into a see-through layer, so overlapping limbs don't show
    val fading = cowboy.alpha < 1f
    if (fading) {
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = cowboy.alpha.coerceIn(0f, 1f) })
        }
    }
    withTransform({
        rotate(cowboy.rotation, pivot = middle)
        if (cowboy.facingLeft) scale(-1f, 1f, pivot = middle)
    }) {
        // Running: thighs swing to and fro, a knee bends most while its leg swings forward; the
        // arms swing against the legs. Standing: feet a little apart, the rope hand in front.
        // Gone limp: arms flung over his head and legs sprawled, flopping about with the flail.
        val flail = cowboy.flail
        val stride = runPhase?.let { sin(it) } ?: 0f
        val farBend = when {
            cowboy.limp -> 0.5f + 0.4f * sin(flail * 0.9f + 1f)
            runPhase != null -> 0.15f + 1.1f * max(0f, -cos(runPhase))
            else -> 0.05f
        }
        val nearBend = when {
            cowboy.limp -> 0.3f + 0.3f * sin(flail * 1.2f)
            runPhase != null -> 0.15f + 1.1f * max(0f, cos(runPhase))
            else -> 0.05f
        }
        val farSwing = when {
            cowboy.limp -> 0.5f + 0.4f * sin(flail * 1.1f)
            runPhase != null -> -0.6f * stride
            else -> -0.1f
        }
        val nearSwing = when {
            cowboy.limp -> -0.3f + 0.4f * sin(flail + 2f)
            runPhase != null -> 0.6f * stride
            else -> 0.1f
        }
        val waving = cowboy.waving
        val farArmSwing = when {
            // Waving goodbye: the arm up high, swinging to and fro
            waving != null -> 2.5f + 0.45f * sin(waving)
            cowboy.limp -> 2.6f + 0.5f * sin(flail * 1.3f)
            runPhase != null -> 0.7f * stride
            else -> -0.2f
        }
        val nearArmSwing = when {
            cowboy.limp -> 2.2f + 0.6f * sin(flail + 1f)
            runPhase != null -> -0.7f * stride
            else -> 0.3f
        }

        val farArmBend = when {
            waving != null -> 0.5f + 0.35f * sin(waving + 0.8f)
            cowboy.limp -> 0.4f + 0.3f * sin(flail)
            else -> 1.1f
        }
        drawSwingingArm(swing = farArmSwing, bend = farArmBend, sleeve = shirtColor.copy(alpha = 0.75f), stump = false)
        drawSwingingLeg(-0.4f, farSwing, farBend, stump = false)
        drawRoundRect(
            color = shirtColor,
            topLeft = p(-1.5f, 12f),
            size = Size(3f * unit, 6f * unit),
            cornerRadius = CornerRadius(1f * unit)
        )
        drawLine(pantsColor, p(-1.4f, 6.6f), p(1.4f, 6.6f), 0.5f * unit)
        drawRect(HAT_COLOR, p(0.6f, 6.95f), Size(0.7f * unit, 0.7f * unit))
        drawSwingingLeg(0.4f, nearSwing, nearBend, stump = RodeoCowboyPart.LEG.isLostAt(wounds))
        drawSwingingArm(
            swing = nearArmSwing,
            bend = if (cowboy.limp) 0.6f else if (runPhase != null) 1.2f else 1.3f,
            sleeve = shirtColor,
            stump = RodeoCowboyPart.ARM.isLostAt(wounds),
        )
        drawCowboyHead(::p, 0.2f, 14.1f, color, scarfColor, unit)
        if (!RodeoCowboyPart.HAT.isLostAt(wounds)) drawCowboyHat(::p, -3f, 16.1f + cowboy.hatLift, unit)
    }
    if (fading) drawIntoCanvas { it.restore() }
}

/**
 * A part the ravens pecked off the cowboy, flying up and back from where it was and tumbling
 * down, fading out at the end.
 */
internal fun DrawScope.drawLostPart(lost: RodeoLostPartUi, context: RodeoDrawContext, skin: Color, shirt: Color, pants: Color) {
    val unit = context.unit
    val progress = lost.progress
    val center = context.p(lost.x - 12f * progress, lost.y + 16f * progress - 30f * progress * progress)
    fun p(x: Float, y: Float) = Offset(center.x + x * unit, center.y - y * unit)
    val alpha = min(1f, (1f - progress) * 4f)
    rotate(-600f * progress, pivot = center) {
        when (lost.part) {
            RodeoCowboyPart.HAT -> drawCowboyHat(::p, -3.2f, 0f, unit, alpha)
            RodeoCowboyPart.ARM -> {
                drawLine(shirt.copy(alpha = alpha), p(0f, 1.6f), p(0f, -1.4f), 1f * unit, StrokeCap.Round)
                drawCircle(skin.copy(alpha = alpha), radius = 0.55f * unit, center = p(0f, -1.9f))
            }
            RodeoCowboyPart.LEG -> {
                drawLine(pants.copy(alpha = alpha), p(0f, 2.2f), p(0f, -1.6f), RIDER_LIMB * unit, StrokeCap.Round)
                drawLine(HAT_COLOR.copy(alpha = alpha), p(0f, -1.9f), p(1.2f, -1.9f), BOOT_WIDTH * unit, StrokeCap.Round)
            }
        }
    }
}

/** The orange cowboy hat: brim with its top-left corner at grid ([brimLeft], [brimTop]), crown above. */
internal fun DrawScope.drawCowboyHat(p: (Float, Float) -> Offset, brimLeft: Float, brimTop: Float, unit: Float, alpha: Float = 1f) {
    drawRoundRect(
        color = HAT_COLOR.copy(alpha = alpha),
        topLeft = p(brimLeft, brimTop),
        size = Size(6.4f * unit, 0.7f * unit),
        cornerRadius = CornerRadius(0.35f * unit)
    )
    drawRoundRect(
        color = HAT_COLOR.copy(alpha = alpha),
        topLeft = p(brimLeft + 1.4f, brimTop + 2.4f),
        size = Size(3.6f * unit, 2.6f * unit),
        cornerRadius = CornerRadius(0.8f * unit)
    )
}

/**
 * A round parachute over the horse, its lines running down to the saddle. [p] converts horse grid
 * coordinates to canvas pixels (see drawHorseAndRider).
 */
private fun DrawScope.drawParachute(p: (Float, Float) -> Offset, unit: Float, canopyColor: Color, stripeColor: Color, lineColor: Color) {
    val left = p(-2f, 44f)
    val right = p(28f, 44f)
    listOf(left, p(8f, 44f), p(18f, 44f), right).forEach { top ->
        drawLine(lineColor, p(13f, 20f), top, 0.2f * unit)
    }
    val width = right.x - left.x
    val height = 18f * unit
    drawArc(canopyColor, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(left.x, left.y - height / 2f), size = Size(width, height))
    // Two stripes
    listOf(0.3f, 0.6f).forEach { fraction ->
        drawArc(
            stripeColor,
            startAngle = 180f + 180f * fraction,
            sweepAngle = 18f,
            useCenter = true,
            topLeft = Offset(left.x, left.y - height / 2f),
            size = Size(width, height)
        )
    }
}

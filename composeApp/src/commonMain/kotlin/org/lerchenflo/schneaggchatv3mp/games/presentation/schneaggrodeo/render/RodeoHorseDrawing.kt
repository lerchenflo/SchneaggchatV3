package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoHorsePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MAX_HORSE_LEVEL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Horse, rider, the cowboy on foot and the parachute, plus friends with their profile picture as
// head. The pose itself is decided in the engine (RodeoHorsePoser); these functions only draw it.

private const val RIDER_MAX_LEAN_DEGREES = 32f
/** Ducking: the rider lies flat on the neck and the whole figure crouches by this share. */
private const val RIDER_DUCK_DEGREES = 45f
private const val DUCK_SQUASH = 0.42f
/** How far a galloping hoof reaches forward and back from under its hip, in horse grid units. */
private const val GALLOP_REACH = 2.6f
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
        // The body rises in the moment of suspension after the front legs pushed off
        val bounce = if (airborne) 0f else cos(gaitPhase - 3.4f) * 0.6f

        if (glow > 0f) {
            drawCircle(
                color = colors.glow.copy(alpha = 0.35f * glow),
                radius = 7f * hindLegScale * unit,
                center = p(7f, 9f - 4.5f * hindLegScale)
            )
        }

        // The horse itself; drawn a second time in gold from the hooves up, as high as its level
        fun drawHorseShape(color: Color) {
            // Legs
            if (airborne) {
                // Front legs tucked, hind legs stretched back - the classic jumping pose
                listOf(17f, 19f).forEach { x ->
                    drawLine(color, p(x, 9f), p(x + 2.5f, 5f), legStroke, StrokeCap.Round)
                    drawLine(color, p(x + 2.5f, 5f), p(x + 0.5f, 3f), legStroke, StrokeCap.Round)
                }
                listOf(6f, 8f).forEach { x ->
                    drawLine(color, p(x, 9f), p(x - 4f * hindLegScale, 9f - 7f * hindLegScale), hindStroke, StrokeCap.Round)
                }
            } else {
                // Four-beat gallop: hind left, hind right, front left, front right, then a short
                // moment with the legs gathered. Each hoof reaches forward lifted and pushes back on
                // the ground; knees fold forward on the front legs, hocks backwards on the hind legs.
                val raise = pose.frontLegRaise
                listOf(17f to 2.0f, 19f to 2.7f).forEachIndexed { index, (x, offset) ->
                    val phase = gaitPhase + offset
                    val lift = max(0f, cos(phase))
                    val reach = sin(phase) * GALLOP_REACH
                    val gallopHoof = p(x + reach - 1.2f * lift, 2.2f * lift)
                    val gallopKnee = p(x + reach / 2f + 1.8f * lift, 4.5f + 1.2f * lift)
                    // Stumbling: the knee buckles forward and the hoof folds under
                    val knee = lerp(gallopKnee, p(x + 2.5f, 5f), frontLegFold)
                    val hoof = lerp(gallopHoof, p(x - 1f, 3.5f), frontLegFold)
                    // Rearing up for the super jump: lifted and pawing the air
                    val paw = sin(gaitPhase * 6f + index * PI.toFloat()) * 1.2f
                    val raisedKnee = p(x + 3f, 7f + paw)
                    val raisedHoof = p(x + 1f, 4.5f + paw * 1.5f)
                    drawLine(color, p(x, 9f), lerp(knee, raisedKnee, raise), legStroke, StrokeCap.Round)
                    drawLine(color, lerp(knee, raisedKnee, raise), lerp(hoof, raisedHoof, raise), legStroke, StrokeCap.Round)
                }
                listOf(6f to 0f, 8f to 0.7f).forEach { (x, offset) ->
                    val phase = gaitPhase + offset
                    val lift = max(0f, cos(phase))
                    val reach = sin(phase) * GALLOP_REACH * hindLegScale
                    val hoofY = 9f - 9f * hindLegScale + 2.2f * lift
                    val hoof = p(x + reach + 0.6f * lift, hoofY)
                    val hock = p(x + reach / 2f - 1.6f - 0.8f * lift, (9f + hoofY) / 2f + 0.8f * lift)
                    drawLine(color, p(x, 9f), hock, hindStroke, StrokeCap.Round)
                    drawLine(color, hock, hoof, hindStroke, StrokeCap.Round)
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


            // Tail
            val tailSwing = if (airborne) 2f else sin(gaitPhase + PI.toFloat()) * 1f
            drawPath(
                path = Path().apply {
                    val start = p(4.5f, 14f + bounce)
                    val control = p(1f, 14f + tailSwing)
                    val end = p(0.5f, 7f + tailSwing)
                    moveTo(start.x, start.y)
                    quadraticTo(control.x, control.y, end.x, end.y)
                },
                color = color,
                style = Stroke(width = 1.4f * unit, cap = StrokeCap.Round)
            )

            // Neck, head and ear
            drawPath(polygon(15f to 15f + bounce, 20f to 12f + bounce, 24f to 21f, 20f to 22.5f), color)
            drawPath(polygon(20f to 22.5f, 24f to 23f, 29f to 19f, 28f to 17f, 23f to 18.5f), color)
            drawPath(polygon(21f to 22.5f, 22f to 25f, 23f to 22.8f), color)
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
        // A friend riding along sits behind him, arms around his waist
        pose.passengerId?.let { friendId ->
            drawLine(riderColor, p(8.5f, 16f + riderY), p(9.8f, 12f + riderY), 1.3f * unit, StrokeCap.Round)
            rotate(degrees = RIDER_MAX_LEAN_DEGREES * pose.riderLean + RIDER_DUCK_DEGREES * pose.duck, pivot = p(8.5f, 16f + riderY)) {
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
        drawLine(riderColor, p(12f, 16f + riderY), p(13.5f, 12f + riderY), 1.3f * unit, StrokeCap.Round)
        rotate(degrees = RIDER_MAX_LEAN_DEGREES * pose.riderLean + RIDER_DUCK_DEGREES * pose.duck, pivot = p(12f, 16f + riderY)) {
            drawRoundRect(
                color = shirtColor,
                topLeft = p(10.5f, 21.5f + riderY),
                size = Size(3f * unit, 6f * unit),
                cornerRadius = CornerRadius(1f * unit)
            )
            drawLine(shirtColor, p(13f, 20.5f + riderY), p(18f, 17.5f + riderY), 1f * unit, StrokeCap.Round)
            val friendId = pose.riderId
            if (friendId != null) {
                // A friend on their own horse: their profile picture is the head
                drawFriendHead(::p, 12.2f, 23.6f + riderY, pictures[friendId], riderColor, colors.headRing, unit)
            } else {
                drawCircle(riderColor, radius = 1.8f * unit, center = p(12.2f, 23.6f + riderY))
                // Hat, popping off the head (and tilting back) when the horse stumbles
                rotate(degrees = -25f * hatLift / 3f, pivot = p(12.2f, 25.6f + riderY + hatLift)) {
                    drawCowboyHat(::p, 9f, 25.6f + riderY + hatLift, unit)
                }
            }
        }
    }
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
 * x = 0 at his center; he faces right unless mirrored, and rotates around his middle.
 */
internal fun DrawScope.drawCowboy(
    cowboy: RodeoCowboyUi,
    groundY: Float,
    unit: Float,
    color: Color,
    shirtColor: Color,
) {
    val centerX = cowboy.x * unit
    val feetY = groundY - cowboy.height * unit
    fun p(x: Float, y: Float) = Offset(centerX + x * unit, feetY - y * unit)
    val middle = p(0f, 9f)

    withTransform({
        rotate(cowboy.rotation, pivot = middle)
        if (cowboy.facingLeft) scale(-1f, 1f, pivot = middle)
    }) {
        // Legs, torso and the arm holding the rope (reaching forward)
        drawLine(color, p(-0.7f, 6f), p(-1.2f, 0f), 1.3f * unit, StrokeCap.Round)
        drawLine(color, p(0.7f, 6f), p(1.2f, 0f), 1.3f * unit, StrokeCap.Round)
        drawRoundRect(
            color = shirtColor,
            topLeft = p(-1.5f, 12f),
            size = Size(3f * unit, 6f * unit),
            cornerRadius = CornerRadius(1f * unit)
        )
        drawLine(shirtColor, p(1f, 11f), p(2f, 10f), 1f * unit, StrokeCap.Round)
        drawCircle(color, radius = 1.8f * unit, center = p(0.2f, 14.1f))
        drawCowboyHat(::p, -3f, 16.1f + cowboy.hatLift, unit)
    }
}

/** The orange cowboy hat: brim with its top-left corner at grid ([brimLeft], [brimTop]), crown above. */
internal fun DrawScope.drawCowboyHat(p: (Float, Float) -> Offset, brimLeft: Float, brimTop: Float, unit: Float) {
    drawRoundRect(
        color = HAT_COLOR,
        topLeft = p(brimLeft, brimTop),
        size = Size(6.4f * unit, 0.7f * unit),
        cornerRadius = CornerRadius(0.35f * unit)
    )
    drawRoundRect(
        color = HAT_COLOR,
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

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCarrotUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMarkerUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMudUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.POLE_THICKNESS
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Things standing on the track: fences, schneaggs, horseshoes, carrots, mud and the highscore markers.

/** The schneagg silhouette sits vertically centered in its square image; its underside is this far down. */
private const val SNAIL_FOOT_FRACTION = 0.785f
/** The background-colored halo that separates the schneagg from poles behind it. */
private const val SNAIL_OUTLINE_SCALE = 1.1f

/**
 * A show-jumping fence: two wooden standards and striped poles in their cups, each pole in its own
 * color; knocked poles lie stacked on the ground. [label] (the height) is drawn below the ground.
 */
internal fun DrawScope.drawFence(
    fence: RodeoFenceUi,
    groundY: Float,
    unit: Float,
    woodColor: Color,
    label: TextLayoutResult,
) {
    val left = fence.x * unit
    val right = (fence.x + fence.width) * unit
    val postWidth = 1.2f * unit
    val postHeight = (fence.top + 2.5f) * unit

    // Wooden standards with little feet
    listOf(left, right - postWidth).forEach { postX ->
        drawRect(woodColor, Offset(postX, groundY - postHeight), Size(postWidth, postHeight))
        drawRect(
            woodColor,
            Offset(postX - unit, groundY - 0.8f * unit),
            Size(postWidth + 2f * unit, 0.8f * unit)
        )
    }

    val poleThickness = POLE_THICKNESS * unit
    val stripeWidth = 2f * unit
    fence.poleHeights.forEachIndexed { index, height ->
        val poleColor = POLE_COLORS[(fence.colorOffset + index) % POLE_COLORS.size]
        val poleBottom = if (fence.knocked) index * POLE_THICKNESS else height - POLE_THICKNESS
        val poleY = groundY - (poleBottom + POLE_THICKNESS) * unit
        var x = left
        var stripe = 0
        while (x < right) {
            val width = min(stripeWidth, right - x)
            drawRect(
                color = if (stripe % 2 == 0) poleColor else poleColor.copy(alpha = 0.45f),
                topLeft = Offset(x, poleY),
                size = Size(width, poleThickness)
            )
            x += stripeWidth
            stripe++
        }
    }

    // Height label below the ground, centered under the fence
    val labelX = (left + right) / 2f - label.size.width / 2f
    if (labelX + label.size.width > 0f && labelX < size.width) {
        drawText(label, topLeft = Offset(labelX, groundY + 2f * unit))
    }
}

/**
 * Draws the schneagg sprite (which faces right) with its underside at [footY], centered on
 * [centerX], with a halo in [outline]. [facingLeft] mirrors it; [tiltDeg] tilts it around its foot.
 */
internal fun DrawScope.drawSnail(
    image: ImageBitmap,
    centerX: Float,
    footY: Float,
    size: Float,
    facingLeft: Boolean,
    tiltDeg: Float,
    outline: ColorFilter,
) {
    val pivot = Offset(size / 2f, size * SNAIL_FOOT_FRACTION)
    val outlineSize = size * SNAIL_OUTLINE_SCALE
    val outlineInset = ((size - outlineSize) / 2f).roundToInt()
    withTransform({
        translate(centerX - size / 2f, footY - size * SNAIL_FOOT_FRACTION)
        rotate(tiltDeg, pivot = pivot)
        if (facingLeft) scale(-1f, 1f, pivot = pivot)
    }) {
        drawImage(
            image = image,
            dstOffset = IntOffset(outlineInset, outlineInset),
            dstSize = IntSize(outlineSize.roundToInt(), outlineSize.roundToInt()),
            colorFilter = outline,
            filterQuality = FilterQuality.Medium,
        )
        drawImage(
            image = image,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.roundToInt(), size.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}

/** A lucky horseshoe, opening up, with a few nail holes. */
internal fun DrawScope.drawHorseshoe(center: Offset, unit: Float, tiltDeg: Float, color: Color, nailColor: Color) {
    val radius = 2.4f * unit
    val stroke = 1.1f * unit
    rotate(tiltDeg, pivot = center) {
        // Open side up: the arc runs from the left prong over the bottom to the right prong
        drawArc(
            color = color,
            startAngle = -20f,
            sweepAngle = 220f,
            useCenter = false,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        listOf(20f, 70f, 110f, 160f).forEach { angle ->
            val radians = angle * PI.toFloat() / 180f
            drawCircle(
                color = nailColor,
                radius = 0.22f * unit,
                center = center + Offset(cos(radians) * radius, sin(radians) * radius)
            )
        }
    }
}

/** A carrot, tip pointing down-left when untilted, with a tuft of leaves on its top. */
internal fun DrawScope.drawCarrot(carrot: RodeoCarrotUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(carrot.x, carrot.height)
    rotate(carrot.tiltDeg, pivot = center) {
        fun p(x: Float, y: Float) = center + Offset(x * unit, -y * unit)
        // Leaves first, the root covers their stems
        listOf(-0.6f to 3.8f, 0f to 4.3f, 0.6f to 3.8f).forEach { (x, y) ->
            drawLine(CARROT_LEAF_COLOR, p(0f, 2.2f), p(x, y), 0.5f * unit, StrokeCap.Round)
        }
        drawPath(polygonPath(::p, -0.9f to 2.4f, 0.9f to 2.4f, 0.15f to -2.8f, -0.15f to -2.8f), CARROT_COLOR)
        drawCircle(CARROT_COLOR, radius = 0.9f * unit, center = p(0f, 2.3f))
        // A few rings across the root
        listOf(1.2f, 0f, -1.2f).forEach { y ->
            val half = 0.75f - (2.4f - y) * 0.12f
            drawLine(MUD_DARK_COLOR.copy(alpha = 0.35f), p(-half, y), p(half * 0.3f, y - 0.2f), 0.18f * unit)
        }
    }
}

/** A flat mud puddle on the ground with darker splotches and a few bubbles, picked by its seed. */
internal fun DrawScope.drawMud(mud: RodeoMudUi, groundY: Float, unit: Float) {
    val left = mud.x * unit
    val width = mud.width * unit
    drawOval(MUD_COLOR, topLeft = Offset(left, groundY - 0.9f * unit), size = Size(width, 2.4f * unit))
    repeat(4) { index ->
        val x = left + width * (0.12f + 0.7f * cloudNoise(mud.seed, index))
        val splotchWidth = width * (0.12f + 0.12f * cloudNoise(mud.seed, index + 10))
        drawOval(
            MUD_DARK_COLOR,
            topLeft = Offset(x, groundY - 0.4f * unit),
            size = Size(splotchWidth, 1f * unit)
        )
    }
    repeat(3) { index ->
        drawCircle(
            MUD_DARK_COLOR,
            radius = (0.25f + 0.2f * cloudNoise(mud.seed, index + 20)) * unit,
            center = Offset(left + width * (0.2f + 0.6f * cloudNoise(mud.seed, index + 30)), groundY - 0.7f * unit)
        )
    }
}

/**
 * A highscore marker: a thin post with a pennant, the player's name and score above it. The post
 * stands where the run of that highscore ended.
 */
internal fun DrawScope.drawMarker(
    marker: RodeoMarkerUi,
    groundY: Float,
    unit: Float,
    postHeight: Float,
    color: Color,
    label: TextLayoutResult,
) {
    val x = marker.x * unit
    val top = groundY - postHeight * unit
    drawLine(
        color = color.copy(alpha = 0.7f),
        start = Offset(x, groundY),
        end = Offset(x, top),
        strokeWidth = 0.5f * unit,
        cap = StrokeCap.Round
    )
    drawPath(
        path = Path().apply {
            moveTo(x, top)
            lineTo(x + 6f * unit, top + 2f * unit)
            lineTo(x, top + 4f * unit)
            close()
        },
        color = color
    )

    val labelX = x - label.size.width / 2f
    if (labelX + label.size.width > 0f && labelX < size.width) {
        drawText(label, topLeft = Offset(labelX, max(0f, top - label.size.height - unit)))
    }
}

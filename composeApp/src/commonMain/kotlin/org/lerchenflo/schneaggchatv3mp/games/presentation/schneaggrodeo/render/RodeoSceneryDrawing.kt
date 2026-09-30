package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

// The backdrop: ground, speed streaks, clouds high up and space. [distance] is how far the ground
// has scrolled; everything here moves with it (some of it slower, for parallax).

private const val SPEED_LINE_COUNT = 9
private const val STAR_COUNT = 70
/** Units between two points of the ground line over the hills. */
private const val GROUND_STEP = 2f

/**
 * The ground line over the hills, the earth under it shaded a little, and pebbles in it. [fill]
 * reaches far down, so the earth still covers the bottom while the picture follows a hill up.
 */
internal fun DrawScope.drawGround(context: RodeoDrawContext, distance: Float, speedBlur: Boolean = false) {
    val unit = context.unit
    val color = context.colors.onSurfaceVariant
    val fill = color.copy(alpha = 0.06f)
    val line = Path()
    val earth = Path()
    var worldX = 0f
    val visibleWidth = size.width / unit
    earth.moveTo(0f, size.height + 200f * unit)
    while (worldX <= visibleWidth + GROUND_STEP) {
        val point = Offset(worldX * unit, context.groundYAt(worldX))
        if (worldX == 0f) line.moveTo(point.x, point.y) else line.lineTo(point.x, point.y)
        earth.lineTo(point.x, point.y)
        worldX += GROUND_STEP
    }
    earth.lineTo(size.width + GROUND_STEP * unit, size.height + 200f * unit)
    earth.close()
    drawPath(earth, fill)
    drawPath(line, color, style = Stroke(width = unit * 0.6f, join = StrokeJoin.Round))

    // Small pebbles scrolling with the ground, so the speed is visible between fences. At tractor
    // speed they smear into long streaks.
    val spacing = 9f * unit
    val shift = (distance * unit) % spacing
    var x = -shift
    var index = (distance * unit / spacing).toInt()
    val pebbleColor = color.copy(alpha = if (speedBlur) 0.3f else 0.5f)
    while (x < size.width) {
        val depth = 1.5f + (index * 7 % 5) * 0.8f
        val length = if (speedBlur) (14f + index * 5 % 3 * 6f) * unit else (1f + index * 5 % 3) * unit
        val pebbleY = context.groundYAt(x / unit) + depth * unit
        drawLine(
            color = pebbleColor,
            start = Offset(x, pebbleY),
            end = Offset(x + length, pebbleY),
            strokeWidth = unit * 0.5f,
            cap = StrokeCap.Round
        )
        x += spacing
        index++
    }
}

/** Wind streaks racing through the sky while a vehicle goes flat out. */
internal fun DrawScope.drawSpeedLines(groundY: Float, unit: Float, distance: Float, color: Color) {
    val lineColor = color.copy(alpha = 0.35f)
    repeat(SPEED_LINE_COUNT) { index ->
        val height = 4f + (index * 37 % 45)
        val length = (18f + index * 11 % 20) * unit
        val period = size.width + length
        // Each line moves at its own pace, so they don't march along in lockstep
        val travelled = distance * unit * (0.6f + (index % 3) * 0.2f) + index * 97f * unit
        val x = size.width - travelled % period
        drawLine(
            color = lineColor,
            start = Offset(x, groundY - height * unit),
            end = Offset(x + length, groundY - height * unit),
            strokeWidth = 0.4f * unit,
            cap = StrokeCap.Round
        )
    }
}

/**
 * A few fair-weather clouds high above the track, drifting by slower than the ground (parallax).
 * They only come into view while the camera follows the horse up.
 */
internal fun DrawScope.drawSkyClouds(groundY: Float, unit: Float, distance: Float, color: Color, outline: Color) {
    val spacing = 70f
    val span = size.width / unit + spacing * 2f
    val offset = (distance * 0.3f) % spacing
    var index = 0
    var x = -offset - spacing
    val firstIndex = ((distance * 0.3f) / spacing).toInt()
    while (x < span) {
        val seed = firstIndex + index
        val height = 72f + 45f * cloudNoise(seed, 1)
        val cloudWidth = 18f + 12f * cloudNoise(seed, 2)
        drawPuffyCloud(
            centerX = (x + spacing * cloudNoise(seed, 3)) * unit,
            baseY = groundY - height * unit,
            width = cloudWidth * unit,
            seed = seed,
            color = color,
            outline = outline,
            unit = unit
        )
        x += spacing
        index++
    }
}

/** A cumulus with a flat base and three to four round puffs on top, outlined once around the whole shape. */
private fun DrawScope.drawPuffyCloud(
    centerX: Float,
    baseY: Float,
    width: Float,
    seed: Int,
    color: Color,
    outline: Color,
    unit: Float,
) {
    val left = centerX - width / 2f
    val baseHeight = width * 0.18f
    val puffs = 3 + (cloudNoise(seed, 4) * 1.99f).toInt()
    val circles = (0 until puffs).map { i ->
        val fraction = (i + 0.5f) / puffs
        // Tallest in the middle
        val radius = width * (0.16f + 0.12f * sin(PI.toFloat() * fraction) + 0.04f * cloudNoise(seed, 10 + i))
        Offset(left + width * fraction, baseY - baseHeight - radius * 0.35f) to radius
    }
    val stroke = 0.35f * unit
    // Outline first, slightly larger, then the fill on top: only the outer edge stays visible
    drawRoundRect(
        outline,
        topLeft = Offset(left - stroke, baseY - baseHeight * 2f - stroke),
        size = Size(width + stroke * 2f, baseHeight * 2f + stroke * 2f),
        cornerRadius = CornerRadius(baseHeight + stroke)
    )
    circles.forEach { (center, radius) -> drawCircle(outline, radius = radius + stroke, center = center) }
    drawRoundRect(
        color,
        topLeft = Offset(left, baseY - baseHeight * 2f),
        size = Size(width, baseHeight * 2f),
        cornerRadius = CornerRadius(baseHeight)
    )
    circles.forEach { (center, radius) -> drawCircle(color, radius = radius, center = center) }
}

/**
 * Space (rocket ride): the whole canvas turns dark navy with twinkling stars, faded in by
 * [alpha]. Drawn in screen space, before the camera pans; the stars drift slowly with [distance].
 */
internal fun DrawScope.drawSpace(distance: Float, unit: Float, alpha: Float) {
    if (alpha <= 0f) return
    drawRect(SPACE_COLOR.copy(alpha = alpha))
    val drift = distance * 0.05f * unit
    repeat(STAR_COUNT) { index ->
        val x = (cloudNoise(index, 1) * size.width - drift).mod(size.width)
        val y = cloudNoise(index, 2) * size.height
        val twinkle = 0.55f + 0.45f * sin(distance * 0.01f + index)
        drawCircle(
            STAR_COLOR.copy(alpha = alpha * twinkle),
            radius = (0.15f + 0.25f * cloudNoise(index, 3)) * unit,
            center = Offset(x, y)
        )
    }
}

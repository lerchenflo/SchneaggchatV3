package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// The rainbow (see engine/RodeoMapSwitch): a sea of clouds below, the rainbow itself as the track
// with its gaps, stars to grab, and the cloud the rainbow ends in back down on the surface.

/** Thickness of one colored band of the track. */
private const val BAND = 1.1f
/** Units between two points of the track's line over a slope. */
private const val TRACK_STEP = 2f
private const val CLOUD_SPACING = 26f
/** The clouds drift by slower than the track. */
private const val CLOUD_PARALLAX = 0.3f
/** The sea of clouds lies this far below the track. */
private const val CLOUD_SEA_DEPTH = 9f
/** The puffs of the cloud the rainbow ends in: where along the way out, and how big. */
private val LANDING_PUFFS = listOf(0.15f to 5f, 0.45f to 7f, 0.8f to 5.5f)

/** A sky tint and a sea of clouds below the track, fixed behind it. */
internal fun DrawScope.drawRainbowBackdrop(distance: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    drawRect(colors.primaryContainer.copy(alpha = 0.3f))
    val cloud = colors.surfaceBright
    val shade = colors.outlineVariant.copy(alpha = 0.5f)
    val shift = distance * CLOUD_PARALLAX
    var index = floor(shift / CLOUD_SPACING).toInt()
    var x = index * CLOUD_SPACING - shift
    val baseY = context.p(0f, -CLOUD_SEA_DEPTH).y
    while (x < size.width / unit + CLOUD_SPACING) {
        val radius = 7f + 4f * cloudNoise(index, 1)
        val center = Offset(x * unit, baseY - 2f * cloudNoise(index, 2) * unit)
        drawCircle(shade, radius = radius * unit, center = center + Offset(0f, 0.8f * unit))
        drawCircle(cloud, radius = radius * unit, center = center)
        // A small cloud high up now and then
        if (cloudNoise(index, 3) > 0.6f) {
            val high = Offset((x + 9f) * unit, context.p(0f, 30f + 12f * cloudNoise(index, 4)).y)
            drawCircle(cloud.copy(alpha = 0.7f), radius = 2.4f * unit, center = high)
            drawCircle(cloud.copy(alpha = 0.7f), radius = 3.2f * unit, center = high + Offset(2.6f * unit, -0.6f * unit))
        }
        x += CLOUD_SPACING
        index++
    }
}

/** The rainbow as the track: its colored bands follow the ground line, broken by the [gaps]. */
internal fun DrawScope.drawRainbowTrack(context: RodeoDrawContext, gaps: List<RodeoGapUi>) {
    val unit = context.unit
    val visibleWidth = size.width / unit
    // Mostly flat: plain rects; down the slide at its end: one line, stroked once per band
    val flat = context.groundYAt(0f) == context.groundYAt(visibleWidth / 2f) && context.groundYAt(0f) == context.groundYAt(visibleWidth)
    val line = if (flat) null else Path().apply {
        var x = 0f
        while (x <= visibleWidth + TRACK_STEP) {
            val point = context.p(x, -BAND / 2f)
            if (x == 0f) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            x += TRACK_STEP
        }
    }
    val drawBands: DrawScope.() -> Unit = {
        RAINBOW_BANDS.forEachIndexed { index, color ->
            if (line == null) {
                drawRect(color, Offset(0f, context.groundYAt(0f) + index * BAND * unit), Size(size.width, BAND * unit + 0.5f))
            } else {
                translate(top = index * BAND * unit) { drawPath(line, color, style = Stroke(width = BAND * unit + 0.5f)) }
            }
        }
    }
    // The solid stretches between the gaps (they come in order along the track)
    var from = 0f
    for (gap in gaps) {
        if (gap.x > from) clipRect(left = from * unit, right = gap.x * unit, block = drawBands)
        from = maxOf(from, gap.x + gap.width)
    }
    if (from < visibleWidth) clipRect(left = from * unit, block = drawBands)
}

/** The cloud the rainbow ends in at the bottom of its slide, [x] the way out's left edge. */
internal fun DrawScope.drawRainbowLanding(x: Float, width: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val cloud = context.colors.surfaceBright
    val shade = context.colors.outlineVariant.copy(alpha = 0.5f)
    LANDING_PUFFS.forEach { (share, radius) ->
        val center = context.p(x + width * share, radius * 0.4f)
        drawCircle(shade, radius = radius * unit, center = center + Offset(0f, 0.8f * unit))
        drawCircle(cloud, radius = radius * unit, center = center)
    }
}

/** A star to grab over a gap, turning slowly. */
internal fun DrawScope.drawStar(gem: RodeoGemUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(gem.x, gem.height)
    val color = RAINBOW_BANDS[2]
    drawCircle(color.copy(alpha = 0.2f), radius = 3.6f * unit, center = center)
    rotate(gem.tiltDeg, pivot = center) {
        val star = Path()
        for (point in 0 until 10) {
            val radius = (if (point % 2 == 0) 2.4f else 1f) * unit
            val angle = PI.toFloat() * point / 5f - PI.toFloat() / 2f
            val corner = center + Offset(cos(angle) * radius, sin(angle) * radius)
            if (point == 0) star.moveTo(corner.x, corner.y) else star.lineTo(corner.x, corner.y)
        }
        star.close()
        drawPath(star, color)
    }
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMudUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSnailUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEA_WATER_LINE
import kotlin.math.floor
import kotlin.math.sin

// The underground sea (see engine/RodeoUnderground): the cave's rock above, deep water below with
// seaweed swaying, buoys instead of fences, sharks instead of runners, swim rings under the
// crawlers, oil slicks instead of mud, pearls instead of crystals - and the water over everything
// below its surface, so the horse swims.

private const val SEAWEED_SPACING = 23f
private const val SEA_PARALLAX = 0.5f

/** The cave's rock and torches above, the water from its surface down, with swaying seaweed. */
internal fun DrawScope.drawSeaBackdrop(distance: Float, context: RodeoDrawContext) {
    drawCaveBackdrop(distance, context)
    val unit = context.unit
    val surfaceY = context.p(0f, SEA_WATER_LINE).y
    drawRect(
        Brush.verticalGradient(listOf(SEA_WATER_LIGHT.copy(alpha = 0.8f), SEA_WATER), startY = surfaceY, endY = size.height),
        topLeft = Offset(0f, surfaceY),
        size = Size(size.width, size.height - surfaceY),
    )
    // Seaweed in the distance, swaying
    val shift = distance * SEA_PARALLAX
    var index = floor(shift / SEAWEED_SPACING).toInt()
    var x = index * SEAWEED_SPACING - shift
    val visibleWidth = size.width / unit
    while (x < visibleWidth + SEAWEED_SPACING) {
        val height = 4f + 6f * cloudNoise(index, 51)
        val sway = 1.2f * sin(distance * 0.05f + index)
        val bottom = Offset(x * unit, size.height)
        val top = context.p(x + sway, -8f + height)
        drawLine(SEAWEED.copy(alpha = 0.6f), bottom, top, 0.8f * unit, StrokeCap.Round)
        x += SEAWEED_SPACING
        index++
    }
}

/**
 * The water over everything below its surface: a see-through layer with a wavy surface line, drawn
 * over the horse so it swims.
 */
internal fun DrawScope.drawWaterOverlay(distance: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val surfaceY = context.p(0f, SEA_WATER_LINE).y
    val waves = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= size.width + unit) {
            lineTo(x, surfaceY + 0.4f * unit * sin(x / unit * 0.5f + distance * 0.2f))
            x += unit
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(waves, SEA_WATER.copy(alpha = 0.45f))
    var x = 0f
    while (x <= size.width) {
        val y = surfaceY + 0.4f * unit * sin(x / unit * 0.5f + distance * 0.2f)
        drawLine(SEA_WATER_LIGHT, Offset(x, y), Offset(x + unit, surfaceY + 0.4f * unit * sin((x + unit) / unit * 0.5f + distance * 0.2f)), 0.25f * unit)
        x += unit
    }
}

/**
 * A buoy instead of a fence: a red and white striped pole up to the fence's height on a float at
 * the water's surface (two with a rope between them for a wide jump); a knocked one floats on its
 * side. [label] (the height) is drawn below.
 */
internal fun DrawScope.drawBuoy(fence: RodeoFenceUi, context: RodeoDrawContext, label: TextLayoutResult) {
    val unit = context.unit
    val wide = fence.width > 12f
    val poles = if (wide) listOf(fence.x + 1.5f, fence.x + fence.width - 1.5f) else listOf(fence.x + fence.width / 2f)
    poles.forEach { poleX ->
        if (fence.knocked) {
            rotate(80f, pivot = context.p(poleX, SEA_WATER_LINE)) { drawBuoyPole(poleX, fence.top * 0.6f, context) }
        } else {
            drawBuoyPole(poleX, fence.top, context)
        }
    }
    if (wide && !fence.knocked) {
        drawLine(context.colors.onSurface, context.p(poles[0], fence.top - 1f), context.p(poles[1], fence.top - 1f), 0.2f * unit)
        repeat(3) { index ->
            val floatX = poles[0] + (poles[1] - poles[0]) * (index + 1) / 4f
            drawCircle(BUOY_RED, radius = 0.5f * unit, center = context.p(floatX, fence.top - 1f))
        }
    }
    val labelX = (fence.x + fence.width / 2f) * unit - label.size.width / 2f
    if (labelX + label.size.width > 0f && labelX < size.width) {
        drawText(label, topLeft = Offset(labelX, context.groundY + 2f * unit))
    }
}

/** One striped buoy pole from the float at the surface up to [top], with a light on it. */
private fun DrawScope.drawBuoyPole(poleX: Float, top: Float, context: RodeoDrawContext) {
    val unit = context.unit
    var y = SEA_WATER_LINE
    var red = true
    while (y < top) {
        val next = minOf(top, y + 1.6f)
        drawLine(if (red) BUOY_RED else BUOY_WHITE, context.p(poleX, y), context.p(poleX, next), 1f * unit)
        red = !red
        y = next
    }
    drawCircle(SEA_WATER_LIGHT, radius = 0.5f * unit, center = context.p(poleX, top + 0.4f))
    drawOval(BUOY_RED, context.p(poleX - 2f, SEA_WATER_LINE + 1f), Size(4f * unit, 2f * unit))
}

/** A shark: gray body with a white belly, dorsal fin, tail, an eye and teeth; [snail] gives its place. */
internal fun DrawScope.drawShark(snail: RodeoSnailUi, context: RodeoDrawContext) {
    val unit = context.unit
    val direction = if (snail.facingLeft) -1f else 1f
    val center = context.p(snail.x, snail.height + SEA_WATER_LINE - 0.5f)
    fun p(dx: Float, dy: Float) = center + Offset(dx * direction * unit, -dy * unit)
    rotate(snail.tiltDeg * 0.5f, pivot = center) {
        drawPath(polygonPath(::p, -4.5f to 0.3f, -6.5f to 2.2f, -6f to 0.3f, -6.5f to -1.6f), SHARK_GRAY)
        drawOval(SHARK_GRAY, center - Offset(4.8f * unit, 1.4f * unit), Size(9.6f * unit, 2.8f * unit))
        drawPath(polygonPath(::p, -3.5f to -0.4f, 4f to -0.4f, 4.8f to 0f, -3f to -1.2f), SHARK_BELLY)
        drawPath(polygonPath(::p, -1f to 1.2f, 0.6f to 4.2f, 1.8f to 1.2f), SHARK_GRAY)
        drawCircle(OIL_BLACK, radius = 0.25f * unit, center = p(3.4f, 0.4f))
        repeat(3) { index -> drawPath(polygonPath(::p, 3.4f + index * 0.4f to -0.5f, 3.6f + index * 0.4f to -0.9f, 3.8f + index * 0.4f to -0.5f), SHARK_BELLY) }
    }
}

/** The pink swim ring a crawler floats on in the sea, drawn under the snail. */
internal fun DrawScope.drawSwimRing(snail: RodeoSnailUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(snail.x, snail.height + 0.3f)
    drawOval(SWIM_RING, center - Offset(2.6f * unit, 0.8f * unit), Size(5.2f * unit, 1.6f * unit), style = Stroke(width = 0.7f * unit))
}

/** An oil slick on the water instead of a mud puddle: black, with a rainbow sheen. */
internal fun DrawScope.drawOilSlick(slick: RodeoMudUi, context: RodeoDrawContext) {
    val unit = context.unit
    val topLeft = context.p(slick.x, SEA_WATER_LINE + 0.5f)
    drawOval(OIL_BLACK.copy(alpha = 0.85f), topLeft, Size(slick.width * unit, 1.2f * unit))
    OIL_SHEEN.forEachIndexed { index, color ->
        val inset = 1f + index * 1.5f + 2f * cloudNoise(slick.seed, index)
        drawOval(
            color.copy(alpha = 0.5f),
            topLeft + Offset(inset * unit, 0.3f * unit),
            Size((slick.width - 2f * inset).coerceAtLeast(1f) * unit, 0.4f * unit),
        )
    }
}

/** A pearl floating in the water instead of a crystal: white with a shine, in a ring of bubbles. */
internal fun DrawScope.drawPearl(gem: RodeoGemUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(gem.x, gem.height)
    drawCircle(SEA_WATER_LIGHT.copy(alpha = 0.3f), radius = 2.6f * unit, center = center, style = Stroke(width = 0.2f * unit))
    drawOval(CLAM_SHELL, center + Offset(-1.8f * unit, 0.6f * unit), Size(3.6f * unit, 1.2f * unit))
    drawCircle(PEARL_WHITE, radius = 1.2f * unit, center = center)
    drawCircle(SNOW_COLOR, radius = 0.4f * unit, center = center + Offset(-0.4f * unit, -0.4f * unit))
}

/** The way down into the sea: a round stone well with water swirling in it. */
internal fun DrawScope.drawSeaEntrance(x: Float, width: Float, context: RodeoDrawContext, distance: Float) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(x + dx, y)
    drawOval(context.colors.outline, p(-1f, 1.8f), Size((width + 2f) * unit, 4.4f * unit))
    drawOval(SEA_WATER, p(1f, 1.2f), Size((width - 2f) * unit, 3f * unit))
    repeat(3) { index ->
        val phase = distance * 0.1f + index * 2f
        drawArc(
            SEA_WATER_LIGHT,
            startAngle = (phase * 57f) % 360f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = p(width / 2f - 2f - index * 2f, 0.6f + index * 0.4f),
            size = Size((4f + index * 4f) * unit, (1f + index * 0.8f) * unit),
            style = Stroke(width = 0.25f * unit)
        )
    }
    // Stone rim
    var stone = 0f
    while (stone < width) {
        drawRoundRect(context.colors.outline, p(stone, 2.6f), Size(2.4f * unit, 1.4f * unit), CornerRadius(0.4f * unit))
        stone += 2.8f
    }
}

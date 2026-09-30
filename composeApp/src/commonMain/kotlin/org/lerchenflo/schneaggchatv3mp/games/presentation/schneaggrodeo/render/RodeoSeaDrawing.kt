package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMudUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSnailUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEA_WATER_LINE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEABED_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoDeepKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoDeepThingUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.TERRAIN_STEP
import kotlin.math.floor
import kotlin.math.sin

// The open sea (see engine/RodeoMapSwitch): the sky above, deep water below with
// seaweed swaying, buoys instead of fences, sharks instead of runners, swim rings under the
// crawlers, oil slicks instead of mud, pearls instead of crystals - and the water over everything
// below its surface, so the horse swims.

private const val SEAWEED_SPACING = 23f
private const val SEA_PARALLAX = 0.5f
/** Units the water and the seabed reach below the picture, so they still cover it while the view looks down. */
private const val BELOW_PICTURE = 60f
/** The sand of the seabed, seen through the deep water. */
private val SEABED = lerp(SAND, SEA_DEEP, 0.6f)

/** Height of the sea's wavy surface at world x [x]. */
private fun seaSurfaceAt(x: Float, distance: Float) = SEA_WATER_LINE + WAVE_HEIGHT * sin(x * 0.5f + distance * 0.2f)

/** How far the waves rise above and sink below the water line. */
private const val WAVE_HEIGHT = 0.4f

/**
 * The open sea, drawn in the world so it moves with the view: water from its surface down to the
 * sandy seabed, getting darker with depth, and seaweed swaying on the seabed.
 */
internal fun DrawScope.drawSeaBackdrop(distance: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val surfaceY = context.p(0f, SEA_WATER_LINE).y
    val bedY = context.p(0f, SEABED_Y).y
    // Opaque, so nothing behind it has to be blended in
    drawRect(
        Brush.verticalGradient(listOf(SEA_WATER_LIGHT, SEA_WATER, SEA_DEEP), startY = surfaceY, endY = bedY),
        topLeft = Offset(0f, surfaceY),
        size = Size(size.width, bedY - surfaceY),
    )
    drawRect(SEABED, topLeft = Offset(0f, bedY), size = Size(size.width, size.height + BELOW_PICTURE * unit))
    // Seaweed on the seabed, swaying, and stones in the sand
    val shift = distance * SEA_PARALLAX
    var index = floor(shift / SEAWEED_SPACING).toInt()
    var x = index * SEAWEED_SPACING - shift
    val visibleWidth = size.width / unit
    while (x < visibleWidth + SEAWEED_SPACING) {
        val height = 5f + 9f * cloudNoise(index, 51)
        val sway = 1.5f * sin(distance * 0.05f + index)
        drawLine(SEAWEED.copy(alpha = 0.7f), context.p(x, SEABED_Y), context.p(x + sway, SEABED_Y + height), 0.8f * unit, StrokeCap.Round)
        drawLine(SEAWEED.copy(alpha = 0.5f), context.p(x + 1f, SEABED_Y), context.p(x + 1f - sway, SEABED_Y + height * 0.6f), 0.6f * unit, StrokeCap.Round)
        drawOval(SHARK_GRAY.copy(alpha = 0.5f), context.p(x + 8f, SEABED_Y + 0.8f), Size((1.5f + 2f * cloudNoise(index, 52)) * unit, 1.2f * unit))
        x += SEAWEED_SPACING
        index++
    }
}

/** Something living (or sunk) in the deep. */
internal fun DrawScope.drawDeepThing(thing: RodeoDeepThingUi, context: RodeoDrawContext) {
    when (thing.kind) {
        RodeoDeepKind.FISH -> drawFishSchool(thing, context)
        RodeoDeepKind.JELLYFISH -> drawJellyfish(thing, context)
        RodeoDeepKind.SHARK -> drawDeepShark(thing, context)
        RodeoDeepKind.WHALE -> drawWhale(thing, context)
        RodeoDeepKind.WRECK -> drawWreck(thing, context)
    }
}

/** A school of small fish swimming left, wiggling. */
private fun DrawScope.drawFishSchool(school: RodeoDeepThingUi, context: RodeoDrawContext) {
    val unit = context.unit
    val color = FISH_COLORS[school.seed % FISH_COLORS.size]
    repeat(6) { index ->
        val dx = 5f * cloudNoise(school.seed, index) + index * 2.2f
        val dy = 5f * cloudNoise(school.seed, index + 10) + 0.6f * sin(school.time * 4f + index)
        val center = context.p(school.x + dx, school.y + dy)
        fun q(x: Float, y: Float) = center + Offset(x * unit, -y * unit)
        drawOval(color, center - Offset(1f * unit, 0.45f * unit), Size(2f * unit, 0.9f * unit))
        drawPath(polygonPath(::q, 0.9f to 0f, 1.8f to 0.6f, 1.8f to -0.6f), color)
        drawCircle(OIL_BLACK, radius = 0.12f * unit, center = q(-0.5f, 0.1f))
    }
}

/** A see-through jellyfish pulsing slowly, its tentacles trailing below. */
private fun DrawScope.drawJellyfish(jelly: RodeoDeepThingUi, context: RodeoDrawContext) {
    val unit = context.unit
    val pulse = 0.85f + 0.15f * sin(jelly.time * 2.5f + jelly.seed)
    val top = context.p(jelly.x, jelly.y + 2f)
    val width = 4.4f * pulse
    drawArc(JELLY_PINK.copy(alpha = 0.6f), 180f, 180f, useCenter = true, topLeft = top - Offset(width / 2f * unit, 0f), size = Size(width * unit, 4f * unit))
    repeat(4) { index ->
        val tx = jelly.x - 1.5f + index
        val sway = 0.5f * sin(jelly.time * 3f + index)
        drawLine(JELLY_PINK.copy(alpha = 0.45f), context.p(tx, jelly.y), context.p(tx + sway, jelly.y - 4f), 0.2f * unit, StrokeCap.Round)
    }
}

/** A shark cruising in the deep, drawn like the ones at the surface. */
private fun DrawScope.drawDeepShark(shark: RodeoDeepThingUi, context: RodeoDrawContext) {
    drawSharkBody(context.p(shark.x, shark.y), context.unit, direction = -1f, tailWag = 0.6f * sin(shark.time * 5f))
}

/**
 * A shark's gray body with a white belly, dorsal fin, tail and an eye around [center], facing
 * [direction] (1 right, -1 left); [tailWag] swings the tail, [teeth] bares them.
 */
private fun DrawScope.drawSharkBody(center: Offset, unit: Float, direction: Float, tailWag: Float = 0f, teeth: Boolean = false) {
    fun p(dx: Float, dy: Float) = center + Offset(dx * direction * unit, -dy * unit)
    drawPath(polygonPath(::p, -4.5f to 0.3f, -6.5f to 2.2f + tailWag, -6f to 0.3f, -6.5f to -1.6f + tailWag), SHARK_GRAY)
    drawOval(SHARK_GRAY, center - Offset(4.8f * unit, 1.4f * unit), Size(9.6f * unit, 2.8f * unit))
    drawPath(polygonPath(::p, -3.5f to -0.4f, 4f to -0.4f, 4.8f to 0f, -3f to -1.2f), SHARK_BELLY)
    drawPath(polygonPath(::p, -1f to 1.2f, 0.6f to 4.2f, 1.8f to 1.2f), SHARK_GRAY)
    drawCircle(OIL_BLACK, radius = 0.25f * unit, center = p(3.4f, 0.4f))
    if (teeth) {
        repeat(3) { index -> drawPath(polygonPath(::p, 3.4f + index * 0.4f to -0.5f, 3.6f + index * 0.4f to -0.9f, 3.8f + index * 0.4f to -0.5f), SHARK_BELLY) }
    }
}

/** A big blue whale gliding by, a spout of bubbles above it. */
private fun DrawScope.drawWhale(whale: RodeoDeepThingUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(dx: Float, dy: Float) = context.p(whale.x + dx, whale.y + dy)
    val tail = 1.2f * sin(whale.time * 1.5f)
    drawPath(polygonPath(::p, 28f to 2f, 36f to 6f + tail, 34f to 2f, 36f to -2f + tail), WHALE_BLUE)
    drawOval(WHALE_BLUE, p(0f, 6f), Size(30f * unit, 10f * unit))
    drawOval(WHALE_BELLY, p(2f, 0f), Size(22f * unit, 3.4f * unit))
    drawCircle(OIL_BLACK, radius = 0.5f * unit, center = p(6f, 1.8f))
    drawLine(WHALE_BELLY.copy(alpha = 0.6f), p(1f, 0.2f), p(7f, 0.8f), 0.3f * unit, StrokeCap.Round)
    repeat(3) { index ->
        val cycle = (whale.time * 0.6f + index / 3f) % 1f
        drawCircle(SEA_WATER_LIGHT.copy(alpha = 1f - cycle), radius = (0.4f + 0.3f * cycle) * unit, center = p(8f + index * 0.8f, 6f + 8f * cycle))
    }
}

/** An old wreck lying tilted on the seabed, its mast broken, weed growing on it. */
private fun DrawScope.drawWreck(wreck: RodeoDeepThingUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(dx: Float, dy: Float) = context.p(wreck.x + dx, wreck.y + dy)
    drawPath(polygonPath(::p, 0f to 7f, 30f to 5f, 26f to 0f, 3f to 0f), WRECK_WOOD)
    listOf(2.5f, 5f).forEach { y -> drawLine(WRECK_WOOD_DARK, p(1.5f, y), p(28f, y - 0.8f), 0.3f * unit) }
    drawCircle(OIL_BLACK.copy(alpha = 0.7f), radius = 1.3f * unit, center = p(18f, 3.2f))
    drawLine(WRECK_WOOD_DARK, p(12f, 6.4f), p(16f, 18f), 0.8f * unit, StrokeCap.Round)
    drawLine(WRECK_WOOD_DARK, p(16f, 18f), p(20f, 14f), 0.6f * unit, StrokeCap.Round)
    drawLine(SEAWEED.copy(alpha = 0.7f), p(5f, 6.6f), p(4f, 10f), 0.4f * unit, StrokeCap.Round)
}

/**
 * The water over everything below its surface: a see-through layer with a wavy surface line, drawn
 * over the horse so it swims.
 */
internal fun DrawScope.drawWaterOverlay(distance: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val water = SEA_WATER.copy(alpha = 0.45f)
    // Only the wavy band along the surface needs a path; below its troughs a plain rect does
    val troughY = context.groundY - (SEA_WATER_LINE - WAVE_HEIGHT) * unit
    val surface = Path()
    val band = Path()
    var x = 0f
    while (x <= size.width / unit + 1f) {
        val point = Offset(x * unit, context.groundY - seaSurfaceAt(x, distance) * unit)
        if (x == 0f) {
            surface.moveTo(point.x, point.y)
            band.moveTo(point.x, troughY)
        }
        surface.lineTo(point.x, point.y)
        band.lineTo(point.x, point.y)
        x += 1f
    }
    band.lineTo(x * unit, troughY)
    band.close()
    drawPath(band, water)
    drawRect(water, Offset(0f, troughY), Size(size.width, size.height + BELOW_PICTURE * unit - troughY))
    drawPath(surface, SEA_WATER_LIGHT, style = Stroke(width = 0.25f * unit))
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
    drawHeightLabel(label, fence.x + fence.width / 2f, context)
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
    val center = context.p(snail.x, snail.height + SEA_WATER_LINE - 0.5f)
    rotate(snail.tiltDeg * 0.5f, pivot = center) {
        drawSharkBody(center, context.unit, direction = if (snail.facingLeft) -1f else 1f, teeth = true)
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
    drawPearlBall(center, unit)
}

/** A white pearl of [radius] with a shine. */
internal fun DrawScope.drawPearlBall(center: Offset, unit: Float, radius: Float = 1.2f) {
    drawCircle(PEARL_WHITE, radius = radius * unit, center = center)
    drawCircle(SNOW_COLOR, radius = radius * 0.35f * unit, center = center + Offset(-0.4f * unit, -0.4f * unit))
}

/** A small wooden treasure chest with a golden band and lock around [center]. */
internal fun DrawScope.drawTreasureChest(center: Offset, unit: Float) {
    drawRect(TREASURE_WOOD, center + Offset(-1.8f * unit, -0.5f * unit), Size(3.6f * unit, 2f * unit))
    drawRoundRect(TREASURE_WOOD, center + Offset(-1.9f * unit, -1.6f * unit), Size(3.8f * unit, 1.3f * unit), CornerRadius(0.6f * unit))
    drawRect(TREASURE_GOLD, center + Offset(-1.9f * unit, -0.5f * unit), Size(3.8f * unit, 0.35f * unit))
    drawRect(TREASURE_GOLD, center + Offset(-0.3f * unit, -0.4f * unit), Size(0.6f * unit, 0.8f * unit))
}

/** Units of water drawn past the beach, up to beyond the picture. */
private const val SEA_REACH = 400f

/**
 * The beach into the sea: sand down the slope that ends at [waterX] with a parasol and a towel,
 * and the sea beginning there, its waves rolling in.
 */
internal fun DrawScope.drawBeach(waterX: Float, context: RodeoDrawContext, distance: Float) {
    val unit = context.unit
    val sandStart = waterX - 2f * TERRAIN_STEP
    // Sand along the slope
    drawGroundStrip(context, from = sandStart, to = waterX + 6f, y = -0.6f, SAND, width = 2f)
    // Parasol and towel on the sand
    val parasolX = sandStart + 40f
    drawLine(context.colors.onSurface, context.p(parasolX, 0f), context.p(parasolX, 14f), 0.4f * unit)
    drawArc(BUOY_RED, 180f, 180f, useCenter = true, topLeft = context.p(parasolX - 7f, 16.5f), size = Size(14f * unit, 5f * unit))
    drawArc(BUOY_WHITE, 225f, 45f, useCenter = true, topLeft = context.p(parasolX - 7f, 16.5f), size = Size(14f * unit, 5f * unit))
    drawLine(SEA_WATER_LIGHT, context.p(parasolX + 4f, 0.3f), context.p(parasolX + 12f, 0.3f), 0.8f * unit)
    // The sea from the waterline on
    val sea = Path()
    val bottom = -30f
    sea.moveTo(context.p(waterX, bottom).x, context.p(waterX, bottom).y)
    var x = waterX
    while (x <= waterX + SEA_REACH) {
        val point = context.p(x, seaSurfaceAt(x, distance))
        sea.lineTo(point.x, point.y)
        x += 2f
    }
    val end = context.p(waterX + SEA_REACH, bottom)
    sea.lineTo(end.x, end.y)
    sea.close()
    drawPath(sea, SEA_WATER.copy(alpha = 0.85f))
    // Surf rolling onto the sand
    val surf = (distance * 0.03f) % 1f
    drawLine(BUOY_WHITE.copy(alpha = 1f - surf), context.p(waterX - 6f * surf, SEA_WATER_LINE * (1f - surf)), context.p(waterX + 4f, SEA_WATER_LINE), 0.5f * unit)
}

/** The harbour pier out of the sea: planks up the ramp that ends at [top], posts down into the water, bollards and a lamp. */
internal fun DrawScope.drawHarbourPier(top: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val start = top - 2f * TERRAIN_STEP
    var x = start
    while (x <= top + 60f) {
        // Posts down into the water
        drawLine(PIER_WOOD_DARK, context.p(x, 0f), context.p(x, -12f), 1f * unit)
        x += 8f
    }
    drawGroundStrip(context, from = start, to = top + 60f, y = 0.5f, PIER_WOOD, width = 1.4f, dash = 1.8f)
    listOf(top + 10f, top + 40f).forEach { bollardX ->
        drawRoundRect(PIER_WOOD_DARK, context.p(bollardX, 3f), Size(1.8f * unit, 2.5f * unit), CornerRadius(0.6f * unit))
    }
    // A lamp post at the end of the ramp
    drawLine(context.colors.onSurface, context.p(top + 2f, 0f), context.p(top + 2f, 22f), 0.5f * unit)
    drawCircle(GOLD_SHINE, radius = 1f * unit, center = context.p(top + 2f, 22.5f))
    drawCircle(GOLD_SHINE.copy(alpha = 0.2f), radius = 4f * unit, center = context.p(top + 2f, 22.5f))
}

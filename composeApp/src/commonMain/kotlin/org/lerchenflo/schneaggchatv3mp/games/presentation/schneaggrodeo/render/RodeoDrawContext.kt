package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCowboyPart
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// Shared drawing helpers. Every drawing works on a grid in world units with y up from the ground
// (or from the thing's own base); a local p(x, y) turns grid points into canvas pixels.

/** Colors and scale every drawing on the track needs. */
class RodeoDrawContext(
    /** Canvas y of the ground line. */
    val groundY: Float,
    /** Pixels per world unit. */
    val unit: Float,
    val colors: ColorScheme,
    /** Pictures and texts some vehicles draw. */
    val assets: RodeoVehicleAssets? = null,
    /** Height of the hills at world x (see engine/RodeoTerrain); everything stands on them. */
    val ground: (Float) -> Float = { 0f },
    /** Hits the cowboy took from the ravens, for vehicles that draw him themselves (see RodeoCowboyPart). */
    val cowboyWounds: Int = 0,
) {
    /** The ravens pecked [part] off the cowboy: vehicles drawing him leave it out. */
    fun cowboyLost(part: RodeoCowboyPart) = part.isLostAt(cowboyWounds)

    /** Canvas position of world point ([x], [y]), [y] above the ground at [x]. */
    fun p(x: Float, y: Float) = Offset(x * unit, groundY - (y + ground(x)) * unit)

    /** Canvas y of the ground at world x [x]. */
    fun groundYAt(x: Float) = groundY - ground(x) * unit

    /** The same without the hills, for backdrops fixed to the screen. */
    fun flat() = RodeoDrawContext(groundY = groundY, unit = unit, colors = colors, assets = assets, cowboyWounds = cowboyWounds)

    /** The same with the ground level at [height] everywhere. */
    fun leveled(height: Float) = RodeoDrawContext(groundY = groundY, unit = unit, colors = colors, assets = assets, ground = { height }, cowboyWounds = cowboyWounds)
}

/**
 * Where something rigid touches the hills, from world x [from] to [to] (a vehicle's wheels, an
 * animal's hooves). Drawn on it, it rests on the ground at both ends and [tilts] with the slope in
 * between as one piece, instead of every part following the ground under it on its own.
 */
@Immutable
data class RodeoFootprint(val from: Float, val to: Float, val tilts: Boolean = true)

/**
 * Draws [block] rigidly on [footprint]: on level ground at the height between its two ends, turned
 * to the slope from one end to the other (see [RodeoFootprint]).
 */
internal inline fun DrawScope.onFootprint(context: RodeoDrawContext, footprint: RodeoFootprint, block: DrawScope.(RodeoDrawContext) -> Unit) {
    val rear = context.ground(footprint.from)
    val front = context.ground(footprint.to)
    val level = context.leveled((rear + front) / 2f)
    if (!footprint.tilts || rear == front) {
        block(level)
        return
    }
    val degrees = -atan2(front - rear, footprint.to - footprint.from) * 180f / PI.toFloat()
    rotate(degrees, pivot = level.p((footprint.from + footprint.to) / 2f, 0f)) { block(level) }
}

/** Pictures and texts vehicles draw, loaded by the track canvas. */
class RodeoVehicleAssets(
    /** Stanislaus standing, facing right (the candy bus picks him up). */
    val stanislaus: ImageBitmap,
    /** The snail, facing right (the shopping cart collects them). */
    val snail: ImageBitmap,
    /** Written on the candy bus. */
    val candySign: String,
    /** The money left in the Escort's trunk, e.g. "12000 €". */
    val moneyText: (Int) -> String,
    val textMeasurer: TextMeasurer,
)

/**
 * Which part of a vehicle is drawn: [BACK] behind everything on the track (skylines, planets),
 * [BODY] under the horse (what it stands on), [FRONT] in front of the horse (smoke, debris, decks).
 */
enum class RodeoLayer { BACK, BODY, FRONT }

internal fun DrawScope.drawVehicle(vehicle: RodeoVehicleUi, layer: RodeoLayer, context: RodeoDrawContext) {
    with(vehicle) { draw(layer, context) }
}

/** Draws [vehicles] whole, layer by layer, with nothing in between. */
internal fun DrawScope.drawVehicles(vehicles: List<RodeoVehicleUi>, context: RodeoDrawContext) {
    RodeoLayer.entries.forEach { layer -> vehicles.forEach { drawVehicle(it, layer, context) } }
}

/**
 * A strip following the ground from world x [from] to [to] at [y] above it, drawn as short pieces
 * [dash] long every 2 units, so it bends with the hills: sand, planks, snow.
 */
internal fun DrawScope.drawGroundStrip(context: RodeoDrawContext, from: Float, to: Float, y: Float, color: Color, width: Float, dash: Float = 2f) {
    var x = from
    while (x < to) {
        drawLine(color, context.p(x, y), context.p(x + dash, y), width * context.unit)
        x += 2f
    }
}

/** A fence's height [label] centered under world x [middle], below the ground there (if on screen). */
internal fun DrawScope.drawHeightLabel(label: TextLayoutResult, middle: Float, context: RodeoDrawContext) {
    val labelX = middle * context.unit - label.size.width / 2f
    if (labelX + label.size.width > 0f && labelX < size.width) {
        drawText(label, topLeft = Offset(labelX, context.groundYAt(middle) + 2f * context.unit))
    }
}

/** A closed polygon through grid [points], converted to pixels by [p]. */
internal fun polygonPath(p: (Float, Float) -> Offset, vararg points: Pair<Float, Float>) = Path().apply {
    points.forEachIndexed { index, (x, y) ->
        val point = p(x, y)
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}

/** Cheap deterministic noise in 0..1 for cloud shapes and star positions. */
internal fun cloudNoise(seed: Int, index: Int): Float {
    val value = sin((seed * 12.9898f + index * 78.233f)) * 43758.547f
    return value - floor(value)
}

/** A treaded tyre with a rim, a hub cap and three spokes turned by [phase] (radians). */
internal fun DrawScope.drawWheel(center: Offset, radius: Float, phase: Float, tyreColor: Color, hubColor: Color, capColor: Color) {
    drawCircle(tyreColor, radius = radius, center = center)
    // Tread blocks around the tyre, turning with it
    repeat(10) { index ->
        val angle = phase + index * 2f * PI.toFloat() / 10f
        val direction = Offset(cos(angle), sin(angle))
        drawLine(hubColor.copy(alpha = 0.3f), center + direction * (radius * 0.8f), center + direction * radius, radius * 0.12f)
    }
    drawCircle(hubColor, radius = radius * 0.55f, center = center)
    repeat(3) { index ->
        val angle = phase + index * 2f * PI.toFloat() / 3f
        drawLine(tyreColor, center, center + Offset(cos(angle), sin(angle)) * (radius * 0.5f), radius * 0.1f)
    }
    drawCircle(capColor, radius = radius * 0.22f, center = center)
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.PI
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
) {
    /** Canvas position of world point ([x], [y]). */
    fun p(x: Float, y: Float) = Offset(x * unit, groundY - y * unit)
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

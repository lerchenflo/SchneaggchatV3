package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.helicopter

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RAINBOW_BANDS
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.abs
import kotlin.math.cos

/** Half the main rotor's span. */
private const val ROTOR_HALF_SPAN = 14f
/** Where the sling's straps run round the horse's belly (from HORSE_X), front and back. */
private const val SLING_BACK_X = HORSE_X + 7f
private const val SLING_FRONT_X = HORSE_X + 23f

/**
 * The helicopter: [x] is its tail's end, [y] its skids above the ground. [slingY] is where the sling
 * holds the horse (null while it hangs empty under the hook), [rainbowAt] the height of the rainbow
 * to fly up to (null when not shown).
 */
@Immutable
data class RodeoHelicopterUi(
    val x: Float,
    val y: Float,
    /** Radians, turns the rotors. */
    val rotor: Float,
    val carrying: Boolean,
    val slingY: Float?,
    val rainbowAt: Float?,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            RodeoLayer.BACK -> rainbowAt?.let { drawRainbowRibbon(it, context) }
            RodeoLayer.BODY -> Unit
            // In front of the horse: the ropes and the sling round its belly, then the helicopter
            RodeoLayer.FRONT -> {
                drawSling(this@RodeoHelicopterUi, context)
                drawHelicopter(this@RodeoHelicopterUi, context)
            }
        }
    }
}

/** The rainbow up high across the whole sky, its top at [height]: where the horse has to get to. */
private fun DrawScope.drawRainbowRibbon(height: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val top = context.p(0f, height).y
    RAINBOW_BANDS.forEachIndexed { index, color ->
        drawRect(color.copy(alpha = 0.85f), Offset(0f, top + index * unit), Size(size.width, unit))
    }
}

/** The rope down to the hook, and the sling: round the horse's belly, or hanging empty. */
private fun DrawScope.drawSling(heli: RodeoHelicopterUi, context: RodeoDrawContext) {
    val unit = context.unit
    val rope = context.colors.onSurface
    val strap = context.colors.secondary
    val hookX = heli.x + HELI_CABIN_X
    val slingY = heli.slingY
    if (slingY == null) {
        // Hanging empty: rope, hook and a loose U of strap below it
        val hook = context.p(hookX, heli.y - HELI_ROPE)
        drawLine(rope, context.p(hookX, heli.y), hook, 0.35f * unit)
        drawLine(strap, hook, context.p(hookX - 4f, heli.y - HELI_ROPE - 6f), 0.6f * unit, StrokeCap.Round)
        drawLine(strap, hook, context.p(hookX + 4f, heli.y - HELI_ROPE - 6f), 0.6f * unit, StrokeCap.Round)
        drawLine(strap, context.p(hookX - 4f, heli.y - HELI_ROPE - 6f), context.p(hookX + 4f, heli.y - HELI_ROPE - 6f), 0.8f * unit, StrokeCap.Round)
        drawCircle(rope, radius = 0.7f * unit, center = hook)
        return
    }
    // Carrying: two ropes from the belly down to the straps under the horse
    val belly = context.p(hookX, heli.y)
    drawLine(rope, belly, context.p(SLING_BACK_X, slingY), 0.35f * unit)
    drawLine(rope, belly, context.p(SLING_FRONT_X, slingY), 0.35f * unit)
    drawLine(strap, context.p(SLING_BACK_X, slingY), context.p(SLING_BACK_X + 1f, slingY - 4f), 1f * unit, StrokeCap.Round)
    drawLine(strap, context.p(SLING_FRONT_X, slingY), context.p(SLING_FRONT_X - 1f, slingY - 4f), 1f * unit, StrokeCap.Round)
}

/** Skids, cabin with its window, tail boom with the tail rotor and the main rotor on top. */
private fun DrawScope.drawHelicopter(heli: RodeoHelicopterUi, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    fun p(x: Float, y: Float) = context.p(heli.x + x, heli.y + y)
    val metal = colors.onSurface
    val body = colors.tertiary

    // Skids and their struts
    drawLine(metal, p(13f, 0f), p(27f, 0f), 0.5f * unit, StrokeCap.Round)
    drawLine(metal, p(15f, 0f), p(16f, 1.8f), 0.4f * unit)
    drawLine(metal, p(24f, 0f), p(23f, 1.8f), 0.4f * unit)
    // Tail boom, fin and the tail rotor's blur
    drawPath(polygonPath(::p, 13f to 6.8f, 13f to 4.6f, 1.5f to 6.2f, 1.5f to 7.2f), body)
    drawPath(polygonPath(::p, 0.5f to 6.4f, 2.8f to 6.4f, 2f to 10f, 0.8f to 10f), body)
    val tailBlade = 2.3f * abs(cos(heli.rotor * 1.7f))
    drawLine(metal, p(1.6f, 8.4f - tailBlade), p(1.6f, 8.4f + tailBlade), 0.35f * unit, StrokeCap.Round)
    // Cabin: rounded, the window up front
    val cabinTopLeft = p(12f, 9.2f)
    drawRoundRect(body, cabinTopLeft, Size(14.5f * unit, 7.6f * unit), CornerRadius(3.5f * unit))
    drawRoundRect(colors.onTertiary, p(12.5f, 5.6f), Size(13.5f * unit, 0.7f * unit), CornerRadius(0.3f * unit))
    drawPath(polygonPath(::p, 20f to 8.4f, 24.2f to 8.4f, 26f to 5f, 20f to 5f), colors.primaryContainer)
    // Rotor mast and the two blades, foreshortened as they turn
    drawLine(metal, p(HELI_CABIN_X, 9.2f), p(HELI_CABIN_X, HELI_HEIGHT - 0.4f), 0.5f * unit)
    val span = ROTOR_HALF_SPAN * abs(cos(heli.rotor))
    drawLine(metal.copy(alpha = 0.25f), p(HELI_CABIN_X - ROTOR_HALF_SPAN, HELI_HEIGHT), p(HELI_CABIN_X + ROTOR_HALF_SPAN, HELI_HEIGHT), 0.3f * unit)
    drawLine(metal, p(HELI_CABIN_X - span, HELI_HEIGHT), p(HELI_CABIN_X + span, HELI_HEIGHT), 0.5f * unit, StrokeCap.Round)
}

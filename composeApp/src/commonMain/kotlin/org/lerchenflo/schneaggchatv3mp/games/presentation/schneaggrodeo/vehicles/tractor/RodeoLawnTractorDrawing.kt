package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.tractor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWheel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi

// Lawn tractor, explicitly requested like the user's Murray rear-engine rider: a maroon body, a black
// engine with a yellow oil cap at the back, a chrome muffler hanging down, a torn black seat with the
// yellow foam showing, black floor mats and a black steering console
private val TRACTOR_COLOR = Color(0xFF9E1B32)
private val TRACTOR_SHADE_COLOR = Color(0xFF6B1020)
private val TRACTOR_BLACK = Color(0xFF1C1C1C)
private val TRACTOR_GRAY = Color(0xFF424242)
private val CHROME = Color(0xFFCFD8DC)
private val CHROME_DARK = Color(0xFF90A4AE)
private val OIL_CAP_YELLOW = Color(0xFFFDD835)
private val FOAM_YELLOW = Color(0xFFE0A030)
private val FRONT_HUB = Color(0xFFBDBDBD)
/** Edge around the black parts, so they stand out on a dark track too. */
private val BLACK_EDGE = Color(0xFF8A8A8A)

/**
 * The Murray lawn tractor, facing right. [x] is its left edge; it stands on the ground and tips over by
 * [rotation] degrees (counterclockwise, around its rear wheel) once wrecked. Parts come off in the
 * order of the PART_ constants: the first [partsLost] are gone.
 */
@Immutable
data class RodeoTractorUi(
    val x: Float,
    val rotation: Float,
    val wheelPhase: Float,
    val partsLost: Int,
    val wrecked: Boolean,
    /** Puffing exhaust while it races. */
    val exhaust: Boolean,
    val debris: List<RodeoDebrisUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        val colors = context.colors
        when (layer) {
            RodeoLayer.BACK -> Unit
            RodeoLayer.BODY -> drawTractor(
                tractor = this@RodeoTractorUi,
                context = context,
                lineColor = colors.onSurface,
                deckColor = colors.onSurfaceVariant,
                hubColor = colors.surfaceContainer,
                lightColor = colors.surfaceBright,
                smokeColor = colors.onSurfaceVariant,
                clippingColor = colors.secondary
            )
            RodeoLayer.FRONT -> debris.forEach { piece ->
                drawDebris(
                    piece = piece,
                    context = context,
                    lineColor = colors.onSurface,
                    deckColor = colors.onSurfaceVariant,
                    hubColor = colors.surfaceContainer,
                    lightColor = colors.surfaceBright
                )
            }
        }
    }
}

/** A part torn off the tractor; [part] is one of the PART_ constants, anything above is scrap. */
@Immutable
data class RodeoDebrisUi(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val part: Int,
)

/**
 * The lawn tractor, facing right, on a grid with y up from the ground and x from its rear end, like a
 * Murray rear-engine rider: the chrome muffler hanging down at the very back, the black engine with
 * its air cleaner and yellow oil cap over the big rear wheel, the torn seat, a low floor with black
 * mats, the steering console with its badge and a small front wheel. The horse stands across it on
 * [TRACTOR_DECK_HEIGHT]. Parts come off in the order of the PART_ constants.
 */
private fun DrawScope.drawTractor(
    tractor: RodeoTractorUi,
    context: RodeoDrawContext,
    lineColor: Color,
    deckColor: Color,
    hubColor: Color,
    lightColor: Color,
    smokeColor: Color,
    clippingColor: Color,
) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(tractor.x + x, y)
    fun polygon(vararg points: Pair<Float, Float>) = polygonPath(::p, *points)
    /** The part is still attached. */
    fun has(part: Int) = tractor.partsLost <= part

    // Exhaust puffs out of the muffler, trailing back in the wind
    if (tractor.exhaust && has(PART_EXHAUST)) {
        repeat(3) { index ->
            val drift = (tractor.wheelPhase * 0.3f + index / 3f) % 1f
            drawCircle(
                color = smokeColor.copy(alpha = 0.35f * (1f - drift)),
                radius = (0.8f + 1.4f * drift) * unit,
                center = p(-1f - 10f * drift, 2f + 2f * drift)
            )
        }
    }
    // Grass clippings spraying out from under it while it races
    if (tractor.exhaust && has(PART_MOWER_DECK)) {
        repeat(6) { index ->
            val t = (tractor.wheelPhase * 0.17f + index / 6f) % 1f
            val x = 16f - 12f * t
            val y = 1f + 6f * t * (1.2f - t)
            val flip = if (index % 2 == 0) 0.5f else -0.5f
            drawLine(clippingColor.copy(alpha = 1f - t), p(x, y), p(x - 0.9f, y + flip), 0.35f * unit, StrokeCap.Round)
        }
    }

    rotate(-tractor.rotation, pivot = p(8f, 0f)) {
        // Engine at the back: the big black shroud with its yellow oil cap and the air cleaner behind it,
        // or the bare engine with its cooling fins once the shroud flew off
        if (has(PART_HOOD)) {
            val edge = Stroke(width = 0.3f * unit)
            drawRoundRect(TRACTOR_BLACK, p(0.4f, 13.2f), Size(4.6f * unit, 4.4f * unit), CornerRadius(0.8f * unit))
            drawRoundRect(BLACK_EDGE, p(0.4f, 13.2f), Size(4.6f * unit, 4.4f * unit), CornerRadius(0.8f * unit), style = edge)
            drawRoundRect(TRACTOR_BLACK, p(4.6f, 14.4f), Size(7.6f * unit, 7f * unit), CornerRadius(1f * unit))
            drawRoundRect(BLACK_EDGE, p(4.6f, 14.4f), Size(7.6f * unit, 7f * unit), CornerRadius(1f * unit), style = edge)
            // Pull-start cover lines on the engine
            listOf(15.8f, 16.8f).forEach { x -> drawLine(BLACK_EDGE.copy(alpha = 0.5f), p(x - 10f, 12.4f), p(x - 5f, 12.4f), 0.2f * unit) }
            drawRoundRect(OIL_CAP_YELLOW, p(8.2f, 15.4f), Size(2.4f * unit, 1f * unit), CornerRadius(0.3f * unit))
            drawRect(CHROME.copy(alpha = 0.7f), p(1f, 11f), Size(3f * unit, 0.5f * unit))
        } else {
            drawRoundRect(deckColor, p(5f, 13f), Size(6.4f * unit, 5.6f * unit), CornerRadius(0.5f * unit))
            listOf(6.2f, 7.8f, 9.4f).forEach { x -> drawLine(TRACTOR_BLACK, p(x, 13f), p(x, 8f), 0.3f * unit) }
        }
        // Rear body with the rim the horse's hind hooves stand on, and the low floor in front
        drawPath(polygon(0f to 2.6f, 0f to 8f, 18f to 8f, 19f to TRACTOR_DECK_HEIGHT, 38f to TRACTOR_DECK_HEIGHT, 40.5f to 7.6f, 40.5f to 5.8f, 38.5f to 3f, 19f to 3f, 17f to 2.6f), TRACTOR_COLOR)
        drawLine(TRACTOR_SHADE_COLOR, p(0.4f, 5.2f), p(17.5f, 5.2f), 0.3f * unit)
        drawLine(TRACTOR_SHADE_COLOR, p(18.5f, 3.4f), p(18.5f, 7.6f), 0.3f * unit)
        // Black grip mats on the floor
        if (has(PART_MOWER_DECK)) {
            drawRect(TRACTOR_BLACK, p(20f, TRACTOR_DECK_HEIGHT + 0.6f), Size(15f * unit, 0.8f * unit))
            var rib = 20.6f
            while (rib < 35f) {
                drawLine(TRACTOR_GRAY, p(rib, TRACTOR_DECK_HEIGHT + 0.5f), p(rib, TRACTOR_DECK_HEIGHT - 0.1f), 0.15f * unit)
                rib += 1f
            }
        }
        // Chrome muffler hanging down at the very back
        if (has(PART_EXHAUST)) {
            drawRoundRect(CHROME, p(-2.2f, 12f), Size(1.9f * unit, 9.6f * unit), CornerRadius(0.8f * unit))
            drawLine(CHROME_DARK, p(-2.2f, 7.4f), p(-0.3f, 7.4f), 0.3f * unit)
            drawRect(CHROME_DARK, p(-1.9f, 2.4f), Size(1.3f * unit, 0.8f * unit))
        }
        // Torn black seat leaning back, the yellow foam showing through the cracks
        if (has(PART_SEAT)) {
            val seat = polygon(11.5f to 8f, 11f to 16.5f, 13f to 17f, 14f to 10f, 19f to 10f, 19.5f to 8.8f)
            drawPath(seat, TRACTOR_BLACK)
            drawPath(seat, BLACK_EDGE, style = Stroke(width = 0.3f * unit))
            listOf(11.8f to 15f, 12.4f to 12.2f, 15.5f to 9.6f, 17.8f to 9.3f).forEach { (x, y) ->
                drawOval(FOAM_YELLOW, p(x, y), Size(0.9f * unit, 0.6f * unit))
            }
        }
        // Steering console with its silver badge, the column and the wheel
        val console = polygon(30f to TRACTOR_DECK_HEIGHT, 31.5f to 12f, 35f to 12f, 36f to TRACTOR_DECK_HEIGHT)
        drawPath(console, TRACTOR_BLACK)
        drawPath(console, BLACK_EDGE, style = Stroke(width = 0.3f * unit))
        if (has(PART_GRILLE)) {
            drawRect(CHROME, p(31.8f, 10.8f), Size(3.2f * unit, 1f * unit))
            drawRect(TRACTOR_SHADE_COLOR, p(32.2f, 10.6f), Size(2.4f * unit, 0.6f * unit))
        }
        if (has(PART_STEERING_WHEEL)) {
            drawLine(BLACK_EDGE, p(33.5f, 12f), p(31f, 16f), 0.6f * unit, StrokeCap.Round)
            drawLine(BLACK_EDGE, p(28f, 16.8f), p(33.5f, 15.4f), 1f * unit, StrokeCap.Round)
            drawLine(TRACTOR_BLACK, p(28.2f, 16.75f), p(33.3f, 15.45f), 0.5f * unit, StrokeCap.Round)
        }
        if (has(PART_HEADLIGHT)) drawCircle(lightColor, radius = 0.6f * unit, center = p(40f, 6.8f))

        if (has(PART_REAR_WHEEL)) drawWheel(p(8f, 5f), 5f * unit, tractor.wheelPhase, TRACTOR_BLACK, TRACTOR_GRAY, TRACTOR_BLACK)
        // Maroon side panel over the rear wheel with its arch
        if (has(PART_FENDER)) {
            drawArc(
                color = TRACTOR_COLOR,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = p(8f - 6f, 5f + 6f),
                size = Size(12f * unit, 12f * unit),
                style = Stroke(width = 1.4f * unit)
            )
        }
        if (has(PART_FRONT_WHEEL)) drawWheel(p(36f, 2.6f), 2.6f * unit, tractor.wheelPhase * 1.8f, TRACTOR_BLACK, FRONT_HUB, TRACTOR_GRAY)
    }
}

/** A part torn off the tractor, tumbling through the air. */
private fun DrawScope.drawDebris(
    piece: RodeoDebrisUi,
    context: RodeoDrawContext,
    lineColor: Color,
    deckColor: Color,
    hubColor: Color,
    lightColor: Color,
) {
    val unit = context.unit
    val center = context.p(piece.x, piece.y)
    rotate(piece.rotation, pivot = center) {
        when (piece.part) {
            PART_HEADLIGHT -> drawCircle(lightColor, radius = 0.75f * unit, center = center)
            PART_EXHAUST -> drawRoundRect(CHROME, center - Offset(0.95f * unit, 4.8f * unit), Size(1.9f * unit, 9.6f * unit), CornerRadius(0.8f * unit))
            PART_STEERING_WHEEL -> {
                drawLine(TRACTOR_BLACK, center, center + Offset(0f, 3f * unit), 0.5f * unit, StrokeCap.Round)
                drawLine(TRACTOR_BLACK, center - Offset(2.5f * unit, 0f), center + Offset(2.5f * unit, 0f), 0.8f * unit, StrokeCap.Round)
            }
            PART_SEAT -> {
                drawRoundRect(TRACTOR_BLACK, center - Offset(3f * unit, 2.5f * unit), Size(6f * unit, 5f * unit), CornerRadius(1f * unit))
                drawOval(FOAM_YELLOW, center - Offset(1f * unit, 0.5f * unit), Size(1.2f * unit, 0.7f * unit))
            }
            PART_GRILLE -> drawRect(CHROME, center - Offset(1.6f * unit, 0.5f * unit), Size(3.2f * unit, 1f * unit))
            PART_FENDER -> drawArc(
                color = TRACTOR_COLOR,
                startAngle = 180f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = center - Offset(6f * unit, 2f * unit),
                size = Size(12f * unit, 12f * unit),
                style = Stroke(width = 1.4f * unit, cap = StrokeCap.Round)
            )
            PART_HOOD -> {
                drawRoundRect(TRACTOR_BLACK, center - Offset(3.8f * unit, 3.5f * unit), Size(7.6f * unit, 7f * unit), CornerRadius(1f * unit))
                drawRoundRect(OIL_CAP_YELLOW, center - Offset(0.4f * unit, 3.9f * unit), Size(2.4f * unit, 1f * unit), CornerRadius(0.3f * unit))
            }
            PART_MOWER_DECK -> drawRect(TRACTOR_BLACK, center - Offset(7.5f * unit, 0.4f * unit), Size(15f * unit, 0.8f * unit))
            PART_FRONT_WHEEL -> drawWheel(center, 2.6f * unit, 0f, TRACTOR_BLACK, FRONT_HUB, TRACTOR_GRAY)
            PART_REAR_WHEEL -> drawWheel(center, 5f * unit, 0f, TRACTOR_BLACK, TRACTOR_GRAY, TRACTOR_BLACK)
            else -> drawRect(TRACTOR_COLOR, center - Offset(0.75f * unit, 0.75f * unit), Size(1.5f * unit, 1.5f * unit))
        }
    }
}

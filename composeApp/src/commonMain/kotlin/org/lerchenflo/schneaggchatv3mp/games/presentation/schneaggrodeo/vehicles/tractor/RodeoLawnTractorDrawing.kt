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

// Lawn tractor, explicitly requested as a fixed red (plus a darker shade of it for vents and trim)
private val TRACTOR_COLOR = Color(0xFFE53935)
private val TRACTOR_SHADE_COLOR = Color(0xFFB71C1C)

/**
 * The red lawn tractor, facing right. [x] is its left edge; it stands on the ground and tips over by
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
 * The lawn tractor, facing right, on a grid with y up from the ground and x from its rear end: a
 * flat deck for the horse over a big fendered rear wheel, a sloped hood with a headlight in front,
 * and the mower deck low between the wheels. Parts come off in the order of the PART_ constants.
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

    // Exhaust puffs trailing back in the wind
    if (tractor.exhaust && has(PART_EXHAUST)) {
        repeat(3) { index ->
            val drift = (tractor.wheelPhase * 0.3f + index / 3f) % 1f
            drawCircle(
                color = smokeColor.copy(alpha = 0.35f * (1f - drift)),
                radius = (0.8f + 1.4f * drift) * unit,
                center = p(35.5f - 12f * drift, 16f + 2f * drift)
            )
        }
    }
    // Grass clippings spraying out behind the mower deck while it races
    if (tractor.exhaust && has(PART_MOWER_DECK)) {
        repeat(6) { index ->
            val t = (tractor.wheelPhase * 0.17f + index / 6f) % 1f
            val x = 10f - 9f * t
            val y = 1.5f + 6f * t * (1.2f - t)
            val flip = if (index % 2 == 0) 0.5f else -0.5f
            drawLine(clippingColor.copy(alpha = 1f - t), p(x, y), p(x - 0.9f, y + flip), 0.35f * unit, StrokeCap.Round)
        }
    }

    rotate(-tractor.rotation, pivot = p(7f, 0f)) {
        // Mower deck housing, low between the wheels
        if (has(PART_MOWER_DECK)) {
            drawPath(polygon(11f to 3.8f, 29f to 3.8f, 30.5f to 1.2f, 10f to 1.2f), deckColor)
            drawLine(hubColor.copy(alpha = 0.5f), p(12f, 2.5f), p(28.5f, 2.5f), 0.25f * unit)
        }
        // Frame rail between the axles
        drawRect(lineColor, p(6f, 5.4f), Size(28f * unit, 1f * unit))

        // Deck the horse stands on, with a darker skirt
        drawRoundRect(TRACTOR_COLOR, p(0f, TRACTOR_DECK_HEIGHT), Size(30f * unit, 2.4f * unit), CornerRadius(0.6f * unit))
        drawRect(TRACTOR_SHADE_COLOR, p(0.6f, 5.3f), Size(28.8f * unit, 0.7f * unit))
        // Seat back behind the horse's tail
        if (has(PART_SEAT)) {
            drawRoundRect(lineColor, p(0.4f, 12.5f), Size(1.6f * unit, 5.5f * unit), CornerRadius(0.6f * unit))
        }

        // Hood with vents, or the bare engine once it flew off
        if (has(PART_HOOD)) {
            drawPath(polygon(27.5f to 4.8f, 27.5f to 11.5f, 37f to 11.5f, 40.5f to 9f, 40.5f to 4.8f), TRACTOR_COLOR)
            drawLine(hubColor.copy(alpha = 0.35f), p(28.2f, 11f), p(36.6f, 11f), 0.35f * unit, StrokeCap.Round)
            listOf(30f, 31.4f, 32.8f).forEach { x ->
                drawLine(TRACTOR_SHADE_COLOR, p(x, 9.8f), p(x, 7.4f), 0.4f * unit, StrokeCap.Round)
            }
        } else {
            drawRoundRect(deckColor, p(28.5f, 10f), Size(7f * unit, 5f * unit), CornerRadius(0.5f * unit))
            listOf(30f, 32f, 34f).forEach { x ->
                drawLine(lineColor, p(x, 10f), p(x, 6f), 0.3f * unit)
            }
        }
        if (has(PART_GRILLE)) drawLine(lineColor.copy(alpha = 0.6f), p(40f, 8.4f), p(40f, 5.4f), 0.35f * unit)
        if (has(PART_HEADLIGHT)) drawCircle(lightColor, radius = 0.75f * unit, center = p(38.8f, 9.3f))
        if (has(PART_STEERING_WHEEL)) {
            drawLine(lineColor, p(28.5f, 11.5f), p(26.5f, 14.5f), 0.5f * unit, StrokeCap.Round)
            drawLine(lineColor, p(25f, 15.2f), p(28f, 14f), 0.7f * unit, StrokeCap.Round)
        }
        if (has(PART_EXHAUST)) {
            drawLine(lineColor, p(35.5f, 11.5f), p(35.5f, 15.5f), 0.7f * unit, StrokeCap.Round)
            drawLine(lineColor, p(35f, 15.6f), p(36.4f, 16f), 0.6f * unit, StrokeCap.Round)
        }

        if (has(PART_REAR_WHEEL)) drawWheel(p(7f, 4.5f), 4.5f * unit, tractor.wheelPhase, lineColor, hubColor, TRACTOR_COLOR)
        if (has(PART_FENDER)) {
            drawArc(
                color = TRACTOR_COLOR,
                startAngle = 180f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = p(7f - 5.6f, 4.5f + 5.6f),
                size = Size(11.2f * unit, 11.2f * unit),
                style = Stroke(width = 1.2f * unit, cap = StrokeCap.Round)
            )
        }
        if (has(PART_FRONT_WHEEL)) drawWheel(p(33f, 2.8f), 2.8f * unit, tractor.wheelPhase * 1.6f, lineColor, hubColor, TRACTOR_COLOR)
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
            PART_EXHAUST -> drawLine(lineColor, center - Offset(0f, 1.5f * unit), center + Offset(0f, 1.5f * unit), 0.8f * unit, StrokeCap.Round)
            PART_STEERING_WHEEL -> {
                drawLine(lineColor, center, center + Offset(0f, 3f * unit), 0.5f * unit, StrokeCap.Round)
                drawLine(lineColor, center - Offset(1.5f * unit, 0f), center + Offset(1.5f * unit, 0f), 0.6f * unit, StrokeCap.Round)
            }
            PART_SEAT -> drawRoundRect(lineColor, center - Offset(0.8f * unit, 2.75f * unit), Size(1.6f * unit, 5.5f * unit), CornerRadius(0.6f * unit))
            PART_GRILLE -> drawLine(lineColor, center - Offset(0f, 1.5f * unit), center + Offset(0f, 1.5f * unit), 0.35f * unit)
            PART_FENDER -> drawArc(
                color = TRACTOR_COLOR,
                startAngle = 180f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = center - Offset(5.6f * unit, 2f * unit),
                size = Size(11.2f * unit, 11.2f * unit),
                style = Stroke(width = 1.2f * unit, cap = StrokeCap.Round)
            )
            PART_HOOD -> {
                drawRoundRect(TRACTOR_COLOR, center - Offset(4f * unit, 2f * unit), Size(8f * unit, 4f * unit), CornerRadius(1f * unit))
                listOf(-1f, 0.5f).forEach { x ->
                    drawLine(TRACTOR_SHADE_COLOR, center + Offset(x * unit, -1f * unit), center + Offset(x * unit, 1f * unit), 0.4f * unit)
                }
            }
            PART_MOWER_DECK -> drawRoundRect(deckColor, center - Offset(4f * unit, 0.75f * unit), Size(8f * unit, 1.5f * unit), CornerRadius(0.5f * unit))
            PART_FRONT_WHEEL -> drawWheel(center, 2.8f * unit, 0f, lineColor, hubColor, TRACTOR_COLOR)
            PART_REAR_WHEEL -> drawWheel(center, 4.5f * unit, 0f, lineColor, hubColor, TRACTOR_COLOR)
            else -> drawRect(TRACTOR_COLOR, center - Offset(0.75f * unit, 0.75f * unit), Size(1.5f * unit, 1.5f * unit))
        }
    }
}

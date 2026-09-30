package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cow

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoFootprint
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.onFootprint
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawSeatedCowboy
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.sin

// The cow's fixed colors: Vorarlberg brown cattle ("Braunvieh") with a light muzzle and a brass bell
private val COW_BROWN = Color(0xFF8D7B6A)
private val COW_BROWN_DARK = Color(0xFF5D4E42)
private val COW_MUZZLE = Color(0xFFE8DCCB)
private val COW_UDDER = Color(0xFFF4B6B6)
private val COW_HORN = Color(0xFFF5F0E6)
private val BELL_BRASS = Color(0xFFD4AF37)
private val COLLAR = Color(0xFF6D2B1A)

/** The cow, facing right: [x] is her tail end; [ring] 0..1 while her bell rings, else null. */
@Immutable
data class RodeoCowUi(
    val x: Float,
    val gait: Float,
    val hasRider: Boolean,
    val ring: Float?,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override val footprint get() = RodeoFootprint(x + 5f, x + 16f)

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        if (layer == RodeoLayer.FRONT) onFootprint(context, footprint) { drawCow(this@RodeoCowUi, it) }
    }
}

/**
 * The cow on a grid with y up from the ground and x from her tail end: four legs swinging with
 * [RodeoCowUi.gait], a round body, udder, tail with a tuft, head with a light muzzle, ears and
 * short horns, and the big bell on its collar - ringing waves while it rings.
 */
private fun DrawScope.drawCow(cow: RodeoCowUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(cow.x + x, y)

    // Legs: the far pair darker, swinging against the near pair
    listOf(5f to 0f, 15f to 1.6f).forEach { (legX, phase) ->
        val swing = 1.2f * sin(cow.gait + phase)
        drawLine(COW_BROWN_DARK, p(legX + 1f, 7f), p(legX + 1f - swing, 0.4f), 1.3f * unit, StrokeCap.Round)
        drawLine(COW_BROWN, p(legX, 7f), p(legX + swing, 0.4f), 1.4f * unit, StrokeCap.Round)
        drawCircle(COW_BROWN_DARK, radius = 0.6f * unit, center = p(legX + swing, 0.5f))
    }
    // Tail with its tuft
    val tailSwing = 0.8f * sin(cow.gait * 0.7f)
    drawLine(COW_BROWN_DARK, p(3f, 11f), p(1.2f + tailSwing, 5.5f), 0.4f * unit, StrokeCap.Round)
    drawOval(COW_BROWN_DARK, p(0.6f + tailSwing, 6f), Size(1.3f * unit, 1.8f * unit))
    // Body and udder
    drawOval(COW_BROWN, p(2.5f, 13f), Size(15.5f * unit, 7.2f * unit))
    drawOval(COW_UDDER, p(6.2f, 6.6f), Size(2.6f * unit, 1.6f * unit))
    // Neck and head
    drawOval(COW_BROWN, p(15.5f, 13.2f), Size(4.5f * unit, 5.5f * unit))
    drawOval(COW_BROWN, p(17.8f, 13.6f), Size(4.2f * unit, 3.6f * unit))
    drawOval(COW_MUZZLE, p(20.4f, 12.2f), Size(2f * unit, 2.2f * unit))
    drawCircle(COW_BROWN_DARK, radius = 0.25f * unit, center = p(21.6f, 11.2f))
    drawCircle(COW_BROWN_DARK, radius = 0.3f * unit, center = p(19.6f, 12.8f))
    drawOval(COW_BROWN_DARK, p(17.2f, 14.4f), Size(1.8f * unit, 0.9f * unit))
    drawLine(COW_HORN, p(18.8f, 13.6f), p(19.4f, 15.2f), 0.45f * unit, StrokeCap.Round)

    // Collar and bell
    drawLine(COLLAR, p(16.4f, 9.4f), p(18.8f, 8.6f), 0.6f * unit, StrokeCap.Round)
    drawOval(BELL_BRASS, p(COW_BELL_X - 1.1f, COW_BELL_Y + 2.4f), Size(2.2f * unit, 2.6f * unit))
    drawCircle(COW_BROWN_DARK, radius = 0.3f * unit, center = p(COW_BELL_X, COW_BELL_Y - 0.1f))
    cow.ring?.let { ring ->
        repeat(2) { index ->
            val radius = (1.5f + 3f * ring + index * 1.4f) * unit
            drawCircle(
                BELL_BRASS.copy(alpha = (1f - ring) * 0.8f),
                radius = radius,
                center = p(COW_BELL_X, COW_BELL_Y + 1f),
                style = Stroke(width = 0.25f * unit)
            )
        }
    }

    if (cow.hasRider) {
        // Astride her back: a leg hanging down her side, holding on to her horn
        drawLine(context.colors.onSurface, p(COW_SEAT_X, COW_SEAT_Y), p(COW_SEAT_X + 1.6f, 7.4f), 1f * unit, StrokeCap.Round)
        drawSeatedCowboy(::p, hipX = COW_SEAT_X, hipY = COW_SEAT_Y, unit = unit, colors = context.colors, armToX = 18.8f, armToY = 14.4f)
    }
}

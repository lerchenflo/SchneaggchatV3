package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.uboat

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.HAT_COLOR
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// The U-boat's fixed colors: a yellow submarine with dark portholes, white pearls and black mines
private val SUB_YELLOW = Color(0xFFFDD835)
private val SUB_YELLOW_DARK = Color(0xFFC6A700)
private val SUB_TRIM = Color(0xFF37474F)
private val PORTHOLE_GLASS = Color(0xFF80DEEA)
private val PEARL = Color(0xFFF8F4EC)
private val PEARL_SHINE = Color(0xFFFFFFFF)
private val MINE_BLACK = Color(0xFF263238)
private val BOOM = Color(0xFFFF7043)

/** The U-boat, facing right: [x] is its stern, [y] the hull's underside. */
@Immutable
data class RodeoUBoatUi(
    val x: Float,
    val y: Float,
    val propeller: Float,
    /** Horse and rider are inside: the horse's head in a porthole, the hat in the tower. */
    val crewAboard: Boolean,
    /** 0..1 after hitting a mine, else null. */
    val boom: Float?,
    val pearls: List<RodeoSeaThingUi>,
    val mines: List<RodeoSeaThingUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            RodeoLayer.BACK -> Unit
            RodeoLayer.BODY -> drawUBoat(this@RodeoUBoatUi, context)
            RodeoLayer.FRONT -> {
                pearls.forEach { drawPearl(it, context) }
                mines.forEach { drawMine(it, context) }
            }
        }
    }
}

/** A pearl or sea mine; [x] / [y] its center. */
@Immutable
data class RodeoSeaThingUi(val x: Float, val y: Float)

/** Rounded hull, conning tower with periscope, portholes, fins, a spinning propeller and bubbles. */
private fun DrawScope.drawUBoat(boat: RodeoUBoatUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(boat.x + x, boat.y + y)

    // Bubbles trailing behind
    repeat(4) { index ->
        val cycle = (boat.propeller * 0.08f + index * 0.25f) % 1f
        drawCircle(PORTHOLE_GLASS.copy(alpha = 0.6f * (1f - cycle)), radius = (0.4f + 0.4f * cycle) * unit, center = p(-1.5f - 5f * cycle, UBOAT_HEIGHT / 2f + 3f * cycle))
    }
    // Propeller
    val blade = 1.8f * cos(boat.propeller)
    drawLine(SUB_TRIM, p(-0.6f, UBOAT_HEIGHT / 2f - blade), p(-0.6f, UBOAT_HEIGHT / 2f + blade), 0.6f * unit, StrokeCap.Round)
    // Tower and periscope
    drawRoundRect(SUB_YELLOW_DARK, p(TOWER_X, UBOAT_HEIGHT + TOWER_HEIGHT), Size(TOWER_WIDTH * unit, (TOWER_HEIGHT + 0.5f) * unit), CornerRadius(1f * unit))
    drawLine(SUB_TRIM, p(TOWER_X + 4.5f, UBOAT_HEIGHT + TOWER_HEIGHT), p(TOWER_X + 4.5f, UBOAT_HEIGHT + TOWER_HEIGHT + 3f), 0.4f * unit)
    drawLine(SUB_TRIM, p(TOWER_X + 4.5f, UBOAT_HEIGHT + TOWER_HEIGHT + 3f), p(TOWER_X + 5.8f, UBOAT_HEIGHT + TOWER_HEIGHT + 3f), 0.4f * unit, StrokeCap.Round)
    if (boat.crewAboard) {
        // The cowboy's hat pokes out of the tower hatch
        drawRoundRect(HAT_COLOR, p(TOWER_X + 0.8f, UBOAT_HEIGHT + TOWER_HEIGHT + 0.9f), Size(3.6f * unit, 0.5f * unit), CornerRadius(0.25f * unit))
        drawRoundRect(HAT_COLOR, p(TOWER_X + 1.6f, UBOAT_HEIGHT + TOWER_HEIGHT + 2.4f), Size(2f * unit, 1.5f * unit), CornerRadius(0.5f * unit))
    }
    // Hull with fins
    drawRoundRect(SUB_YELLOW, p(0f, UBOAT_HEIGHT), Size(UBOAT_LENGTH * unit, UBOAT_HEIGHT * unit), CornerRadius(3.5f * unit))
    drawLine(SUB_YELLOW_DARK, p(2f, 1.2f), p(UBOAT_LENGTH - 2f, 1.2f), 0.5f * unit)
    drawLine(SUB_YELLOW_DARK, p(1f, UBOAT_HEIGHT / 2f), p(-0.4f, UBOAT_HEIGHT / 2f + 2.5f), 0.7f * unit, StrokeCap.Round)
    drawLine(SUB_YELLOW_DARK, p(1f, UBOAT_HEIGHT / 2f), p(-0.4f, UBOAT_HEIGHT / 2f - 2.5f), 0.7f * unit, StrokeCap.Round)
    // Portholes
    listOf(8f, 13f, 26f).forEach { holeX ->
        drawCircle(SUB_TRIM, radius = 1.3f * unit, center = p(holeX, UBOAT_HEIGHT / 2f + 0.3f))
        drawCircle(PORTHOLE_GLASS, radius = 1f * unit, center = p(holeX, UBOAT_HEIGHT / 2f + 0.3f))
    }
    if (boat.crewAboard) {
        // The horse looks out of the middle porthole
        drawOval(context.colors.onSurface, p(12.4f, UBOAT_HEIGHT / 2f + 0.9f), Size(1.6f * unit, 1.1f * unit))
        drawCircle(Color.Black, radius = 0.15f * unit, center = p(13.4f, UBOAT_HEIGHT / 2f + 0.6f))
    }
    boat.boom?.let { progress ->
        repeat(6) { index ->
            val angle = index * PI.toFloat() / 3f
            val distance = (2f + 6f * progress) * unit
            drawCircle(
                BOOM.copy(alpha = 1f - progress),
                radius = (1.5f + 2f * progress) * unit,
                center = p(UBOAT_LENGTH * 0.7f, UBOAT_HEIGHT / 2f) + Offset(cos(angle) * distance, sin(angle) * distance)
            )
        }
    }
}

private fun DrawScope.drawPearl(pearl: RodeoSeaThingUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(pearl.x, pearl.y)
    drawCircle(PEARL, radius = PEARL_RADIUS * unit, center = center)
    drawCircle(PEARL_SHINE, radius = PEARL_RADIUS * 0.35f * unit, center = center + Offset(-0.4f * unit, -0.4f * unit))
}

/** A sea mine: black ball with spikes, anchored by a chain going down. */
private fun DrawScope.drawMine(mine: RodeoSeaThingUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(mine.x, mine.y)
    drawLine(MINE_BLACK.copy(alpha = 0.6f), center, center + Offset(0f, 12f * unit), 0.2f * unit)
    repeat(8) { index ->
        val angle = index * PI.toFloat() / 4f
        drawLine(MINE_BLACK, center, center + Offset(cos(angle), sin(angle)) * ((MINE_RADIUS + 0.8f) * unit), 0.35f * unit, StrokeCap.Round)
    }
    drawCircle(MINE_BLACK, radius = MINE_RADIUS * unit, center = center)
    drawCircle(Color.White.copy(alpha = 0.25f), radius = MINE_RADIUS * 0.4f * unit, center = center + Offset(-0.6f * unit, -0.6f * unit), style = Stroke(width = 0.2f * unit))
}

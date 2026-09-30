package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.carriage

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HOOVES_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoHorsePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MAX_HORSE_LEVEL
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoHorseColors
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawHorseAndRider
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWheel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.sin

// The golden carriage's fixed colors: gold and its shades, red velvet curtains, white horses
private val GOLD = Color(0xFFFFC107)
private val GOLD_LIGHT = Color(0xFFFFE082)
private val GOLD_DARK = Color(0xFFB8860B)
private val VELVET = Color(0xFFB71C1C)
private val WHITE_HORSE = Color(0xFFF5F5F5)

/** The golden carriage, facing right: [x] is its rear; two golden horses pull it. */
@Immutable
data class RodeoGoldenCarriageUi(
    val x: Float,
    val wheelPhase: Float,
    /** Drives the pulling horses' legs. */
    val gait: Float,
) : RodeoVehicleUi {

    override val lassoHint: RodeoLassoHintUi? = null

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            RodeoLayer.BACK -> drawPullingHorse(x + PULL_HORSE_X + 3f, gait + 1.4f, context)
            RodeoLayer.BODY -> {
                drawCarriage(this@RodeoGoldenCarriageUi, context)
                drawPullingHorse(x + PULL_HORSE_X, gait, context)
            }
            RodeoLayer.FRONT -> Unit
        }
    }
}

/** A white horse at the top level (gold from the hooves up), pulling without a rider. */
private fun DrawScope.drawPullingHorse(left: Float, gait: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    val pose = RodeoHorsePose(
        height = 0f,
        gaitPhase = gait,
        airborne = false,
        riderLean = 0f,
        pitchDegrees = 2.5f * sin(gait + 0.8f),
        pivotX = 12f,
        pivotY = 12f,
        hindLegScale = 1f,
        frontLegFold = 0f,
        hatLift = 0f,
        glow = 0f,
        hasRider = false,
        level = MAX_HORSE_LEVEL,
    )
    val horseColors = RodeoHorseColors(
        body = WHITE_HORSE,
        shirt = colors.primary,
        glow = GOLD,
        canopy = colors.secondary,
        friendShirt = colors.tertiary,
        headRing = colors.surfaceContainer,
    )
    drawHorseAndRider(left = left * unit, groundY = context.groundYAt(left + HOOVES_X), unit = unit, pose = pose, colors = horseColors, bodyLabel = null)
}

/** Big spoked wheels, a golden body with scrolls and a window with curtains, a crown and the shaft. */
private fun DrawScope.drawCarriage(carriage: RodeoGoldenCarriageUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(carriage.x + x, y)
    // Shaft and harness to the horses
    drawLine(GOLD_DARK, p(CARRIAGE_BODY_LENGTH - 2f, 7f), p(PULL_HORSE_X + 16f, 13f), 0.7f * unit, StrokeCap.Round)
    // Coachman's box in front
    drawRect(GOLD_DARK, p(CARRIAGE_BODY_LENGTH - 1f, 11f), Size(5f * unit, 1.2f * unit))
    drawLine(GOLD_DARK, p(CARRIAGE_BODY_LENGTH + 3f, 11f), p(CARRIAGE_BODY_LENGTH + 1f, 6f), 0.5f * unit)
    // Body: rounded bottom, straight top
    drawPath(
        polygonPath(
            ::p,
            2f to CARRIAGE_ROOF - 1f, CARRIAGE_BODY_LENGTH - 2f to CARRIAGE_ROOF - 1f,
            CARRIAGE_BODY_LENGTH to 11f, CARRIAGE_BODY_LENGTH - 3f to 5f, 3f to 5f, 0f to 11f,
        ),
        GOLD,
    )
    drawLine(GOLD_LIGHT, p(3f, 6.5f), p(CARRIAGE_BODY_LENGTH - 3f, 6.5f), 0.4f * unit)
    // Window with red velvet curtains and a door frame
    drawRoundRect(GOLD_DARK, p(9.5f, 16f), Size(11f * unit, 7f * unit), CornerRadius(1.2f * unit))
    drawRoundRect(context.colors.surfaceVariant.copy(alpha = 0.5f), p(10.3f, 15.2f), Size(9.4f * unit, 5.4f * unit), CornerRadius(1f * unit))
    drawPath(polygonPath(::p, 10.3f to 15.2f, 13f to 15.2f, 11.2f to 9.8f, 10.3f to 9.8f), VELVET)
    drawPath(polygonPath(::p, 19.7f to 15.2f, 17f to 15.2f, 18.8f to 9.8f, 19.7f to 9.8f), VELVET)
    // Golden scrolls at the corners
    listOf(2.5f to 14f, CARRIAGE_BODY_LENGTH - 2.5f to 14f).forEach { (cx, cy) ->
        drawCircle(GOLD_DARK, radius = 1.4f * unit, center = p(cx, cy), style = Stroke(width = 0.4f * unit))
    }
    // Roof rail the horse stands on, and a crown on top at the back
    drawRoundRect(GOLD_DARK, p(0.5f, CARRIAGE_ROOF + 0.4f), Size((CARRIAGE_BODY_LENGTH - 1f) * unit, 1.4f * unit), CornerRadius(0.5f * unit))
    drawPath(polygonPath(::p, 1f to CARRIAGE_ROOF + 0.4f, 1f to CARRIAGE_ROOF + 3f, 2f to CARRIAGE_ROOF + 1.8f, 3f to CARRIAGE_ROOF + 3.4f, 4f to CARRIAGE_ROOF + 1.8f, 5f to CARRIAGE_ROOF + 3f, 5f to CARRIAGE_ROOF + 0.4f), GOLD_LIGHT)
    // Wheels: big at the back, smaller in front
    drawWheel(p(6f, 5f), 5f * unit, carriage.wheelPhase, GOLD_DARK, GOLD_LIGHT, GOLD)
    drawWheel(p(CARRIAGE_BODY_LENGTH - 5f, 3.8f), 3.8f * unit, carriage.wheelPhase * 1.3f, GOLD_DARK, GOLD_LIGHT, GOLD)
    // A glitter now and then
    val glitter = (carriage.wheelPhase * 0.2f) % 1f
    drawCircle(Color.White.copy(alpha = 1f - glitter), radius = (0.3f + glitter * 0.6f) * unit, center = p(8f + 14f * glitter, 12f))
}

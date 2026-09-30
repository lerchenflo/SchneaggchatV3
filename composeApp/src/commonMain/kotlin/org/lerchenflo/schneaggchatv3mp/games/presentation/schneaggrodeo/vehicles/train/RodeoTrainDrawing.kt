package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.train

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWheel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.cos
import kotlin.math.sin

/** The Wälderbähnle, facing right: [x] is the rear end of its coach. */
@Immutable
data class RodeoTrainUi(
    val x: Float,
    val wheelPhase: Float,
    /** Left edges of the low bridges over the track. */
    val bridges: List<Float>,
    val smoke: List<RodeoSmokeUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        val colors = context.colors
        when (layer) {
            // Bridge piers stand beside the track, behind the train
            RodeoLayer.BACK -> bridges.forEach { bridgeX ->
                listOf(bridgeX + 1f, bridgeX + BRIDGE_WIDTH - 3f).forEach { pierX ->
                    drawRect(
                        colors.surfaceVariant,
                        context.p(pierX, BRIDGE_UNDERSIDE),
                        Size(2f * context.unit, BRIDGE_UNDERSIDE * context.unit)
                    )
                }
            }
            RodeoLayer.BODY -> drawTrain(
                train = this@RodeoTrainUi,
                context = context,
                engineColor = colors.onSurface,
                coachColor = colors.secondaryContainer,
                trimColor = colors.secondary,
                windowColor = colors.surfaceBright,
                wheelColor = colors.primary,
            )
            RodeoLayer.FRONT -> {
                smoke.forEach { puff ->
                    drawCircle(
                        color = colors.onSurfaceVariant.copy(alpha = 0.45f * (1f - puff.progress)),
                        radius = (1.2f + 3f * puff.progress) * context.unit,
                        center = context.p(puff.x, puff.y)
                    )
                }
                bridges.forEach { bridgeX -> drawBridgeDeck(bridgeX, context, colors.outline, colors.onSurfaceVariant) }
            }
        }
    }
}

/** A puff of steam; [progress] runs from 0 (fresh out of the chimney) to 1 (gone). */
@Immutable
data class RodeoSmokeUi(val x: Float, val y: Float, val progress: Float)

/** Stone deck of a low bridge with a railing on top. */
private fun DrawScope.drawBridgeDeck(bridgeX: Float, context: RodeoDrawContext, stoneColor: Color, lineColor: Color) {
    val unit = context.unit
    drawRoundRect(
        stoneColor,
        context.p(bridgeX, BRIDGE_UNDERSIDE + BRIDGE_DECK),
        Size(BRIDGE_WIDTH * unit, BRIDGE_DECK * unit),
        CornerRadius(0.6f * unit)
    )
    // Stone joints
    listOf(BRIDGE_UNDERSIDE + 1.7f, BRIDGE_UNDERSIDE + 3.4f).forEachIndexed { row, y ->
        drawLine(lineColor.copy(alpha = 0.4f), context.p(bridgeX, y), context.p(bridgeX + BRIDGE_WIDTH, y), 0.2f * unit)
        var joint = if (row == 0) 2f else 4f
        while (joint < BRIDGE_WIDTH) {
            drawLine(lineColor.copy(alpha = 0.4f), context.p(bridgeX + joint, y - 1.7f), context.p(bridgeX + joint, y), 0.2f * unit)
            joint += 4f
        }
    }
    // Railing
    val railY = BRIDGE_UNDERSIDE + BRIDGE_DECK + 2.5f
    drawLine(lineColor, context.p(bridgeX, railY), context.p(bridgeX + BRIDGE_WIDTH, railY), 0.35f * unit)
    var post = 0.5f
    while (post < BRIDGE_WIDTH) {
        drawLine(lineColor, context.p(bridgeX + post, BRIDGE_UNDERSIDE + BRIDGE_DECK), context.p(bridgeX + post, railY), 0.3f * unit)
        post += 2.6f
    }
}

/**
 * The train on a grid with y up from the ground and x from the rear of its coach: a coach with
 * windows, the coupling, then a small tank engine with cab, boiler, dome, chimney and cowcatcher.
 */
private fun DrawScope.drawTrain(
    train: RodeoTrainUi,
    context: RodeoDrawContext,
    engineColor: Color,
    coachColor: Color,
    trimColor: Color,
    windowColor: Color,
    wheelColor: Color,
) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(train.x + x, y)
    fun polygon(vararg points: Pair<Float, Float>) = polygonPath(::p, *points)

    // Coach: body, roof the horse stands on, windows, grab rail at the back
    drawRoundRect(coachColor, p(0f, 12.5f), Size(30f * unit, 9f * unit), CornerRadius(0.8f * unit))
    drawRoundRect(trimColor, p(-0.5f, TRAIN_ROOF), Size(31f * unit, 1.2f * unit), CornerRadius(0.6f * unit))
    drawRect(trimColor, p(0f, 5f), Size(30f * unit, 0.8f * unit))
    listOf(3f, 9.5f, 16f, 22.5f).forEach { windowX ->
        drawRoundRect(windowColor, p(windowX, 11f), Size(4.5f * unit, 3.5f * unit), CornerRadius(0.5f * unit))
    }
    drawLine(engineColor, p(TRAIN_HITCH_X - 0.8f, 5.5f), p(TRAIN_HITCH_X - 0.8f, 11f), 0.35f * unit, StrokeCap.Round)
    // Coupling
    drawLine(engineColor, p(30f, 4.5f), p(32f, 4.5f), 0.6f * unit)

    // Engine frame, cab and boiler
    drawRect(engineColor, p(32f, 5f), Size(28f * unit, 1.5f * unit))
    drawRect(engineColor, p(32f, 16.5f), Size(9f * unit, 11.5f * unit))
    drawRoundRect(engineColor, p(31.2f, 18f), Size(10.6f * unit, 1.5f * unit), CornerRadius(0.5f * unit))
    drawRoundRect(windowColor, p(34f, 15f), Size(4.5f * unit, 3.5f * unit), CornerRadius(0.5f * unit))
    drawRoundRect(engineColor, p(41f, 13.5f), Size(17f * unit, 8f * unit), CornerRadius(4f * unit))
    listOf(45f, 51f).forEach { bandX -> drawRect(trimColor, p(bandX, 13.5f), Size(0.8f * unit, 8f * unit)) }
    drawRoundRect(engineColor, p(56.5f, 14f), Size(3.5f * unit, 9f * unit), CornerRadius(1f * unit))
    // Dome and flared chimney
    drawRoundRect(trimColor, p(46.5f, 15.5f), Size(2.5f * unit, 2.5f * unit), CornerRadius(1.2f * unit))
    drawPath(
        polygon(
            TRAIN_CHIMNEY_X - 1f to 13.5f,
            TRAIN_CHIMNEY_X - 1f to 17.5f,
            TRAIN_CHIMNEY_X - 2f to TRAIN_CHIMNEY_TOP,
            TRAIN_CHIMNEY_X + 2f to TRAIN_CHIMNEY_TOP,
            TRAIN_CHIMNEY_X + 1f to 17.5f,
            TRAIN_CHIMNEY_X + 1f to 13.5f,
        ),
        engineColor
    )
    drawCircle(windowColor, radius = 0.8f * unit, center = p(59.2f, 12.5f))
    // Cowcatcher
    drawPath(polygon(59f to 5f, 64f to 1f, 59f to 1f), trimColor)

    // Wheels: two under the coach, three coupled drivers on the engine
    listOf(5f, 25f).forEach { wheelX ->
        drawWheel(p(wheelX, 2.2f), 2.2f * unit, train.wheelPhase, engineColor, windowColor, wheelColor)
    }
    val drivers = listOf(43f, 49f, 55f)
    drivers.forEach { wheelX ->
        drawWheel(p(wheelX, 2.6f), 2.6f * unit, train.wheelPhase, engineColor, wheelColor, engineColor)
    }
    // Side rod coupling the drivers, going round with them
    val crank = Offset(cos(train.wheelPhase), sin(train.wheelPhase)) * (1.2f * unit)
    drawLine(trimColor, p(drivers.first(), 2.6f) + crank, p(drivers.last(), 2.6f) + crank, 0.5f * unit, StrokeCap.Round)
}

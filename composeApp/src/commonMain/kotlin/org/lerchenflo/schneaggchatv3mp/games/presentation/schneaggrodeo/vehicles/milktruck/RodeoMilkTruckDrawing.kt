package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.milktruck

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoFootprint
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.onFootprint
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

/** The milk truck, facing right: [x] is the rear end of its cargo box. */
@Immutable
data class RodeoMilkTruckUi(
    val x: Float,
    val wheelPhase: Float,
    /** Cans still standing in the rack on the roof. */
    val cansInRack: Int,
    /** Cans flying, dangling from the lasso or spilled on the road. */
    val cans: List<RodeoMilkCanUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override val footprint get() = RodeoFootprint(x + 7f, x + 36f)

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        val colors = context.colors
        when (layer) {
            RodeoLayer.BACK -> Unit
            RodeoLayer.BODY -> onFootprint(context, footprint) { level ->
                drawMilkTruck(
                    truck = this@RodeoMilkTruckUi,
                    context = level,
                    boxColor = colors.surfaceBright,
                    spotColor = colors.onSurface,
                    cabColor = colors.primary,
                    glassColor = colors.primaryContainer,
                    lineColor = colors.onSurface,
                    hubColor = colors.surfaceContainer,
                    canColor = colors.outline,
                    lidColor = colors.onSurfaceVariant,
                )
            }
            RodeoLayer.FRONT -> cans.forEach { can ->
                val center = context.p(can.x, can.y)
                if (can.spilled) {
                    drawOval(
                        colors.surfaceBright,
                        topLeft = center + Offset(-4f * context.unit, 0.4f * context.unit),
                        size = Size(8f * context.unit, 1.2f * context.unit)
                    )
                }
                drawMilkCan(center, can.rotation, context.unit, colors.outline, colors.onSurfaceVariant)
            }
        }
    }
}

/** [x] / [y] are the can's center; lying in a milk puddle once [spilled]. */
@Immutable
data class RodeoMilkCanUi(val x: Float, val y: Float, val rotation: Float, val spilled: Boolean)

/** An upright milk can centered on [center]: body, narrower neck, lid and a handle bar. */
private fun DrawScope.drawMilkCan(center: Offset, rotation: Float, unit: Float, canColor: Color, lidColor: Color) {
    rotate(rotation, pivot = center) {
        val bodyTop = center.y - 0.6f * unit
        drawRoundRect(
            canColor,
            Offset(center.x - CAN_WIDTH / 2f * unit, bodyTop),
            Size(CAN_WIDTH * unit, (CAN_HEIGHT / 2f + 0.6f) * unit),
            CornerRadius(0.4f * unit)
        )
        drawRect(canColor, Offset(center.x - 0.6f * unit, bodyTop - 0.8f * unit), Size(1.2f * unit, 0.8f * unit))
        drawRoundRect(
            lidColor,
            Offset(center.x - 0.8f * unit, center.y - CAN_HEIGHT / 2f * unit),
            Size(1.6f * unit, 0.5f * unit),
            CornerRadius(0.25f * unit)
        )
        drawLine(lidColor, Offset(center.x - CAN_WIDTH / 2f * unit, bodyTop + 0.4f * unit), Offset(center.x + CAN_WIDTH / 2f * unit, bodyTop + 0.4f * unit), 0.25f * unit)
    }
}

/**
 * The truck on a grid with y up from the ground and x from the rear of its cargo box: a tall,
 * cow-spotted box with a rack of milk cans on the roof, and a rounded cab with a windshield in front.
 */
private fun DrawScope.drawMilkTruck(
    truck: RodeoMilkTruckUi,
    context: RodeoDrawContext,
    boxColor: Color,
    spotColor: Color,
    cabColor: Color,
    glassColor: Color,
    lineColor: Color,
    hubColor: Color,
    canColor: Color,
    lidColor: Color,
) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(truck.x + x, y)
    fun polygon(vararg points: Pair<Float, Float>) = polygonPath(::p, *points)

    // Chassis
    drawRect(lineColor, p(1f, 5f), Size(43f * unit, 1.6f * unit))

    // Cargo box with an outline, cow spots and the rear door
    val boxTopLeft = p(0f, TRUCK_ROOF)
    val boxSize = Size(30f * unit, (TRUCK_ROOF - 5f) * unit)
    drawRoundRect(boxColor, boxTopLeft, boxSize, CornerRadius(0.8f * unit))
    drawRoundRect(lineColor, boxTopLeft, boxSize, CornerRadius(0.8f * unit), style = Stroke(width = 0.35f * unit))
    listOf(
        Triple(6f, 13f, 2.2f), Triple(13f, 8.5f, 1.6f), Triple(19f, 13.5f, 2.6f),
        Triple(25f, 9f, 1.8f), Triple(9.5f, 7f, 1.1f),
    ).forEach { (spotX, spotY, radius) ->
        drawOval(spotColor, p(spotX - radius * 1.3f, spotY + radius), Size(radius * 2.6f * unit, radius * 2f * unit))
    }
    drawLine(lineColor.copy(alpha = 0.5f), p(TRUCK_HITCH_X + 1.5f, 15.5f), p(TRUCK_HITCH_X + 1.5f, 6f), 0.25f * unit)
    drawLine(lineColor, p(TRUCK_HITCH_X, 10f), p(TRUCK_HITCH_X, 8f), 0.5f * unit, StrokeCap.Round)

    // Rack of milk cans on the roof
    drawLine(lineColor, p(RACK_X - 0.8f, TRUCK_ROOF + 2f), p(RACK_X + RACK_SLOTS * (CAN_WIDTH + CAN_SPACING), TRUCK_ROOF + 2f), 0.25f * unit)
    repeat(truck.cansInRack) { slot ->
        drawMilkCan(p(rackSlotX(slot), TRUCK_ROOF + CAN_HEIGHT / 2f), 0f, unit, canColor, lidColor)
    }

    // Cab with windshield, headlight and bumper
    drawPath(polygon(31f to 5f, 31f to 14.5f, 38f to 14.5f, 42f to 10f, 44f to 9.5f, 44f to 5f), cabColor)
    drawPath(polygon(37.8f to 13.6f, 41.2f to 10f, 37.8f to 10f), glassColor)
    drawRoundRect(glassColor, p(32.5f, 13.6f), Size(4f * unit, 3.2f * unit), CornerRadius(0.4f * unit))
    drawCircle(boxColor, radius = 0.7f * unit, center = p(43f, 7.8f))
    drawRect(lineColor, p(43.5f, 5.6f), Size(1.2f * unit, 1.4f * unit))

    drawWheel(p(7f, 3f), 3f * unit, truck.wheelPhase, lineColor, hubColor, cabColor)
    drawWheel(p(36f, 3f), 3f * unit, truck.wheelPhase, lineColor, hubColor, cabColor)
}

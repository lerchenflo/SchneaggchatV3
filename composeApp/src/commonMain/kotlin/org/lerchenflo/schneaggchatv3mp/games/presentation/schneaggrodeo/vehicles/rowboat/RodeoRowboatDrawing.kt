package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rowboat

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.sin

// The rowboat's fixed colors: varnished wood, a painted stripe, and silver-orange fish
private val WOOD = Color(0xFFA0703E)
private val WOOD_DARK = Color(0xFF6D4C2A)
private val STRIPE = Color(0xFF1E88E5)
private val FISH_BODY = Color(0xFFFF8A3D)
private val FISH_BELLY = Color(0xFFFFE0B2)

/** The rowboat, facing right: [x] is its stern. */
@Immutable
data class RodeoRowboatUi(
    val x: Float,
    val rowPhase: Float,
    val fish: List<RodeoFishUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            RodeoLayer.BACK -> Unit
            // The far oar and the inside of the hull, under the horse
            RodeoLayer.BODY -> drawBoatInside(this@RodeoRowboatUi, context)
            // The near side of the hull covers the horse's legs; the fish jump in front
            RodeoLayer.FRONT -> {
                drawBoatSide(this@RodeoRowboatUi, context)
                fish.forEach { drawFish(it, context) }
            }
        }
    }
}

/** A fish jumping out of the water or dangling in the lasso; [x] / [y] its center. */
@Immutable
data class RodeoFishUi(val x: Float, val y: Float, val rotation: Float)

private fun DrawScope.drawBoatInside(boat: RodeoRowboatUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(boat.x + x, y)
    drawPath(polygonPath(::p, 1f to BOAT_GUNWALE, BOAT_LENGTH - 1f to BOAT_GUNWALE, BOAT_LENGTH - 3f to BOAT_FLOOR, 3f to BOAT_FLOOR), WOOD_DARK)
    // Far oar, dipping behind the boat
    val swing = sin(boat.rowPhase)
    drawLine(WOOD_DARK, p(20f, BOAT_GUNWALE + 0.5f), p(20f + 7f * swing, 2f + 1.5f * swing), 0.5f * unit, StrokeCap.Round)
}

/** Near side of the hull with its stripe, and the near oar. */
private fun DrawScope.drawBoatSide(boat: RodeoRowboatUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(boat.x + x, y)
    drawPath(
        polygonPath(::p, 0f to BOAT_GUNWALE, BOAT_LENGTH to BOAT_GUNWALE + 1.2f, BOAT_LENGTH - 4f to 1.5f, 3f to 1.5f),
        WOOD
    )
    drawLine(STRIPE, p(0.8f, BOAT_GUNWALE - 1.2f), p(BOAT_LENGTH - 0.6f, BOAT_GUNWALE), 0.7f * unit)
    listOf(8f, 16f, 24f).forEach { plankX -> drawLine(WOOD_DARK.copy(alpha = 0.5f), p(plankX, BOAT_GUNWALE - 0.2f), p(plankX, 2f), 0.15f * unit) }
    // Near oar in its oarlock, blade in the water
    val swing = sin(boat.rowPhase + 0.4f)
    val blade = p(18f - 8f * swing, 2.5f + 1.5f * swing)
    drawLine(WOOD_DARK, p(18f, BOAT_GUNWALE + 0.8f), blade, 0.5f * unit, StrokeCap.Round)
    drawOval(WOOD_DARK, blade - Offset(1.2f * unit, 0.5f * unit), Size(2.4f * unit, 1f * unit))
}

/** A fish: orange body with a light belly, tail fin and an eye. */
private fun DrawScope.drawFish(fish: RodeoFishUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(fish.x, fish.y)
    fun p(dx: Float, dy: Float) = center + Offset(dx * unit, -dy * unit)
    rotate(fish.rotation, pivot = center) {
        drawOval(FISH_BODY, p(-FISH_LENGTH / 2f, 0.7f), Size(FISH_LENGTH * unit, 1.4f * unit))
        drawOval(FISH_BELLY, p(-FISH_LENGTH / 2f + 0.4f, 0f), Size((FISH_LENGTH - 0.8f) * unit, 0.6f * unit))
        drawPath(polygonPath(::p, -FISH_LENGTH / 2f to 0f, -FISH_LENGTH / 2f - 1.2f to 0.8f, -FISH_LENGTH / 2f - 1.2f to -0.8f), FISH_BODY)
        drawCircle(Color.Black, radius = 0.15f * unit, center = p(FISH_LENGTH / 2f - 0.6f, 0.2f))
    }
}

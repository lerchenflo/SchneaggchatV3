package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pirateship

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.sin

// The pirate ship's fixed colors: dark and light planks, patched canvas, the black flag with its
// white skull, iron cannon, smoke and the treasure chests' gold
private val PLANK_DARK = Color(0xFF4E342E)
private val PLANK_LIGHT = Color(0xFF795548)
private val SAIL = Color(0xFFF5E6C8)
private val SAIL_PATCH = Color(0xFFD7C4A0)
private val FLAG_BLACK = Color(0xFF111111)
private val SKULL_WHITE = Color(0xFFFAFAFA)
private val IRON = Color(0xFF263238)
private val SMOKE = Color(0xFF9E9E9E)
private val CHEST_WOOD = Color(0xFF8D6E63)
private val GOLD = Color(0xFFFFC107)

/** A cannonball in flight. */
@Immutable
data class RodeoCannonballUi(val x: Float, val y: Float)

/** A treasure chest bobbing on the water; [seed] picks its tilt. */
@Immutable
data class RodeoTreasureChestUi(val x: Float, val y: Float, val seed: Int)

/** The pirate ship, facing right: [x] is its stern; [time] makes the sails and flag billow. */
@Immutable
data class RodeoPirateShipUi(
    val x: Float,
    val time: Float,
    /** 0..1 while the muzzle smoke clears, else null. */
    val muzzleSmoke: Float?,
    val cannonballs: List<RodeoCannonballUi>,
    val chests: List<RodeoTreasureChestUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            // Masts and sails behind the horse on the deck
            RodeoLayer.BACK -> drawRigging(this@RodeoPirateShipUi, context)
            RodeoLayer.BODY -> drawHull(this@RodeoPirateShipUi, context)
            RodeoLayer.FRONT -> {
                chests.forEach { drawTreasureChest(it, context) }
                cannonballs.forEach { ball ->
                    drawCircle(IRON, radius = 0.8f * context.unit, center = context.p(ball.x, ball.y))
                }
                muzzleSmoke?.let { progress ->
                    repeat(3) { index ->
                        val center = context.p(x + CANNON_X + 2f + index * 1.6f * (1f + progress), CANNON_Y + index * 0.8f * progress)
                        drawCircle(SMOKE.copy(alpha = 0.6f * (1f - progress)), radius = (1f + progress * 1.5f) * context.unit, center = center)
                    }
                }
            }
        }
    }
}

/** Two masts with billowing sails and the black flag with a skull on the main mast. */
private fun DrawScope.drawRigging(ship: RodeoPirateShipUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(ship.x + x, y)
    val billow = 1f + 0.4f * sin(ship.time * 2f)
    listOf(MAIN_MAST_X to MAST_TOP, FORE_MAST_X to MAST_TOP - 6f).forEach { (mastX, top) ->
        drawLine(PLANK_DARK, p(mastX, SHIP_DECK), p(mastX, top), 0.9f * unit)
        // Two square sails, one above the other, bulging forward in the wind
        listOf(top - 3f to top - 12f, top - 14f to top - 24f).forEach { (sailTop, sailBottom) ->
            drawLine(PLANK_DARK, p(mastX - 7f, sailTop), p(mastX + 7f, sailTop), 0.5f * unit)
            drawPath(
                polygonPath(
                    ::p,
                    mastX - 6.5f to sailTop, mastX + 6.5f to sailTop,
                    mastX + 6.5f + billow to (sailTop + sailBottom) / 2f,
                    mastX + 6.5f to sailBottom, mastX - 6.5f to sailBottom,
                    mastX - 6.5f + billow to (sailTop + sailBottom) / 2f,
                ),
                SAIL,
            )
            drawRect(SAIL_PATCH, p(mastX - 3f, sailTop - 2f), Size(2.5f * unit, 2f * unit))
        }
    }
    // Rope from the bow to the fore mast
    drawLine(PLANK_DARK.copy(alpha = 0.7f), p(SHIP_LENGTH + 2f, SHIP_DECK + 3f), p(FORE_MAST_X, MAST_TOP - 6f), 0.2f * unit)
    // Black flag flapping at the top of the main mast, a skull and crossed bones on it
    val flapA = 0.6f * sin(ship.time * 6f)
    val flapB = 0.6f * sin(ship.time * 6f + 1.5f)
    val flagLeft = MAIN_MAST_X + 0.4f
    drawPath(polygonPath(::p, flagLeft to MAST_TOP, flagLeft + 4f to MAST_TOP + flapA, flagLeft + 8f to MAST_TOP + flapB, flagLeft + 8f to MAST_TOP - 5f + flapB, flagLeft + 4f to MAST_TOP - 5f + flapA, flagLeft to MAST_TOP - 5f), FLAG_BLACK)
    val skull = p(flagLeft + 4f, MAST_TOP - 2f + flapA)
    drawLine(SKULL_WHITE, skull + Offset(-1.6f * unit, 1.6f * unit), skull + Offset(1.6f * unit, -0.4f * unit), 0.35f * unit, StrokeCap.Round)
    drawLine(SKULL_WHITE, skull + Offset(-1.6f * unit, -0.4f * unit), skull + Offset(1.6f * unit, 1.6f * unit), 0.35f * unit, StrokeCap.Round)
    drawCircle(SKULL_WHITE, radius = 1.1f * unit, center = skull)
    drawCircle(FLAG_BLACK, radius = 0.25f * unit, center = skull + Offset(-0.4f * unit, -0.1f * unit))
    drawCircle(FLAG_BLACK, radius = 0.25f * unit, center = skull + Offset(0.4f * unit, -0.1f * unit))
}

/** Planked hull with a raised stern castle, gun ports, railing and the bow cannon. */
private fun DrawScope.drawHull(ship: RodeoPirateShipUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(ship.x + x, y)
    drawPath(
        polygonPath(
            ::p,
            -1f to STERN_CASTLE_TOP, STERN_CASTLE_WIDTH to STERN_CASTLE_TOP, STERN_CASTLE_WIDTH to SHIP_DECK,
            SHIP_LENGTH + 4f to SHIP_DECK + 2f, SHIP_LENGTH - 4f to 0.5f, 3f to 0.5f,
        ),
        PLANK_DARK,
    )
    // Plank lines along the hull
    listOf(3f, 5.5f, 8f).forEach { y -> drawLine(PLANK_LIGHT, p(1f, y), p(SHIP_LENGTH - 1f, y + 0.3f), 0.25f * unit) }
    // Gun ports with cannon barrels poking out
    var port = 16f
    while (port < SHIP_LENGTH - 8f) {
        drawRect(IRON, p(port, 7.2f), Size(1.8f * unit, 1.6f * unit))
        drawLine(IRON, p(port + 0.9f, 6.4f), p(port + 0.9f, 5.6f), 0.7f * unit)
        port += 8f
    }
    // Stern castle windows and railing along the deck
    drawRect(GOLD.copy(alpha = 0.7f), p(2f, STERN_CASTLE_TOP - 1.5f), Size(2f * unit, 1.5f * unit))
    drawRect(GOLD.copy(alpha = 0.7f), p(6f, STERN_CASTLE_TOP - 1.5f), Size(2f * unit, 1.5f * unit))
    drawLine(PLANK_LIGHT, p(STERN_CASTLE_WIDTH, SHIP_DECK + 2.4f), p(SHIP_LENGTH + 2f, SHIP_DECK + 3.4f), 0.3f * unit)
    var post = STERN_CASTLE_WIDTH + 2f
    while (post < SHIP_LENGTH + 2f) {
        drawLine(PLANK_LIGHT, p(post, SHIP_DECK), p(post, SHIP_DECK + 2.4f + (post / SHIP_LENGTH)), 0.2f * unit)
        post += 3f
    }
    // Bow cannon on its carriage, pointing ahead
    drawRoundRect(IRON, p(CANNON_X - 3f, CANNON_Y + 0.8f), Size(5f * unit, 1.6f * unit), CornerRadius(0.8f * unit))
    drawRect(PLANK_LIGHT, p(CANNON_X - 2.6f, CANNON_Y - 0.6f), Size(3f * unit, 1.4f * unit))
}

/** A small wooden chest with a gold band and lock, coins glinting from under the lid. */
private fun DrawScope.drawTreasureChest(chest: RodeoTreasureChestUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(chest.x, chest.y)
    drawRect(CHEST_WOOD, center + Offset(-1.8f * unit, -0.6f * unit), Size(3.6f * unit, 2.2f * unit))
    drawRoundRect(CHEST_WOOD, center + Offset(-1.9f * unit, -1.8f * unit), Size(3.8f * unit, 1.4f * unit), CornerRadius(0.7f * unit))
    drawRect(GOLD, center + Offset(-1.9f * unit, -0.6f * unit), Size(3.8f * unit, 0.35f * unit))
    drawRect(GOLD, center + Offset(-0.3f * unit, -0.5f * unit), Size(0.6f * unit, 0.8f * unit))
    drawCircle(GOLD.copy(alpha = 0.5f + 0.5f * sin(chest.seed.toFloat())), radius = 0.3f * unit, center = center + Offset(1.2f * unit, -2f * unit))
}

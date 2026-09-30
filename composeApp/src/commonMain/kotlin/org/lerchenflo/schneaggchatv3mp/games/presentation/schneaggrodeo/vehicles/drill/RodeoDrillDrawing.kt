package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.drill

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.GOLD_NUGGET
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.GOLD_SHINE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.MINE_EARTH_LIGHT
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.MINE_ROCK
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.cloudNoise
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawCowboyHat
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWheel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi

// The drill machine's fixed colors: yellow paint, dark tracks, steel drill, the tunnel's darkness
private val DRILL_YELLOW = Color(0xFFFBC02D)
private val DRILL_YELLOW_DARK = Color(0xFFC49000)
private val TRACK_DARK = Color(0xFF263238)
private val DRILL_STEEL = Color(0xFFB0BEC5)
private val DRILL_STEEL_DARK = Color(0xFF607D8B)
private val TUNNEL_DARK = Color(0xFF1B120C)
private val EARTH_DEEP = Color(0xFF3E2723)
private val WINDOW = Color(0xFF81D4FA)

/** How deep the earth is drawn below the track. */
private const val EARTH_DEPTH = 45f
private const val TUNNEL_RADIUS = 5.2f * DRILL_SCALE

/** A piece of the tunnel dug behind the machine: its middle. */
@Immutable
data class RodeoTunnelUi(val x: Float, val y: Float)

/** A gold nugget or a hard rock in the earth; [y] is its middle, below the ground. */
@Immutable
data class RodeoDrillFindUi(val x: Float, val y: Float, val rock: Boolean, val seed: Int)

/** The drill machine, facing right: [x] is its rear, [y] the underside of its tracks. */
@Immutable
data class RodeoDrillUi(
    val x: Float,
    val y: Float,
    val drillPhase: Float,
    /** The cowboy's hat sticks out of the hatch while he drives. */
    val hatInHatch: Boolean,
    /** The earth below the track, the tunnel and what lies in it are drawn while digging. */
    val earthVisible: Boolean,
    val tunnel: List<RodeoTunnelUi>,
    val finds: List<RodeoDrillFindUi>,
    /** Shaking against a rock. */
    val shake: Float,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            RodeoLayer.BACK -> if (earthVisible) drawEarth(this@RodeoDrillUi, context)
            RodeoLayer.BODY -> drawMachine(this@RodeoDrillUi, context)
            RodeoLayer.FRONT -> Unit
        }
    }
}

/** The earth below the track with strata, the dug tunnel, nuggets and rocks. */
private fun DrawScope.drawEarth(drill: RodeoDrillUi, context: RodeoDrawContext) {
    val unit = context.unit
    val top = context.p(0f, 0f).y
    // Solid earth, darker than the mine's walls, with the track's edge on top
    drawRect(EARTH_DEEP, Offset(0f, top), Size(size.width, EARTH_DEPTH * unit))
    drawLine(MINE_EARTH_LIGHT, Offset(0f, top), Offset(size.width, top), 0.6f * unit)
    listOf(6f, 14f, 25f, 36f).forEachIndexed { index, depth ->
        val y = context.p(0f, -depth).y
        drawLine(MINE_EARTH_LIGHT, Offset(0f, y), Offset(size.width, y + (index % 2) * unit), 0.8f * unit)
    }
    // The tunnel stays open behind the machine
    clipRect(top = top) {
        drill.tunnel.forEach { piece ->
            drawCircle(TUNNEL_DARK, radius = TUNNEL_RADIUS * unit, center = context.p(piece.x, piece.y))
        }
    }
    drill.finds.forEach { find ->
        val center = context.p(find.x, find.y)
        fun q(dx: Float, dy: Float) = center + Offset(dx * unit, -dy * unit)
        if (find.rock) {
            val r = ROCK_RADIUS
            drawPath(polygonPath(::q, -r to -0.4f * r, -0.6f * r to 0.8f * r, 0.3f * r to r, r to 0.3f * r, 0.8f * r to -0.8f * r, -0.3f * r to -r), MINE_ROCK)
            drawLine(TRACK_DARK.copy(alpha = 0.5f), q(-0.3f * r, 0.4f * r), q(0.4f * r, -0.3f * r), 0.25f * unit)
        } else {
            val r = NUGGET_RADIUS
            drawCircle(GOLD_NUGGET.copy(alpha = 0.25f), radius = r * 2f * unit, center = center)
            drawPath(polygonPath(::q, -r to -0.3f * r, -0.5f * r to 0.8f * r, 0.6f * r to r, r to 0f, 0.3f * r to -r), GOLD_NUGGET)
            drawCircle(GOLD_SHINE, radius = 0.35f * unit, center = q(-0.2f * r, 0.3f * r + 0.2f * cloudNoise(find.seed, 1)))
        }
    }
}

/** Tracks, yellow body with a window and hatch (and the hat in it), exhaust pipe and the turning drill. */
private fun DrawScope.drawMachine(drill: RodeoDrillUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(drill.x + x + drill.shake, drill.y + y)
    scale(DRILL_SCALE, pivot = context.p(drill.x, drill.y)) { drawMachineGrid(drill, context) }
    // The hat in the hatch, at its normal size
    if (drill.hatInHatch) drawCowboyHat(::p, DRILL_HATCH_X * DRILL_SCALE - 3.2f, (DRILL_BODY_TOP + 1.6f) * DRILL_SCALE + 0.7f, unit)
}

/** The machine on its own grid, scaled up by the caller. */
private fun DrawScope.drawMachineGrid(drill: RodeoDrillUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(drill.x + x + drill.shake, drill.y + y)
    // Tracks with their wheels
    drawRoundRect(TRACK_DARK, p(0.5f, DRILL_TRACK_HEIGHT), Size((DRILL_BODY_LENGTH - 1f) * unit, DRILL_TRACK_HEIGHT * unit), CornerRadius(1.4f * unit))
    listOf(2f, 6.5f, 11f, 15.5f).forEach { wheelX ->
        drawWheel(p(wheelX, 1.4f), 1.1f * unit, drill.drillPhase * 0.3f, TRACK_DARK, DRILL_STEEL_DARK, DRILL_STEEL)
    }
    // Body with a slanted front, a window, stripes and the hatch on top
    drawPath(polygonPath(::p, 0f to DRILL_TRACK_HEIGHT, 0f to DRILL_BODY_TOP, DRILL_BODY_LENGTH - 3f to DRILL_BODY_TOP, DRILL_BODY_LENGTH to DRILL_BODY_TOP - 2f, DRILL_BODY_LENGTH to DRILL_TRACK_HEIGHT), DRILL_YELLOW)
    drawRoundRect(WINDOW, p(DRILL_BODY_LENGTH - 6f, DRILL_BODY_TOP - 1.2f), Size(3.2f * unit, 2.4f * unit), CornerRadius(0.4f * unit))
    var stripe = 1f
    while (stripe < DRILL_BODY_LENGTH - 7f) {
        drawLine(TRACK_DARK, p(stripe, DRILL_TRACK_HEIGHT + 0.8f), p(stripe + 1.4f, DRILL_TRACK_HEIGHT + 2.4f), 0.5f * unit)
        stripe += 2.4f
    }
    drawRoundRect(DRILL_YELLOW_DARK, p(DRILL_HATCH_X - 2.5f, DRILL_BODY_TOP + 0.7f), Size(5f * unit, 0.9f * unit), CornerRadius(0.4f * unit))
    // Exhaust pipe at the back
    drawLine(TRACK_DARK, p(2f, DRILL_BODY_TOP), p(2f, DRILL_BODY_TOP + 3.5f), 0.7f * unit)
    // The drill: a steel cone with spiral grooves running towards its tip
    val middle = (DRILL_TRACK_HEIGHT + DRILL_BODY_TOP) / 2f
    val half = (DRILL_BODY_TOP - DRILL_TRACK_HEIGHT) / 2f
    drawRect(DRILL_STEEL_DARK, p(DRILL_BODY_LENGTH - 0.3f, middle + half * 0.8f), Size(1.2f * unit, half * 1.6f * unit))
    val coneStart = DRILL_BODY_LENGTH + 0.9f
    val coneLength = DRILL_LENGTH - coneStart
    drawPath(polygonPath(::p, coneStart to middle + half * 0.8f, DRILL_LENGTH to middle, coneStart to middle - half * 0.8f), DRILL_STEEL)
    val groove = (drill.drillPhase % 1f + 1f) % 1f
    repeat(4) { index ->
        val share = (index + groove) / 4f
        val grooveX = coneStart + share * coneLength
        val grooveHalf = half * 0.8f * (1f - share)
        drawLine(DRILL_STEEL_DARK, p(grooveX, middle + grooveHalf), p(grooveX + 1.2f, middle - grooveHalf), 0.25f * unit, StrokeCap.Round)
    }
}

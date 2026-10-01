package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoBridgeUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.BRIDGE_LENGTH

// A gorge with a river at its bottom and a plank bridge on ropes across it (see engine/RodeoTerrain).
// The gorge is cut out of the ground; the bridge's planks lie on the ground's (sagging) line.

/** Depth of the gorge below its banks, and of the river at its bottom (units). */
private const val GORGE_DEPTH = 22f
private const val RIVER_DEPTH = 4f
/** Planks: one every PLANK_STEP units, PLANK_WIDTH long with a gap to the next. */
private const val PLANK_STEP = 2.3f
private const val PLANK_WIDTH = 1.9f
private const val PLANK_THICKNESS = 1.1f
/** The hand rope above the planks, and the posts it hangs from at both banks. */
private const val ROPE_HEIGHT = 5f
private const val POST_HEIGHT = 6f
private const val POST_WIDTH = 1.2f
/** Every that many planks a hanger ties the planks to the hand rope. */
private const val PLANKS_PER_HANGER = 2
private const val RIVER_WAVE_SPACING = 9f

/** The gorge's walls from bank to bank, [top] units above the banks at both ends (world units, x from the bridge's start). */
private fun gorgeOutline(top: Float) = listOf(
    0f to top, 0f to 0f, 2.5f to -4f, 5f to -7.5f, 9f to -12f, 12f to -GORGE_DEPTH,
    BRIDGE_LENGTH - 12f to -GORGE_DEPTH, BRIDGE_LENGTH - 9f to -13f, BRIDGE_LENGTH - 5.5f to -8f,
    BRIDGE_LENGTH - 2.5f to -3.5f, BRIDGE_LENGTH to 0f, BRIDGE_LENGTH to top,
)

/** The gorge of [bridge] on level ground at its banks. */
private fun gorgePath(bridge: RodeoBridgeUi, context: RodeoDrawContext, top: Float): Path {
    val level = context.leveled(context.ground(bridge.x))
    return polygonPath({ x, y -> level.p(bridge.x + x, y) }, *gorgeOutline(top).toTypedArray())
}

/**
 * Draws [ground] with the gorges of [bridges] cut out of it: the line over the gorge (the planks'
 * line) and the earth under it are left out.
 */
internal fun DrawScope.drawWithGorges(bridges: List<RodeoBridgeUi>, context: RodeoDrawContext, ground: DrawScope.() -> Unit) {
    if (bridges.isEmpty()) {
        ground()
        return
    }
    val holes = Path().apply { bridges.forEach { addPath(gorgePath(it, context, top = 2f)) } }
    clipPath(holes, ClipOp.Difference) { ground() }
}

/** The gorge under [bridge]: its shaded far wall, the river at the bottom and the rim. */
internal fun DrawScope.drawGorge(bridge: RodeoBridgeUi, context: RodeoDrawContext, distance: Float) {
    val unit = context.unit
    val line = context.colors.onSurfaceVariant
    val walls = gorgePath(bridge, context, top = 0f)
    drawPath(walls, line.copy(alpha = 0.12f))
    clipPath(walls) {
        // The river, with glints drifting along
        val level = context.leveled(context.ground(bridge.x))
        val surface = level.p(bridge.x, -GORGE_DEPTH + RIVER_DEPTH).y
        drawRect(SEA_WATER_LIGHT, Offset(bridge.x * unit, surface), Size(BRIDGE_LENGTH * unit, (RIVER_DEPTH + 1f) * unit))
        val shift = (distance * 0.4f) % RIVER_WAVE_SPACING
        var x = bridge.x + 10f - shift
        while (x < bridge.x + BRIDGE_LENGTH - 10f) {
            val glint = level.p(x, -GORGE_DEPTH + RIVER_DEPTH * 0.6f)
            drawLine(SNOW_COLOR.copy(alpha = 0.6f), glint, glint + Offset(2.5f * unit, 0f), 0.35f * unit)
            x += RIVER_WAVE_SPACING
        }
    }
    drawPath(walls, line, style = Stroke(width = 0.6f * unit, join = StrokeJoin.Round))
}

/** The bridge's posts at both banks, the planks along the ground's sagging line and the hand rope. */
internal fun DrawScope.drawBridge(bridge: RodeoBridgeUi, context: RodeoDrawContext) {
    val unit = context.unit
    val from = bridge.x
    val to = bridge.x + BRIDGE_LENGTH
    listOf(from - POST_WIDTH, to).forEach { postX ->
        drawRect(PIER_WOOD_DARK, context.p(postX, POST_HEIGHT), Size(POST_WIDTH * unit, (POST_HEIGHT + 3f) * unit))
    }
    // Hand rope from post to post, following the planks' sag
    val rope = Path().apply {
        val start = context.p(from - POST_WIDTH / 2f, POST_HEIGHT - 0.5f)
        moveTo(start.x, start.y)
        var x = from
        while (x <= to) {
            val point = context.p(x, ROPE_HEIGHT)
            lineTo(point.x, point.y)
            x += PLANK_STEP
        }
        val end = context.p(to + POST_WIDTH / 2f, POST_HEIGHT - 0.5f)
        lineTo(end.x, end.y)
    }
    drawPath(rope, BRIDGE_ROPE, style = Stroke(width = 0.35f * unit, join = StrokeJoin.Round))
    var index = 0
    var x = from
    while (x < to) {
        if (index % PLANKS_PER_HANGER == 0) {
            drawLine(BRIDGE_ROPE, context.p(x + 0.3f, 0f), context.p(x + 0.3f, ROPE_HEIGHT), 0.22f * unit)
        }
        val plankEnd = minOf(x + PLANK_WIDTH, to)
        drawLine(PIER_WOOD, context.p(x, -PLANK_THICKNESS / 2f), context.p(plankEnd, -PLANK_THICKNESS / 2f), PLANK_THICKNESS * unit)
        x += PLANK_STEP
        index++
    }
}

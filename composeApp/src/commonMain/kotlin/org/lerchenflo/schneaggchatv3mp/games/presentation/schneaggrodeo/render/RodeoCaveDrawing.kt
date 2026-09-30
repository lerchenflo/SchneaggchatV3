package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPortalUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.TERRAIN_STEP
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoMap
import kotlin.math.floor
import kotlin.math.sin

// The underground (see engine/RodeoUnderground): the cave backdrop, stalagmites standing in for the
// fences, crystals, and the portals between the maps - the mine shaft in the track and the shaft of
// light out of the cave. Colors come from the cave paint (RodeoPaint) plus a few fixed ones.

private const val STALACTITE_SPACING = 7f
private const val TORCH_SPACING = 90f
/** The backdrop drifts by slower than the track. */
private const val CAVE_PARALLAX = 0.6f

/** Rock wall, stalactites along the ceiling and torches with flickering light, fixed behind the track. */
internal fun DrawScope.drawCaveBackdrop(distance: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    drawRect(colors.surfaceContainer)
    val shift = distance * CAVE_PARALLAX
    val visibleWidth = size.width / unit

    // Stalactites hanging from the top edge
    val rock = colors.onSurface.copy(alpha = 0.25f)
    var index = floor(shift / STALACTITE_SPACING).toInt()
    var x = index * STALACTITE_SPACING - shift
    while (x < visibleWidth + STALACTITE_SPACING) {
        val length = 3f + 9f * cloudNoise(index, 1)
        val width = 2.5f + 3f * cloudNoise(index, 2)
        drawPath(
            Path().apply {
                moveTo((x - width / 2f) * unit, 0f)
                lineTo((x + width / 2f) * unit, 0f)
                lineTo((x + 0.2f) * unit, length * unit)
                close()
            },
            rock
        )
        x += STALACTITE_SPACING
        index++
    }

    // Torches on the wall, their light flickering
    var torch = floor(shift / TORCH_SPACING).toInt()
    var torchX = torch * TORCH_SPACING - shift
    while (torchX < visibleWidth + TORCH_SPACING) {
        val center = context.p(torchX + 20f, 34f)
        val flicker = 0.8f + 0.2f * sin(distance * 0.05f + torch * 2f)
        drawCircle(TORCH_COLOR.copy(alpha = 0.12f * flicker), radius = 9f * unit, center = center)
        drawLine(TREE_TRUNK, center + Offset(0f, 1f * unit), center + Offset(0f, 5f * unit), 0.8f * unit, StrokeCap.Round)
        drawCircle(TORCH_COLOR.copy(alpha = flicker), radius = 1.1f * unit, center = center)
        torchX += TORCH_SPACING
        torch++
    }
}

/**
 * A stalagmite in place of a fence: a rock spike up to the fence's height (two for a wide jump),
 * broken off to a stump once knocked. [label] (the height) is drawn below the ground.
 */
internal fun DrawScope.drawStalagmite(fence: RodeoFenceUi, context: RodeoDrawContext, label: TextLayoutResult) {
    val unit = context.unit
    val rock = context.colors.onSurface
    val shade = context.colors.onSurface.copy(alpha = 0.6f)
    val wide = fence.width > 12f
    val spikes = if (wide) listOf(0f to 0.55f, 0.45f to 0.55f) else listOf(0f to 1f)
    spikes.forEachIndexed { index, (start, share) ->
        val left = fence.x + fence.width * start
        val width = fence.width * share
        val top = if (fence.knocked) fence.top * 0.3f else fence.top * (if (wide && index == 0) 0.85f else 1f)
        fun p(dx: Float, y: Float) = context.p(left + dx, y)
        drawPath(polygonPath(::p, 0f to 0f, width * 0.3f to top * 0.6f, width * 0.5f to top, width * 0.65f to top * 0.55f, width to 0f), rock)
        // A lighter streak down its face
        drawPath(polygonPath(::p, width * 0.5f to top, width * 0.58f to top * 0.4f, width * 0.45f to 0f, width * 0.3f to top * 0.6f), shade)
    }
    if (fence.knocked) {
        // Rubble next to the stump
        repeat(3) { index ->
            drawCircle(shade, radius = (0.6f + 0.3f * index) * unit, center = context.p(fence.x + fence.width + 1f + index * 1.4f, 0.6f))
        }
    }

    drawHeightLabel(label, fence.x + fence.width / 2f, context)
}

/** A crystal: a faceted diamond with a bright facet and a glint. */
internal fun DrawScope.drawGem(gem: RodeoGemUi, context: RodeoDrawContext) {
    val unit = context.unit
    val color = CRYSTAL_COLORS[gem.hue % CRYSTAL_COLORS.size]
    val center = context.p(gem.x, gem.height)
    rotate(gem.tiltDeg, pivot = center) {
        fun p(dx: Float, dy: Float) = center + Offset(dx * unit, -dy * unit)
        drawCircle(color.copy(alpha = 0.18f), radius = 3.5f * unit, center = center)
        drawPath(polygonPath(::p, -2f to 0.8f, -1f to 2f, 1f to 2f, 2f to 0.8f, 0f to -2.5f), color)
        drawPath(polygonPath(::p, -1f to 2f, 0f to 0.8f, 0f to -2.5f, -2f to 0.8f), context.colors.onSurface.copy(alpha = 0.25f))
        drawLine(context.colors.onSurface, p(0.8f, 1.6f), p(1.3f, 1.1f), 0.25f * unit, StrokeCap.Round)
    }
}

/**
 * A way to another map: the shaft into the cave (a dark hole with glowing crystals round its rim),
 * the timber-framed mine shaft, the beach into the sea - or the way back: a ramp up to daylight out
 * of the cave and the mine, the harbour pier out of the sea.
 */
internal fun DrawScope.drawPortal(portal: RodeoPortalUi, context: RodeoDrawContext, distance: Float) {
    if (portal.exit) {
        when (portal.origin) {
            RodeoMap.SEA -> drawHarbourPier(portal.x + portal.width, context)
            else -> drawRampOut(portal.x + portal.width, context)
        }
        return
    }
    when (portal.destination) {
        RodeoMap.SEA -> drawBeach(portal.x, context, distance)
        RodeoMap.MINE -> drawMineEntrance(portal.x, portal.width, context)
        else -> drawCaveShaft(portal, context, distance)
    }
}

/** The shaft into the cave: a dark hole with crystals glowing round its rim and a few stones. */
private fun DrawScope.drawCaveShaft(portal: RodeoPortalUi, context: RodeoDrawContext, distance: Float) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(portal.x + dx, y)
    drawOval(context.colors.scrim, topLeft = p(0f, 1f), size = Size(portal.width * unit, 3.5f * unit))
    listOf(-1.5f to 0.8f, 2f to 1.4f, portal.width - 3f to 1.2f, portal.width + 1f to 0.9f).forEachIndexed { index, (dx, size) ->
        val glow = 0.6f + 0.4f * sin(distance * 0.08f + index * 1.9f)
        val center = p(dx, size)
        drawCircle(TORCH_COLOR.copy(alpha = 0.15f * glow), radius = 3f * unit, center = center)
        drawPath(
            polygonPath(::p, dx - size to 0f, dx to 3f * size, dx + size to 0f),
            CRYSTAL_COLORS[index % CRYSTAL_COLORS.size].copy(alpha = 0.6f + 0.4f * glow)
        )
    }
}

/** A plank ramp up the slope that ends at [top], with daylight pouring in at the top. */
private fun DrawScope.drawRampOut(top: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val bottom = top - 2f * TERRAIN_STEP
    // Daylight at the top
    drawCircle(TORCH_COLOR.copy(alpha = 0.15f), radius = 22f * unit, center = context.p(top + 6f, 14f))
    drawCircle(TORCH_COLOR.copy(alpha = 0.2f), radius = 12f * unit, center = context.p(top + 6f, 12f))
    // Planks along the slope and a rail beside them
    drawGroundStrip(context, from = bottom, to = top + 20f, y = 0.4f, MINE_TIMBER, width = 1f, dash = 1.6f)
    var x = bottom
    while (x <= top) {
        drawLine(MINE_TIMBER, context.p(x, 0f), context.p(x, 6f), 0.4f * unit)
        drawLine(MINE_TIMBER, context.p(x, 6f), context.p(x + 10f, 6f), 0.4f * unit)
        x += 10f
    }
    // Timber frame of the way out at the top
    listOf(top, top + 16f).forEach { postX -> drawRect(MINE_TIMBER, context.p(postX, 20f), Size(1.4f * unit, 20f * unit)) }
    drawRect(MINE_TIMBER, context.p(top - 1f, 21.5f), Size(19.4f * unit, 1.8f * unit))
}

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPortalUi
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

    val labelX = (fence.x + fence.width / 2f) * unit - label.size.width / 2f
    if (labelX + label.size.width > 0f && labelX < size.width) {
        drawText(label, topLeft = Offset(labelX, context.groundY + 2f * unit))
    }
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
 * A way to the other map: an entrance in the track - the shaft into the cave (a dark hole under a
 * wooden frame with a lantern), the well into the sea, the timber-framed mine shaft - or the shaft
 * of light falling in from above that leads back up.
 */
internal fun DrawScope.drawPortal(portal: RodeoPortalUi, context: RodeoDrawContext, distance: Float) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(portal.x + dx, y)
    if (portal.exit) {
        // Light pouring down from the top edge, widening towards the ground
        drawPath(
            polygonPath(::p, portal.width * 0.3f to 80f, portal.width * 0.7f to 80f, portal.width + 4f to 0f, -4f to 0f),
            TORCH_COLOR.copy(alpha = 0.22f)
        )
        drawPath(
            polygonPath(::p, portal.width * 0.42f to 80f, portal.width * 0.58f to 80f, portal.width * 0.8f to 0f, portal.width * 0.2f to 0f),
            TORCH_COLOR.copy(alpha = 0.25f)
        )
        return
    }
    when (portal.destination) {
        RodeoMap.SEA -> return drawSeaEntrance(portal.x, portal.width, context, distance)
        RodeoMap.MINE -> return drawMineEntrance(portal.x, portal.width, context)
        else -> Unit
    }
    // The hole, reaching a bit below the ground line
    drawOval(context.colors.scrim, topLeft = p(0f, 1f), size = Size(portal.width * unit, 3.5f * unit))
    // Wooden frame over it: two posts, a beam and a lantern
    listOf(0f, portal.width - 1.2f).forEach { postX ->
        drawRect(TREE_TRUNK, p(postX, 16f), Size(1.2f * unit, 16f * unit))
    }
    drawRect(TREE_TRUNK, p(-1f, 17.5f), Size((portal.width + 2f) * unit, 1.5f * unit))
    drawLine(TREE_TRUNK, p(portal.width / 2f, 16f), p(portal.width / 2f, 14f), 0.3f * unit)
    drawCircle(TORCH_COLOR, radius = 0.9f * unit, center = p(portal.width / 2f, 13.2f))
    drawCircle(TORCH_COLOR.copy(alpha = 0.2f), radius = 3f * unit, center = p(portal.width / 2f, 13.2f))
}

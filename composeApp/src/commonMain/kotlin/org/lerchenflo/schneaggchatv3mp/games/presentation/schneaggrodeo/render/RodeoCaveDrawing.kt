package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextLayoutResult
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMapWayUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMoundUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.TERRAIN_STEP
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// The cave (see engine/RodeoMapSwitch): its backdrop, stalagmites standing in for the
// fences, crystals, dirt mounds with a shovel, and the ways between the maps - the gold mine on the
// surface and the ramps back up.
// Colors come from the cave paint (RodeoPaint) plus a few fixed ones.

private const val STALACTITE_SPACING = 7f
/** The gold mine stands back behind the track: a little smaller, and its foot a bit higher up. */
private const val MINE_SCALE = 0.8f
private const val MINE_BACK = 2f
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

/** A dirt mound to dig through, with a shovel stuck in it. */
internal fun DrawScope.drawMound(mound: RodeoMoundUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(mound.x + dx, y)
    // The shovel stuck in the top, or dangling from the lasso
    if (mound.hasShovel) {
        drawShovel(context, mound.x, 0f, -15f + 30f * cloudNoise(mound.seed, 1))
    } else {
        mound.shovelX?.let { x -> drawShovel(context, x - 0.5f, mound.shovelY - 6f, 160f) }
    }
    drawArc(EARTH_LIGHT, 180f, 180f, useCenter = true, topLeft = p(-4f, 3.4f), size = Size(8f * unit, 6.8f * unit))
    repeat(4) { index ->
        drawCircle(EARTH_DARK, radius = (0.3f + 0.2f * cloudNoise(mound.seed, index)) * unit, center = p(-2.5f + index * 1.6f, 0.6f + 1.8f * cloudNoise(mound.seed, index + 5)))
    }
}

/** A shovel with its blade at ([x], [y]) + (0.5, 1) world units, tilted by [tilt] degrees around its blade. */
private fun DrawScope.drawShovel(context: RodeoDrawContext, x: Float, y: Float, tilt: Float) {
    val unit = context.unit
    fun p(dx: Float, dy: Float) = context.p(x + dx, y + dy)
    rotate(tilt, pivot = p(0.5f, 3f)) {
        drawLine(MINE_TIMBER, p(0.5f, 3f), p(0.5f, 9f), 0.4f * unit, StrokeCap.Round)
        drawLine(MINE_TIMBER, p(-0.4f, 9f), p(1.4f, 9f), 0.4f * unit, StrokeCap.Round)
        drawPath(polygonPath(::p, -0.4f to 3.4f, 1.4f to 3.4f, 1.2f to 1.6f, 0.5f to 1f, -0.2f to 1.6f), SHOVEL_STEEL)
    }
}

/**
 * A way to another map: the mine entrance into the cave, the beach into the sea - or the way back: a ramp up to daylight out
 * of the cave, the harbour pier out of the sea.
 */
internal fun DrawScope.drawMapWay(way: RodeoMapWayUi, context: RodeoDrawContext, distance: Float) {
    if (way.exit) {
        when (way.origin) {
            RodeoMap.SEA -> drawHarbourPier(way.x + way.width, context)
            RodeoMap.RAINBOW -> drawRainbowLanding(way.x, way.width, context)
            else -> drawRampOut(way.x + way.width, context)
        }
        return
    }
    when (way.destination) {
        RodeoMap.SEA -> drawBeach(way.x, context, distance)
        else -> drawMineEntrance(way, context, distance)
    }
}

/**
 * The gold mine the way into the cave starts from, standing back behind the track (a little smaller,
 * the track passes in front), in the style of a Clash of Clans gold mine: a stone base with a chunky
 * timber-framed tunnel mouth under a little shingle roof, a hoist with a big rope wheel on top, heaps
 * of gold beside it, a lantern and a stub of rails the mine cart comes out on.
 */
internal fun DrawScope.drawMineEntrance(way: RodeoMapWayUi, context: RodeoDrawContext, distance: Float) {
    val mouth = way.x + way.width
    scale(MINE_SCALE, pivot = context.p(mouth + 5f, MINE_BACK)) { drawGoldMine(mouth, context, distance) }
}

private fun DrawScope.drawGoldMine(mouth: Float, context: RodeoDrawContext, distance: Float) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(mouth + dx, y + MINE_BACK)
    val stone = SHOVEL_STEEL
    val stoneDark = SHOVEL_STEEL.copy(alpha = 0.6f)
    val beam = MINE_TIMBER
    val beamDark = EARTH_DARK
    // Stone base: big round boulders piled round the mouth
    listOf(-6f to 3.5f, -2f to 5f, 4f to 6f, 10f to 5.5f, 15f to 4f, 0f to 8f, 8f to 8.5f).forEach { (dx, radius) ->
        drawCircle(stoneDark, radius = (radius + 0.4f) * unit, center = p(dx, radius * 0.8f))
        drawCircle(stone, radius = radius * unit, center = p(dx, radius * 0.8f))
    }
    // Hoist on top: two beams up to a big rope wheel, the rope running down into the shaft
    val wheelCenter = p(5f, 27f)
    drawLine(beamDark, p(-1f, 14f), wheelCenter, 1.3f * unit, StrokeCap.Round)
    drawLine(beamDark, p(11f, 14f), wheelCenter, 1.3f * unit, StrokeCap.Round)
    drawLine(beam, p(1.5f, 19f), p(8.5f, 19f), 0.8f * unit, StrokeCap.Round)
    val turn = distance * 0.05f
    drawCircle(beam, radius = 4.2f * unit, center = wheelCenter, style = Stroke(width = 0.9f * unit))
    repeat(4) { index ->
        val angle = turn + index * PI.toFloat() / 4f
        val spoke = Offset(cos(angle), sin(angle)) * (4.2f * unit)
        drawLine(beam, wheelCenter - spoke, wheelCenter + spoke, 0.45f * unit)
    }
    drawCircle(beamDark, radius = 0.9f * unit, center = wheelCenter)
    drawLine(context.colors.onSurface.copy(alpha = 0.7f), p(9.2f, 27f), p(9.2f, 13f), 0.3f * unit)
    // Shingle roof over the mouth
    drawPath(polygonPath(::p, -4f to 13.5f, 5f to 19f, 14f to 13.5f), beamDark)
    repeat(3) { row ->
        val y = 14.2f + row * 1.5f
        val half = 8.2f - row * 2.4f
        drawLine(beam, p(5f - half, y), p(5f + half, y), 0.5f * unit, StrokeCap.Round)
    }
    // The dark tunnel mouth in a chunky timber frame with corner braces
    drawRect(context.colors.scrim, p(-0.5f, 11f), Size(11f * unit, 11f * unit))
    drawLine(beam, p(-1.5f, 0f), p(-1.5f, 12.5f), 1.8f * unit)
    drawLine(beam, p(11.5f, 0f), p(11.5f, 12.5f), 1.8f * unit)
    drawLine(beam, p(-3.5f, 12.5f), p(13.5f, 12.5f), 2f * unit, StrokeCap.Round)
    drawLine(beamDark, p(-0.6f, 9.5f), p(1.8f, 11.8f), 0.7f * unit, StrokeCap.Round)
    drawLine(beamDark, p(10.6f, 9.5f), p(8.2f, 11.8f), 0.7f * unit, StrokeCap.Round)
    listOf(-1.5f, 11.5f).forEach { postX ->
        drawCircle(beamDark, radius = 0.35f * unit, center = p(postX, 12.5f))
        drawCircle(beamDark, radius = 0.35f * unit, center = p(postX, 6f))
    }
    // A lantern hanging from the beam
    val flicker = 0.75f + 0.25f * sin(distance * 0.3f)
    drawLine(beamDark, p(0.8f, 11.5f), p(0.8f, 9.6f), 0.2f * unit)
    drawCircle(TORCH_COLOR.copy(alpha = 0.25f * flicker), radius = 3.5f * unit, center = p(0.8f, 9f))
    drawCircle(TORCH_COLOR, radius = 0.7f * unit, center = p(0.8f, 9f))
    // Heaps of gold beside the mouth, glinting
    listOf(-8f, 17f).forEachIndexed { heap, heapX ->
        repeat(7) { index ->
            val nx = heapX - 2.4f + (index % 4) * 1.6f + (index / 4) * 0.8f
            val ny = 0.7f + (index / 4) * 1.3f
            drawCircle(LEVEL_GOLD_COLOR, radius = (0.7f + 0.25f * cloudNoise(index, heap + 61)) * unit, center = p(nx, ny))
        }
        val glint = 0.5f + 0.5f * sin(distance * 0.2f + heap * 2f)
        drawCircle(SNOW_COLOR.copy(alpha = 0.7f * glint), radius = 0.3f * unit, center = p(heapX, 2.6f))
    }
    // A stub of rails out of the tunnel, for the mine cart
    var sleeper = -14f
    while (sleeper < 8f) {
        drawRect(beam, p(sleeper, 0.6f), Size(1.4f * unit, 0.8f * unit))
        sleeper += 4f
    }
    drawLine(SHOVEL_STEEL, p(-14f, 0.5f), p(9f, 0.5f), 0.35f * unit)
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

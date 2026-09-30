package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMoundUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSnailUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SNAIL_SIZE
import kotlin.math.floor
import kotlin.math.sin

// The mine (see engine/RodeoUnderground): earth walls with timber frames and lanterns, stacks of
// crates and rock piles instead of fences, gold nuggets instead of crystals, dirt mounds with a
// shovel stuck in them to dig through, and every snail wearing a miner's helmet.

private const val FRAME_SPACING = 60f
private const val MINE_PARALLAX = 0.7f
private const val CRATE_SIZE = 3.2f

/** Earth wall with strata, timber frames holding up the ceiling and lanterns hanging from them. */
internal fun DrawScope.drawMineBackdrop(distance: Float, context: RodeoDrawContext) {
    val unit = context.unit
    drawRect(MINE_EARTH)
    // Strata across the wall
    listOf(0.2f, 0.45f, 0.7f).forEachIndexed { index, share ->
        val y = size.height * share
        drawLine(MINE_EARTH_LIGHT, Offset(0f, y), Offset(size.width, y + 3f * unit * sin(index + distance * 0.001f)), 1.2f * unit)
    }
    val shift = distance * MINE_PARALLAX
    var index = floor(shift / FRAME_SPACING).toInt()
    var x = index * FRAME_SPACING - shift
    val visibleWidth = size.width / unit
    while (x < visibleWidth + FRAME_SPACING) {
        // Two posts and a beam across the top
        val top = context.p(x, 48f).y
        drawLine(MINE_TIMBER, Offset(x * unit, top), Offset(x * unit, size.height), 1.4f * unit)
        drawLine(MINE_TIMBER, Offset((x + 30f) * unit, top), Offset((x + 30f) * unit, size.height), 1.4f * unit)
        drawLine(MINE_TIMBER, Offset((x - 1f) * unit, top), Offset((x + 31f) * unit, top), 1.6f * unit)
        // Lantern hanging from the beam, flickering
        val lantern = context.p(x + 15f, 44f)
        val flicker = 0.8f + 0.2f * sin(distance * 0.05f + index * 1.7f)
        drawLine(MINE_TIMBER, Offset(lantern.x, top), lantern, 0.2f * unit)
        drawCircle(TORCH_COLOR.copy(alpha = 0.15f * flicker), radius = 8f * unit, center = lantern)
        drawCircle(TORCH_COLOR.copy(alpha = flicker), radius = 0.9f * unit, center = lantern)
        // Ore glinting in the wall
        repeat(3) { ore ->
            drawCircle(
                GOLD_NUGGET.copy(alpha = 0.5f),
                radius = 0.3f * unit,
                center = context.p(x + 5f + 50f * cloudNoise(index, 60 + ore), 15f + 30f * cloudNoise(index, 63 + ore))
            )
        }
        x += FRAME_SPACING
        index++
    }
}

/**
 * Instead of a fence: a stack of wooden crates up to its height (a wide one also gets a rock pile),
 * broken into planks once knocked. [label] (the height) is drawn below the ground.
 */
internal fun DrawScope.drawCrateStack(fence: RodeoFenceUi, context: RodeoDrawContext, label: TextLayoutResult) {
    val unit = context.unit
    val wide = fence.width > 12f
    val stackX = fence.x + fence.width / 2f - CRATE_SIZE / 2f + if (wide) 3f else 0f
    if (fence.knocked) {
        repeat(4) { index ->
            rotate(20f + index * 35f, pivot = context.p(stackX + index * 1.4f, 0.6f)) {
                drawRect(MINE_TIMBER, context.p(stackX + index * 1.4f - 1.2f, 0.9f), Size(2.4f * unit, 0.6f * unit))
            }
        }
    } else {
        var bottom = 0f
        var row = 0
        while (bottom < fence.top - 0.2f) {
            val height = minOf(CRATE_SIZE, fence.top - bottom)
            val crateX = stackX + if (row % 2 == 0) 0f else 0.3f
            drawRect(CRATE_WOOD_LIGHT, context.p(crateX, bottom + height), Size(CRATE_SIZE * unit, height * unit))
            drawRect(MINE_TIMBER, context.p(crateX, bottom + height), Size(CRATE_SIZE * unit, height * unit), style = Stroke(width = 0.25f * unit))
            drawLine(MINE_TIMBER, context.p(crateX, bottom), context.p(crateX + CRATE_SIZE, bottom + height), 0.2f * unit)
            bottom += height
            row++
        }
    }
    if (wide) {
        // A rock pile in front of the crates
        val pileX = fence.x + 1f
        listOf(0f to 1.4f, 1.8f to 1.8f, 3.4f to 1.2f, 1.2f to 2.8f).forEach { (dx, radius) ->
            drawCircle(MINE_ROCK, radius = radius * unit, center = context.p(pileX + dx, radius * (if (fence.knocked) 0.5f else 0.9f)))
        }
    }
    drawHeightLabel(label, fence.x + fence.width / 2f, context)
}

/** A yellow miner's helmet with a lamp, on top of a snail. */
internal fun DrawScope.drawMinerHelmet(snail: RodeoSnailUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(snail.x, snail.height + SNAIL_SIZE * 0.78f)
    val direction = if (snail.facingLeft) -1f else 1f
    rotate(snail.tiltDeg, pivot = context.p(snail.x, snail.height)) {
        drawArc(HELMET_YELLOW, 180f, 180f, useCenter = true, topLeft = center - Offset(1.6f * unit, 1.2f * unit), size = Size(3.2f * unit, 2.4f * unit))
        drawLine(HELMET_YELLOW, center + Offset(-2f * unit, 0f), center + Offset(2f * unit, 0f), 0.35f * unit, StrokeCap.Round)
        val lamp = center + Offset(direction * 1.3f * unit, -0.6f * unit)
        drawCircle(GOLD_SHINE, radius = 0.4f * unit, center = lamp)
        drawCircle(GOLD_SHINE.copy(alpha = 0.2f), radius = 1.4f * unit, center = lamp + Offset(direction * 1f * unit, 0f))
    }
}

/** A gold nugget instead of a crystal: a lumpy golden rock with a shine. */
internal fun DrawScope.drawNugget(gem: RodeoGemUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(gem.x, gem.height)
    fun p(dx: Float, dy: Float) = center + Offset(dx * unit, -dy * unit)
    rotate(gem.tiltDeg, pivot = center) {
        drawCircle(GOLD_NUGGET.copy(alpha = 0.2f), radius = 3f * unit, center = center)
        drawPath(polygonPath(::p, -1.8f to -0.6f, -1.2f to 1f, 0.2f to 1.5f, 1.6f to 0.8f, 1.9f to -0.5f, 0.4f to -1.3f), GOLD_NUGGET)
        drawCircle(GOLD_SHINE, radius = 0.45f * unit, center = p(-0.4f, 0.6f))
    }
}

/** A dirt mound to dig through, with a shovel stuck in it. */
internal fun DrawScope.drawMound(mound: RodeoMoundUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(mound.x + dx, y)
    // The shovel stuck in the top
    val tilt = -15f + 30f * cloudNoise(mound.seed, 1)
    rotate(tilt, pivot = p(0.5f, 3f)) {
        drawLine(MINE_TIMBER, p(0.5f, 3f), p(0.5f, 9f), 0.4f * unit, StrokeCap.Round)
        drawLine(MINE_TIMBER, p(-0.4f, 9f), p(1.4f, 9f), 0.4f * unit, StrokeCap.Round)
        drawPath(polygonPath(::p, -0.4f to 3.4f, 1.4f to 3.4f, 1.2f to 1.6f, 0.5f to 1f, -0.2f to 1.6f), SHOVEL_STEEL)
    }
    drawArc(MINE_EARTH_LIGHT, 180f, 180f, useCenter = true, topLeft = p(-4f, 3.4f), size = Size(8f * unit, 6.8f * unit))
    repeat(4) { index ->
        drawCircle(MINE_EARTH, radius = (0.3f + 0.2f * cloudNoise(mound.seed, index)) * unit, center = p(-2.5f + index * 1.6f, 0.6f + 1.8f * cloudNoise(mound.seed, index + 5)))
    }
}

/** The way down into the mine: a timber-framed shaft with rails running into the dark. */
internal fun DrawScope.drawMineEntrance(x: Float, width: Float, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(x + dx, y)
    drawOval(context.colors.scrim, p(0f, 1f), Size(width * unit, 3.5f * unit))
    // Rails leading in
    drawLine(SHOVEL_STEEL, p(-6f, 0.4f), p(width / 2f, -0.6f), 0.3f * unit)
    listOf(-5f, -3f, -1f, 1f).forEach { sleeper -> drawRect(MINE_TIMBER, p(sleeper, 0.8f), Size(1f * unit, 0.6f * unit)) }
    // Timber frame with crossed pickaxes on the beam
    listOf(0f, width - 1.4f).forEach { postX -> drawRect(MINE_TIMBER, p(postX, 16f), Size(1.4f * unit, 16f * unit)) }
    drawRect(MINE_TIMBER, p(-1f, 17.5f), Size((width + 2f) * unit, 1.8f * unit))
    val middle = p(width / 2f, 16.6f)
    drawLine(SHOVEL_STEEL, middle + Offset(-1.6f * unit, -1.6f * unit), middle + Offset(1.6f * unit, 1.6f * unit), 0.4f * unit, StrokeCap.Round)
    drawLine(SHOVEL_STEEL, middle + Offset(1.6f * unit, -1.6f * unit), middle + Offset(-1.6f * unit, 1.6f * unit), 0.4f * unit, StrokeCap.Round)
}

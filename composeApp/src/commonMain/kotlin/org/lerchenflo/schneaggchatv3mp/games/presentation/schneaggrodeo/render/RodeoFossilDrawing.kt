package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// The fossil layer deep below the cave (see engine/RodeoMapSwitch): rock in layers with fossils in
// it, dinosaur bones standing in for the fences, and ammonites to grab. Colors come from the cave
// paint (RodeoPaint), so it is dark in a light theme like the cave.

private const val FOSSIL_SPACING = 48f
/** The backdrop drifts by slower than the track. */
private const val FOSSIL_PARALLAX = 0.6f
private const val BONE_WIDTH = 1.1f
/** Layers of rock across the wall: height and thickness. */
private val ROCK_LAYERS = listOf(16f to 5f, 30f to 7f, 48f to 4f)

/** Rock in layers with ammonites and bones in it, fixed behind the track. */
internal fun DrawScope.drawFossilBackdrop(distance: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    drawRect(colors.surfaceContainer)
    // Layers of rock across the wall
    val layer = colors.onSurface.copy(alpha = 0.07f)
    ROCK_LAYERS.forEach { (height, thickness) ->
        drawRect(layer, Offset(0f, context.p(0f, height).y), Size(size.width, thickness * unit))
    }
    // Fossils in the rock
    val fossil = colors.onSurface.copy(alpha = 0.2f)
    val shift = distance * FOSSIL_PARALLAX
    var index = floor(shift / FOSSIL_SPACING).toInt()
    var x = index * FOSSIL_SPACING - shift
    while (x < size.width / unit + FOSSIL_SPACING) {
        val height = 22f + 30f * cloudNoise(index, 1)
        if (index % 2 == 0) {
            drawAmmoniteShape(context.p(x, height), (2.5f + 2f * cloudNoise(index, 2)) * unit, fossil, 0.4f * unit)
        } else {
            drawBone(context.p(x - 3f, height), context.p(x + 3f, height + 1.5f), fossil, unit)
        }
        x += FOSSIL_SPACING
        index++
    }
}

/**
 * Dinosaur bones in place of a fence: an upright bone at every pole and one across the top (rib
 * arches over a wide jump), lying scattered on the ground once knocked. [label] (the height) is
 * drawn below the ground.
 */
internal fun DrawScope.drawBoneFence(fence: RodeoFenceUi, context: RodeoDrawContext, label: TextLayoutResult) {
    val unit = context.unit
    val bone = context.colors.onSurface
    if (fence.knocked) {
        drawBone(context.p(fence.x - 1f, 0.6f), context.p(fence.x + fence.width * 0.6f, 1f), bone, unit)
        drawBone(context.p(fence.x + fence.width * 0.4f, 0.6f), context.p(fence.x + fence.width + 1f, 0.8f), bone, unit)
    } else {
        val top = fence.top
        drawBone(context.p(fence.x, 0.6f), context.p(fence.x, top), bone, unit)
        drawBone(context.p(fence.x + fence.width, 0.6f), context.p(fence.x + fence.width, top), bone, unit)
        drawBone(context.p(fence.x - 0.6f, top), context.p(fence.x + fence.width + 0.6f, top), bone, unit)
        if (fence.width > 12f) {
            // Ribs between the two uprights
            val rib = bone.copy(alpha = 0.7f)
            listOf(0.33f, 0.66f).forEach { share ->
                val ribX = fence.x + fence.width * share
                drawLine(rib, context.p(ribX, top), context.p(ribX + 1.2f, top * 0.35f), BONE_WIDTH * 0.7f * unit, StrokeCap.Round)
            }
        }
    }
    drawHeightLabel(label, fence.x + fence.width / 2f, context)
}

/** An ammonite to grab: a glowing spiral shell, turning slowly. */
internal fun DrawScope.drawAmmonite(gem: RodeoGemUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(gem.x, gem.height)
    val color = context.colors.secondary
    drawCircle(color.copy(alpha = 0.2f), radius = 3.5f * unit, center = center)
    drawCircle(context.colors.surfaceContainer, radius = 2.2f * unit, center = center)
    drawAmmoniteShape(center, 2.2f * unit, color, 0.35f * unit, turn = gem.tiltDeg)
}

/** A bone from [start] to [end]: a shaft with knobbly ends. */
private fun DrawScope.drawBone(start: Offset, end: Offset, color: Color, unit: Float) {
    drawLine(color, start, end, BONE_WIDTH * unit, StrokeCap.Round)
    val side = Offset(0f, 0.5f * unit)
    drawCircle(color, radius = 0.75f * unit, center = start - side)
    drawCircle(color, radius = 0.75f * unit, center = start + side)
    drawCircle(color, radius = 0.75f * unit, center = end - side)
    drawCircle(color, radius = 0.75f * unit, center = end + side)
}

/** An ammonite's spiral of [radius] around [center]: shrinking half circles, turned by [turn] degrees. */
private fun DrawScope.drawAmmoniteShape(center: Offset, radius: Float, color: Color, width: Float, turn: Float = 0f) {
    var r = radius
    var angle = turn
    var middle = center
    repeat(5) {
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = middle - Offset(r, r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(width = width, cap = StrokeCap.Round),
        )
        // The next half circle is smaller and shifted, so the arcs join into a spiral
        val next = r * 0.62f
        val radians = angle * PI.toFloat() / 180f
        middle += Offset(cos(radians) * (r - next), sin(radians) * (r - next))
        r = next
        angle += 180f
    }
}

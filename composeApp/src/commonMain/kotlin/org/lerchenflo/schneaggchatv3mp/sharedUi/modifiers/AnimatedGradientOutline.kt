package org.lerchenflo.schneaggchatv3mp.sharedUi.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Outline in [shape] whose [colors] slide around it endlessly, e.g. to make a highlighted item
 * stand out. The animation runs inside the modifier and only redraws, it never recomposes the
 * content. The colors loop: the first one is repeated at the end when needed, so there is no jump.
 */
fun Modifier.animatedGradientOutline(
    colors: List<Color>,
    shape: Shape = RoundedCornerShape(12.dp),
    width: Dp = 2.dp,
    cycleMillis: Int = 3_000,
): Modifier = this then AnimatedGradientOutlineElement(
    colors = if (colors.isEmpty() || colors.first() == colors.last()) colors else colors + colors.first(),
    shape = shape,
    width = width,
    cycleMillis = cycleMillis,
)

object AnimatedGradientOutlineDefaults {

    /** Primary, tertiary and secondary of the current theme, so the outline follows light, dark and custom themes. */
    @Composable
    fun themeColors(): List<Color> = with(MaterialTheme.colorScheme) {
        listOf(primary, tertiary, secondary)
    }
}

private data class AnimatedGradientOutlineElement(
    val colors: List<Color>,
    val shape: Shape,
    val width: Dp,
    val cycleMillis: Int,
) : ModifierNodeElement<AnimatedGradientOutlineNode>() {

    override fun create() = AnimatedGradientOutlineNode(colors, shape, width, cycleMillis)

    override fun update(node: AnimatedGradientOutlineNode) {
        node.update(colors, shape, width, cycleMillis)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "animatedGradientOutline"
        properties["colors"] = colors
        properties["shape"] = shape
        properties["width"] = width
        properties["cycleMillis"] = cycleMillis
    }
}

private class AnimatedGradientOutlineNode(
    private var colors: List<Color>,
    private var shape: Shape,
    private var width: Dp,
    private var cycleMillis: Int,
) : Modifier.Node(), DrawModifierNode {

    /** 0 → 1 over one cycle, then again from 0. */
    private val phase = Animatable(0f)
    private var animationJob: Job? = null

    // The outline only changes with the size, so a frame only builds the gradient
    private var cachedOutline: Outline? = null
    private var cachedSize = Size.Unspecified
    private var cachedLayoutDirection: LayoutDirection? = null

    override fun onAttach() = startAnimation()

    // coroutineScope is cancelled on detach, which stops the animation
    override fun onDetach() {
        animationJob = null
    }

    fun update(colors: List<Color>, shape: Shape, width: Dp, cycleMillis: Int) {
        if (shape != this.shape || width != this.width) cachedOutline = null
        val restart = cycleMillis != this.cycleMillis
        this.colors = colors
        this.shape = shape
        this.width = width
        this.cycleMillis = cycleMillis
        if (restart && isAttached) startAnimation()
        invalidateDraw()
    }

    private fun startAnimation() {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            phase.snapTo(0f)
            phase.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = cycleMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            ) { invalidateDraw() }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (colors.size < 2) return

        val strokePx = width.toPx()
        // Drawn inside the bounds: the stroke is centered on the outline, so inset it by half
        val outlineSize = Size(size.width - strokePx, size.height - strokePx)
        if (outlineSize.width <= 0f || outlineSize.height <= 0f) return

        val outline = cachedOutline
            ?.takeIf { cachedSize == outlineSize && cachedLayoutDirection == layoutDirection }
            ?: shape.createOutline(outlineSize, layoutDirection, this).also {
                cachedOutline = it
                cachedSize = outlineSize
                cachedLayoutDirection = layoutDirection
            }

        // One gradient tile spans the diagonal. Moving the start by exactly one tile per cycle
        // lands on the same picture again, so the restart at phase 1 → 0 doesn't jump.
        val tile = Offset(size.width, size.height)
        val start = tile * phase.value
        val brush = Brush.linearGradient(
            colors = colors,
            start = start,
            end = start + tile,
            tileMode = TileMode.Repeated,
        )
        translate(left = strokePx / 2, top = strokePx / 2) {
            drawOutline(outline = outline, brush = brush, style = Stroke(width = strokePx))
        }
    }
}

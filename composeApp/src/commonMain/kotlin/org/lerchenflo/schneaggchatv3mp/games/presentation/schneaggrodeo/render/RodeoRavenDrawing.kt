package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoRavenUi

// The ravens chasing the horse (see engine/RodeoPack): black birds with a thin white outline,
// flapping along behind it.

/** Half the body's length and height, in world units. */
private const val BODY_HALF_LENGTH = 2.8f
private const val BODY_HALF_HEIGHT = 1.2f
/** How far a wing tip swings up and down with a flap. */
private const val WING_SWING = 5f
/** The thin white outline that keeps them visible in the dark. */
private const val OUTLINE_WIDTH = 0.5f

/** A raven facing right (or left), [RodeoRavenUi.x] the middle of its body, flapping with [RodeoRavenUi.flap]. */
internal fun DrawScope.drawRaven(raven: RodeoRavenUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(raven.x, raven.height)
    fun p(x: Float, y: Float) = Offset(center.x + x * unit, center.y - y * unit)
    val body = RAVEN_BLACK
    val flap = raven.flap

    val outline = Stroke(width = OUTLINE_WIDTH * unit, join = StrokeJoin.Round)

    /** The whole bird once in [style]: first as a white outline, then filled in black on top. */
    fun drawBird(style: DrawStyle, outlineColor: Color?) {
        fun paint(color: Color) = outlineColor ?: color
        // The far wing, a beat behind and a little paler
        drawWing(::p, flap * 0.8f, -0.6f, paint(body.copy(alpha = 0.7f)), style)
        // Wedge of a tail, body, head with its heavy beak
        drawPath(polygonPath(::p, -2.2f to 0.4f, -5f to 1.2f, -5.2f to -0.6f, -2.2f to -0.5f), paint(body), style = style)
        drawOval(paint(body), p(-BODY_HALF_LENGTH - 0.3f, BODY_HALF_HEIGHT), Size(2f * BODY_HALF_LENGTH * unit, 2f * BODY_HALF_HEIGHT * unit), style = style)
        drawCircle(paint(body), radius = 1.3f * unit, center = p(2.6f, 0.7f), style = style)
        drawPath(polygonPath(::p, 3.4f to 1.2f, 5.4f to 0.4f, 3.6f to 0f), paint(RAVEN_BEAK), style = style)
        // The near wing in front of the body
        drawWing(::p, flap, 0f, paint(body), style)
    }

    withTransform({
        if (raven.facingLeft) scale(-1f, 1f, pivot = center)
        rotate(raven.tiltDeg, pivot = center)
    }) {
        drawBird(outline, RAVEN_OUTLINE)
        drawBird(Fill, null)
        drawCircle(RAVEN_EYE, radius = 0.17f * unit, center = p(3f, 1.05f))
    }
}

/** A wing from the shoulder, its tip up at [flap] 1 and down at -1, shifted back by [shift]. */
private fun DrawScope.drawWing(p: (Float, Float) -> Offset, flap: Float, shift: Float, color: Color, style: DrawStyle) {
    val elbowY = 0.6f + 0.55f * WING_SWING * flap
    val tipY = 0.6f + WING_SWING * flap
    drawPath(
        polygonPath(p, 1.4f + shift to 0.6f, -0.6f + shift to elbowY + 0.6f, -3.6f + shift to tipY, -1.6f + shift to elbowY - 0.4f, -1.4f + shift to 0.3f),
        color,
        style = style,
    )
}

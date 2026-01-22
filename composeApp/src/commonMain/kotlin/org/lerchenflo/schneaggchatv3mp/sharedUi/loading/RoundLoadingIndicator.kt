package org.lerchenflo.schneaggchatv3mp.sharedUi.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun RoundLoadingIndicator(
    visible: Boolean,
    size: Dp = 25.dp,
    strokeWidth: Dp = 5.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    if (visible){
        val density = LocalDensity.current
        val strokePx = with(density) { strokeWidth.toPx() }
        val sizePx = with(density) { size.toPx() }

        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing)
            )
        )

        val sweep by infiniteTransition.animateFloat(
            initialValue = 20f,
            targetValue = 300f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    20f at 0
                    300f at 600 // Half of 1200ms
                },
                repeatMode = RepeatMode.Reverse
            )
        )

        Canvas(
            modifier = Modifier
                .clickable{
                    onClick()
                }
                .size(size),
            fun DrawScope.() {
            val arcSize = Size(sizePx - strokePx, sizePx - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Draw background track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Draw animated arc
            rotate(degrees = rotation) {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        })
    }


}
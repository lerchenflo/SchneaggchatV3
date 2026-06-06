package org.lerchenflo.schneaggchatv3mp.sharedUi.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutoScrollText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 16.sp),
    backgroundColor: Color = Color.Black,       // ← background color
    scrollSpeedDp: Int = 80,
    height: Dp = 40.dp
) {
    var containerWidth by remember { mutableStateOf(0) }
    var textWidth by remember { mutableStateOf(0) }

    val totalDistance = containerWidth + textWidth

    val infiniteTransition = rememberInfiniteTransition()
    val offsetX by infiniteTransition.animateFloat(
        initialValue = containerWidth.toFloat(),
        targetValue = -textWidth.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (scrollSpeedDp > 0 && totalDistance > 0)
                    (totalDistance / scrollSpeedDp.toFloat() * 1000).toInt()
                else 5000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)        // ← applied here
            .clipToBounds()
            .onSizeChanged { containerWidth = it.width }
    ) {
        Text(
            text = text,
            style = style,
            maxLines = 1,
            modifier = Modifier
                .wrapContentSize()
                .onSizeChanged { textWidth = it.width }
                .graphicsLayer { translationX = offsetX }
        )
    }
}
package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lerchenflo.schneaggchatv3mp.games.domain.MonthCountUi

// Shared building blocks for the recap story pages. All entrance animations run
// below the recomposition layer (graphicsLayer) where possible.

/**
 * Full-screen page background with a vertical gradient and generous padding for content.
 */
@Composable
fun RecapPageBackground(
    topColor: Color,
    bottomColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .padding(horizontal = 28.dp, vertical = 72.dp)
    ) {
        content()
    }
}

/**
 * Staggered entrance: fades in and slides up when [visible] becomes true.
 * [index] delays the animation so page content reveals one element after another.
 */
@Composable
fun RevealItem(
    visible: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 550,
            delayMillis = if (visible) index * 200 else 0,
            easing = FastOutSlowInEasing
        )
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 60.dp.toPx()
        }
    ) {
        content()
    }
}

/**
 * Big number that counts up from 0 when [running] becomes true.
 */
@Composable
fun CountUpText(
    target: Long,
    running: Boolean,
    color: Color,
    fontSize: TextUnit = 64.sp,
    modifier: Modifier = Modifier,
    delayMillis: Int = 300,
) {
    var displayed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running, target) {
        if (!running) return@LaunchedEffect
        val animatable = Animatable(0f)
        animatable.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = 1500, delayMillis = delayMillis, easing = FastOutSlowInEasing)
        ) {
            displayed = value.toLong()
        }
        displayed = target
    }
    Text(
        text = formatCount(displayed),
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
        lineHeight = fontSize * 1.05f,
        modifier = modifier
    )
}

/**
 * Small pill showing a label + value, used for secondary stats on a page.
 */
@Composable
fun StatPill(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Twelve-month activity chart. Bars grow from the bottom when [visible] becomes true.
 */
@Composable
fun MonthBarChart(
    months: List<MonthCountUi>,
    peakMonth: Int?,
    visible: Boolean,
    barColor: Color,
    peakColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp,
) {
    val growth by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 500, easing = FastOutSlowInEasing)
    )
    val maxCount = months.maxOfOrNull { it.count }?.coerceAtLeast(1L) ?: 1L

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        months.forEach { month ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val fraction = (month.count.toFloat() / maxCount).coerceIn(0.04f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight * fraction)
                        .graphicsLayer {
                            scaleY = growth
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (month.month == peakMonth) peakColor else barColor)
                )
                Text(
                    text = month.month.toString(),
                    color = labelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Groups digits with a dot separator (12345 -> "12.345").
 */
fun formatCount(value: Long): String {
    val digits = value.toString()
    if (digits.length <= 3) return digits
    return digits.reversed().chunked(3).joinToString(".").reversed()
}

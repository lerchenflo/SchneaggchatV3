package org.lerchenflo.schneaggchatv3mp.games.presentation.recap.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.lerchenflo.schneaggchatv3mp.games.domain.EmojiCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.MonthCountUi
import org.lerchenflo.schneaggchatv3mp.games.domain.RankedRowUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapPageTheme

// Shared building blocks for the recap story pages. Entrance animations run below the
// recomposition layer (graphicsLayer / drawBehind) wherever possible.

/** Story content never grows wider than this - on desktop the recap keeps a phone-like column. */
val RecapContentMaxWidth: Dp = 480.dp

/** Vertical room the story chrome (progress bars + close button) needs above page content. */
val RecapChromeHeight: Dp = 84.dp

/**
 * One full-bleed story page: gradient background, slowly drifting decor shapes, and a content
 * column that is vertically centered while it fits and becomes scrollable once it does not. The
 * column respects the system bars and leaves room for the story chrome at the top.
 *
 * Also fixes the base text style: Material's default `bodyLarge` carries a fixed 24sp line height
 * that makes any wrapped headline above ~20sp overlap its own lines - here line height is relative.
 */
@Composable
fun RecapPage(
    theme: RecapPageTheme,
    visible: Boolean,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.backgroundTop, theme.backgroundBottom)))
            .recapDecor(theme.decor, animate = visible)
    ) {
        val viewportHeight = maxHeight
        ProvideTextStyle(
            LocalTextStyle.current.copy(
                color = theme.onBackground,
                fontWeight = FontWeight.Medium,
                lineHeight = 1.15.em
            )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = RecapContentMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 28.dp)
                    .padding(top = RecapChromeHeight, bottom = 56.dp),
                verticalArrangement = verticalArrangement,
                content = content
            )
        }
    }
}

/**
 * Three big translucent shapes behind the content, drifting very slowly while the page is the
 * current one. Drawn in drawBehind so the animation never recomposes the page.
 */
@Composable
private fun Modifier.recapDecor(color: Color, animate: Boolean): Modifier {
    val drift: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "recapDecor").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 9000, easing = LinearEasing), RepeatMode.Reverse),
            label = "drift"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    return drawBehind {
        val d = drift.value
        val w = size.width
        val h = size.height
        drawCircle(
            color = color.copy(alpha = 0.16f),
            radius = w * 0.55f,
            center = Offset(w * (0.98f + 0.04f * d), h * (0.10f - 0.03f * d))
        )
        drawCircle(
            color = color.copy(alpha = 0.20f),
            radius = w * 0.34f,
            center = Offset(w * 0.02f, h * (0.90f + 0.03f * d)),
            style = Stroke(width = 26.dp.toPx())
        )
        drawCircle(
            color = color.copy(alpha = 0.32f),
            radius = w * 0.055f,
            center = Offset(w * (0.82f - 0.08f * d), h * 0.70f)
        )
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
            delayMillis = if (visible) index * 180 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "reveal"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 48.dp.toPx()
        }
    ) {
        content()
    }
}

/** Small uppercase label above a headline or number - the "eyebrow" of a story page. */
@Composable
fun RecapEyebrow(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 13.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/**
 * Page headline in black weight. Shrinks (down to 22sp) instead of clipping when a long
 * translation would need more than four lines at [maxFontSize].
 */
@Composable
fun RecapHeadline(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 40.sp,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Black,
        maxLines = 4,
        textAlign = textAlign,
        autoSize = TextAutoSize.StepBased(minFontSize = 22.sp, maxFontSize = maxFontSize, stepSize = 1.sp),
        modifier = modifier.fillMaxWidth()
    )
}

/** Regular body copy for a page, with an optional emphasis color. */
@Composable
fun RecapBody(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 17.sp,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

/**
 * Hero number that counts up from 0 when [running] becomes true. Always a single line - the
 * font shrinks to fit the width, so "1.234.567" never clips at the page edge.
 */
@Composable
fun RecapBigNumber(
    target: Long,
    running: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 96.sp,
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
        fontWeight = FontWeight.Black,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = maxFontSize, stepSize = 2.sp),
        modifier = modifier.fillMaxWidth()
    )
}

/** Hero-sized static text (a rank like "#3", a year) - single line, shrinks to fit. */
@Composable
fun RecapBigText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 96.sp,
) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = maxFontSize, stepSize = 2.sp),
        modifier = modifier.fillMaxWidth()
    )
}

/** Small pill showing a label + value, used for secondary stats on a page. */
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** Pills laid out in a wrapping row, so five stats never overflow a narrow phone. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PillFlow(
    pills: List<String>,
    theme: RecapPageTheme,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pills.forEach { pill ->
            StatPill(
                text = pill,
                textColor = theme.onBackground,
                backgroundColor = theme.onBackground.copy(alpha = 0.14f)
            )
        }
    }
}

/** Translucent rounded card for a quote, a highlighted stat or a small list. */
@Composable
fun RecapCard(
    theme: RecapPageTheme,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.onBackground.copy(alpha = 0.11f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

/** One row of a ranking: rank badge, name, metric. The requester's own row is accent-tinted. */
@Composable
fun RankRow(
    row: RankedRowUi,
    valueText: String,
    theme: RecapPageTheme,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (row.isMe) theme.accent.copy(alpha = 0.26f)
                else theme.onBackground.copy(alpha = 0.09f)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (row.isMe) theme.accent else theme.onBackground.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = row.rank.toString(),
                color = if (row.isMe) theme.backgroundBottom else theme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = row.username,
            color = if (row.isMe) theme.accent else theme.onBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = valueText,
            color = theme.onBackground.copy(alpha = 0.75f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** A row of emojis with their counts underneath, under a small title. */
@Composable
fun EmojiRow(
    title: String,
    emojis: List<EmojiCountUi>,
    theme: RecapPageTheme,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = theme.onBackground.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            emojis.forEach { entry ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = entry.emoji, fontSize = 30.sp, lineHeight = 36.sp)
                    Text(
                        text = formatCount(entry.count),
                        color = theme.onBackground.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
        animationSpec = tween(durationMillis = 900, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "barGrowth"
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = month.month.toString(),
                    color = labelColor,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
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

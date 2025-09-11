package org.lerchenflo.schneaggchatv3mp.settings.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.ktor.websocket.FrameType.Companion.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ConfirmSlider(
    modifier: Modifier = Modifier,
    text: String,
    thumbIcon: ImageVector,
    width: Dp = 320.dp,
    height: Dp = 56.dp,
    thumbSize: Dp = 48.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    thumbColor: Color = MaterialTheme.colorScheme.onPrimary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    completedThreshold: Float = 0.9f,
    autoResetMillis: Long? = 1500L,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
    onConfirm: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current

    var trackWidthPx by remember { mutableStateOf(0f) }
    val thumbSizePx = with(LocalDensity.current) { thumbSize.toPx() }
    val offset = remember { Animatable(0f) }

    val maxOffset = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)

    val progress by derivedStateOf {
        if (maxOffset <= 0f) 0f else (offset.value / maxOffset).coerceIn(0f, 1f)
    }

    val semanticState = if (!enabled) "disabled" else "progress ${(progress * 100).roundToInt()} percent"

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .semantics { stateDescription = semanticState }
            .clip(RoundedCornerShape(28.dp))
            .background(trackColor)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (progress >= completedThreshold) {
                                offset.animateTo(maxOffset, animationSpec = tween(150))
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onConfirm()
                                if (autoResetMillis != null) {
                                    delay(autoResetMillis)
                                    offset.animateTo(0f, animationSpec = tween(250))
                                }
                            } else {
                                offset.animateTo(0f, animationSpec = tween(250))
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dx = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) dragAmount.x else -dragAmount.x
                        val new = (offset.value + dx).coerceIn(0f, maxOffset)
                        scope.launch { offset.snapTo(new) }
                    }
                )
            }
            .onSizeChanged { size -> trackWidthPx = size.width.toFloat() }
    ) {
        // progress fill
        Box(modifier = Modifier.matchParentSize()) {
            val fillWidth = (progress * (trackWidthPx)).coerceIn(0f, trackWidthPx)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(LocalDensity.current) { fillWidth.toDp() })
                    .clip(RoundedCornerShape(28.dp))
                    .background(progressColor)
            )
        }

        // center text
        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            Text(text = text, color = textColor, style = MaterialTheme.typography.bodyLarge)
        }

        // trailing icon (end of track)
        val iconPadding = thumbSize / 2 + 8.dp // keep it away from the very edge and thumb
        val endAlignment = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) Alignment.CenterEnd else Alignment.CenterStart

        // animate icon tint between onSurface and progressColor based on progress
        val iconTint by animateColorAsState(
            targetValue = androidx.compose.ui.graphics.lerp(
                MaterialTheme.colorScheme.onSurface,
                progressColor,
                progress
            )
        )

        if (trailingIcon != null) {
            Box(
                modifier = Modifier
                    .align(endAlignment)
                    .padding(end = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) iconPadding else 0.dp,
                        start = if (layoutDirection != androidx.compose.ui.unit.LayoutDirection.Ltr) iconPadding else 0.dp)
                    .size(height),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides iconTint) {
                    trailingIcon()
                }
            }
        } else {
            // default check icon
            Box(
                modifier = Modifier
                    .align(endAlignment)
                    .padding(end = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) iconPadding else 0.dp,
                        start = if (layoutDirection != androidx.compose.ui.unit.LayoutDirection.Ltr) iconPadding else 0.dp)
                    .size(height),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = null,
                    tint = iconTint
                )
            }
        }

        // draggable thumb
        Box(
            modifier = Modifier
                .size(thumbSize)
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .align(Alignment.CenterStart)
                .padding(4.dp)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = thumbIcon,
                contentDescription = null,
                tint = progressColor
            )
        }
    }
}

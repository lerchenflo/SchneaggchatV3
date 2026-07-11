package org.lerchenflo.schneaggchatv3mp.games.presentation.fingerpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_fingerpicker_hint
import schneaggchatv3mp.composeapp.generated.resources.games_fingerpicker_holding
import schneaggchatv3mp.composeapp.generated.resources.games_fingerpicker_lift_hint
import schneaggchatv3mp.composeapp.generated.resources.games_fingerpicker_result
import schneaggchatv3mp.composeapp.generated.resources.games_fingerpicker_title
import schneaggchatv3mp.composeapp.generated.resources.games_fingerpicker_winner_count

private val TOUCH_RADIUS = 44.dp
private val PROGRESS_RING_PADDING = 10.dp
private val PROGRESS_RING_WIDTH = 6.dp

@Composable
fun FingerPickerScreen(
    viewModel: FingerPickerViewModel = koinInject(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.phase) {
        if (state.phase == FingerPickerPhase.RESULT) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = stringResource(Res.string.games_fingerpicker_title),
            onBackClick = onBackClick
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.games_fingerpicker_winner_count),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.onAction(FingerPickerAction.OnWinnerCountChange(state.winnerCount - 1)) },
                enabled = state.winnerCount > 1
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = null)
            }
            Text(
                text = state.winnerCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp)
            )
            IconButton(
                onClick = { viewModel.onAction(FingerPickerAction.OnWinnerCountChange(state.winnerCount + 1)) }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FingerPickerBoard(
                state = state,
                onTouchesChanged = { viewModel.onAction(FingerPickerAction.OnTouchesChanged(it)) },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(24.dp)
            ) {
                when {
                    state.phase == FingerPickerPhase.RESULT -> {
                        Text(
                            text = stringResource(Res.string.games_fingerpicker_result, state.winners.size),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.games_fingerpicker_lift_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.phase == FingerPickerPhase.HOLDING -> {
                        Text(
                            text = stringResource(Res.string.games_fingerpicker_holding),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.touches.isEmpty() -> {
                        Text(
                            text = stringResource(Res.string.games_fingerpicker_hint),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FingerPickerBoard(
    state: FingerPickerState,
    onTouchesChanged: (Map<PointerId, Offset>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                val touches = mutableMapOf<PointerId, Offset>()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                touches[change.id] = change.position
                            } else {
                                touches.remove(change.id)
                            }
                            change.consume()
                        }
                        onTouchesChanged(touches.toMap())
                    }
                }
            }
    ) {
        val radiusPx = TOUCH_RADIUS.toPx()
        val ringPaddingPx = PROGRESS_RING_PADDING.toPx()
        val ringWidthPx = PROGRESS_RING_WIDTH.toPx()

        state.touches.forEach { (id, position) ->
            val isWinner = id in state.winners
            val color = when {
                state.phase == FingerPickerPhase.RESULT && isWinner -> colors.tertiary
                state.phase == FingerPickerPhase.RESULT && !isWinner -> colors.outlineVariant
                else -> colors.primary
            }

            drawCircle(color = color, radius = radiusPx, center = position)

            if (state.phase == FingerPickerPhase.HOLDING) {
                val ringRadius = radiusPx + ringPaddingPx
                drawArc(
                    color = colors.tertiary,
                    startAngle = -90f,
                    sweepAngle = 360f * state.holdProgress,
                    useCenter = false,
                    topLeft = Offset(position.x - ringRadius, position.y - ringRadius),
                    size = Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = ringWidthPx, cap = StrokeCap.Round)
                )
            }
        }
    }
}

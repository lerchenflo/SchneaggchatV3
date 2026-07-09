package org.lerchenflo.schneaggchatv3mp.games.presentation.coinflip

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_flip
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_flipping
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_heads
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_swipe_hint
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_tails_default
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_tails_label
import schneaggchatv3mp.composeapp.generated.resources.games_coinflip_title
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative

private const val FLIP_DURATION_MS = 900
private const val MIN_SPINS = 3
private const val MAX_SPINS = 5
private val THROW_HEIGHT = 150.dp
private val SWIPE_THRESHOLD = 60.dp

//Secondary wobble layered on top of the main X-axis flip so the coin tumbles diagonally instead
//of rotating flat on one axis. Kept well under 90° - the X-axis counter-rotation trick (see the
//back-face Box below) only needs to exist because X sweeps past edge-on; as long as Y/Z stay
//bounded here they never go edge-on, so no equivalent counter-rotation is needed for them.
private const val MIN_WOBBLE_Y_DEGREES = 15
private const val MAX_WOBBLE_Y_DEGREES = 30
private const val MIN_WOBBLE_Z_DEGREES = 5
private const val MAX_WOBBLE_Z_DEGREES = 15

@Composable
fun CoinFlipScreen(
    viewModel: CoinFlipViewModel = koinInject(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val hasFlippedOnce = state.flipTrigger > 0

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_coinflip_title),
            onBackClick = onBackClick
        )

        OutlinedTextField(
            value = state.tailsLabel,
            onValueChange = { viewModel.onAction(CoinFlipAction.OnTailsLabelChange(it)) },
            label = { Text(stringResource(Res.string.games_coinflip_tails_label)) },
            placeholder = { Text(stringResource(Res.string.games_coinflip_tails_default)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )

        var swipeAccumulator by remember { mutableStateOf(0f) }
        val swipeThresholdPx = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(state.isFlipping) {
                    // Disabled while flipping so a swipe mid-animation can't queue another flip
                    if (state.isFlipping) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { swipeAccumulator = 0f },
                        onDragEnd = { swipeAccumulator = 0f },
                        onDragCancel = { swipeAccumulator = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            swipeAccumulator += dragAmount
                            if (swipeAccumulator < -swipeThresholdPx) {
                                viewModel.onAction(CoinFlipAction.Flip)
                                swipeAccumulator = 0f
                            }
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Coin(
                result = state.result,
                flipTrigger = state.flipTrigger,
                tailsLabel = state.tailsLabel.ifBlank { stringResource(Res.string.games_coinflip_tails_default) },
                onAnimationFinished = { viewModel.onAction(CoinFlipAction.AnimationFinished) }
            )

            if (hasFlippedOnce) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when (state.result) {
                        CoinSide.HEADS -> stringResource(Res.string.games_coinflip_heads)
                        CoinSide.TAILS -> state.tailsLabel.ifBlank { stringResource(Res.string.games_coinflip_tails_default) }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isFlipping)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Text(
            text = stringResource(Res.string.games_coinflip_swipe_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.onAction(CoinFlipAction.Flip) },
            enabled = !state.isFlipping,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = if (state.isFlipping)
                    stringResource(Res.string.games_coinflip_flipping)
                else
                    stringResource(Res.string.games_coinflip_flip)
            )
        }
    }
}

@Composable
private fun Coin(
    result: CoinSide,
    flipTrigger: Int,
    tailsLabel: String,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current

    // Secondary wobble (see MIN/MAX_WOBBLE_* above) - randomized per toss, and its 0..1 progress
    // is derived from `rotation` itself below rather than adding a third Animatable.
    val wobbleAmplitudeY = remember { mutableStateOf(0f) }
    val wobbleAmplitudeZ = remember { mutableStateOf(0f) }
    val flipStartRotation = remember { mutableStateOf(0f) }
    val flipTargetRotation = remember { mutableStateOf(0f) }

    LaunchedEffect(flipTrigger) {
        // Trigger 0 is the initial resting state - nothing to animate yet
        if (flipTrigger == 0) return@LaunchedEffect

        val spins = Random.nextInt(MIN_SPINS, MAX_SPINS + 1)
        val faceOffset = if (result == CoinSide.TAILS) 180f else 0f
        val currentBase = rotation.value - (rotation.value % 360f)
        val targetRotation = currentBase + spins * 360f + faceOffset
        val peakOffsetPx = with(density) { -THROW_HEIGHT.toPx() }

        flipStartRotation.value = rotation.value
        flipTargetRotation.value = targetRotation
        wobbleAmplitudeY.value = (if (Random.nextBoolean()) 1f else -1f) *
            Random.nextInt(MIN_WOBBLE_Y_DEGREES, MAX_WOBBLE_Y_DEGREES + 1)
        wobbleAmplitudeZ.value = (if (Random.nextBoolean()) 1f else -1f) *
            Random.nextInt(MIN_WOBBLE_Z_DEGREES, MAX_WOBBLE_Z_DEGREES + 1)

        offsetY.snapTo(0f)

        coroutineScope {
            launch {
                // Throw the coin up and let it fall back down, like gravity
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = FLIP_DURATION_MS
                        0f at 0
                        peakOffsetPx at (FLIP_DURATION_MS * 0.42f).toInt() using FastOutSlowInEasing
                        0f at FLIP_DURATION_MS using FastOutLinearInEasing
                    }
                )
            }
            launch {
                // Spin continuously through the toss, settling on the picked face
                rotation.animateTo(
                    targetValue = targetRotation,
                    animationSpec = tween(
                        durationMillis = FLIP_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }

        onAnimationFinished()
    }

    val normalizedRotation = rotation.value % 360f
    val showFront = normalizedRotation <= 90f || normalizedRotation >= 270f

    // 0 -> 1 -> 0 across the toss, derived from rotation's own progress, so the wobble peaks
    // mid-flip and always returns to exactly 0 by the time the coin lands (flat, readable result).
    val flipSpan = flipTargetRotation.value - flipStartRotation.value
    val flipProgress = if (abs(flipSpan) < 0.01f) 0f
        else ((rotation.value - flipStartRotation.value) / flipSpan).coerceIn(0f, 1f)
    val wobbleEnvelope = sin(flipProgress * PI.toFloat())

    Box(
        modifier = modifier
            .size(160.dp)
            .graphicsLayer {
                translationY = offsetY.value
                rotationX = rotation.value
                rotationY = wobbleAmplitudeY.value * wobbleEnvelope
                rotationZ = wobbleAmplitudeZ.value * wobbleEnvelope
                cameraDistance = 10f * density.density
            },
        contentAlignment = Alignment.Center
    ) {
        if (showFront) {
            CoinFace(isHeads = true, label = null)
        } else {
            // Counter-rotate the back face so its content isn't mirrored
            Box(modifier = Modifier.graphicsLayer { rotationX = 180f }) {
                CoinFace(isHeads = false, label = tailsLabel)
            }
        }
    }
}

@Composable
private fun CoinFace(
    isHeads: Boolean,
    label: String?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(colors.primaryContainer, colors.primary)
                )
            )
            .border(4.dp, colors.onPrimary.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isHeads) {
            Image(
                painter = painterResource(Res.drawable.icon_schneagg_alternative),
                contentDescription = stringResource(Res.string.games_coinflip_heads),
                modifier = Modifier.size(96.dp)
            )
        } else {
            Text(
                text = label.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

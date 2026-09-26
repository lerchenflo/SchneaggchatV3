package io.github.lerchenflo.taptarget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Texts the overlay shows by itself. Defaults are English, pass localized ones to
 * [TapTargetOverlay] (e.g. resolved via `stringResource` at the call site).
 */
data class TourStrings(
    val tapHighlighted: String = "Tap the highlighted area to continue",
    val tapAnywhere: String = "Tap anywhere to continue",
    val continueButton: String = "Continue",
)

/** Default headline text for a step's bubble. */
@Composable
fun TourTitle(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(text = text, modifier = modifier, style = MaterialTheme.typography.titleMedium, textAlign = textAlign)
}

/** Default body text for a step's bubble. */
@Composable
fun TourDescription(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(text = text, modifier = modifier, style = MaterialTheme.typography.bodyMedium, textAlign = textAlign)
}

/**
 * Full-screen overlay that renders the tour spotlight effect.
 *
 * Mount this **once** as the topmost layer inside the root [Box] that wraps your navigation:
 *
 * ```kotlin
 * CompositionLocalProvider(LocalTapTargetController provides controller) {
 *     Box {
 *         AppNavHost(navController)
 *         TapTargetOverlay(controller)
 *     }
 * }
 * ```
 *
 * The overlay:
 * - Returns immediately when the tour is finished ([TapTargetController.isActive] == false).
 * - Draws nothing for the current frame when the target composable is not yet laid out
 *   (e.g. while navigation is in progress).
 * - Punches a rounded hole in the scrim around the target bounds.
 * - Shows the step's content bubble just below (or above) the spotlight hole.
 * - Advances when the highlighted box is tapped (or anywhere, if the step allows it).
 */
@Composable
fun TapTargetOverlay(
    controller: TapTargetController,
    strings: TourStrings = TourStrings(),
    skipButton: (@Composable (onSkip: () -> Unit) -> Unit)? = null,
) {
    if (!controller.isActive) return

    LaunchedEffect(controller.currentIndex) {
        val targetVisible = controller.ensureCurrentStepVisible()
        //Effect gets cancelled when the step changes meanwhile, so this only skips the stuck step
        if (!targetVisible && controller.tourSettings.skipMissingTargets) controller.next()
    }

    val step = controller.currentStep ?: return

    //Plain Box without pointer input - taps still reach the app during free-roam steps
    BoxWithConstraints(Modifier.fillMaxSize()) {
        TourStepLayer(controller, step, strings)

        if (skipButton != null) {
            val target = controller.currentTarget
            //Spotlight steps show the button only once the spotlight itself is drawn
            if (step.id == null || step.freeRoamPosition != null || target != null) {
                val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                Box(
                    modifier = Modifier
                        .align(skipButtonAlignment(step, target, screenHeightPx))
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(8.dp)
                ) {
                    skipButton { controller.skip() }
                }
            }
        }
    }
}

/**
 * Default skip button for [TapTargetOverlay]'s `skipButton` slot:
 *
 * ```kotlin
 * TapTargetOverlay(
 *     controller = controller,
 *     skipButton = { onSkip -> TourSkipButton(stringResource(Res.string.skip), onSkip) }
 * )
 * ```
 */
@Composable
fun TourSkipButton(text: String, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onSkip, modifier = modifier) {
        Text(text)
    }
}

/** Puts the skip button in the half of the screen opposite to the spotlight / free-roam bar. */
private fun skipButtonAlignment(step: TourStep, target: TargetInfo?, screenHeightPx: Float): Alignment =
    when {
        step.freeRoamPosition == FreeRoamBarPosition.Top -> Alignment.BottomEnd
        step.freeRoamPosition != null -> Alignment.TopEnd
        target != null && target.bounds.center.y < screenHeightPx / 2f -> Alignment.BottomEnd
        else -> Alignment.TopEnd
    }

@Composable
private fun TourStepLayer(controller: TapTargetController, step: TourStep, strings: TourStrings) {
    // Free-roam: non-blocking hint bar, user explores freely, advances via button only.
    if (step.freeRoamPosition != null) {
        FreeRoamTourBar(
            step = step,
            position = step.freeRoamPosition,
            continueText = step.continueButtonText?.invoke() ?: strings.continueButton,
            onContinue = { controller.next() },
        )
        return
    }

    // Centered: no spotlight target, full block, tap-anywhere advances.
    if (step.id == null) {
        CenteredTourStep(step, strings, onTap = { controller.next() })
        return
    }

    val target = controller.currentTarget ?: return   // not laid out yet → draw nothing

    var bubbleSize by remember(step.id) { mutableStateOf(IntSize.Zero) }

    // Wrong-tap feedback: a one-shot pulse layered on top of the ambient ring animation.
    // 1f = settled/idle, 0f = just triggered — decays back to 1f so the boost fades out.
    var wrongTapCount by remember(step.id) { mutableIntStateOf(0) }
    val wrongTapPulse = remember(step.id) { Animatable(1f) }
    LaunchedEffect(wrongTapCount) {
        if (wrongTapCount > 0) {
            wrongTapPulse.snapTo(0f)
            wrongTapPulse.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
    }

    // Ambient pulse ring shown only while the user must tap the exact highlighted spot.
    val ringTransition = rememberInfiniteTransition(label = "tapTargetRing")
    val ringProgress by ringTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tapTargetRingProgress",
    )
    val ringColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density       = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val edgePaddingPx = with(density) { 16.dp.toPx() }
        val iconPaddingPx = with(density) { controller.tourSettings.iconPadding.toPx() }
        val cornerRadiusPx = with(density) { controller.tourSettings.cornerRadius.toPx() }
        val highlightedBounds = target.bounds.inflate(iconPaddingPx)
        val ringInflatePx = with(density) { 10.dp.toPx() }
        val ringStrokePx = with(density) { 2.dp.toPx() }

        // ── Scrim with punched-out spotlight ─────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(step.id) {
                    detectTapGestures { offset ->
                        if (!step.requireExactTap || highlightedBounds.contains(offset)) {
                            controller.next()
                        } else {
                            wrongTapCount++
                        }
                    }
                }
        ) {
            val scrim = Path().apply { addRect(size.toRect()) }
            val hole = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = highlightedBounds,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                )
            }
            val diff  = Path().apply { op(scrim, hole, PathOperation.Difference) }
            drawPath(diff, color = step.backgroundColor)

            // Draw a pulsing ring around the hole to signal "tap exactly here".
            if (step.tapHint == TapHint.EXACT) {
                // wrongTapBoost decays from 1 (just tapped wrong) to 0 (settled) over ~400ms.
                val wrongTapBoost = 1f - wrongTapPulse.value
                val inflate = ringInflatePx * ringProgress + ringInflatePx * 1.5f * wrongTapBoost
                val alpha = (0.8f * (1f - ringProgress)) + (0.6f * wrongTapBoost)
                val ringBounds = highlightedBounds.inflate(inflate)
                drawRoundRect(
                    color = ringColor,
                    topLeft = ringBounds.topLeft,
                    size = ringBounds.size,
                    cornerRadius = CornerRadius(cornerRadiusPx + inflate, cornerRadiusPx + inflate),
                    style = Stroke(width = ringStrokePx + ringStrokePx * wrongTapBoost),
                    alpha = alpha.coerceIn(0f, 1f),
                )
            }
        }

        // ── Info bubble ──────────────────────────────────────────────
        if (step.content != null || step.tapHint != null) {

            // Horizontal: keep within [edgePadding, screenWidth - bubbleWidth - edgePadding]
            val clampedX = target.bounds.left
                .coerceIn(
                    edgePaddingPx,
                    (screenWidthPx - bubbleSize.width - edgePaddingPx).coerceAtLeast(edgePaddingPx)
                )
                .toInt()

            // Vertical: prefer below the spotlight; flip above if it would clip the bottom
            val belowY = target.bounds.bottom + edgePaddingPx
            val aboveY = target.bounds.top   - edgePaddingPx - bubbleSize.height

            val clampedY = if (belowY + bubbleSize.height <= screenHeightPx) {
                belowY.toInt()
            } else {
                // Show above; clamp so it never goes above the top edge either
                aboveY.coerceAtLeast(edgePaddingPx).toInt()
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(clampedX, clampedY) }
                    .widthIn(max = 260.dp)
                    .onGloballyPositioned { bubbleSize = it.size }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        step.content?.invoke(this)
                        step.tapHint?.let { TapHintRow(it, strings, hasContentAbove = step.content != null) }
                    }
                }
            }
        }
    }
}

/**
 * Renders a step with no spotlight — full scrim, no punched hole, bubble centered on screen.
 */
@Composable
private fun CenteredTourStep(step: TourStep, strings: TourStrings, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(step.id) {
                detectTapGestures { onTap() }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(color = step.backgroundColor)
        }

        if (step.content != null || step.tapHint != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 280.dp)
                    .padding(24.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    step.content?.invoke(this)
                    step.tapHint?.let { TapHintRow(it, strings, hasContentAbove = step.content != null) }
                }
            }
        }
    }
}

/**
 * Small icon + label row explaining whether this step expects a tap on the highlighted
 * target ([TapHint.EXACT]) or a tap anywhere on screen ([TapHint.ANYWHERE]).
 */
@Composable
private fun ColumnScope.TapHintRow(hint: TapHint, strings: TourStrings, hasContentAbove: Boolean) {
    if (hasContentAbove) {
        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))
    }
    Row {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (hint == TapHint.EXACT) strings.tapHighlighted else strings.tapAnywhere,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Non-blocking hint bar for free-roam steps. Anchored at [position]; everything outside
 * the bar itself remains fully interactive so the user can explore the real app. Only
 * the Continue button advances — there's no tap-anywhere-to-advance here, since a stray
 * tap during free exploration shouldn't accidentally skip the step.
 */
@Composable
private fun FreeRoamTourBar(
    step: TourStep,
    position: FreeRoamBarPosition,
    continueText: String,
    onContinue: () -> Unit,
) {
    val alignment = when (position) {
        FreeRoamBarPosition.Top    -> Alignment.TopCenter
        FreeRoamBarPosition.Center -> Alignment.Center
        FreeRoamBarPosition.Bottom -> Alignment.BottomCenter
    }
    // Top/Bottom read as a spanning banner; Center reads as a compact card.
    val widthModifier = if (position == FreeRoamBarPosition.Center) {
        Modifier.widthIn(max = 320.dp)
    } else {
        Modifier.fillMaxWidth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = alignment,
    ) {
        Surface(
            modifier = widthModifier,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                step.content?.invoke(this)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onContinue) {
                    Text(continueText)
                }
            }
        }
    }
}

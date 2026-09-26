package io.github.lerchenflo.taptarget

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Registers this composable as the spotlight target for the tour step with [id].
 *
 * This is the **only** thing a call site needs to add — the controller is read
 * automatically from [LocalTapTargetController]:
 *
 * ```kotlin
 * IconButton(
 *     modifier = Modifier.tapTarget("new_chat"),
 *     onClick = { … }
 * ) { Icon(Icons.Default.Add, null) }
 * ```
 *
 * If no controller is provided in the composition tree this modifier becomes a no-op,
 * so existing screens stay safe even outside a tour context.
 */
fun Modifier.tapTarget(id: String): Modifier = composed {
    val controller = LocalTapTargetController.current ?: return@composed this

    val bringIntoViewRequester = remember(id) { BringIntoViewRequester() }
    var lastCoords by remember(id) { mutableStateOf<LayoutCoordinates?>(null) }
    // Guards against re-triggering a scroll on every relayout pass while the bring-into-view
    // animation itself is in flight (which keeps calling onGloballyPositioned). Reset whenever
    // this id stops being the active step so it's armed again next time the tour reaches it.
    var hasAutoScrolledForStep by remember(id) { mutableStateOf(false) }

    // Bounds are only valid while this composable is on screen. Without this the entry would
    // linger after the screen leaves composition, and the tour would spotlight (and gate taps on)
    // a stale rectangle where the element used to be.
    DisposableEffect(controller, id) {
        onDispose { controller.unregister(id) }
    }

    // Auto-scrolls this target into the center 40% band of the screen once it becomes the
    // active step's target. Only fires once per activation — not on every layout pass — so it
    // doesn't fight the scroll animation it just started.
    LaunchedEffect(controller.currentIndex, id) {
        if (controller.currentStep?.id != id) {
            hasAutoScrolledForStep = false
            return@LaunchedEffect
        }
        if (hasAutoScrolledForStep) return@LaunchedEffect
        hasAutoScrolledForStep = true

        withTimeoutOrNull(2000.milliseconds) {
            while (lastCoords == null) delay(16.milliseconds)
        }
        val coords = lastCoords ?: return@LaunchedEffect
        if (!coords.isAttached) return@LaunchedEffect

        val rootSize = coords.findRootCoordinates().size
        val pos = coords.positionInRoot()
        val targetCenterY = pos.y + coords.size.height / 2f
        val bandTop = rootSize.height * 0.3f
        val bandBottom = rootSize.height * 0.7f

        if (targetCenterY < bandTop || targetCenterY > bandBottom) {
            val w = coords.size.width.toFloat()
            val h = coords.size.height.toFloat()
            // Vertical span is padded to the root's height, centered on the target's own local
            // center — against Compose's default bring-into-view algorithm this resolves to
            // "scroll until the target's center reaches the viewport's center" on whichever
            // ancestor is vertically scrollable, i.e. it centers rather than just nudges into view.
            //
            // Horizontal span is left as the target's own natural bounds (0..w), NOT padded the
            // same way. A target inside a HorizontalPager (used for swipe navigation between
            // tabs) would otherwise look off-page to that pager, which then pages forward to
            // "bring it into view" — instantly swiping the user to the next tab. Only touch the
            // axis we actually mean to center on.
            bringIntoViewRequester.bringIntoView(
                Rect(
                    left   = 0f,
                    top    = h / 2f - rootSize.height / 2f,
                    right  = w,
                    bottom = h / 2f + rootSize.height / 2f,
                )
            )
        }
    }

    this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onGloballyPositioned { coords ->
            lastCoords = coords
            val pos = coords.positionInRoot()
            controller.register(
                id,
                Rect(
                    left   = pos.x,
                    top    = pos.y,
                    right  = pos.x + coords.size.width,
                    bottom = pos.y + coords.size.height,
                )
            )
        }
}

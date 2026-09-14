package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.clearFocusOnTap() : Modifier {
    val focusManager = LocalFocusManager.current
    // IOSKEYBOARDFIX: on iOS clearFocus() alone does not always resign the first responder,
    // so the keyboard stays up. Every dialog in this app already pairs it with hide().
    val keyboardController = LocalSoftwareKeyboardController.current
    return this.pointerInput(Unit) {
        detectTapGestures {
            keyboardController?.hide() // IOSKEYBOARDFIX
            focusManager.clearFocus()
        }
    }
}

// Desktop mouse wheels only report scroll input on the y axis, but horizontalScroll only
// reacts to its own (x) axis - so without this, a horizontally scrolling row can't be
// scrolled with a mouse wheel on desktop, only by dragging.
private val MOUSE_WHEEL_SCROLL_STEP = 48.dp

/**
 * Drop-in replacement for [Modifier.horizontalScroll] that also reacts to a desktop mouse
 * wheel by forwarding its (vertical) scroll delta into [state].
 */
@Composable
fun Modifier.horizontalScrollWithMouseWheel(state: ScrollState = rememberScrollState()): Modifier {
    val density = LocalDensity.current
    val mouseWheelScrollStepPx = with(density) { MOUSE_WHEEL_SCROLL_STEP.toPx() }

    return this
        .horizontalScroll(state)
        .pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (scrollDelta != 0f) {
                            state.dispatchRawDelta(scrollDelta * mouseWheelScrollStepPx)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        }
}

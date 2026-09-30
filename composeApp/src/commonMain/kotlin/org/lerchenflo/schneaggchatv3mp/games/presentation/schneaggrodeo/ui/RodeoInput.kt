package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.SchneaggRodeoAction

/**
 * Turns the whole play area into the controls: a press anywhere (that no button takes) jumps and
 * holding keeps the jump boosted; while flying ([steers]) the left half steers up and the right
 * half down. On desktop the keys do the same:
 *
 *  - space / W / arrow up: jump (steer up)
 *  - L / S / arrow down: lasso (steer down while flying)
 *  - J / arrow right: super jump
 *  - R: rocket
 *  - G: golden carriage
 *
 * [steers] and [onAction] are read on every event, so they may change without restarting input.
 */
internal fun Modifier.rodeoInput(
    steers: () -> Boolean,
    onAction: (SchneaggRodeoAction) -> Unit,
): Modifier = this
    .onKeyEvent { event ->
        val isJumpKey = event.key == Key.Spacebar || event.key == Key.DirectionUp || event.key == Key.W
        val isLassoKey = event.key == Key.L || event.key == Key.DirectionDown || event.key == Key.S
        val isSuperJumpKey = event.key == Key.J || event.key == Key.DirectionRight
        val isRocketKey = event.key == Key.R
        val isCarriageKey = event.key == Key.G
        val down = event.type == KeyEventType.KeyDown
        val up = event.type == KeyEventType.KeyUp
        val action = when {
            // In the plane and the rocket the lasso keys steer down, the jump keys up
            isLassoKey && steers() && down -> SchneaggRodeoAction.OnDivePressed
            isLassoKey && steers() && up -> SchneaggRodeoAction.OnDiveReleased
            isLassoKey && down -> SchneaggRodeoAction.OnLassoClick
            isSuperJumpKey && down -> SchneaggRodeoAction.OnSuperJumpClick
            isRocketKey && down -> SchneaggRodeoAction.OnRocketClick
            isCarriageKey && down -> SchneaggRodeoAction.OnCarriageClick
            isJumpKey && down -> SchneaggRodeoAction.OnJumpPressed
            isJumpKey && up -> SchneaggRodeoAction.OnJumpReleased
            else -> null
        }
        action?.let(onAction)
        action != null
    }
    .pointerInput(Unit) {
        detectTapGestures(onPress = { offset ->
            val dive = steers() && offset.x > size.width / 2f
            onAction(if (dive) SchneaggRodeoAction.OnDivePressed else SchneaggRodeoAction.OnJumpPressed)
            tryAwaitRelease()
            onAction(if (dive) SchneaggRodeoAction.OnDiveReleased else SchneaggRodeoAction.OnJumpReleased)
        })
    }

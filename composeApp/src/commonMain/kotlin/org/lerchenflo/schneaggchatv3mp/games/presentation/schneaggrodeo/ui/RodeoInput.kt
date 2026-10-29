package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.SchneaggRodeoAction

/**
 * What is held down right now, from the keys and from every finger on the play area. Jump and dive
 * are only pressed once the first source goes down and released once the last one lets go, so
 * letting go of a key does not end a jump a finger still holds (and the other way around).
 * Remember one per screen: the key and the pointer handlers of [rodeoInput] share it.
 */
internal class RodeoHeldInput {
    private var keyJump = false
    private var keyDive = false
    private var touchJump = false
    private var touchDive = false
    private var jump = false
    private var dive = false

    fun keyJump(down: Boolean, onAction: (SchneaggRodeoAction) -> Unit) {
        keyJump = down
        report(onAction)
    }

    fun keyDive(down: Boolean, onAction: (SchneaggRodeoAction) -> Unit) {
        keyDive = down
        report(onAction)
    }

    /** The fingers down at [positions] on a play area [width] wide. */
    fun touches(positions: Collection<Offset>, width: Int, steers: Boolean, onAction: (SchneaggRodeoAction) -> Unit) {
        val half = width / 2f
        touchDive = steers && positions.any { it.x > half }
        touchJump = positions.any { !steers || it.x <= half }
        report(onAction)
    }

    private fun report(onAction: (SchneaggRodeoAction) -> Unit) {
        val jumpNow = keyJump || touchJump
        val diveNow = keyDive || touchDive
        if (jumpNow != jump) {
            jump = jumpNow
            onAction(if (jumpNow) SchneaggRodeoAction.OnJumpPressed else SchneaggRodeoAction.OnJumpReleased)
        }
        if (diveNow != dive) {
            dive = diveNow
            onAction(if (diveNow) SchneaggRodeoAction.OnDivePressed else SchneaggRodeoAction.OnDiveReleased)
        }
    }
}

/**
 * Turns the whole play area into the controls: a press anywhere (that no button takes) jumps and
 * holding keeps the jump boosted; while flying ([steers]) the left half steers up and the right
 * half down. Every finger counts, and a finger sliding over to the other half switches. On
 * desktop the keys do the same:
 *
 *  - space / W / arrow up: jump (steer up)
 *  - S / arrow down: lasso, or steer down (dive, duck) on the vehicles that take it ([downKey])
 *  - L: lasso
 *  - J / arrow right: super jump
 *  - R: rocket
 *  - G: golden carriage
 *
 * [steers], [downKey] and [onAction] are read on every event, so they may change without
 * restarting input.
 */
internal fun Modifier.rodeoInput(
    held: RodeoHeldInput,
    steers: () -> Boolean,
    downKey: () -> Boolean,
    onAction: (SchneaggRodeoAction) -> Unit,
): Modifier = this
    .onKeyEvent { event ->
        val isJumpKey = event.key == Key.Spacebar || event.key == Key.DirectionUp || event.key == Key.W
        val isLassoKey = event.key == Key.L
        val isDownKey = event.key == Key.DirectionDown || event.key == Key.S
        val isSuperJumpKey = event.key == Key.J || event.key == Key.DirectionRight
        val isRocketKey = event.key == Key.R
        val isCarriageKey = event.key == Key.G
        val down = event.type == KeyEventType.KeyDown
        val up = event.type == KeyEventType.KeyUp
        when {
            // A release always ends the dive, so it can't get stuck if the vehicle ended meanwhile
            isDownKey && up -> held.keyDive(false, onAction)
            isDownKey && down && downKey() -> held.keyDive(true, onAction)
            (isLassoKey || isDownKey) && down -> onAction(SchneaggRodeoAction.OnLassoClick)
            isSuperJumpKey && down -> onAction(SchneaggRodeoAction.OnSuperJumpClick)
            isRocketKey && down -> onAction(SchneaggRodeoAction.OnRocketClick)
            isCarriageKey && down -> onAction(SchneaggRodeoAction.OnCarriageClick)
            isJumpKey && (down || up) -> held.keyJump(down, onAction)
            else -> return@onKeyEvent false
        }
        true
    }
    .pointerInput(held) {
        // Only fingers that went down on the play area itself, not on a button over it
        val fingers = mutableMapOf<PointerId, Offset>()
        try {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { change ->
                        when {
                            change.changedToDown() -> fingers[change.id] = change.position
                            !change.pressed -> fingers.remove(change.id)
                            change.id in fingers -> fingers[change.id] = change.position
                        }
                    }
                    held.touches(fingers.values, size.width, steers(), onAction)
                }
            }
        } finally {
            fingers.clear()
            held.touches(positions = emptyList(), width = 0, steers = false, onAction = onAction)
        }
    }

package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.SchneaggRodeoAction
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_catch_horse
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_lasso
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_rocket
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_golden_carriage_button
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_super_jump

/** Big enough to hit with a thumb without looking. */
private val BUTTON_MIN_HEIGHT = 56.dp
private val BUTTON_PADDING = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
/** Keeps the centered ride hint clear of the lasso button on the left. */
private val HINT_SIDE_PADDING = 132.dp

/**
 * The area under the track with the buttons, one side per thumb: lasso on the left, super jump
 * and rocket on the right, each vertically centered. Taps that miss the buttons fall through to
 * the play area and jump, so the free space above and below them is a jump button too. Thrown off,
 * the lasso moves to the center - it is the only way back up. While riding a vehicle a hint on how
 * to ride it takes the center.
 */
@Composable
internal fun RodeoControls(
    superJumpCharges: Int,
    isOnFoot: Boolean,
    ride: RodeoVehicleKind?,
    rocketReady: Boolean,
    carriageReady: Boolean,
    showKeyHints: Boolean,
    enabled: Boolean,
    onAction: (SchneaggRodeoAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when {
            isOnFoot -> CatchHorseButton(
                showKeyHints = showKeyHints,
                enabled = enabled,
                onClick = { onAction(SchneaggRodeoAction.OnLassoClick) },
                modifier = Modifier.align(Alignment.Center)
            )
            ride != null -> {
                // Buttons would swallow the taps; the whole play area is the control now
                RodeoRideHint(
                    ride = ride,
                    showKeyHints = showKeyHints,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = if (ride.keepsButtons) HINT_SIDE_PADDING else 0.dp)
                )
                if (ride.keepsButtons) {
                    LassoButton(showKeyHints, enabled, onAction, Modifier.align(Alignment.CenterStart), label = ride.lassoLabel)
                }
            }
            else -> {
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = superJumpCharges > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        RodeoBigButton(
                            text = stringResource(Res.string.games_schneaggrodeo_super_jump, superJumpCharges) + keyHint("J", showKeyHints),
                            icon = Icons.Default.KeyboardDoubleArrowUp,
                            enabled = enabled,
                            onClick = { onAction(SchneaggRodeoAction.OnSuperJumpClick) },
                        )
                    }
                    // Easter egg: shows up once enough snails are saved up instead of spent on super jumps
                    AnimatedVisibility(
                        visible = rocketReady,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        RodeoBigButton(
                            text = stringResource(Res.string.games_schneaggrodeo_rocket) + keyHint("R", showKeyHints),
                            icon = Icons.Default.RocketLaunch,
                            enabled = enabled,
                            onClick = { onAction(SchneaggRodeoAction.OnRocketClick) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                        )
                    }
                    // Even more snails saved up: the golden carriage
                    AnimatedVisibility(
                        visible = carriageReady,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        RodeoBigButton(
                            text = stringResource(Res.string.games_schneaggrodeo_golden_carriage_button) + keyHint("G", showKeyHints),
                            icon = Icons.Default.EmojiEvents,
                            enabled = enabled,
                            onClick = { onAction(SchneaggRodeoAction.OnCarriageClick) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                        )
                    }
                }
                LassoButton(showKeyHints, enabled, onAction, Modifier.align(Alignment.CenterStart))
            }
        }
    }
}

@Composable
private fun LassoButton(
    showKeyHints: Boolean,
    enabled: Boolean,
    onAction: (SchneaggRodeoAction) -> Unit,
    modifier: Modifier = Modifier,
    label: StringResource? = null,
) {
    FilledTonalButton(
        onClick = { onAction(SchneaggRodeoAction.OnLassoClick) },
        enabled = enabled,
        contentPadding = BUTTON_PADDING,
        // Keeps keyboard focus on the play area so space / L keep working after a click
        modifier = modifier
            .heightIn(min = BUTTON_MIN_HEIGHT)
            .focusProperties { canFocus = false }
    ) {
        Text(
            text = stringResource(label ?: Res.string.games_schneaggrodeo_lasso) + keyHint("L", showKeyHints),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/** Thrown off: the lasso is the way back into the saddle, so it pulses in the middle. */
@Composable
private fun CatchHorseButton(
    showKeyHints: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulse by rememberInfiniteTransition(label = "lassoPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "lassoPulseScale"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = BUTTON_PADDING,
        modifier = modifier
            .heightIn(min = BUTTON_MIN_HEIGHT)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .focusProperties { canFocus = false }
    ) {
        Text(
            text = stringResource(Res.string.games_schneaggrodeo_catch_horse) + keyHint("L", showKeyHints),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun RodeoBigButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        contentPadding = BUTTON_PADDING,
        modifier = Modifier
            .heightIn(min = BUTTON_MIN_HEIGHT)
            .focusProperties { canFocus = false }
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

/** Tells how to ride the current vehicle, in place of (or next to) the buttons. */
@Composable
private fun RodeoRideHint(ride: RodeoVehicleKind, showKeyHints: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(if (showKeyHints) ride.rideHintKeys else ride.rideHint),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

/** Key name appended to a button label on desktop, e.g. "Lasso [L]". */
private fun keyHint(key: String, show: Boolean): String = if (show) " [$key]" else ""

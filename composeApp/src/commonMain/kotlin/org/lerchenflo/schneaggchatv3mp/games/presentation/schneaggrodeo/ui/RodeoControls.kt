package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.SchneaggRodeoAction
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SNAILS_PER_SUPER_JUMP
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.carriage.SNAILS_PER_CARRIAGE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rocket.SNAILS_PER_ROCKET
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_catch_horse
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_on_foot_hint
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_on_foot_hint_keys
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_golden_carriage_button
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_lasso
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_rocket
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_super_jump
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative

/** Big enough to hit with a thumb without looking. */
private val BUTTON_MIN_HEIGHT = 56.dp
private val BUTTON_PADDING = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
/** The round buttons for what the snails buy, on the right. */
private val POWER_BUTTON_SIZE = 44.dp
private val POWER_ICON_SIZE = 22.dp
private val COST_SNAIL_SIZE = 12.dp

/**
 * The buttons floating on the track: the lasso on the left, the small round buttons for what the
 * caught snails buy (super jump, rocket, golden carriage) in a row at the bottom center, each with
 * its price. Taps that miss the buttons fall through to the play area and jump. Thrown off, the
 * lasso moves to the bottom center - it is the only way back up. While riding a vehicle a hint on
 * how to ride it takes the bottom center.
 */
@Composable
internal fun RodeoControls(
    superJumpCharges: Int,
    isOnFoot: Boolean,
    canCatchHorse: Boolean,
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
            isOnFoot -> Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RodeoHint(
                    text = stringResource(
                        if (showKeyHints) Res.string.games_schneaggrodeo_on_foot_hint_keys else Res.string.games_schneaggrodeo_on_foot_hint
                    )
                )
                // Throwable any time; it pulses once the horse is in reach of the lasso
                CatchHorseButton(
                    showKeyHints = showKeyHints,
                    enabled = enabled,
                    inReach = canCatchHorse,
                    onClick = { onAction(SchneaggRodeoAction.OnLassoClick) },
                )
            }
            ride != null -> {
                // Buttons would swallow the taps; the whole play area is the control now
                RodeoRideHint(
                    ride = ride,
                    showKeyHints = showKeyHints,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                if (ride.keepsButtons) {
                    LassoButton(showKeyHints, enabled, onAction, Modifier.align(Alignment.CenterStart), label = ride.lassoLabel)
                }
            }
            else -> {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(
                        visible = superJumpCharges > 0,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        RodeoPowerButton(
                            icon = Icons.Default.KeyboardDoubleArrowUp,
                            label = stringResource(Res.string.games_schneaggrodeo_super_jump, superJumpCharges),
                            cost = SNAILS_PER_SUPER_JUMP,
                            charges = superJumpCharges,
                            key = keyHint("J", showKeyHints),
                            enabled = enabled,
                            onClick = { onAction(SchneaggRodeoAction.OnSuperJumpClick) },
                        )
                    }
                    // Easter egg: shows up once enough snails are saved up instead of spent on super jumps
                    AnimatedVisibility(
                        visible = rocketReady,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        RodeoPowerButton(
                            icon = Icons.Default.RocketLaunch,
                            label = stringResource(Res.string.games_schneaggrodeo_rocket),
                            cost = SNAILS_PER_ROCKET,
                            key = keyHint("R", showKeyHints),
                            enabled = enabled,
                            onClick = { onAction(SchneaggRodeoAction.OnRocketClick) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                        )
                    }
                    // Even more snails saved up: the golden carriage
                    AnimatedVisibility(
                        visible = carriageReady,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        RodeoPowerButton(
                            icon = Icons.Default.EmojiEvents,
                            label = stringResource(Res.string.games_schneaggrodeo_golden_carriage_button),
                            cost = SNAILS_PER_CARRIAGE,
                            key = keyHint("G", showKeyHints),
                            enabled = enabled,
                            onClick = { onAction(SchneaggRodeoAction.OnCarriageClick) },
                            colors = IconButtonDefaults.filledIconButtonColors(
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

/** Thrown off: the lasso is the way back into the saddle, so it pulses in the middle once the horse is [inReach]. */
@Composable
private fun CatchHorseButton(
    showKeyHints: Boolean,
    enabled: Boolean,
    inReach: Boolean,
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
                val scale = if (inReach) pulse else 1f
                scaleX = scale
                scaleY = scale
            }
            .focusProperties { canFocus = false }
    ) {
        Text(
            text = stringResource(Res.string.games_schneaggrodeo_catch_horse) + keyHint("L", showKeyHints),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * A small round button for something the snails buy: [icon] with the [charges] bought (if more
 * than one) in a badge, and its price in snails under it. [label] is read out by screen readers.
 */
@Composable
private fun RodeoPowerButton(
    icon: ImageVector,
    label: String,
    cost: Int,
    key: String,
    enabled: Boolean,
    onClick: () -> Unit,
    charges: Int = 1,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BadgedBox(
            badge = {
                if (charges > 1) Badge { Text(text = "$charges") }
            }
        ) {
            FilledIconButton(
                onClick = onClick,
                enabled = enabled,
                colors = colors,
                modifier = Modifier
                    .size(POWER_BUTTON_SIZE)
                    .focusProperties { canFocus = false }
            ) {
                Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(POWER_ICON_SIZE))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 2.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "$cost",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Image(
                painter = painterResource(Res.drawable.icon_schneagg_alternative),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(COST_SNAIL_SIZE)
            )
            if (key.isNotEmpty()) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Tells how to ride the current vehicle, in place of (or next to) the buttons. */
@Composable
private fun RodeoRideHint(ride: RodeoVehicleKind, showKeyHints: Boolean, modifier: Modifier = Modifier) {
    RodeoHint(text = stringResource(if (showKeyHints) ride.rideHintKeys else ride.rideHint), modifier = modifier)
}

/** A short hint on how to play, in a small box over the track. */
@Composable
private fun RodeoHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        textAlign = TextAlign.Center
    )
}

/** Key name appended to a button label on desktop, e.g. "Lasso [L]". */
private fun keyHint(key: String, show: Boolean): String = if (show) " [$key]" else ""

package org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.player_selector_delete_player
import schneaggchatv3mp.composeapp.generated.resources.player_selector_friend
import schneaggchatv3mp.composeapp.generated.resources.player_selector_you
import schneaggchatv3mp.composeapp.generated.resources.remove

/** Profile picture for friends, a tonal circle with the first letter for players on this device. */
@Composable
fun PlayerAvatar(player: SelectablePlayer, size: Dp, modifier: Modifier = Modifier) {
    when (player) {
        is SelectablePlayer.Friend -> ProfilePictureView(
            filepath = player.user.profilePictureUrl,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
        is SelectablePlayer.Local -> Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = player.name.trim().take(1).uppercase(),
                style = if (size >= 40.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** The 1-based turn position of a picked player. */
@Composable
private fun TurnBadge(position: Int, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * One pickable player. Tapping the row picks or unpicks; a picked row is tinted and shows its turn
 * position. Players on this device can also be deleted.
 */
@Composable
fun PlayerRow(
    player: SelectablePlayer,
    isOwnUser: Boolean,
    turnPosition: Int?,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val selected = turnPosition != null
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "playerRowContainer",
    )
    val badgeScale by animateFloatAsState(if (selected) 1f else 0f, label = "playerRowBadge")
    // The badge shrinks away after unpicking; it keeps showing the last position meanwhile
    var shownPosition by remember { mutableIntStateOf(1) }
    if (turnPosition != null) shownPosition = turnPosition

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayerAvatar(player = player, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            if (player is SelectablePlayer.Friend) {
                Text(
                    text = stringResource(if (isOwnUser) Res.string.player_selector_you else Res.string.player_selector_friend),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.player_selector_delete_player),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Keeps its space while unpicked, so names do not jump when a player is picked
        TurnBadge(
            position = shownPosition,
            size = 28.dp,
            modifier = Modifier.scale(badgeScale),
        )
    }
}

/** A picked player in the turn order strip; tapping it unpicks the player. */
@Composable
fun SelectedPlayerChip(
    player: SelectablePlayer,
    position: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = {
            Text(
                text = "$position. ${player.name}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        avatar = { PlayerAvatar(player = player, size = InputChipDefaults.AvatarSize) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.remove),
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
        modifier = modifier.padding(vertical = 2.dp),
    )
}

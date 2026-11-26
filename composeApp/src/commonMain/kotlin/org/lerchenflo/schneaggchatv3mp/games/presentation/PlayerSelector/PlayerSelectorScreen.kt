package org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.player_selector_add_player
import schneaggchatv3mp.composeapp.generated.resources.player_selector_new_player
import schneaggchatv3mp.composeapp.generated.resources.player_selector_no_friends
import schneaggchatv3mp.composeapp.generated.resources.player_selector_no_local_players
import schneaggchatv3mp.composeapp.generated.resources.player_selector_order_hint
import schneaggchatv3mp.composeapp.generated.resources.player_selector_start_format
import schneaggchatv3mp.composeapp.generated.resources.player_selector_tab_friends
import schneaggchatv3mp.composeapp.generated.resources.player_selector_tab_local
import schneaggchatv3mp.composeapp.generated.resources.player_selector_title

@Composable
fun PlayerSelector(
    onDismiss: () -> Unit,
    onFinish: (List<GamePlayer>) -> Unit,
    viewModel: PlayerSelectorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PlayerSelectorSheet(
        state = state,
        onAction = viewModel::onAction,
        onDismiss = onDismiss,
        onFinish = onFinish,
    )
}

/**
 * Picks the players of a shared-device game: friends (yourself first) and players that only exist
 * on this device. The pick order is the turn order, shown in the strip at the top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelectorSheet(
    state: PlayerSelectorState,
    onAction: (PlayerSelectorAction) -> Unit,
    onDismiss: () -> Unit,
    onFinish: (List<GamePlayer>) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded), // no half-open state
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = stringResource(Res.string.player_selector_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))

            TurnOrderStrip(state = state, onAction = onAction)
            Spacer(Modifier.height(12.dp))

            NewPlayerField(state = state, onAction = onAction)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 120.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                playerSection(
                    sectionKey = "friends",
                    title = Res.string.player_selector_tab_friends,
                    emptyText = Res.string.player_selector_no_friends,
                    players = state.friends,
                    state = state,
                    onAction = onAction,
                )
                playerSection(
                    sectionKey = "local",
                    title = Res.string.player_selector_tab_local,
                    emptyText = Res.string.player_selector_no_local_players,
                    players = state.localPlayers,
                    state = state,
                    onAction = onAction,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onFinish(state.selectedGamePlayers)
                        onAction(PlayerSelectorAction.OnClearSelection)
                        onDismiss()
                    },
                    enabled = state.selectedCount > 0,
                ) {
                    Text(stringResource(Res.string.player_selector_start_format, state.selectedCount))
                }
            }
        }
    }
}

/** The picked players in turn order; empty, it explains how picking works. */
@Composable
private fun TurnOrderStrip(state: PlayerSelectorState, onAction: (PlayerSelectorAction) -> Unit) {
    val selected = state.selectedPlayers
    if (selected.isEmpty()) {
        Text(
            text = stringResource(Res.string.player_selector_order_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.heightIn(min = 36.dp),
        )
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(selected, key = { _, player -> player.key }) { index, player ->
            SelectedPlayerChip(
                player = player,
                position = index + 1,
                onRemove = { onAction(PlayerSelectorAction.OnToggleSelection(player)) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun NewPlayerField(state: PlayerSelectorState, onAction: (PlayerSelectorAction) -> Unit) {
    val canAdd = state.newPlayerName.isNotBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.newPlayerName,
            onValueChange = { onAction(PlayerSelectorAction.OnNewPlayerNameChange(it)) },
            placeholder = { Text(stringResource(Res.string.player_selector_new_player)) },
            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canAdd) onAction(PlayerSelectorAction.OnAddLocalPlayer) }),
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(
            onClick = { onAction(PlayerSelectorAction.OnAddLocalPlayer) },
            enabled = canAdd,
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.player_selector_add_player))
        }
    }
}

private fun LazyListScope.playerSection(
    sectionKey: String,
    title: StringResource,
    emptyText: StringResource,
    players: List<SelectablePlayer>,
    state: PlayerSelectorState,
    onAction: (PlayerSelectorAction) -> Unit,
) {
    item(key = "header:$sectionKey") {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
        )
    }
    if (players.isEmpty()) {
        item(key = "empty:$sectionKey") {
            Text(
                text = stringResource(emptyText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        return
    }
    items(players, key = { it.key }) { player ->
        PlayerRow(
            player = player,
            isOwnUser = player is SelectablePlayer.Friend && player.user.id == state.ownUserId,
            turnPosition = state.turnPosition(player),
            onToggle = { onAction(PlayerSelectorAction.OnToggleSelection(player)) },
            onDelete = (player as? SelectablePlayer.Local)?.let { local ->
                { onAction(PlayerSelectorAction.OnDeleteLocalPlayer(local)) }
            },
        )
    }
}

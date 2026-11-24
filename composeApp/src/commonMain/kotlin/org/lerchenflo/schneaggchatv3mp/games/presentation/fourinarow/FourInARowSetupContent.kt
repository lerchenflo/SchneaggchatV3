package org.lerchenflo.schneaggchatv3mp.games.presentation.fourinarow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FOUR_IN_A_ROW_BEST_OF
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FOUR_IN_A_ROW_MAX_PLAYERS
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FOUR_IN_A_ROW_MOVE_SECONDS
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOptionSwitchRow
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadDoneText
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSymbol
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_best_of
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_board_info
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_board_twist
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_board_twist_description
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_choose_players
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_move_timer
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_move_timer_description
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_players_hint
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_players_title
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_pop_out
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_pop_out_description
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_power_discs
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_power_discs_description
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_seconds
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_series_open
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_series_title
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_start
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_teams
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_teams_description
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_too_many_players
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_twists_title
import schneaggchatv3mp.composeapp.generated.resources.remove

/** Default move timer when the timer is switched on. */
private const val DEFAULT_MOVE_SECONDS = 20

@Composable
fun ColumnScope.FourInARowSetupContent(
    state: FourInARowState,
    onAction: (FourInARowAction) -> Unit,
) {
    val config = state.setupConfig
    val setup = state.setup
    val changeSetup: (FourInARowSetup) -> Unit = { onAction(FourInARowAction.OnSetupChange(it)) }

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Result of the upload offered when the last match was finished
        HighscoreUploadDoneText(state = state.highscoreUpload, modifier = Modifier.fillMaxWidth())

        PlayersCard(state = state, onAction = onAction)

        Text(
            text = stringResource(Res.string.fourinarow_board_info, config.columns, config.rows),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(Res.string.fourinarow_twists_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                // Teams need exactly four players, so the option only shows up then
                if (state.players.size == FOUR_IN_A_ROW_MAX_PLAYERS) {
                    GameOptionSwitchRow(
                        icon = Icons.Default.Groups,
                        title = stringResource(Res.string.fourinarow_teams),
                        description = stringResource(Res.string.fourinarow_teams_description),
                        checked = setup.teams,
                        onCheckedChange = { changeSetup(setup.copy(teams = it)) },
                    )
                }
                GameOptionSwitchRow(
                    icon = Icons.Default.LocalFireDepartment,
                    title = stringResource(Res.string.fourinarow_power_discs),
                    description = stringResource(Res.string.fourinarow_power_discs_description),
                    checked = setup.powerDiscs,
                    onCheckedChange = { changeSetup(setup.copy(powerDiscs = it)) },
                )
                GameOptionSwitchRow(
                    icon = Icons.Default.ScreenRotation,
                    title = stringResource(Res.string.fourinarow_board_twist),
                    description = stringResource(Res.string.fourinarow_board_twist_description, config.turnsPerTwist),
                    checked = setup.boardTwist,
                    onCheckedChange = { changeSetup(setup.copy(boardTwist = it)) },
                )
                GameOptionSwitchRow(
                    icon = Icons.Default.VerticalAlignBottom,
                    title = stringResource(Res.string.fourinarow_pop_out),
                    description = stringResource(Res.string.fourinarow_pop_out_description),
                    checked = setup.popOut,
                    onCheckedChange = { changeSetup(setup.copy(popOut = it)) },
                )
                GameOptionSwitchRow(
                    icon = Icons.Default.Timer,
                    title = stringResource(Res.string.fourinarow_move_timer),
                    description = stringResource(Res.string.fourinarow_move_timer_description),
                    checked = setup.moveSeconds != null,
                    onCheckedChange = { on -> changeSetup(setup.copy(moveSeconds = if (on) DEFAULT_MOVE_SECONDS else null)) },
                )
                if (setup.moveSeconds != null) {
                    ChoiceChips(
                        options = FOUR_IN_A_ROW_MOVE_SECONDS,
                        selected = setup.moveSeconds,
                        label = { stringResource(Res.string.fourinarow_seconds, it) },
                        onSelect = { changeSetup(setup.copy(moveSeconds = it)) },
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(Res.string.fourinarow_series_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ChoiceChips(
                    options = listOf(null) + FOUR_IN_A_ROW_BEST_OF,
                    selected = setup.bestOf,
                    label = { bestOf ->
                        if (bestOf == null) {
                            stringResource(Res.string.fourinarow_series_open)
                        } else {
                            stringResource(Res.string.fourinarow_best_of, bestOf)
                        }
                    },
                    onSelect = { changeSetup(setup.copy(bestOf = it)) },
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onAction(FourInARowAction.OnStartGame) },
        enabled = state.canStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(Res.string.fourinarow_start))
    }
}

@Composable
private fun PlayersCard(state: FourInARowState, onAction: (FourInARowAction) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.fourinarow_players_title),
                style = MaterialTheme.typography.titleMedium,
            )
            state.players.forEachIndexed { index, player ->
                SetupPlayerRow(
                    index = index,
                    player = player,
                    onRemove = { onAction(FourInARowAction.OnRemovePlayer(index)) },
                )
            }
            if (state.players.size > FOUR_IN_A_ROW_MAX_PLAYERS) {
                Text(
                    text = stringResource(Res.string.fourinarow_too_many_players, FOUR_IN_A_ROW_MAX_PLAYERS),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = stringResource(Res.string.fourinarow_players_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = { onAction(FourInARowAction.OnShowPlayerSelector) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.fourinarow_choose_players))
            }
        }
    }
}

@Composable
private fun SetupPlayerRow(index: Int, player: GamePlayer, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Only the first four play; more are listed so they can be removed
        if (index < FOUR_IN_A_ROW_MAX_PLAYERS) {
            PlayerSymbol(player = index, modifier = Modifier.size(22.dp))
        } else {
            Spacer(Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = player.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.remove))
        }
    }
}

@Composable
private fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

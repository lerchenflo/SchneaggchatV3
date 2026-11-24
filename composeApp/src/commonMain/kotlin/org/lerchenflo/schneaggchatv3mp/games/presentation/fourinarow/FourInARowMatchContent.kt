package org.lerchenflo.schneaggchatv3mp.games.presentation.fourinarow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FourInARowRound
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.PowerDisc
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.TwistKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSymbol
import org.lerchenflo.schneaggchatv3mp.games.presentation.playerColor
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_anvil
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_anvil_armed
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_bomb
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_bomb_armed
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_draw
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_finish_match
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_joker
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_joker_armed
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_next_round
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_pop_out
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_pop_out_armed
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_round
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_round_best_of
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_series_winner
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_team_name
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_time_left
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_time_up
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_turn
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_twist_countdown
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_twist_flip
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_twist_row
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_undo
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_winner

@Composable
fun ColumnScope.FourInARowMatchContent(
    state: FourInARowState,
    round: FourInARowRound,
    onAction: (FourInARowAction) -> Unit,
) {
    val bestOf = state.setup.bestOf
    Text(
        text = if (bestOf == null) {
            stringResource(Res.string.fourinarow_round, state.roundNumber)
        } else {
            stringResource(Res.string.fourinarow_round_best_of, state.roundNumber, bestOf)
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))

    Scoreboard(state = state, round = round)

    Spacer(Modifier.height(12.dp))
    StatusLine(state = state, round = round)
    Spacer(Modifier.height(12.dp))

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FourInARowBoard(
            round = round,
            popOutArmed = state.armedTool == FourInARowTool.PopOut,
            onColumnClick = { onAction(FourInARowAction.OnColumnClick(it)) },
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(12.dp))

    if (round.isOver) {
        RoundOverButtons(seriesOver = state.seriesWinner != null, onAction = onAction)
    } else {
        ToolRow(state = state, round = round, onAction = onAction)
    }
}

/** A side's name: the player, or with teams both team members. */
@Composable
private fun sideName(state: FourInARowState, round: FourInARowRound, side: Int): String {
    if (!round.config.hasTeams) return state.players.getOrNull(side)?.name.orEmpty()
    val members = state.players.filterIndexed { index, _ -> round.config.sideOf(index) == side }
    return stringResource(
        Res.string.fourinarow_team_name,
        members.getOrNull(0)?.name.orEmpty(),
        members.getOrNull(1)?.name.orEmpty(),
    )
}

@Composable
private fun Scoreboard(state: FourInARowState, round: FourInARowRound) {
    val config = round.config
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (side in 0 until config.sideCount) {
            val active = !round.isOver && config.sideOf(round.currentPlayer) == side || round.winnerSide == side
            val scale by animateFloatAsState(if (active) 1.05f else 1f, label = "scoreScale")
            val borderColor by animateColorAsState(
                if (active) playerColor(side) else MaterialTheme.colorScheme.outlineVariant,
                label = "scoreBorder",
            )
            val members = (0 until config.playerCount).filter { config.sideOf(it) == side }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale),
                border = BorderStroke(if (active) 2.dp else 1.dp, borderColor),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        members.forEach { player ->
                            PlayerSymbol(player = player, color = playerColor(side), modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = sideName(state, round, side),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.wins.getOrElse(side) { 0 }.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = playerColor(side),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLine(state: FourInARowState, round: FourInARowRound) {
    val winner = round.winnerSide
    val seriesWinner = state.seriesWinner
    val currentName = state.players.getOrNull(round.currentPlayer)?.name.orEmpty()
    val (text, color) = when {
        seriesWinner != null ->
            stringResource(Res.string.fourinarow_series_winner, sideName(state, round, seriesWinner)) to playerColor(seriesWinner)
        winner != null -> stringResource(Res.string.fourinarow_winner, sideName(state, round, winner)) to playerColor(winner)
        round.draw -> stringResource(Res.string.fourinarow_draw) to MaterialTheme.colorScheme.onSurface
        state.armedTool != null -> stringResource(armedHint(state.armedTool)) to MaterialTheme.colorScheme.error
        else -> stringResource(Res.string.fourinarow_turn, currentName) to sideColor(round, round.currentPlayer)
    }
    val scale by animateFloatAsState(if (round.isOver) 1.15f else 1f, label = "statusScale")

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.scale(scale)) {
            if (!round.isOver && state.armedTool == null) {
                PlayerSymbol(
                    player = round.currentPlayer,
                    color = sideColor(round, round.currentPlayer),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
        if (!round.isOver) {
            TurnInfoLines(state = state, round = round)
        }
    }
}

/** Timer, what the last turn did to the board, and how long until the next twist. */
@Composable
private fun TurnInfoLines(state: FourInARowState, round: FourInARowRound) {
    val secondsLeft = state.secondsLeft
    if (secondsLeft != null) {
        Text(
            text = stringResource(Res.string.fourinarow_time_left, secondsLeft),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (secondsLeft <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    round.lastSkippedPlayer?.let { skipped ->
        Text(
            text = stringResource(Res.string.fourinarow_time_up, state.players.getOrNull(skipped)?.name.orEmpty()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
    val twist = round.lastTwist
    val twistText = when {
        twist?.kind == TwistKind.FLIP -> stringResource(Res.string.fourinarow_twist_flip)
        twist != null -> stringResource(Res.string.fourinarow_twist_row, twist.row + 1)
        else -> round.turnsUntilTwist?.let { stringResource(Res.string.fourinarow_twist_countdown, it) }
    }
    if (twistText != null) {
        Text(
            text = twistText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (twist != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun armedHint(tool: FourInARowTool): StringResource = when (tool) {
    FourInARowTool.PopOut -> Res.string.fourinarow_pop_out_armed
    is FourInARowTool.Power -> when (tool.power) {
        PowerDisc.BOMB -> Res.string.fourinarow_bomb_armed
        PowerDisc.ANVIL -> Res.string.fourinarow_anvil_armed
        PowerDisc.JOKER -> Res.string.fourinarow_joker_armed
    }
}

@Composable
private fun RoundOverButtons(seriesOver: Boolean, onAction: (FourInARowAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (seriesOver) {
            // The series is decided: only finishing (and the leaderboard offer) is left
            Button(
                onClick = { onAction(FourInARowAction.OnFinishMatch) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.fourinarow_finish_match), textAlign = TextAlign.Center)
            }
        } else {
            OutlinedButton(
                onClick = { onAction(FourInARowAction.OnFinishMatch) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.fourinarow_finish_match), textAlign = TextAlign.Center)
            }
            Button(
                onClick = { onAction(FourInARowAction.OnNextRound) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.fourinarow_next_round), textAlign = TextAlign.Center)
            }
        }
    }
}

/** Undo plus the tools the twists allow: the three power discs and pop-out. */
@Composable
private fun ToolRow(
    state: FourInARowState,
    round: FourInARowRound,
    onAction: (FourInARowAction) -> Unit,
) {
    val player = round.currentPlayer
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        ToolChip(
            icon = Icons.AutoMirrored.Filled.Undo,
            label = stringResource(Res.string.fourinarow_undo),
            selected = false,
            enabled = state.canUndo,
            onClick = { onAction(FourInARowAction.OnUndo) },
        )
        if (round.config.powerDiscs) {
            PowerDisc.entries.forEach { power ->
                val tool = FourInARowTool.Power(power)
                ToolChip(
                    icon = powerIcon(power),
                    label = stringResource(powerLabel(power)),
                    selected = state.armedTool == tool,
                    enabled = round.canUsePower(player, power),
                    onClick = { onAction(FourInARowAction.OnToggleTool(tool)) },
                )
            }
        }
        if (round.config.popOut) {
            ToolChip(
                icon = Icons.Default.VerticalAlignBottom,
                label = stringResource(Res.string.fourinarow_pop_out),
                selected = state.armedTool == FourInARowTool.PopOut,
                enabled = round.canPopAny(player),
                onClick = { onAction(FourInARowAction.OnToggleTool(FourInARowTool.PopOut)) },
            )
        }
    }
}

@Composable
private fun ToolChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
    )
}

private fun powerIcon(power: PowerDisc): ImageVector = when (power) {
    PowerDisc.BOMB -> Icons.Default.LocalFireDepartment
    PowerDisc.ANVIL -> Icons.Default.Anchor
    PowerDisc.JOKER -> Icons.Default.Star
}

private fun powerLabel(power: PowerDisc): StringResource = when (power) {
    PowerDisc.BOMB -> Res.string.fourinarow_bomb
    PowerDisc.ANVIL -> Res.string.fourinarow_anvil
    PowerDisc.JOKER -> Res.string.fourinarow_joker
}

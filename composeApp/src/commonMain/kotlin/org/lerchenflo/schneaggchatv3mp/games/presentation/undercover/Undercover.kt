package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameResetButton
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadDialog
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadDoneText
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector.PlayerSelector
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_undercover_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_add
import schneaggchatv3mp.composeapp.generated.resources.undercover_autohide_enabled
import schneaggchatv3mp.composeapp.generated.resources.undercover_autohide_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_cancel
import schneaggchatv3mp.composeapp.generated.resources.undercover_continue
import schneaggchatv3mp.composeapp.generated.resources.undercover_discussion_instructions
import schneaggchatv3mp.composeapp.generated.resources.undercover_discussion_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_eliminate
import schneaggchatv3mp.composeapp.generated.resources.undercover_eliminated_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_game_over
import schneaggchatv3mp.composeapp.generated.resources.undercover_hide_and_pass_phone
import schneaggchatv3mp.composeapp.generated.resources.undercover_i_am_player
import schneaggchatv3mp.composeapp.generated.resources.undercover_minus
import schneaggchatv3mp.composeapp.generated.resources.undercover_mr_white_cannot_start
import schneaggchatv3mp.composeapp.generated.resources.undercover_mr_white_guess_instructions
import schneaggchatv3mp.composeapp.generated.resources.undercover_mr_white_guess_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_mr_white_tip_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_mr_white_tip_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_new_game
import schneaggchatv3mp.composeapp.generated.resources.undercover_no_player
import schneaggchatv3mp.composeapp.generated.resources.undercover_no_valid_starter
import schneaggchatv3mp.composeapp.generated.resources.undercover_pass_phone_to
import schneaggchatv3mp.composeapp.generated.resources.undercover_players_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_plus
import schneaggchatv3mp.composeapp.generated.resources.undercover_remove
import schneaggchatv3mp.composeapp.generated.resources.undercover_reset
import schneaggchatv3mp.composeapp.generated.resources.undercover_restart_same_players
import schneaggchatv3mp.composeapp.generated.resources.undercover_reveal_mr_white
import schneaggchatv3mp.composeapp.generated.resources.undercover_reveal_word_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_role_civilian
import schneaggchatv3mp.composeapp.generated.resources.undercover_role_mr_white
import schneaggchatv3mp.composeapp.generated.resources.undercover_role_undercover
import schneaggchatv3mp.composeapp.generated.resources.undercover_role_was_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_roles_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_button
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_close
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_description_phase
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_description_phase_text
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_discussion_phase
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_discussion_phase_text
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_elimination_phase
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_elimination_phase_text
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_explanation
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_explanation_text
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_gameplay
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_roles
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_roles_text
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_secret_word
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_secret_word_text
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_victory
import schneaggchatv3mp.composeapp.generated.resources.undercover_rules_victory_text
import schneaggchatv3mp.composeapp.generated.resources.undercover_seconds_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_sniff_button
import schneaggchatv3mp.composeapp.generated.resources.undercover_sniff_confirm_message
import schneaggchatv3mp.composeapp.generated.resources.undercover_sniff_confirm_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_sniff_confirm_yes
import schneaggchatv3mp.composeapp.generated.resources.undercover_sniff_hide
import schneaggchatv3mp.composeapp.generated.resources.undercover_sniff_select_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_start_game
import schneaggchatv3mp.composeapp.generated.resources.undercover_start_voting
import schneaggchatv3mp.composeapp.generated.resources.undercover_starting_player_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_starting_player_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_submit_guess
import schneaggchatv3mp.composeapp.generated.resources.undercover_voting_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_word_guess_label

@Composable
fun UndercoverRoot(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: UndercoverViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Leaving the screen (e.g. opening a chat from a notification) keeps the running game
    DisposableEffect(Unit) {
        onDispose { viewModel.persist() }
    }

    UndercoverScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UndercoverScreen(
    state: UndercoverState,
    onAction: (UndercoverAction) -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_undercover_title),
            onBackClick = onBackClick,
            // Back to setup with the same players and settings; also drops the saved game
            actions = {
                if (state.phase != UndercoverPhase.SETUP) {
                    GameResetButton(onReset = { onAction(UndercoverAction.OnResetGame) })
                }
            }
        )
        when (state.phase) {
            UndercoverPhase.SETUP -> SetupPhase(state = state, onAction = onAction)
            UndercoverPhase.PASS_PHONE -> PassPhonePhase(state = state, onAction = onAction)
            UndercoverPhase.REVEAL -> RevealPhase(state = state, onAction = onAction)
            UndercoverPhase.CHOOSE_STARTER -> ChooseStarterPhase(state = state, onAction = onAction)
            UndercoverPhase.DISCUSSION -> DiscussionPhase(onAction = onAction)
            UndercoverPhase.VOTING -> VotingPhase(state = state, onAction = onAction)
            UndercoverPhase.MR_WHITE_GUESS -> MrWhiteGuessPhase(state = state, onAction = onAction)
            UndercoverPhase.GAME_OVER -> GameOverPhase(state = state, onAction = onAction)
        }

        if (state.canSniff) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onAction(UndercoverAction.OnOpenSniff) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.undercover_sniff_button))
            }
        }
    }

    SniffDialogs(state = state, onAction = onAction)

    // Asked once the game is decided - wins only reach the leaderboard on explicit confirmation
    HighscoreUploadDialog(
        state = state.highscoreUpload,
        onUpload = { onAction(UndercoverAction.OnUploadHighscores) },
        onDecline = { onAction(UndercoverAction.OnDeclineHighscoreUpload) },
    )

    if (state.showRulesDialog) {
        RulesDialog(onAction = onAction)
    }

    if (state.showPlayerSelector) {
        PlayerSelector(
            onDismiss = { onAction(UndercoverAction.OnHidePlayerSelector) },
            onFinish = { selectedPlayers -> onAction(UndercoverAction.OnPlayersSelected(selectedPlayers)) }
        )
    }
}

@Composable
private fun ColumnScope.SetupPhase(
    state: UndercoverState,
    onAction: (UndercoverAction) -> Unit,
) {
    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { onAction(UndercoverAction.OnShowPlayerSelector) },
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(Res.string.undercover_add))
        }
    }

    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(Res.string.undercover_players_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(8.dp))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.setupPlayers) { name ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = name)
                    Button(onClick = { onAction(UndercoverAction.OnRemoveSetupPlayer(name)) }) {
                        Text(stringResource(Res.string.undercover_remove))
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(Res.string.undercover_roles_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            RoleCounterRow(
                title = stringResource(Res.string.undercover_role_mr_white),
                value = state.setupMrWhiteCount,
                onMinus = { onAction(UndercoverAction.OnDecrementMrWhiteCount) },
                onPlus = { onAction(UndercoverAction.OnIncrementMrWhiteCount) }
            )

            Spacer(Modifier.height(8.dp))

            RoleCounterRow(
                title = stringResource(Res.string.undercover_role_undercover),
                value = state.setupUndercoverCount,
                onMinus = { onAction(UndercoverAction.OnDecrementUndercoverCount) },
                onPlus = { onAction(UndercoverAction.OnIncrementUndercoverCount) }
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.undercover_autohide_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = state.autoHideEnabled,
                    onCheckedChange = { onAction(UndercoverAction.OnToggleAutoHide(it)) }
                )
            }

            if (state.autoHideEnabled) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(Res.string.undercover_seconds_format, state.autoHideSeconds))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAction(UndercoverAction.OnDecrementAutoHideSeconds) }) {
                            Text(stringResource(Res.string.undercover_minus))
                        }
                        Button(onClick = { onAction(UndercoverAction.OnIncrementAutoHideSeconds) }) {
                            Text(stringResource(Res.string.undercover_plus))
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.undercover_mr_white_tip_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Switch(
                checked = state.mrWhiteTipEnabled,
                onCheckedChange = { onAction(UndercoverAction.OnToggleMrWhiteTip(it)) }
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onAction(UndercoverAction.OnShowRules) },
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(Res.string.undercover_rules_button))
        }
        Button(
            onClick = { onAction(UndercoverAction.OnStartGame) },
            enabled = state.canStartGame,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(Res.string.undercover_start_game))
        }
    }
}

@Composable
private fun ColumnScope.PassPhonePhase(
    state: UndercoverState,
    onAction: (UndercoverAction) -> Unit,
) {
    val name = state.currentRevealPlayerName

    Spacer(Modifier.weight(1f))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.undercover_pass_phone_to, name),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onAction(UndercoverAction.OnConfirmPlayerIdentity) }) {
                Text(stringResource(Res.string.undercover_i_am_player, name))
            }
        }
    }

    Spacer(Modifier.weight(1f))
}

@Composable
private fun ColumnScope.RevealPhase(
    state: UndercoverState,
    onAction: (UndercoverAction) -> Unit,
) {
    val player = state.currentRevealPlayer
    if (player == null) {
        Text(stringResource(Res.string.undercover_no_player))
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onAction(UndercoverAction.OnResetGame) }) {
            Text(stringResource(Res.string.undercover_reset))
        }
        return
    }

    Spacer(Modifier.weight(1f))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.revealWord?.let { stringResource(Res.string.undercover_reveal_word_format, it) }
                    ?: stringResource(Res.string.undercover_reveal_mr_white),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            state.revealMrWhiteTip?.let { tip ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.undercover_mr_white_tip_format, tip),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onAction(UndercoverAction.OnHideAndPassPhone) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.undercover_hide_and_pass_phone))
            }
            if (state.autoHideEnabled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.undercover_autohide_enabled),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    Spacer(Modifier.weight(1f))
}

@Composable
private fun ColumnScope.ChooseStarterPhase(
    state: UndercoverState,
    onAction: (UndercoverAction) -> Unit,
) {
    LaunchedEffect(Unit) {
        onAction(UndercoverAction.OnPickRandomStarter)
    }

    Text(
        text = stringResource(Res.string.undercover_starting_player_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(Res.string.undercover_mr_white_cannot_start),
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(12.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.selectedStarterName
                    ?.let { stringResource(Res.string.undercover_starting_player_format, it) }
                    ?: stringResource(Res.string.undercover_no_valid_starter),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(Modifier.weight(1f))

    Spacer(Modifier.height(12.dp))

    Button(
        onClick = { onAction(UndercoverAction.OnConfirmStarter) },
        enabled = state.selectedStarterPlayerId != null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(Res.string.undercover_continue))
    }
}

@Composable
private fun ColumnScope.DiscussionPhase(onAction: (UndercoverAction) -> Unit) {
    Text(
        text = stringResource(Res.string.undercover_discussion_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.undercover_discussion_instructions),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    Spacer(Modifier.weight(1f))

    Button(
        onClick = { onAction(UndercoverAction.OnStartVoting) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(Res.string.undercover_start_voting))
    }
}

@Composable
private fun ColumnScope.VotingPhase(
    state: UndercoverState,
    onAction: (UndercoverAction) -> Unit,
) {
    Text(
        text = stringResource(Res.string.undercover_voting_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))

    val result = state.votingResult
    if (result == null) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.votingCandidates, key = { it.id }) { player ->
                val isSelected = state.votingSelectedPlayerId == player.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(UndercoverAction.OnSelectVote(player.id)) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onAction(UndercoverAction.OnSelectVote(player.id)) }
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(text = player.name)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onAction(UndercoverAction.OnConfirmVote) },
            enabled = state.votingSelectedPlayerId != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.undercover_eliminate))
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.undercover_eliminated_format, result.eliminatedPlayerName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.undercover_role_was_format, roleText(result.revealedRole)),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onAction(UndercoverAction.OnContinueAfterVotingResult) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.undercover_continue))
        }
    }
}

@Composable
private fun roleText(role: UndercoverRole): String = when (role) {
    UndercoverRole.CIVILIAN -> stringResource(Res.string.undercover_role_civilian)
    UndercoverRole.UNDERCOVER -> stringResource(Res.string.undercover_role_undercover)
    UndercoverRole.MR_WHITE -> stringResource(Res.string.undercover_role_mr_white)
}

@Composable
private fun ColumnScope.MrWhiteGuessPhase(
    state: UndercoverState,
    onAction: (UndercoverAction) -> Unit,
) {
    Text(
        text = stringResource(Res.string.undercover_mr_white_guess_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(Res.string.undercover_mr_white_guess_instructions),
        style = MaterialTheme.typography.bodyLarge
    )
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = state.mrWhiteGuessInput,
        onValueChange = { onAction(UndercoverAction.OnMrWhiteGuessChange(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.undercover_word_guess_label)) },
        singleLine = true
    )

    Spacer(Modifier.height(12.dp))

    Button(
        onClick = { onAction(UndercoverAction.OnSubmitMrWhiteGuess) },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.mrWhiteGuessInput.trim().isNotEmpty()
    ) {
        Text(stringResource(Res.string.undercover_submit_guess))
    }
}

@Composable
private fun ColumnScope.GameOverPhase(
    state: UndercoverState,
    onAction: (UndercoverAction) -> Unit,
) {
    Spacer(Modifier.weight(1f))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.undercover_game_over),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.winnerText?.asString() ?: "",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            HighscoreUploadDoneText(
                state = state.highscoreUpload,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { onAction(UndercoverAction.OnRestartWithSamePlayers) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(Res.string.undercover_restart_same_players))
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = { onAction(UndercoverAction.OnResetGame) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(Res.string.undercover_new_game))
    }

    Spacer(Modifier.weight(1f))
}

@Composable
private fun RoleCounterRow(
    title: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onMinus) { Text(stringResource(Res.string.undercover_minus)) }
            Text(text = value.toString(), fontWeight = FontWeight.SemiBold)
            Button(onClick = onPlus) { Text(stringResource(Res.string.undercover_plus)) }
        }
    }
}

/**
 * Lets a player look at their own word again mid-game: pick your name, confirm it is really you
 * (so a mis-tap never exposes someone else's word), then only that player's word is shown.
 */
@Composable
private fun SniffDialogs(state: UndercoverState, onAction: (UndercoverAction) -> Unit) {
    when (state.sniffStep) {
        SniffStep.CLOSED -> Unit

        SniffStep.SELECT_PLAYER -> {
            AlertDialog(
                onDismissRequest = { onAction(UndercoverAction.OnCloseSniff) },
                icon = { Icon(imageVector = Icons.Default.Visibility, contentDescription = null) },
                title = { Text(stringResource(Res.string.undercover_sniff_select_title)) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.sniffCandidates, key = { it.id }) { player ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAction(UndercoverAction.OnSelectSniffPlayer(player.id)) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { onAction(UndercoverAction.OnCloseSniff) }) {
                        Text(stringResource(Res.string.undercover_cancel))
                    }
                }
            )
        }

        SniffStep.CONFIRM_IDENTITY -> {
            val player = state.sniffPlayer ?: return
            AlertDialog(
                onDismissRequest = { onAction(UndercoverAction.OnCloseSniff) },
                title = {
                    Text(
                        text = stringResource(Res.string.undercover_sniff_confirm_title, player.name),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = stringResource(Res.string.undercover_sniff_confirm_message, player.name),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                confirmButton = {
                    Button(onClick = { onAction(UndercoverAction.OnConfirmSniffIdentity) }) {
                        Text(stringResource(Res.string.undercover_sniff_confirm_yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(UndercoverAction.OnCloseSniff) }) {
                        Text(stringResource(Res.string.undercover_cancel))
                    }
                }
            )
        }

        SniffStep.REVEAL -> {
            val player = state.sniffPlayer ?: return
            AlertDialog(
                onDismissRequest = { onAction(UndercoverAction.OnCloseSniff) },
                title = {
                    Text(
                        text = player.name,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.sniffWord?.let { stringResource(Res.string.undercover_reveal_word_format, it) }
                                ?: stringResource(Res.string.undercover_reveal_mr_white),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        state.sniffMrWhiteTip?.let { tip ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.undercover_mr_white_tip_format, tip),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (state.autoHideEnabled) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.undercover_autohide_enabled),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { onAction(UndercoverAction.OnCloseSniff) }) {
                        Text(stringResource(Res.string.undercover_sniff_hide))
                    }
                }
            )
        }
    }
}

@Composable
private fun RulesDialog(onAction: (UndercoverAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(UndercoverAction.OnHideRules) },
        title = {
            Text(
                text = stringResource(Res.string.undercover_rules_button),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                RulesSection(
                    title = stringResource(Res.string.undercover_rules_explanation),
                    body = stringResource(Res.string.undercover_rules_explanation_text),
                )
                Spacer(Modifier.height(16.dp))
                RulesSection(
                    title = stringResource(Res.string.undercover_rules_roles),
                    body = stringResource(Res.string.undercover_rules_roles_text),
                )
                Spacer(Modifier.height(16.dp))
                RulesSection(
                    title = stringResource(Res.string.undercover_rules_secret_word),
                    body = stringResource(Res.string.undercover_rules_secret_word_text),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.undercover_rules_gameplay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                RulesSection(
                    title = stringResource(Res.string.undercover_rules_description_phase),
                    body = stringResource(Res.string.undercover_rules_description_phase_text),
                    titleStyle = RulesTitleStyle.SUB,
                )
                Spacer(Modifier.height(12.dp))
                RulesSection(
                    title = stringResource(Res.string.undercover_rules_discussion_phase),
                    body = stringResource(Res.string.undercover_rules_discussion_phase_text),
                    titleStyle = RulesTitleStyle.SUB,
                )
                Spacer(Modifier.height(12.dp))
                RulesSection(
                    title = stringResource(Res.string.undercover_rules_elimination_phase),
                    body = stringResource(Res.string.undercover_rules_elimination_phase_text),
                    titleStyle = RulesTitleStyle.SUB,
                )
                Spacer(Modifier.height(16.dp))
                RulesSection(
                    title = stringResource(Res.string.undercover_rules_victory),
                    body = stringResource(Res.string.undercover_rules_victory_text),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(UndercoverAction.OnHideRules) }) {
                Text(stringResource(Res.string.undercover_rules_close))
            }
        }
    )
}

private enum class RulesTitleStyle { MAIN, SUB }

@Composable
private fun RulesSection(title: String, body: String, titleStyle: RulesTitleStyle = RulesTitleStyle.MAIN) {
    Text(
        text = title,
        style = if (titleStyle == RulesTitleStyle.MAIN) {
            MaterialTheme.typography.titleMedium
        } else {
            MaterialTheme.typography.titleSmall
        },
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Preview
@Composable
private fun UndercoverScreenPreview() {
    UndercoverScreen(
        state = UndercoverState(
            setupPlayers = listOf("Flo", "Manu", "Lisa"),
            wordListReady = true,
        ),
        onAction = {},
    )
}

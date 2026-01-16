package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.undercover_add
import schneaggchatv3mp.composeapp.generated.resources.undercover_autohide_enabled
import schneaggchatv3mp.composeapp.generated.resources.undercover_autohide_title
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
import schneaggchatv3mp.composeapp.generated.resources.undercover_new_game
import schneaggchatv3mp.composeapp.generated.resources.undercover_no_player
import schneaggchatv3mp.composeapp.generated.resources.undercover_no_valid_starter
import schneaggchatv3mp.composeapp.generated.resources.undercover_pass_phone_to
import schneaggchatv3mp.composeapp.generated.resources.undercover_player_name_label
import schneaggchatv3mp.composeapp.generated.resources.undercover_players_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_plus
import schneaggchatv3mp.composeapp.generated.resources.undercover_remove
import schneaggchatv3mp.composeapp.generated.resources.undercover_reveal_mr_white
import schneaggchatv3mp.composeapp.generated.resources.undercover_reveal_word_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_reset
import schneaggchatv3mp.composeapp.generated.resources.undercover_role_civilian
import schneaggchatv3mp.composeapp.generated.resources.undercover_role_mr_white
import schneaggchatv3mp.composeapp.generated.resources.undercover_role_undercover
import schneaggchatv3mp.composeapp.generated.resources.undercover_role_was_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_roles_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_seconds_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_start_game
import schneaggchatv3mp.composeapp.generated.resources.undercover_start_voting
import schneaggchatv3mp.composeapp.generated.resources.undercover_starting_player_format
import schneaggchatv3mp.composeapp.generated.resources.undercover_starting_player_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_submit_guess
import schneaggchatv3mp.composeapp.generated.resources.undercover_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_voting_title
import schneaggchatv3mp.composeapp.generated.resources.undercover_word_guess_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Undercover(
    modifier: Modifier = Modifier
) {
    val viewModel = koinInject<UndercoverViewModel>()
    val state = viewModel.state

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (state.phase) {
                UndercoverViewModel.Phase.SETUP -> {
                    Text(
                        text = stringResource(Res.string.undercover_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                   
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.setupPlayerNameInput,
                            onValueChange = viewModel::updateSetupPlayerNameInput,
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(Res.string.undercover_player_name_label)) },
                            singleLine = true
                        )
                        Button(
                            onClick = viewModel::addSetupPlayer
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
                                    Button(onClick = { viewModel.removeSetupPlayer(name) }) {
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
                                onMinus = viewModel::decrementMrWhiteCount,
                                onPlus = viewModel::incrementMrWhiteCount
                            )

                            Spacer(Modifier.height(8.dp))

                            RoleCounterRow(
                                title = stringResource(Res.string.undercover_role_undercover),
                                value = state.setupUndercoverCount,
                                onMinus = viewModel::decrementUndercoverCount,
                                onPlus = viewModel::incrementUndercoverCount
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
                                    onCheckedChange = viewModel::toggleAutoHide
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
                                        Button(onClick = viewModel::decrementAutoHideSeconds) { Text(stringResource(Res.string.undercover_minus)) }
                                        Button(onClick = viewModel::incrementAutoHideSeconds) { Text(stringResource(Res.string.undercover_plus)) }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = viewModel::startGame,
                        enabled = viewModel.canStartGame(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.undercover_start_game))
                    }
                }

                UndercoverViewModel.Phase.PASS_PHONE -> {
                    val name = viewModel.currentRevealPlayerNameOrEmpty()

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
                            Button(onClick = viewModel::onConfirmPlayerIdentity) {
                                Text(stringResource(Res.string.undercover_i_am_player, name))
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                }

                UndercoverViewModel.Phase.REVEAL -> {
                    val player = viewModel.currentRevealPlayerOrNull()
                    if (player == null) {
                        Text(stringResource(Res.string.undercover_no_player))
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = viewModel::resetGame) { Text(stringResource(Res.string.undercover_reset)) }
                    } else {
                        val word = viewModel.getWordForPlayer(player)

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
                                //Spacer(Modifier.height(12.dp))
                                //Text(
                                //    text = "Role: $role",
                                //    style = MaterialTheme.typography.titleLarge
                                //)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = word?.let { stringResource(Res.string.undercover_reveal_word_format, it) }
                                        ?: stringResource(Res.string.undercover_reveal_mr_white),
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = viewModel::onHideAndPassPhone,
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
                }


                UndercoverViewModel.Phase.CHOOSE_STARTER -> {
                    LaunchedEffect(Unit) {
                        viewModel.selectRandomStarterIfNeeded()
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

                    val selectedName = state.selectedStarterPlayerId
                        ?.let { viewModel.getPlayerNameById(it) }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = selectedName?.let { stringResource(Res.string.undercover_starting_player_format, it) }
                                    ?: stringResource(Res.string.undercover_no_valid_starter),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = viewModel::confirmStarter,
                        enabled = state.selectedStarterPlayerId != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.undercover_continue))
                    }
                }

                UndercoverViewModel.Phase.DISCUSSION -> {
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
                        onClick = viewModel::startVoting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.undercover_start_voting))
                    }
                }

                UndercoverViewModel.Phase.VOTING -> {
                    Text(
                        text = stringResource(Res.string.undercover_voting_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    val result = state.votingResult
                    if (result == null) {
                        val candidates = viewModel.votingCandidates()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(candidates) { p ->
                                val isSelected = state.votingSelectedPlayerId == p.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectVote(p.id) },
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
                                                onClick = { viewModel.selectVote(p.id) }
                                            )
                                            Spacer(Modifier.size(8.dp))
                                            Text(text = p.name)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = viewModel::confirmVote,
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
                                val roleText = when (result.revealedRole) {
                                    UndercoverViewModel.ActualRole.CIVILIAN -> stringResource(Res.string.undercover_role_civilian)
                                    UndercoverViewModel.ActualRole.UNDERCOVER -> stringResource(Res.string.undercover_role_undercover)
                                    UndercoverViewModel.ActualRole.MR_WHITE -> stringResource(Res.string.undercover_role_mr_white)
                                }
                                Text(
                                    text = stringResource(Res.string.undercover_role_was_format, roleText),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = viewModel::onContinueAfterVotingResult,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.undercover_continue))
                        }
                    }
                }

                UndercoverViewModel.Phase.MR_WHITE_GUESS -> {
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
                        onValueChange = viewModel::updateMrWhiteGuessInput,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.undercover_word_guess_label)) },
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = viewModel::submitMrWhiteGuess,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.mrWhiteGuessInput.trim().isNotEmpty()
                    ) {
                        Text(stringResource(Res.string.undercover_submit_guess))
                    }
                }

                UndercoverViewModel.Phase.GAME_OVER -> {
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
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = viewModel::resetGame,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.undercover_new_game))
                    }

                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
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

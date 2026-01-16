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
import org.koin.compose.koinInject

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
                        text = "Undercover",
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
                            label = { Text("Player name") },
                            singleLine = true
                        )
                        Button(
                            onClick = viewModel::addSetupPlayer
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Players",
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
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Roles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))

                            RoleCounterRow(
                                title = "Mr. White",
                                value = state.setupMrWhiteCount,
                                onMinus = viewModel::decrementMrWhiteCount,
                                onPlus = viewModel::incrementMrWhiteCount
                            )

                            Spacer(Modifier.height(8.dp))

                            RoleCounterRow(
                                title = "Undercover",
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
                                    text = "Auto-hide reveal",
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
                                    Text(text = "Seconds: ${state.autoHideSeconds}")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = viewModel::decrementAutoHideSeconds) { Text("-") }
                                        Button(onClick = viewModel::incrementAutoHideSeconds) { Text("+") }
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
                        Text("Start Game")
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
                                text = "Pass the phone to $name",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = viewModel::onConfirmPlayerIdentity) {
                                Text("I am $name")
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                }

                UndercoverViewModel.Phase.REVEAL -> {
                    val player = viewModel.currentRevealPlayerOrNull()
                    if (player == null) {
                        Text("No player")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = viewModel::resetGame) { Text("Reset") }
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
                                    text = word?.let { "Word: $it" } ?: "You are Mr White",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = viewModel::onHideAndPassPhone,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Hide & Pass Phone")
                                }
                                if (state.autoHideEnabled) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Auto-hiding enabled",
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
                        text = "Starting player",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Mr. White cannot start.",
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
                                text = selectedName?.let { "Starting player: $it" } ?: "No valid starter found",
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
                        Text("Continue")
                    }
                }

                UndercoverViewModel.Phase.DISCUSSION -> {
                    Text(
                        text = "Discussion",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Each player describes their word without saying it. Then vote to eliminate someone.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = viewModel::startVoting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Voting")
                    }
                }

                UndercoverViewModel.Phase.VOTING -> {
                    Text(
                        text = "Voting",
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
                            Text("Eliminate")
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
                                    text = "Eliminated: ${result.eliminatedPlayerName}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Role was: ${result.revealedRole}",
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
                            Text("Continue")
                        }
                    }
                }

                UndercoverViewModel.Phase.MR_WHITE_GUESS -> {
                    Text(
                        text = "Mr. White Guess",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Mr. White was eliminated. Enter your guess for the civilian word.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.mrWhiteGuessInput,
                        onValueChange = viewModel::updateMrWhiteGuessInput,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Word guess") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = viewModel::submitMrWhiteGuess,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.mrWhiteGuessInput.trim().isNotEmpty()
                    ) {
                        Text("Submit Guess")
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
                                text = "Game Over",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = state.winnerText ?: "",
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
                        Text("New Game")
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
            Button(onClick = onMinus) { Text("-") }
            Text(text = value.toString(), fontWeight = FontWeight.SemiBold)
            Button(onClick = onPlus) { Text("+") }
        }
    }
}

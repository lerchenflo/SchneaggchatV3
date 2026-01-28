package org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity
import org.lerchenflo.schneaggchatv3mp.chat.domain.User

@Composable
fun PlayerSelector(
    onDismiss: () -> Unit,
    onFinish: (List<String>) -> Unit
) {
    val viewModel = koinInject<PlayerSelectorViewModel>()
    val allPlayers = viewModel.allPlayers
    val selectedPlayers = viewModel.selectedPlayers
    var newPlayerName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Select Players",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Add Player Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newPlayerName,
                        onValueChange = { newPlayerName = it },
                        label = { Text("New Player") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            viewModel.addPlayer(newPlayerName)
                            newPlayerName = ""
                        },
                        enabled = newPlayerName.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Player",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Player List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 300.dp)
                ) {
                    // Local Players Section
                    if (viewModel.localPlayers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Local Players",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(viewModel.localPlayers) { player ->
                            PlayerItem(
                                player = player,
                                isSelected = selectedPlayers.contains(player),
                                onToggleRequest = { viewModel.toggleSelection(player) },
                                onDeleteRequest = { viewModel.deletePlayer(player) }
                            )
                        }
                    }

                    // Friends Section
                    if (viewModel.friends.isNotEmpty()) {
                        if (viewModel.localPlayers.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        
                        item {
                            Text(
                                text = "Friends",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(viewModel.friends) { friend ->
                            FriendItem(
                                friend = friend,
                                isSelected = selectedPlayers.contains(friend),
                                onToggleRequest = { viewModel.toggleSelection(friend) }
                            )
                        }
                    }
                    
                    if (allPlayers.isEmpty()) {
                        item {
                            Text(
                                text = "No players yet. Add some!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onFinish(viewModel.getSelectedPlayerNames())
                            viewModel.clearSelection()
                            onDismiss()
                        },
                        enabled = selectedPlayers.isNotEmpty()
                    ) {
                        Text("Start (${selectedPlayers.size})")
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerItem(
    player: PlayerEntity,
    isSelected: Boolean,
    onToggleRequest: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleRequest)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleRequest() }
        )
        Text(
            text = player.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        IconButton(onClick = onDeleteRequest) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Player",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun FriendItem(
    friend: User,
    isSelected: Boolean,
    onToggleRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleRequest)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleRequest() }
        )
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Friend",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp)
        )
        Text(
            text = friend.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

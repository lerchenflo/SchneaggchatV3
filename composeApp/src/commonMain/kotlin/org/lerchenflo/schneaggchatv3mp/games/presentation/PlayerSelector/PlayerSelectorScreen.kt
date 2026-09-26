package org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerEntity
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer

@Composable
fun PlayerSelector(
    onDismiss: () -> Unit,
    onFinish: (List<GamePlayer>) -> Unit,
    viewModel: PlayerSelectorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PlayerSelectorDialog(
        state = state,
        onAction = viewModel::onAction,
        onDismiss = onDismiss,
        onFinish = onFinish,
    )
}

@Composable
fun PlayerSelectorDialog(
    state: PlayerSelectorState,
    onAction: (PlayerSelectorAction) -> Unit,
    onDismiss: () -> Unit,
    onFinish: (List<GamePlayer>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

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
                        value = state.newPlayerName,
                        onValueChange = { onAction(PlayerSelectorAction.OnNewPlayerNameChange(it)) },
                        label = { Text("New Player") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = { onAction(PlayerSelectorAction.OnAddLocalPlayer) },
                        enabled = state.newPlayerName.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Player",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs with HorizontalPager
                val pagerState = rememberPagerState(pageCount = { 2 })
                val tabIndex = pagerState.currentPage
                
                // Tab Row
                PrimaryTabRow(
                    selectedTabIndex = tabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { 
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                ) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { 
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        text = { 
                            Text(
                                "Local Players",
                                fontWeight = if (tabIndex == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { 
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        text = { 
                            Text(
                                "Friends",
                                fontWeight = if (tabIndex == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HorizontalPager for swipeable content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) { page ->
                    when (page) {
                        0 -> {
                            // Local Players Tab
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (state.localPlayers.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No local players yet. Add some!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }
                                } else {
                                    items(state.localPlayers, key = { it.key }) { player ->
                                        PlayerItem(
                                            player = player.entity,
                                            isSelected = state.isSelected(player),
                                            onToggleRequest = { onAction(PlayerSelectorAction.OnToggleSelection(player)) },
                                            onDeleteRequest = { onAction(PlayerSelectorAction.OnDeleteLocalPlayer(player)) }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Friends Tab
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (state.friends.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No friends available.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }
                                } else {
                                    items(state.friends, key = { it.key }) { friend ->
                                        FriendItem(
                                            friend = friend.user,
                                            isSelected = state.isSelected(friend),
                                            onToggleRequest = { onAction(PlayerSelectorAction.OnToggleSelection(friend)) }
                                        )
                                    }
                                }
                            }
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
                            onFinish(state.selectedGamePlayers)
                            onAction(PlayerSelectorAction.OnClearSelection)
                            onDismiss()
                        },
                        enabled = state.selectedCount > 0
                    ) {
                        Text("Start (${state.selectedCount})")
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
            .padding(vertical = 8.dp, horizontal = 4.dp),
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
            .padding(vertical = 8.dp, horizontal = 4.dp),
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

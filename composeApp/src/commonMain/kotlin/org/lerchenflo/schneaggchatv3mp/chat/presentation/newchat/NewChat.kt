package org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.sharedUi.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.common_friends
import schneaggchatv3mp.composeapp.generated.resources.enter_username
import schneaggchatv3mp.composeapp.generated.resources.friend_request_accept
import schneaggchatv3mp.composeapp.generated.resources.friend_request_cancel
import schneaggchatv3mp.composeapp.generated.resources.friend_request_cancel_title
import schneaggchatv3mp.composeapp.generated.resources.friend_request_deny
import schneaggchatv3mp.composeapp.generated.resources.friend_request_title
import schneaggchatv3mp.composeapp.generated.resources.invite_friend
import schneaggchatv3mp.composeapp.generated.resources.new_chat
import schneaggchatv3mp.composeapp.generated.resources.new_group
import schneaggchatv3mp.composeapp.generated.resources.pending_friend_requests
import schneaggchatv3mp.composeapp.generated.resources.search_results
import schneaggchatv3mp.composeapp.generated.resources.search_user
import schneaggchatv3mp.composeapp.generated.resources.send_friend_request
import schneaggchatv3mp.composeapp.generated.resources.send_friend_request_description

@OptIn(InternalResourceApi::class)
@Composable
fun NewChat(
    modifier: Modifier = Modifier
        .fillMaxWidth()
){
    val viewModel = koinViewModel<NewChatViewModel>()
    val searchterm by viewModel.searchterm.collectAsStateWithLifecycle()
    val newChats by viewModel.availableChats.collectAsStateWithLifecycle()

    val pendingFriends by viewModel.pendingFriends.collectAsStateWithLifecycle()
    val pendingFriendPopup by viewModel.pendingFriendPopup.collectAsStateWithLifecycle()


    Scaffold {
        Column(
            modifier = modifier
        ){
            // Header with back button
            ActivityTitle(
                title = stringResource(Res.string.new_chat),
                onBackClick = {
                    viewModel.onBackClick()
                }
            )


            //Create group buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ActionCard(
                    text = stringResource(Res.string.new_group),
                    icon = Icons.Default.GroupAdd,
                    onClick = { viewModel.onGroupCreatorClick() },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                ActionCard(
                    text = stringResource(Res.string.invite_friend),
                    icon = Icons.Default.ContactMail,
                    onClick = {
                        viewModel.onInviteFriendClick()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Search Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.search_user),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = searchterm,
                    maxLines = 1,
                    onValueChange = { viewModel.updateSearchterm(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.enter_username)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(Res.string.search_user)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            //pending friends view
            if (pendingFriends.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.pending_friend_requests, pendingFriends.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                ),
            ) {
                items(
                    items = pendingFriends,
                    key = {it.id}
                ) { friend ->
                    UserButton(
                        selectedChat = friend,
                        useOnClickGes = true,
                        onClickGes = {
                            viewModel.onPendingFriendRequestClick(friend)
                        }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp
                    )
                }
            }



            // Results Text
            if (newChats.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.search_results, newChats.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            //New users lazycolumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
            ) {
                items(newChats) { user ->

                    var showFriendRequestAlert by remember { mutableStateOf(false) }

                    UserResultCard(
                        username = user.username,
                        commonFriendCount = user.commonFriendCount,
                        onClick = {
                            showFriendRequestAlert = true
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if(showFriendRequestAlert){
                        FriendRequestAlert(
                            onDismiss = {
                                showFriendRequestAlert = false
                            },
                            onConfirm = {
                                showFriendRequestAlert = false
                                viewModel.addFriend(user.id)
                            },
                            friendName = user.username
                        )
                    }
                }
            }

        }

        pendingFriendPopup?.let { selectedChat->

            //Popup to accept friend request / cancel outgoing friend request

            //You are no friends with this person, but he sent you a friend request, you can accept it
            //selectedChat.requesterId!! != SessionCache.getOwnIdValue()
            val incomingRequest = selectedChat.requesterId!! != SessionCache.getOwnIdValue()


            //You sent the friend request to this user, and it is still pending, you can cancel it
            //selectedChat.requesterId!! == SessionCache.getOwnIdValue())

            if (incomingRequest){
                AlertDialog(
                    onDismissRequest = {
                        viewModel.dismissPendingFriendDialog()
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.acceptFriend(selectedChat.id)
                            },
                        ) {
                            Text(
                                text = stringResource(Res.string.friend_request_accept)
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.denyFriend(selectedChat.id)
                            },
                        ) {
                            Text(
                                text = stringResource(Res.string.friend_request_deny)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(Res.string.friend_request_title),
                        )
                    },
                )
            } else {
                //Outgoing request, cancelable
                AlertDialog(
                    onDismissRequest = {
                        viewModel.dismissPendingFriendDialog()
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.denyFriend(selectedChat.id)
                            },
                        ) {
                            Text(
                                text = stringResource(Res.string.friend_request_cancel)
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.dismissPendingFriendDialog()
                            },
                        ) {
                            Text(
                                text = stringResource(Res.string.close)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(Res.string.friend_request_cancel_title)
                        )
                    },
                )
            }

        }

    }
}

@Preview(showBackground = true)
@Composable
private fun ActionCardPreview(){
    ActionCard(
        text = "Daeawdawd",
        icon = Icons.Default.GroupAdd,
        onClick = {},
    )
}

@Composable
fun ActionCard(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UserResultCard(
    username: String,
    commonFriendCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (commonFriendCount > 0) {
                    Text(
                        text = stringResource(Res.string.common_friends, commonFriendCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Add Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        onClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.add),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FriendRequestAlert(
    friendName:String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
){
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = stringResource(Res.string.send_friend_request))
        },
        text = {
            Text(text = stringResource(Res.string.send_friend_request_description, friendName))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(stringResource(Res.string.add))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )

}
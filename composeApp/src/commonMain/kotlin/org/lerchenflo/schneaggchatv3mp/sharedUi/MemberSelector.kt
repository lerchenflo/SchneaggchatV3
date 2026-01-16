package org.lerchenflo.schneaggchatv3mp.sharedUi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.UserButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.search_user

/**
 * Reusable component for selecting members from a list of users.
 * Features:
 * - Horizontal scroll of selected users with remove option
 * - Search bar
 * - LazyColumn of available users
 */
@Composable
fun MemberSelector(
    availableUsers: List<SelectedChat>,
    selectedUsers: List<SelectedChat>,
    searchTerm: String,
    onSearchTermChange: (String) -> Unit,
    onUserSelected: (SelectedChat) -> Unit,
    onUserDeselected: (SelectedChat) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Selected users horizontal scroll view
        if (selectedUsers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(
                        start = 10.dp,
                        end = 2.dp,
                        bottom = 10.dp
                    )
            ) {
                selectedUsers.forEach { user ->
                    Column(
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .clickable {
                                onUserDeselected(user)
                            }
                    ) {
                        val size = 70.dp
                        Box(
                            modifier = Modifier.size(size)
                        ) {
                            // Profile picture
                            ProfilePictureView(
                                filepath = user.profilePictureUrl,
                                modifier = Modifier
                                    .size(size)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                            )

                            // Trash icon overlay
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove user",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(30.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .padding(2.dp)
                            )
                        }
                        Text(
                            text = user.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(size)
                        )
                    }
                }
            }
        }

        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 5.dp,
                    bottom = 5.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchTerm,
                maxLines = 1,
                onValueChange = { onSearchTermChange(it) },
                modifier = Modifier
                    .weight(1f),
                placeholder = { Text(stringResource(Res.string.search_user)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "search"
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent
                )
            )
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .aspectRatio(1f)
                    .clickable { onSearchTermChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Reset Search Text",
                    modifier = Modifier
                        .size(30.dp)
                )
            }
        }

        // List of available users
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Take remaining space
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
        ) {
            items(availableUsers) { user ->
                val selected = selectedUsers.contains(user)

                UserButton(
                    selectedChat = user,
                    useOnClickGes = true,
                    lastMessage = null,
                    bottomTextOverride = "",
                    selected = selected,
                    onClickGes = {
                        if (selected) {
                            onUserDeselected(user)
                        } else {
                            onUserSelected(user)
                        }
                    }
                )
                HorizontalDivider(
                    thickness = 0.5.dp
                )
            }
        }
    }
}

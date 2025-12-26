package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureBigDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.sharedUi.UserButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.admin
import schneaggchatv3mp.composeapp.generated.resources.unknown_user

@Composable
fun GroupMembersView(
    members: List<GroupMemberWithUser>,
    onUserClick: (String) -> Unit //Returns the user id
) {
    Column {
        members.forEach { (groupMember, user) ->
            var profilePictureDialogShown by remember { mutableStateOf(false) }
            ChatButtonView(
                profilePictureFilePath = user?.profilePictureUrl ?: "",
                name = user?.name ?: stringResource(Res.string.unknown_user),
                isAdmin = groupMember.isAdmin,
                onClickText = {
                    // todo dropdown with make admin, open chat, typ ussewerfa, etc.
                },
                onClickImage = {
                    profilePictureDialogShown = true
                }
            )

            if (profilePictureDialogShown) {
                ProfilePictureBigDialog(
                    filepath =  user?.profilePictureUrl ?: "",
                    onDismiss = {
                        profilePictureDialogShown = false
                    }
                )
            }


            HorizontalDivider()
        }
    }
}

@Composable
fun CommonGroupsView(
    groups: List<Group>
){
    Column {
        groups.forEach { group ->

            var profilePictureDialogShown by remember { mutableStateOf(false) }
            ChatButtonView(
                profilePictureFilePath = group.profilePictureUrl,
                name = group.name,
                onClickText = {

                },
                onClickImage = {
                    profilePictureDialogShown
                }
            )

            if (profilePictureDialogShown) {
                ProfilePictureBigDialog(
                    filepath =  group.profilePictureUrl,
                    onDismiss = {
                        profilePictureDialogShown = false
                    }
                )
            }

            HorizontalDivider()
        }
    }
}


@Composable
private fun ChatButtonView(
    profilePictureFilePath: String,
    name: String,
    isAdmin: Boolean = false,
    onClickText: () -> Unit = {},  // Add click for name + admin icon
    onClickImage: () -> Unit = {},  // Add click for image (profilepicture)
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)
        .height(60.dp)
){
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfilePictureView(
            filepath = profilePictureFilePath,
            modifier = Modifier
                .size(50.dp) // Use square aspect ratio
                .padding(end = 8.dp) // Right padding only
                .clip(CircleShape) // Circular image
                .clickable{onClickImage()}
        )

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .clickable{onClickText()},
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if(isAdmin){
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = stringResource(Res.string.admin),
                )
            }
        }



    }
}
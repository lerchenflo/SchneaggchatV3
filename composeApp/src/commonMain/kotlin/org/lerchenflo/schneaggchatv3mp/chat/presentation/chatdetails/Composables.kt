package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.RemoveModerator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureBigDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.ProfilePictureView
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.admin
import schneaggchatv3mp.composeapp.generated.resources.make_admin
import schneaggchatv3mp.composeapp.generated.resources.open_chat
import schneaggchatv3mp.composeapp.generated.resources.remove_admin_status
import schneaggchatv3mp.composeapp.generated.resources.unknown_user
import schneaggchatv3mp.composeapp.generated.resources.you_with_brackets

@Composable
fun GroupMembersView(
    members: List<GroupMemberWithUser>,
    viewmodel: ChatDetailsViewmodel,
    //iAmAdmin: Boolean,
) {
    val ownid = SessionCache.getOwnIdValue().toString()
    val iAmAdmin = members.find { it.groupMember.userId.equals(ownid) }?.groupMember?.admin == true
    Column {
        members.forEach { (groupMember, user) ->
            var profilePictureDialogShown by remember { mutableStateOf(false) }

            var userOptionPopupExpanded by remember { mutableStateOf(false) }
            val me = groupMember.userId == ownid

            ChatButtonView(
                profilePictureFilePath = user?.profilePictureUrl ?: "",
                name = if(me){ // if own user is displays add a hint
                    (user?.name ?: stringResource(Res.string.unknown_user)) + stringResource(Res.string.you_with_brackets)
                }else{
                    user?.name ?: stringResource(Res.string.unknown_user)
                },
                isAdmin = groupMember.admin,
                onClickText = {
                    if(!me) userOptionPopupExpanded = true // only show popup for others
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

            UserOptionPopup(
                expanded = userOptionPopupExpanded,
                iAmAdmin = iAmAdmin,
                groupMember = groupMember,
                user = user,
                onDismissRequest = { userOptionPopupExpanded = false },
                onOpenChat = {
                    if(user != null) viewmodel.navigateToChat(user)
                },
                onAdminStatusChange = {
                    // todo
                },
                onRemoveUser = {
                    // todo
                },
            )


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


@Composable
fun UserOptionPopup(
    expanded: Boolean,
    iAmAdmin: Boolean,
    groupMember: GroupMember,
    user: User?,
    onDismissRequest: () -> Unit,
    onOpenChat: () -> Unit,
    onAdminStatusChange: () -> Unit,
    onRemoveUser: () -> Unit,
    modifier: Modifier = Modifier
) {



    Box(
        modifier = modifier,
        // contentAlignment = if (myMessage) Alignment.TopEnd else Alignment.TopStart
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
        ) {

            if(user != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.open_chat)) },
                    onClick = {
                        onOpenChat()
                        onDismissRequest()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(Res.string.open_chat)
                        )
                    }
                )
            }

            if(iAmAdmin) {
                DropdownMenuItem(
                    text = {
                        if(groupMember.admin){
                            Text(stringResource(Res.string.remove_admin_status))
                        }else{
                            Text(stringResource(Res.string.make_admin))
                        }
                    },
                    onClick = {
                        onAdminStatusChange()
                        onDismissRequest()
                    },
                    leadingIcon = {
                        if(groupMember.admin){
                            Icon(
                                imageVector = Icons.Default.RemoveModerator,
                                contentDescription = stringResource(Res.string.remove_admin_status)
                            )
                        }else{
                            Icon(
                                imageVector = Icons.Default.AddModerator,
                                contentDescription = stringResource(Res.string.make_admin)
                            )
                        }

                    }
                )
            }

        }
    }
}
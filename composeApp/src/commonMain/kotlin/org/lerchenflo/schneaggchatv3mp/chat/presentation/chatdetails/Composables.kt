package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.RemoveModerator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.SelectedChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.TooltipIconButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.MemberSelector
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureBigDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.confirm_remove_member
import schneaggchatv3mp.composeapp.generated.resources.add_description_placeholder
import schneaggchatv3mp.composeapp.generated.resources.admin
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.change
import schneaggchatv3mp.composeapp.generated.resources.common_groups
import schneaggchatv3mp.composeapp.generated.resources.group_description
import schneaggchatv3mp.composeapp.generated.resources.groupmembers
import schneaggchatv3mp.composeapp.generated.resources.make_admin
import schneaggchatv3mp.composeapp.generated.resources.member_since
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.open_chat
import schneaggchatv3mp.composeapp.generated.resources.remove_admin_status
import schneaggchatv3mp.composeapp.generated.resources.remove_from_group
import schneaggchatv3mp.composeapp.generated.resources.unknown_user
import schneaggchatv3mp.composeapp.generated.resources.user_description
import schneaggchatv3mp.composeapp.generated.resources.yes
import schneaggchatv3mp.composeapp.generated.resources.you_with_brackets

/**
 * Reusable confirmation dialog for destructive actions.
 */
@Composable
fun ConfirmationDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(stringResource(Res.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        text = {
            Text(message)
        }
    )
}

@Composable
fun GroupMembersView(
    members: List<GroupMemberWithUser>,
    navigateToChat:(selectedChat: SelectedChat)-> Unit,
    changeAdminStatus:(groupMember: GroupMember)-> Unit,
    removeMember: (memberId: String)-> Unit,
    //iAmAdmin: Boolean,
) {
    val ownid = SessionCache.getOwnIdValue().toString()
    val iAmAdmin = members.find { it.groupMember.userId == ownid }?.groupMember?.admin == true


    Column(
        modifier = Modifier.padding(16.dp)
    ) {

        if (members.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.groupmembers, members.size),
                )
        }

        Spacer(modifier = Modifier.height(8.dp))


        members.forEach { (groupMember, user) ->
            var profilePictureDialogShown by remember { mutableStateOf(false) }
            var userOptionPopupExpanded by remember { mutableStateOf(false) }
            var showRemoveMemberConfirmation by remember { mutableStateOf(false) }
            val me = groupMember.userId == ownid
            val userName = user?.name ?: groupMember.memberName

            ChatButtonView(
                profilePictureFilePath = user?.profilePictureUrl ?: "",
                name = if(me){ // if own user is displays add a hint
                    userName + stringResource(Res.string.you_with_brackets)
                }else{
                    userName
                },
                isAdmin = groupMember.admin,
                onClickText = {
                    if(!me) userOptionPopupExpanded = true // only show popup for others
                },
                onClickImage = {
                    profilePictureDialogShown = true
                },
                joindate = groupMember.joinDate
            )

            if (profilePictureDialogShown) {
                ProfilePictureBigDialog(
                    filepath =  user?.profilePictureUrl ?: "",
                    onDismiss = {
                        profilePictureDialogShown = false
                    }
                )
            }

            // Confirmation dialog for removing member
            if (showRemoveMemberConfirmation) {
                ConfirmationDialog(
                    message = stringResource(Res.string.confirm_remove_member, userName),
                    onConfirm = {
                        removeMember(groupMember.userId)
                    },
                    onDismiss = {
                        showRemoveMemberConfirmation = false
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
                    if(user != null) navigateToChat(user.toSelectedChat(
                        unreadCount = 0,
                        unsentCount = 0,
                        lastMessage = null
                    ))
                },
                onAdminStatusChange = {
                    changeAdminStatus(groupMember)
                },
                onRemoveUser = {
                    showRemoveMemberConfirmation = true  // Show confirmation instead of directly removing
                },
            )


            //HorizontalDivider()
        }
    }
}

@Composable
fun CommonGroupsView(
    groups: List<Group>,
    viewmodel: ChatDetailsViewmodel
){
    Column(
        modifier = Modifier.padding(16.dp)
    ) {

        if (groups.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.common_groups, groups.size),

            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        groups.forEach { group ->

            var profilePictureDialogShown by remember { mutableStateOf(false) }
            ChatButtonView(
                profilePictureFilePath = group.profilePictureUrl,
                name = group.name,
                onClickText = {
                    viewmodel.navigateToChat(group.toSelectedChat(
                        unreadCount = 0,
                        unsentCount = 0,
                        lastMessage = null
                    ))
                },
                onClickImage = {
                    profilePictureDialogShown = true
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

            //HorizontalDivider()
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
    joindate: String? = null,
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
                .fillMaxSize()
                .clickable{onClickText()},
            verticalAlignment = Alignment.CenterVertically
        ){
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (joindate != null) {
                    Text(
                        text = stringResource(Res.string.member_since, millisToTimeDateOrYesterday(joindate.toLong())),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

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

                DropdownMenuItem(
                    text = {
                        Text(stringResource(Res.string.remove_from_group))
                    },
                    onClick = {
                        onRemoveUser()
                        onDismissRequest()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = stringResource(Res.string.remove_from_group)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ChangeDescription(
    onDismiss: () -> Unit,
    descriptionText: TextFieldValue = TextFieldValue(""),
    updateDescription:(selectedChat: SelectedChat) -> Unit = {},
    updateDescriptionText:(value: TextFieldValue) -> Unit = {},
    selectedChat: SelectedChat,
    isGroup: Boolean
){
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current // Also helpful to hide keyboard

    LaunchedEffect(selectedChat) {
        updateDescriptionText(TextFieldValue(selectedChat.description ?: ""))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    updateDescription(selectedChat)
                    onDismiss()
                },
            ) {
                Text(
                    text = stringResource(Res.string.change)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(Res.string.cancel)
                )
            }
        },
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) { // sorry iphone user aber ihr dürfen ned zu viel Zeilen macha. I krigs ned zum richta
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        })
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = if (isGroup) stringResource(Res.string.group_description) else stringResource(Res.string.user_description),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descriptionText,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 14.sp // You can adjust this value as needed
                        ),
                        maxLines = 5,
                        onValueChange = { newValue ->
                            updateDescriptionText(newValue)
                        },
                        modifier = Modifier
                            .onPreviewKeyEvent { event ->
                                // Check if the key is 'Escape' and it's a 'KeyDown' event
                                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                                    onDismiss()
                                }
                                false // Pass all other events (letters, backspace, etc.) to the TextField
                            }
                            .fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.add_description_placeholder)) }
                    )

                }
            }

        },
    )
}

@Composable
fun DescriptionStatusRow(
    onClick: () -> Unit,
    titleText: String,
    bodyText: String,
    infoText: String
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{
                onClick()
            }
            .padding(
                16.dp
            )
    ){
        Column(){
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    modifier = Modifier.weight(1f),  // Takes available space but allows other items to show
                    autoSize = TextAutoSize.StepBased(13.sp, 19.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,  // Shows "..." if text is too long
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TooltipIconButton(infoText)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row {
                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = bodyText,
                    fontSize = 14.sp,  // Smaller body text
                    lineHeight = 16.sp,
                    maxLines = 20,
                )
            }
        }

    }
}

@Composable
fun AddUserToGroupPopup(
    onDismiss: () -> Unit,
    onSuccess: (List<SelectedChat>) -> Unit,
    availableUsers: List<SelectedChat>

) {

    val selectedUsers = remember {
        mutableStateListOf<SelectedChat>()
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSuccess(selectedUsers)
            }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }) {
                Text(stringResource(Res.string.cancel))
            }
        },
        text = {
            var searchterm by remember {
                mutableStateOf("")
            }

            MemberSelector(
                availableUsers = availableUsers,
                selectedUsers = selectedUsers,
                searchTerm = searchterm,
                onSearchTermChange = {searchterm = it},
                onUserSelected = {selectedUsers += it},
                onUserDeselected = {selectedUsers -= it}
            )
        }
    )
}
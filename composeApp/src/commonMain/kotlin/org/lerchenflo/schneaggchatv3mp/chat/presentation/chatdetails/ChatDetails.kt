package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ismoy.imagepickerkmp.domain.config.CameraCaptureConfig
import io.github.ismoy.imagepickerkmp.domain.config.CropConfig
import io.github.ismoy.imagepickerkmp.domain.models.CapturePhotoPreference
import io.github.ismoy.imagepickerkmp.domain.models.CompressionLevel
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserChat
import org.lerchenflo.schneaggchatv3mp.chat.domain.toGroup
import org.lerchenflo.schneaggchatv3mp.chat.domain.toUser
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.QuotedText
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.DeleteButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureBigDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ChangeStringDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ErrorMessage
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.iso8601DateFormatter
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add_users_to_group
import schneaggchatv3mp.composeapp.generated.resources.change_group_name
import schneaggchatv3mp.composeapp.generated.resources.confirm_leave_group
import schneaggchatv3mp.composeapp.generated.resources.confirm_remove_friend
import schneaggchatv3mp.composeapp.generated.resources.description_info_group
import schneaggchatv3mp.composeapp.generated.resources.description_info_user
import schneaggchatv3mp.composeapp.generated.resources.group_description
import schneaggchatv3mp.composeapp.generated.resources.group_name
import schneaggchatv3mp.composeapp.generated.resources.leave_group
import schneaggchatv3mp.composeapp.generated.resources.no_description
import schneaggchatv3mp.composeapp.generated.resources.others_say_about
import schneaggchatv3mp.composeapp.generated.resources.remove_friend
import schneaggchatv3mp.composeapp.generated.resources.status_info
import schneaggchatv3mp.composeapp.generated.resources.today
import kotlin.time.Clock


@Composable
fun ChatDetails(
    modifier: Modifier = Modifier
        .fillMaxWidth()
) {

    val chatdetailsViewmodel = koinViewModel<ChatDetailsViewmodel>()

    val selectedChat by chatdetailsViewmodel.chatDetails.collectAsStateWithLifecycle()
    val availableMembers by chatdetailsViewmodel.availableNewMembers.collectAsStateWithLifecycle()
    val searchTerm by chatdetailsViewmodel.searchterm.collectAsStateWithLifecycle()

    // Early return if chat not selected - don't render anything
    /*
    if (chatDetails.isNotSelected()){
        println("Chat not selected, navigating back")
        chatdetailsViewmodel.onBackClick()
        return
    }

     */

    val group = selectedChat.isGroup

    val ownId = SessionCache.requireLoggedIn()?.userId ?: return

    var profilePictureDialogShown by remember { mutableStateOf(false) }
    var showLeaveGroupConfirmation by remember { mutableStateOf(false) }
    var showRemoveFriendConfirmation by remember { mutableStateOf(false) }

    var showAddMemberPopup by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showGroupRenameDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }


    // Profilbild größer azoaga
    if (profilePictureDialogShown) {
        ProfilePictureBigDialog(
            onDismiss = { profilePictureDialogShown = false },
            filepath = selectedChat.profilePictureUrl,
            showEditButton = group,
            onEdit = {
                profilePictureDialogShown = false
                showImagePickerDialog = true
            }
        )
    }

    if (showGroupRenameDialog) {
        var errorMessage by remember { mutableStateOf<ErrorMessage?>(null) }

        ChangeStringDialog(
            title = stringResource(Res.string.change_group_name),
            oldString = selectedChat.name,
            maxLines = 1,
            placeholder = stringResource(Res.string.group_name),
            errorMessage = errorMessage,
            onDismiss = { showGroupRenameDialog = false },
            updateString = { newString ->
                val error = chatdetailsViewmodel.validateGroupName(newString)
                errorMessage = error
                if (error == null) {
                    chatdetailsViewmodel.updateGroupName(newString)
                    showGroupRenameDialog = false
                }
            }
        )
    }

    if (showNicknameDialog) {
        var errorMessage by remember { mutableStateOf<ErrorMessage?>(null) }
        val currentNickname = chatdetailsViewmodel.selectedUser?.nickName ?: chatdetailsViewmodel.selectedUser?.name ?: "Unknown"

        ChangeStringDialog(
            title = "Change Nickname",
            oldString = currentNickname,
            maxLines = 1,
            placeholder = "Enter nickname",
            errorMessage = errorMessage,
            onDismiss = { showNicknameDialog = false },
            updateString = { newString ->
                chatdetailsViewmodel.updateNickname(newString)
                showNicknameDialog = false
            }
        )
    }



    Column(
        modifier = modifier
    ) {

        ActivityTitle(
            alternativeTitleComposable = if (group) {
                {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGroupRenameDialog = true
                            },
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedChat.name,
                            modifier = Modifier
                                // fill = false prevents the Text from forcing itself to be wide
                                .weight(1f, fill = false)
                                .padding(start = 10.dp),
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit group name",
                            modifier = Modifier.padding(start = 8.dp)
                        )


                    }

                }
            } else {
                {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showNicknameDialog = true
                            },
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val baseFontSize = 24.sp
                        val nicknameFontSize = baseFontSize * 0.8f

                        Text(
                            text = buildAnnotatedString {
                                append(selectedChat.name)

                                //Show nickname if set
                                if (selectedChat is UserChat && (selectedChat as UserChat).nickName != null) {
                                    append(" (\"")

                                    withStyle(
                                        style = SpanStyle(
                                            fontStyle = FontStyle.Italic,
                                            fontSize = nicknameFontSize
                                        )
                                    ) {
                                        append((selectedChat as UserChat).nickName)
                                    }

                                    append("\")")
                                }
                            },
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(start = 10.dp),
                            fontSize = baseFontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit nickname",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            onBackClick = {
                chatdetailsViewmodel.onBackClick()
            }
        )

        HorizontalDivider()

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Profile picture
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {

                ProfilePictureView(
                    filepath = selectedChat.profilePictureUrl,
                    modifier = Modifier
                        .size(200.dp) // Use square aspect ratio
                        .padding(vertical = 10.dp)
                        .clickable {
                            profilePictureDialogShown = true
                        }
                )

            }

            // Status only for user
            if (!group) {
                HorizontalDivider()

                // Birthdate
                chatdetailsViewmodel.selectedUser?.birthDate?.let { birthDate ->

                    val birthdateParsed = remember { LocalDate.parse(birthDate) }
                    val today = remember {
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }

                    val isToday =
                        birthdateParsed.month == today.month && birthdateParsed.day == today.day

                    val infiniteTransition = rememberInfiniteTransition(label = "birthday")
                    val animatedAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = EaseInOut),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    ListItem(
                        headlineContent = {
                            val formattedDate = iso8601DateFormatter(
                                iso8601Format = birthDate,
                                format = "dd.MM."
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isToday) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primary,
                                    ) {
                                        Text(
                                            text = "🎂 " + stringResource(Res.string.today),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = null,
                                tint = if (isToday)
                                    MaterialTheme.colorScheme.primary.copy(alpha = animatedAlpha)
                                else
                                    MaterialTheme.colorScheme.primary,
                                modifier = if (isToday) Modifier.size(28.dp) else Modifier
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isToday)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else
                                Color.Transparent
                        ),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                // Status
                val statusInfoString = stringResource(Res.string.status_info)

                selectedChat.status?.let {
                    if (it.isNotEmpty()) {
                        QuotedText(
                            text = it,
                            author = "~ " + selectedChat.name,
                            onClick = {
                                SnackbarManager.showMessage(statusInfoString)
                            }
                        )
                    }
                }

                /*
                DescriptionStatusRow(
                    onClick = {
                        SnackbarManager.showMessage(statusInfoString)
                    },
                    titleText = stringResource(Res.string.status),
                    bodyText = chatDetails.status
                        .takeIf { !it.isNullOrBlank() }
                        ?.replace("\\n", "\n")
                        ?: stringResource(Res.string.no_status),
                    infoText = stringResource(Res.string.status_info)
                )

                 */

            }

            HorizontalDivider()


            // description for group and user
            var showDescriptionChangeDialog by retain { mutableStateOf(false) } // retain dass ma es handy dräha kann (neue compose ding)

            if (showDescriptionChangeDialog) {
                ChangeDescription(
                    onDismiss = { showDescriptionChangeDialog = false },
                    selectedChat = selectedChat,
                    descriptionText = chatdetailsViewmodel.descriptionText,
                    updateDescriptionText = chatdetailsViewmodel::updateDescriptionText,
                    updateDescription = chatdetailsViewmodel::updateDescription,
                    isGroup = group
                )
            }

            DescriptionStatusRow(
                onClick = { showDescriptionChangeDialog = true },
                titleText = if (group) stringResource(Res.string.group_description) else stringResource(
                    Res.string.others_say_about,
                    selectedChat.name
                ),
                bodyText = selectedChat.description
                    .takeIf { !it.isNullOrBlank() }
                    ?.replace("\\n", "\n")
                    ?: stringResource(Res.string.no_description),
                infoText = if (group) stringResource(Res.string.description_info_group) else stringResource(
                    Res.string.description_info_user,
                    selectedChat.name
                )
            )

            HorizontalDivider()

            //Common groups / Common friends
            if (group) {
                selectedChat.toGroup()?.let { groupChat ->
                    GroupMembersView(
                        members = groupChat.groupMembersWithUsers,
                        navigateToChat = chatdetailsViewmodel::navigateToChat,
                        changeAdminStatus = chatdetailsViewmodel::changeAdminStatus,
                        removeMember = chatdetailsViewmodel::removeMember,
                        sendFriendRequest = chatdetailsViewmodel::sendFriendRequest,
                        ownId = ownId
                    )
                }
            } else {
                selectedChat.toUser()?.let { userChat ->
                    if (userChat.commonGroups.isNotEmpty()) {
                        CommonGroupsView(
                            groups = userChat.commonGroups,
                            viewmodel = chatdetailsViewmodel
                        )
                    }
                }
            }

            HorizontalDivider()


            if (group) {

                val iAmAdmin =
                    selectedChat.toGroup()?.groupMembersWithUsers?.find { it.groupMember.userId == ownId }?.groupMember?.admin == true

                if (iAmAdmin) {
                    // add partypeople
                    NormalButton(
                        text = stringResource(Res.string.add_users_to_group),
                        onClick = {
                            showAddMemberPopup = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    if (showAddMemberPopup) {
                        AddUserToGroupPopup(
                            onDismiss = { showAddMemberPopup = false },
                            onSuccess = {
                                it.forEach { user ->
                                    chatdetailsViewmodel.addMember(user.id)
                                }
                                showAddMemberPopup = false
                            },
                            availableUsers = availableMembers,
                            selectedUsers = chatdetailsViewmodel.selectedNewMembers,
                            searchterm = searchTerm,
                            onSearchTermChange = chatdetailsViewmodel::onSearchTermChange,
                            onUserSelected = chatdetailsViewmodel::onUserSelected,
                            onUserDeselected = chatdetailsViewmodel::onUserDeSelected,
                        )
                    }

                }

                // Confirmation dialog for leaving group
                if (showLeaveGroupConfirmation) {
                    ConfirmationDialog(
                        message = stringResource(Res.string.confirm_leave_group),
                        onConfirm = {
                            chatdetailsViewmodel.removeMember(ownId)
                            chatdetailsViewmodel.navigateChatSelExitAllPrevious()
                        },
                        onDismiss = {
                            showLeaveGroupConfirmation = false
                        }
                    )
                }

                // Leave group (Always there)
                DeleteButton(
                    text = stringResource(Res.string.leave_group),
                    onClick = {
                        showLeaveGroupConfirmation = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            } else {
                // Confirmation dialog for removing friend
                if (showRemoveFriendConfirmation) {
                    ConfirmationDialog(
                        message = stringResource(
                            Res.string.confirm_remove_friend,
                            selectedChat.name
                        ),
                        onConfirm = {
                            chatdetailsViewmodel.removeFriend()
                        },
                        onDismiss = {
                            showRemoveFriendConfirmation = false
                        }
                    )
                }

                // remove friend
                DeleteButton(
                    text = stringResource(Res.string.remove_friend),
                    onClick = {
                        showRemoveFriendConfirmation = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

        }


    }

    if (showImagePickerDialog) {
        GalleryPickerLauncher(
            onPhotosSelected = {
                chatdetailsViewmodel.updateProfilePic(it.first())
                showImagePickerDialog = false
            },
            onError = {
                showImagePickerDialog = false
            },
            onDismiss = {
                showImagePickerDialog = false
            },
            allowMultiple = false,
            enableCrop = true,
            cameraCaptureConfig = CameraCaptureConfig(
                compressionLevel = CompressionLevel.HIGH,
                preference = CapturePhotoPreference.FAST, //No flash
                cropConfig = CropConfig(
                    enabled = true,
                    aspectRatioLocked = true,
                    circularCrop = true,
                    squareCrop = false,
                    freeformCrop = false
                ),
            )
        )
    }
}

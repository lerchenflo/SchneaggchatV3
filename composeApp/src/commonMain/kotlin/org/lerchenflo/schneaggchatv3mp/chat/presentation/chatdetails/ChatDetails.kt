package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import schneaggchatv3mp.composeapp.generated.resources.shared_messages
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent.SharedContentTab
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ismoy.imagepickerkmp.config.CropConfig
import io.github.ismoy.imagepickerkmp.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.picker.ImagePickerKMPConfig
import io.github.ismoy.imagepickerkmp.picker.ImagePickerResult
import io.github.ismoy.imagepickerkmp.picker.CompressionLevel
import io.github.ismoy.imagepickerkmp.picker.MimeType
import io.github.ismoy.imagepickerkmp.picker.rememberImagePickerKMP
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventDeleteDialog
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventItem
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.QuotedText
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.DeleteButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureBigDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ChangeStringDialog
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ErrorMessage
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.isBirthdayToday
import org.lerchenflo.schneaggchatv3mp.utilities.iso8601DateFormatter
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.formatCountdown
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.rememberCountdownMillis
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import kotlin.time.Clock
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add_users_to_group
import schneaggchatv3mp.composeapp.generated.resources.camera
import schneaggchatv3mp.composeapp.generated.resources.change_group_name
import schneaggchatv3mp.composeapp.generated.resources.change_nickname
import schneaggchatv3mp.composeapp.generated.resources.choose_image_source
import schneaggchatv3mp.composeapp.generated.resources.confirm_delete_group_timer
import schneaggchatv3mp.composeapp.generated.resources.confirm_leave_group
import schneaggchatv3mp.composeapp.generated.resources.confirm_remove_friend
import schneaggchatv3mp.composeapp.generated.resources.delete_event
import schneaggchatv3mp.composeapp.generated.resources.delete_group_timer
import schneaggchatv3mp.composeapp.generated.resources.set_group_timer
import schneaggchatv3mp.composeapp.generated.resources.group_timer_picker_title
import schneaggchatv3mp.composeapp.generated.resources.group_timer_picker_info
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.description_info_group
import schneaggchatv3mp.composeapp.generated.resources.description_info_user
import schneaggchatv3mp.composeapp.generated.resources.enter_nickname
import schneaggchatv3mp.composeapp.generated.resources.gallery
import schneaggchatv3mp.composeapp.generated.resources.group_description
import schneaggchatv3mp.composeapp.generated.resources.group_expired
import schneaggchatv3mp.composeapp.generated.resources.group_expires_at
import schneaggchatv3mp.composeapp.generated.resources.group_expires_in
import schneaggchatv3mp.composeapp.generated.resources.group_name
import schneaggchatv3mp.composeapp.generated.resources.image_picker_error
import schneaggchatv3mp.composeapp.generated.resources.unknown_error
import schneaggchatv3mp.composeapp.generated.resources.leave_group
import schneaggchatv3mp.composeapp.generated.resources.no_description
import schneaggchatv3mp.composeapp.generated.resources.others_say_about
import schneaggchatv3mp.composeapp.generated.resources.remove
import schneaggchatv3mp.composeapp.generated.resources.remove_friend
import schneaggchatv3mp.composeapp.generated.resources.status_info
import schneaggchatv3mp.composeapp.generated.resources.shared_images
import schneaggchatv3mp.composeapp.generated.resources.shared_links
import schneaggchatv3mp.composeapp.generated.resources.today
import schneaggchatv3mp.composeapp.generated.resources.wake_button
import schneaggchatv3mp.composeapp.generated.resources.wake_reason_placeholder
import schneaggchatv3mp.composeapp.generated.resources.wake_reason_title
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DividerDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.TooltipIconButton
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsDivider
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import schneaggchatv3mp.composeapp.generated.resources.birthday_label
import schneaggchatv3mp.composeapp.generated.resources.chat_details_actions
import schneaggchatv3mp.composeapp.generated.resources.common_groups
import schneaggchatv3mp.composeapp.generated.resources.edit_profile_picture
import schneaggchatv3mp.composeapp.generated.resources.groupmembers
import schneaggchatv3mp.composeapp.generated.resources.info
import schneaggchatv3mp.composeapp.generated.resources.shared_content_title


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetails(
    chatId: String,
    isGroup: Boolean,
    modifier: Modifier = Modifier
        .fillMaxWidth()
) {

    val chatdetailsViewmodel = koinViewModel<ChatDetailsViewmodel> { parametersOf(chatId, isGroup) }

    val selectedChat by chatdetailsViewmodel.chatDetails.collectAsStateWithLifecycle()
    val availableMembers by chatdetailsViewmodel.availableNewMembers.collectAsStateWithLifecycle()
    val searchTerm by chatdetailsViewmodel.searchterm.collectAsStateWithLifecycle()
    val connectedEvent by chatdetailsViewmodel.connectedEvent.collectAsStateWithLifecycle()
    val sharedImageCount by chatdetailsViewmodel.sharedImageCount.collectAsStateWithLifecycle()
    val sharedLinkCount by chatdetailsViewmodel.sharedLinkCount.collectAsStateWithLifecycle()
    val messageCount by chatdetailsViewmodel.messageCount.collectAsStateWithLifecycle()

    SessionCache.authStateValue // reactive read: recompose once autologin finishes instead of staying blank
    val ownId = SessionCache.requireLoggedIn()?.userId ?: return
    val iAmAdmin =
        (selectedChat as? ChatDetailsState.GroupDetails)?.members?.find { it.groupMember.userId == ownId }?.groupMember?.admin == true

    var profilePictureDialogShown by remember { mutableStateOf(false) }
    var showLeaveGroupConfirmation by remember { mutableStateOf(false) }
    var showDecoupleExpiryConfirmation by remember { mutableStateOf(false) }
    var showExpiryPicker by remember { mutableStateOf(false) }
    var showRemoveFriendConfirmation by remember { mutableStateOf(false) }
    var showDeleteEventConfirmation by remember { mutableStateOf(false) }

    var showAddMemberPopup by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showGroupRenameDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showWakeReasonDialog by remember { mutableStateOf(false) }


    // Profilbild größer azoaga
    if (profilePictureDialogShown) {
        ProfilePictureBigDialog(
            onDismiss = { profilePictureDialogShown = false },
            filepath = selectedChat.profilePictureUrl,
            showEditButton = isGroup,
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
            },

        )
    }

    if (showWakeReasonDialog) {
        ChangeStringDialog(
            title = stringResource(Res.string.wake_reason_title),
            oldString = "",
            maxLines = 3,
            placeholder = stringResource(Res.string.wake_reason_placeholder),
            confirmText = stringResource(Res.string.wake_button),
            onDismiss = { showWakeReasonDialog = false },
            updateString = { reason ->
                chatdetailsViewmodel.sendWake(reason)
                showWakeReasonDialog = false
            },
        )
    }

    if (showNicknameDialog) {
        var errorMessage by remember { mutableStateOf<ErrorMessage?>(null) }
        val currentNickname = (selectedChat as? ChatDetailsState.UserDetails)?.user?.displayName ?: "Unknown"

        ChangeStringDialog(
            title = stringResource(Res.string.change_nickname),
            oldString = currentNickname,
            maxLines = 1,
            placeholder = stringResource(Res.string.enter_nickname),
            errorMessage = errorMessage,
            onDismiss = { showNicknameDialog = false },
            updateString = { newString ->
                chatdetailsViewmodel.updateNickname(newString)
                showNicknameDialog = false
            },

            //Remove nickname button
            thirdButton = {
                TextButton(
                    onClick = {
                        chatdetailsViewmodel.updateNickname("")
                        showNicknameDialog = false
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.remove)
                    )
                }
            }
        )
    }



    Column(
        modifier = modifier
    ) {

        ActivityTitle(
            title = stringResource(Res.string.info),
            onBackClick = {
                chatdetailsViewmodel.onBackClick()
            }
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Every row below is followed by its own divider, so section headers skip their top one
            // and no two dividers ever stack, whichever optional rows are shown.

            ChatDetailsHeader(
                name = selectedChat.name,
                nickName = (selectedChat as? ChatDetailsState.UserDetails)?.user?.nickName,
                profilePictureUrl = selectedChat.profilePictureUrl,
                showEditPictureBadge = isGroup,
                editNameDescription = if (isGroup) stringResource(Res.string.change_group_name) else stringResource(Res.string.change_nickname),
                onPictureClick = { profilePictureDialogShown = true },
                onEditPictureClick = { showImagePickerDialog = true },
                onNameClick = {
                    if (isGroup) showGroupRenameDialog = true else showNicknameDialog = true
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Status, written by the user themselves
            if (!isGroup) {
                val statusInfoString = stringResource(Res.string.status_info)

                selectedChat.status?.takeIf { it.isNotEmpty() }?.let { status ->
                    QuotedText(
                        text = status,
                        author = "~ " + selectedChat.name,
                        onClick = {
                            SnackbarManager.showMessage(statusInfoString)
                        }
                    )

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            }

            // Birthdate
            if (!isGroup) {
                (selectedChat as? ChatDetailsState.UserDetails)?.user?.birthDate?.let { birthDate ->
                    val isToday = remember(birthDate) { isBirthdayToday(birthDate) }

                    SettingsOption(
                        icon = Icons.Default.Cake,
                        text = stringResource(Res.string.birthday_label),
                        subtext = iso8601DateFormatter(
                            iso8601Format = birthDate,
                            format = "dd.MM."
                        ),
                        highlighted = isToday,
                        onClick = {
                            chatdetailsViewmodel.navigateToBirthdays()
                        },
                        rightSideIcon = {
                            if (isToday) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        text = "🎂 " + stringResource(Res.string.today),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    )

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            }

            // description for group and user
            var showDescriptionChangeDialog by retain { mutableStateOf(false) } // retain dass ma es handy dräha kann (neue compose ding)

            if (showDescriptionChangeDialog) {
                ChangeDescription(
                    onDismiss = { showDescriptionChangeDialog = false },
                    currentDescription = selectedChat.description,
                    descriptionText = chatdetailsViewmodel.descriptionText,
                    updateDescriptionText = chatdetailsViewmodel::updateDescriptionText,
                    updateDescription = chatdetailsViewmodel::updateDescription,
                    isGroup = isGroup
                )
            }

            SettingsOption(
                icon = Icons.AutoMirrored.Filled.Notes,
                text = if (isGroup) stringResource(Res.string.group_description) else stringResource(
                    Res.string.others_say_about,
                    selectedChat.name
                ),
                subtext = selectedChat.description
                    .takeIf { !it.isNullOrBlank() }
                    ?.replace("\\n", "\n")
                    ?: stringResource(Res.string.no_description),
                onClick = { showDescriptionChangeDialog = true },
                rightSideIcon = {
                    TooltipIconButton(
                        if (isGroup) stringResource(Res.string.description_info_group) else stringResource(
                            Res.string.description_info_user,
                            selectedChat.name
                        )
                    )
                }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Delete timer of a group chat. Every member sees the countdown; only admins can set,
            // move or clear it - the server enforces the same rule on /groups/setexpiry, so a
            // non-admin must not get a button that can only fail.
            if (isGroup) {
                val groupExpiresAt = (selectedChat as? ChatDetailsState.GroupDetails)?.group?.expiresAt

                if (showExpiryPicker) {
                    GroupExpiryPickerDialog(
                        initialExpiresAt = groupExpiresAt,
                        onConfirm = { newExpiresAt ->
                            chatdetailsViewmodel.setGroupExpiry(newExpiresAt)
                            showExpiryPicker = false
                        },
                        onDismiss = { showExpiryPicker = false }
                    )
                }

                if (showDecoupleExpiryConfirmation) {
                    ConfirmationDialog(
                        message = stringResource(Res.string.confirm_delete_group_timer),
                        onConfirm = {
                            chatdetailsViewmodel.setGroupExpiry(null)
                        },
                        onDismiss = {
                            showDecoupleExpiryConfirmation = false
                        }
                    )
                }

                if (groupExpiresAt != null) {
                    SettingsOption(
                        icon = Icons.Default.Schedule,
                        text = groupExpiryText(expiresAt = groupExpiresAt),
                        subtext = stringResource(
                            Res.string.group_expires_at,
                            millisToString(groupExpiresAt, format = "dd.MM.yyyy HH:mm")
                        ),
                        onClick = {
                            if (iAmAdmin) showExpiryPicker = true
                        },
                        rightSideIcon = {
                            if (iAmAdmin) {
                                IconButton(onClick = { showDecoupleExpiryConfirmation = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(Res.string.delete_group_timer),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                } else if (iAmAdmin) {
                    SettingsOption(
                        icon = Icons.Default.Schedule,
                        text = stringResource(Res.string.set_group_timer),
                        onClick = { showExpiryPicker = true }
                    )

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            }

            // Event this group was created for, if any
            if (isGroup) {
                connectedEvent?.let { event ->
                    val creatorProfilePictureUrl = (selectedChat as? ChatDetailsState.GroupDetails)
                        ?.members
                        ?.find { it.groupMember.userId == event.creatorId }
                        ?.user
                        ?.profilePictureUrl

                    if (showDeleteEventConfirmation) {
                        EventDeleteDialog(
                            hasGroup = true, // this card only shows for an event with a group - this one
                            onDismiss = { showDeleteEventConfirmation = false },
                            onConfirm = { deleteGroup, deleteEvent ->
                                chatdetailsViewmodel.detachConnectedEvent(
                                    eventId = event.id,
                                    deleteGroup = deleteGroup,
                                    deleteEvent = deleteEvent,
                                )
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EventItem(
                            event = event,
                            creatorProfilePictureUrl = creatorProfilePictureUrl,
                            isOwnEvent = event.creatorId == ownId,
                            onClick = { chatdetailsViewmodel.navigateToConnectedEvent(event.id) },
                            modifier = Modifier.weight(1f)
                        )

                        if (event.creatorId == ownId) {
                            IconButton(onClick = { showDeleteEventConfirmation = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.delete_event),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            }

            // Everything ever shared in this chat, one row per tab of the shared content screen
            SettingsDivider(
                title = stringResource(Res.string.shared_content_title),
                showTopDivider = false
            )

            SettingsOption(
                icon = Icons.Default.PhotoLibrary,
                text = stringResource(Res.string.shared_images),
                onClick = {
                    chatdetailsViewmodel.navigateToSharedContent(SharedContentTab.IMAGES)
                },
                rightSideIcon = { CountLabel(sharedImageCount) }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            SettingsOption(
                icon = Icons.Default.Link,
                text = stringResource(Res.string.shared_links),
                onClick = {
                    chatdetailsViewmodel.navigateToSharedContent(SharedContentTab.LINKS)
                },
                rightSideIcon = { CountLabel(sharedLinkCount) }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            SettingsOption(
                icon = Icons.AutoMirrored.Filled.Chat,
                text = stringResource(Res.string.shared_messages),
                onClick = {
                    chatdetailsViewmodel.navigateToSharedContent(SharedContentTab.MESSAGES)
                },
                rightSideIcon = { CountLabel(messageCount) }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            //Group members / Common groups
            if (isGroup) {
                (selectedChat as? ChatDetailsState.GroupDetails)?.let { groupDetails ->
                    SettingsDivider(
                        title = stringResource(Res.string.groupmembers, groupDetails.members.size),
                        showTopDivider = false
                    )

                    if (iAmAdmin) {
                        // add partypeople
                        SettingsOption(
                            icon = Icons.Default.PersonAdd,
                            text = stringResource(Res.string.add_users_to_group),
                            onClick = {
                                showAddMemberPopup = true
                            }
                        )

                        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

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
                                isSelected = chatdetailsViewmodel::isItemSelected,
                            )
                        }
                    }

                    GroupMembersView(
                        members = groupDetails.members,
                        navigateToChat = chatdetailsViewmodel::navigateToChat,
                        changeAdminStatus = chatdetailsViewmodel::changeAdminStatus,
                        removeMember = chatdetailsViewmodel::removeMember,
                        sendFriendRequest = chatdetailsViewmodel::sendFriendRequest,
                        ownId = ownId
                    )

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            } else {
                (selectedChat as? ChatDetailsState.UserDetails)?.let { userDetails ->
                    if (userDetails.commonGroups.isNotEmpty()) {
                        SettingsDivider(
                            title = stringResource(Res.string.common_groups, userDetails.commonGroups.size),
                            showTopDivider = false
                        )

                        CommonGroupsView(
                            groups = userDetails.commonGroups,
                            viewmodel = chatdetailsViewmodel
                        )

                        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                    }
                }
            }

            SettingsDivider(
                title = stringResource(Res.string.chat_details_actions),
                showTopDivider = false
            )

            //Waking is Android only - the receiving alarm service has no iOS/Desktop counterpart.
            //Allow wakeup from everyone
            SettingsOption(
                icon = Icons.Default.Alarm,
                text = stringResource(Res.string.wake_button),
                onClick = { showWakeReasonDialog = true }
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            if (isGroup) {
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
                        .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
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
                        .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                )
            }
        }
    }



    val picker = rememberImagePickerKMP(
        config = ImagePickerKMPConfig(
            cropConfig = CropConfig(
                enabled = true,
                aspectRatioLocked = true,
                circularCrop = true,
                squareCrop = false,
                freeformCrop = false
            ),
            galleryConfig = GalleryConfig(
                allowMultiple = false,
                mimeTypes = listOf(MimeType.IMAGE_ALL),
                includeExif = false,
                // Makes the picker apply the EXIF orientation to the pixels, otherwise
                // rotated photos are cropped and uploaded sideways.
                compressionLevel = CompressionLevel.LOW
            )
        )
    )
    val result = picker.result


    if (showImagePickerDialog) {

        // Handle side effects safely
        LaunchedEffect(result) {
            when (result) {
                is ImagePickerResult.Success -> {
                    chatdetailsViewmodel.updateProfilePic(result.photos.first())

                    picker.reset()
                    showImagePickerDialog = false
                }

                is ImagePickerResult.Dismissed -> {
                    picker.reset()
                    showImagePickerDialog = false
                }

                else -> {}
            }
        }

        BasicAlertDialog(
            onDismissRequest = {
                showImagePickerDialog = false
            }
        ) {

            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .width(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    when (result) {

                        is ImagePickerResult.Loading -> {
                            CircularProgressIndicator()
                        }

                        is ImagePickerResult.Error -> {
                            Text(
                                text = stringResource(
                                    Res.string.image_picker_error,
                                    result.exception.message ?: stringResource(Res.string.unknown_error)
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        is ImagePickerResult.Idle -> {

                            Text(
                                stringResource(Res.string.choose_image_source)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Button(
                                    onClick = {
                                        picker.launchCamera()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(Res.string.camera))
                                }

                                Button(
                                    onClick = {
                                        picker.launchGallery()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(Res.string.gallery))
                                }
                            }
                        }

                        is ImagePickerResult.Success,
                        is ImagePickerResult.Dismissed -> {
                            // handled in LaunchedEffect
                        }
                    }
                }
            }
        }
    }
}

/**
 * Centered profile picture with the chat name below it. The name opens the rename (group) or
 * nickname (user) dialog; [showEditPictureBadge] adds the same edit badge as the own profile in
 * the user settings.
 */
@Composable
private fun ChatDetailsHeader(
    name: String,
    nickName: String?,
    profilePictureUrl: String,
    showEditPictureBadge: Boolean,
    editNameDescription: String,
    onPictureClick: () -> Unit,
    onEditPictureClick: () -> Unit,
    onNameClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(160.dp)
        ) {
            ProfilePictureView(
                filepath = profilePictureUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable { onPictureClick() }
            )

            if (showEditPictureBadge) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .clickable { onEditPictureClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.edit_profile_picture),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { onNameClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // fill = false keeps the edit icon right next to a short name
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = editNameDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        if (!nickName.isNullOrBlank()) {
            Text(
                text = "\"$nickName\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CountLabel(count: Int) {
    Text(
        text = count.toString(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun groupExpiryText(expiresAt: Long): String {
    val timeRemaining = rememberCountdownMillis(key = expiresAt) {
        expiresAt - Clock.System.now().toEpochMilliseconds()
    }

    return if (timeRemaining > 0) {
        stringResource(Res.string.group_expires_in, formatCountdown(timeRemaining))
    } else {
        stringResource(Res.string.group_expired)
    }
}

/**
 * Wheel picker for a group's delete timer. Seeded with the current timer, or one day from now for
 * a group that has none (or whose timer already passed); the earliest pick is a few minutes ahead,
 * matching the server's "expiry must be in the future" rule so the request can't fail on that.
 */
@Composable
private fun GroupExpiryPickerDialog(
    initialExpiresAt: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val timeZone = TimeZone.currentSystemDefault()
    val earliest = Clock.System.now() + 5.minutes
    var pickedDateTime by remember {
        val seed = initialExpiresAt
            ?.let { Instant.fromEpochMilliseconds(it) }
            ?.takeIf { it > earliest }
            ?: (Clock.System.now() + 1.days)
        mutableStateOf(seed.toLocalDateTime(timeZone))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.group_timer_picker_title)) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(pickedDateTime.toInstant(timeZone).toEpochMilliseconds())
            }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.group_timer_picker_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                WheelDateTimePicker(
                    modifier = Modifier.fillMaxWidth(),
                    rowCount = 3,
                    startDateTime = pickedDateTime,
                    minDateTime = earliest.toLocalDateTime(timeZone),
                    textColor = MaterialTheme.colorScheme.onSurface,
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ),
                    onSnappedDateTime = { snapped: LocalDateTime ->
                        pickedDateTime = snapped
                    }
                )
            }
        }
    )
}

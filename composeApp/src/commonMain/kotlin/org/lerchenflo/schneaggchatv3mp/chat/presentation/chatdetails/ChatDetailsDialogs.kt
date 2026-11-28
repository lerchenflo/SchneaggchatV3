package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventDeleteDialog
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.AddExtensionDialog
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.ExtensionListAction
import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.DateTimePickerPopup
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ChangeStringDialog
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change_nickname
import schneaggchatv3mp.composeapp.generated.resources.confirm_delete_group
import schneaggchatv3mp.composeapp.generated.resources.confirm_delete_group_timer
import schneaggchatv3mp.composeapp.generated.resources.confirm_leave_group
import schneaggchatv3mp.composeapp.generated.resources.confirm_leave_group_event
import schneaggchatv3mp.composeapp.generated.resources.confirm_leave_group_own_event
import schneaggchatv3mp.composeapp.generated.resources.confirm_remove_friend
import schneaggchatv3mp.composeapp.generated.resources.enter_nickname
import schneaggchatv3mp.composeapp.generated.resources.group_timer_picker_info
import schneaggchatv3mp.composeapp.generated.resources.group_timer_picker_title
import schneaggchatv3mp.composeapp.generated.resources.remove
import schneaggchatv3mp.composeapp.generated.resources.wake_button
import schneaggchatv3mp.composeapp.generated.resources.wake_reason_placeholder
import schneaggchatv3mp.composeapp.generated.resources.wake_reason_title
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.ImagePickPurpose
import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.ImageSourcePickerDialog

/** The dialog [ChatDetailsState.dialog] names, if any. */
@Composable
fun ChatDetailsDialogs(
    state: ChatDetailsState,
    onAction: (ChatDetailsAction) -> Unit,
) {
    val dismiss = { onAction(ChatDetailsAction.OnDialogDismiss) }

    when (val dialog = state.dialog) {
        null -> Unit

        ChatDetailsDialog.GroupSettings -> state.groupDetails?.group?.let { group ->
            GroupSettingsDialog(
                group = group,
                iAmAdmin = state.iAmAdmin,
                nameError = state.groupNameError,
                onConfirm = { name, description, membersCanAdd ->
                    onAction(ChatDetailsAction.OnGroupSettingsConfirm(name, description, membersCanAdd))
                },
                onDismiss = dismiss,
            )
        }

        ChatDetailsDialog.Nickname -> ChangeStringDialog(
            title = stringResource(Res.string.change_nickname),
            oldString = state.userDetails?.user?.displayName.orEmpty(),
            maxLines = 1,
            placeholder = stringResource(Res.string.enter_nickname),
            onDismiss = dismiss,
            updateString = { onAction(ChatDetailsAction.OnNicknameConfirm(it)) },
            thirdButton = {
                TextButton(onClick = { onAction(ChatDetailsAction.OnNicknameConfirm("")) }) {
                    Text(text = stringResource(Res.string.remove))
                }
            }
        )

        ChatDetailsDialog.Description -> ChangeDescription(
            onDismiss = dismiss,
            descriptionText = state.descriptionDraft,
            updateDescription = { onAction(ChatDetailsAction.OnDescriptionConfirm) },
            updateDescriptionText = { onAction(ChatDetailsAction.OnDescriptionDraftChange(it)) },
            isGroup = state.isGroup
        )

        ChatDetailsDialog.WakeReason -> ChangeStringDialog(
            title = stringResource(Res.string.wake_reason_title),
            oldString = "",
            maxLines = 3,
            placeholder = stringResource(Res.string.wake_reason_placeholder),
            confirmText = stringResource(Res.string.wake_button),
            onDismiss = dismiss,
            updateString = { onAction(ChatDetailsAction.OnWakeConfirm(it)) },
        )

        ChatDetailsDialog.PicturePicker -> ImageSourcePickerDialog(
            visible = true,
            purpose = ImagePickPurpose.ROUND_PICTURE,
            onImagesPicked = { photos, _ -> photos.firstOrNull()?.let { onAction(ChatDetailsAction.OnPicturePicked(it)) } },
            onDismiss = dismiss
        )

        ChatDetailsDialog.ExpiryPicker -> GroupExpiryPickerDialog(
            initialExpiresAt = state.groupExpiresAt,
            onConfirm = { onAction(ChatDetailsAction.OnGroupTimerConfirm(it)) },
            onDismiss = dismiss
        )

        ChatDetailsDialog.RemoveExpiry -> ConfirmationDialog(
            message = stringResource(Res.string.confirm_delete_group_timer),
            onConfirm = { onAction(ChatDetailsAction.OnRemoveGroupTimerConfirm) },
            onDismiss = dismiss
        )

        ChatDetailsDialog.AddMembers -> AddUserToGroupPopup(
            onDismiss = dismiss,
            onSuccess = { onAction(ChatDetailsAction.OnAddMembersConfirm) },
            availableUsers = state.availableNewMembers,
            selectedUsers = state.selectedNewMembers,
            searchterm = state.newMemberSearchTerm,
            onSearchTermChange = { onAction(ChatDetailsAction.OnNewMemberSearchChange(it)) },
            onUserSelected = { onAction(ChatDetailsAction.OnNewMemberSelected(it)) },
            onUserDeselected = { onAction(ChatDetailsAction.OnNewMemberDeselected(it)) },
            isSelected = state::isNewMemberSelected,
        )

        is ChatDetailsDialog.AddExtension -> AddExtensionDialog(
            type = dialog.type,
            onDismiss = dismiss,
            onCreate = { title, settings ->
                onAction(
                    ChatDetailsAction.OnExtensionAction(ExtensionListAction.OnCreateExtension(dialog.type, title, settings))
                )
            }
        )

        ChatDetailsDialog.LeaveGroup -> ConfirmationDialog(
            message = listOfNotNull(stringResource(Res.string.confirm_leave_group), leaveGroupEventHint(state))
                .joinToString("\n\n"),
            onConfirm = { onAction(ChatDetailsAction.OnLeaveGroupConfirm) },
            onDismiss = dismiss
        )

        ChatDetailsDialog.DeleteGroup -> ConfirmationDialog(
            message = stringResource(Res.string.confirm_delete_group),
            onConfirm = { onAction(ChatDetailsAction.OnDeleteGroupConfirm) },
            onDismiss = dismiss
        )

        ChatDetailsDialog.DeleteEvent -> EventDeleteDialog(
            hasGroup = true, // the event of a group details screen always has this group
            onDismiss = dismiss,
            onConfirm = { deleteGroup, deleteEvent ->
                onAction(ChatDetailsAction.OnDeleteEventConfirm(deleteGroup = deleteGroup, deleteEvent = deleteEvent))
            }
        )

        ChatDetailsDialog.RemoveFriend -> ConfirmationDialog(
            message = stringResource(Res.string.confirm_remove_friend, state.chat.name),
            onConfirm = { onAction(ChatDetailsAction.OnRemoveFriendConfirm) },
            onDismiss = dismiss
        )
    }
}

/**
 * Date and time picker for a group's delete timer. Seeded with the current timer, or one day from now for
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
    val initialDateTime = remember {
        val seed = initialExpiresAt
            ?.let { Instant.fromEpochMilliseconds(it) }
            ?.takeIf { it > earliest }
            ?: (Clock.System.now() + 1.days)
        seed.toLocalDateTime(timeZone)
    }

    DateTimePickerPopup(
        initialDateTime = initialDateTime,
        minDateTime = earliest.toLocalDateTime(timeZone),
        title = stringResource(Res.string.group_timer_picker_title),
        infoText = stringResource(Res.string.group_timer_picker_info),
        onConfirm = { onConfirm(it.toInstant(timeZone).toEpochMilliseconds()) },
        onDismiss = onDismiss,
    )
}

/** Leaving an event's group takes back the own "going" (server), except for the event creator. */
@Composable
private fun leaveGroupEventHint(state: ChatDetailsState): String? {
    val event = state.connectedEvent ?: return null
    val hintRes = if (state.isEventCreator) Res.string.confirm_leave_group_own_event else Res.string.confirm_leave_group_event
    return stringResource(hintRes, event.title)
}

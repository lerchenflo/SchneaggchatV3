package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionType
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.addExtensionMenuItem
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.TitleMenuItem
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change_group_timer
import schneaggchatv3mp.composeapp.generated.resources.change_nickname
import schneaggchatv3mp.composeapp.generated.resources.delete_event
import schneaggchatv3mp.composeapp.generated.resources.delete_group
import schneaggchatv3mp.composeapp.generated.resources.delete_group_timer
import schneaggchatv3mp.composeapp.generated.resources.leave_group
import schneaggchatv3mp.composeapp.generated.resources.remove_friend
import schneaggchatv3mp.composeapp.generated.resources.rename_group
import schneaggchatv3mp.composeapp.generated.resources.set_group_timer
import schneaggchatv3mp.composeapp.generated.resources.wake_button

/** What the ⋮ of the chat details can do. Each one opens its dialog in [ChatDetails]. */
sealed interface ChatDetailsMenuAction {
    data object Rename : ChatDetailsMenuAction
    data class AddExtension(val type: GroupExtensionType) : ChatDetailsMenuAction
    data object SetGroupTimer : ChatDetailsMenuAction
    data object RemoveGroupTimer : ChatDetailsMenuAction
    data object Wake : ChatDetailsMenuAction
    data object LeaveGroup : ChatDetailsMenuAction
    data object DeleteGroup : ChatDetailsMenuAction
    data object DeleteEvent : ChatDetailsMenuAction
    data object RemoveFriend : ChatDetailsMenuAction
}

/** Items of the ⋮ of the chat details, the leaving / deleting ones last. */
@Composable
fun chatDetailsMenuItems(
    isGroup: Boolean,
    iAmAdmin: Boolean,
    hasGroupTimer: Boolean,
    /** The group belongs to an event the user created. */
    isEventCreator: Boolean,
    onAction: (ChatDetailsMenuAction) -> Unit,
): List<TitleMenuItem> = if (isGroup) {
    groupMenuItems(
        iAmAdmin = iAmAdmin,
        hasGroupTimer = hasGroupTimer,
        isEventCreator = isEventCreator,
        onAction = onAction
    )
} else {
    userMenuItems(onAction = onAction)
}

@Composable
private fun groupMenuItems(
    iAmAdmin: Boolean,
    hasGroupTimer: Boolean,
    isEventCreator: Boolean,
    onAction: (ChatDetailsMenuAction) -> Unit,
): List<TitleMenuItem> {
    val rename = TitleMenuItem(
        text = stringResource(Res.string.rename_group),
        icon = Icons.Default.Edit,
        onClick = { onAction(ChatDetailsMenuAction.Rename) }
    )
    val addExtension = addExtensionMenuItem(onTypeClick = { onAction(ChatDetailsMenuAction.AddExtension(it)) })
    // Only admins can set, move or clear the delete timer - the server enforces the same rule on
    // /groups/setexpiry, so a non-admin must not get an item that can only fail
    val setTimer = TitleMenuItem(
        text = stringResource(if (hasGroupTimer) Res.string.change_group_timer else Res.string.set_group_timer),
        icon = Icons.Default.Schedule,
        onClick = { onAction(ChatDetailsMenuAction.SetGroupTimer) }
    )
    val removeTimer = TitleMenuItem(
        text = stringResource(Res.string.delete_group_timer),
        icon = Icons.Default.TimerOff,
        onClick = { onAction(ChatDetailsMenuAction.RemoveGroupTimer) }
    )
    val leave = TitleMenuItem(
        text = stringResource(Res.string.leave_group),
        icon = Icons.AutoMirrored.Filled.Logout,
        onClick = { onAction(ChatDetailsMenuAction.LeaveGroup) },
        isDestructive = true
    )
    // Deleting the whole group for everyone is for admins only, the server rejects anyone else
    val delete = TitleMenuItem(
        text = stringResource(Res.string.delete_group),
        icon = Icons.Default.DeleteForever,
        onClick = { onAction(ChatDetailsMenuAction.DeleteGroup) },
        isDestructive = true
    )
    // Only the event's creator can delete it or detach it from the group (the dialog offers both)
    val deleteEvent = TitleMenuItem(
        text = stringResource(Res.string.delete_event),
        icon = Icons.Default.EventBusy,
        onClick = { onAction(ChatDetailsMenuAction.DeleteEvent) },
        isDestructive = true
    )
    val wake = wakeMenuItem(onAction)

    return buildList {
        add(rename)
        add(addExtension)
        if (iAmAdmin) add(setTimer)
        if (iAmAdmin && hasGroupTimer) add(removeTimer)
        add(wake)
        if (isEventCreator) add(deleteEvent)
        add(leave)
        if (iAmAdmin) add(delete)
    }
}

@Composable
private fun userMenuItems(onAction: (ChatDetailsMenuAction) -> Unit): List<TitleMenuItem> = listOf(
    TitleMenuItem(
        text = stringResource(Res.string.change_nickname),
        icon = Icons.Default.Edit,
        onClick = { onAction(ChatDetailsMenuAction.Rename) }
    ),
    wakeMenuItem(onAction),
    TitleMenuItem(
        text = stringResource(Res.string.remove_friend),
        icon = Icons.Default.PersonRemove,
        onClick = { onAction(ChatDetailsMenuAction.RemoveFriend) },
        isDestructive = true
    ),
)

// Waking is Android only - the receiving alarm service has no iOS/Desktop counterpart. Everyone may wake
@Composable
private fun wakeMenuItem(onAction: (ChatDetailsMenuAction) -> Unit) = TitleMenuItem(
    text = stringResource(Res.string.wake_button),
    icon = Icons.Default.Alarm,
    onClick = { onAction(ChatDetailsMenuAction.Wake) }
)

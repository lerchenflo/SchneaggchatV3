package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.addExtensionMenuItem
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.TitleMenuItem
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.change_group_timer
import schneaggchatv3mp.composeapp.generated.resources.change_nickname
import schneaggchatv3mp.composeapp.generated.resources.delete_event
import schneaggchatv3mp.composeapp.generated.resources.delete_group
import schneaggchatv3mp.composeapp.generated.resources.delete_group_timer
import schneaggchatv3mp.composeapp.generated.resources.group_settings_title
import schneaggchatv3mp.composeapp.generated.resources.leave_group
import schneaggchatv3mp.composeapp.generated.resources.remove_friend
import schneaggchatv3mp.composeapp.generated.resources.set_group_timer
import schneaggchatv3mp.composeapp.generated.resources.wake_button

/** Items of the ⋮ of the chat details, the leaving / deleting ones last. */
@Composable
fun chatDetailsMenuItems(
    isGroup: Boolean,
    iAmAdmin: Boolean,
    hasGroupTimer: Boolean,
    /** The group belongs to an event the user created. */
    isEventCreator: Boolean,
    onAction: (ChatDetailsAction) -> Unit,
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
    onAction: (ChatDetailsAction) -> Unit,
): List<TitleMenuItem> {
    // Name, description and (admins) whether members may add people
    val groupSettings = TitleMenuItem(
        text = stringResource(Res.string.group_settings_title),
        icon = Icons.Default.Settings,
        onClick = { onAction(ChatDetailsAction.OnGroupSettingsClick) }
    )
    val addExtension = addExtensionMenuItem(onTypeClick = { onAction(ChatDetailsAction.OnAddExtensionClick(it)) })
    // Only admins can set, move or clear the delete timer - the server enforces the same rule on
    // /groups/setexpiry, so a non-admin must not get an item that can only fail
    val setTimer = TitleMenuItem(
        text = stringResource(if (hasGroupTimer) Res.string.change_group_timer else Res.string.set_group_timer),
        icon = Icons.Default.Schedule,
        onClick = { onAction(ChatDetailsAction.OnSetGroupTimerClick) }
    )
    val removeTimer = TitleMenuItem(
        text = stringResource(Res.string.delete_group_timer),
        icon = Icons.Default.TimerOff,
        onClick = { onAction(ChatDetailsAction.OnRemoveGroupTimerClick) }
    )
    val leave = TitleMenuItem(
        text = stringResource(Res.string.leave_group),
        icon = Icons.AutoMirrored.Filled.Logout,
        onClick = { onAction(ChatDetailsAction.OnLeaveGroupClick) },
        isDestructive = true
    )
    // Deleting the whole group for everyone is for admins only, the server rejects anyone else
    val delete = TitleMenuItem(
        text = stringResource(Res.string.delete_group),
        icon = Icons.Default.DeleteForever,
        onClick = { onAction(ChatDetailsAction.OnDeleteGroupClick) },
        isDestructive = true
    )
    // Only the event's creator can delete it or detach it from the group (the dialog offers both)
    val deleteEvent = TitleMenuItem(
        text = stringResource(Res.string.delete_event),
        icon = Icons.Default.EventBusy,
        onClick = { onAction(ChatDetailsAction.OnDeleteEventClick) },
        isDestructive = true
    )
    val wake = wakeMenuItem(onAction)

    return buildList {
        add(groupSettings)
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
private fun userMenuItems(onAction: (ChatDetailsAction) -> Unit): List<TitleMenuItem> = listOf(
    TitleMenuItem(
        text = stringResource(Res.string.change_nickname),
        icon = Icons.Default.Edit,
        onClick = { onAction(ChatDetailsAction.OnNicknameClick) }
    ),
    wakeMenuItem(onAction),
    TitleMenuItem(
        text = stringResource(Res.string.remove_friend),
        icon = Icons.Default.PersonRemove,
        onClick = { onAction(ChatDetailsAction.OnRemoveFriendClick) },
        isDestructive = true
    ),
)

// Waking is Android only - the receiving alarm service has no iOS/Desktop counterpart. Everyone may wake
@Composable
private fun wakeMenuItem(onAction: (ChatDetailsAction) -> Unit) = TitleMenuItem(
    text = stringResource(Res.string.wake_button),
    icon = Icons.Default.Alarm,
    onClick = { onAction(ChatDetailsAction.OnWakeClick) }
)

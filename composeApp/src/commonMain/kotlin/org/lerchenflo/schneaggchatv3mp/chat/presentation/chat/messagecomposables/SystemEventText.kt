package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables

import schneaggchatv3mp.composeapp.generated.resources.system_event_cancelled
import schneaggchatv3mp.composeapp.generated.resources.system_group_extension_created
import schneaggchatv3mp.composeapp.generated.resources.system_group_extension_deleted
import schneaggchatv3mp.composeapp.generated.resources.system_group_extension_renamed
import schneaggchatv3mp.composeapp.generated.resources.system_event_location_changed
import schneaggchatv3mp.composeapp.generated.resources.system_event_start_changed
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.SystemEventMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.SystemEventType
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.text
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.system_admin_granted
import schneaggchatv3mp.composeapp.generated.resources.system_admin_revoked
import schneaggchatv3mp.composeapp.generated.resources.system_event_changed
import schneaggchatv3mp.composeapp.generated.resources.system_friendship_accepted
import schneaggchatv3mp.composeapp.generated.resources.system_group_created
import schneaggchatv3mp.composeapp.generated.resources.system_group_description_changed
import schneaggchatv3mp.composeapp.generated.resources.system_group_name_changed
import schneaggchatv3mp.composeapp.generated.resources.system_group_picture_changed
import schneaggchatv3mp.composeapp.generated.resources.system_member_added
import schneaggchatv3mp.composeapp.generated.resources.system_member_joined_event
import schneaggchatv3mp.composeapp.generated.resources.system_member_left
import schneaggchatv3mp.composeapp.generated.resources.system_member_removed
import schneaggchatv3mp.composeapp.generated.resources.system_unknown_event
import schneaggchatv3mp.composeapp.generated.resources.system_wake_sent
import schneaggchatv3mp.composeapp.generated.resources.system_wake_sent_reason

/**
 * Localizes a [SystemEventMessage] into the WhatsApp-style sentence shown for it - in the chat as
 * a centered pill (see `SystemMessageItem` in SmallComposables.kt) and in the chat list as the
 * last-message preview (see `UserButton.kt`). Kept in one place so wording never drifts between
 * the two render sites.
 *
 * @param names the names of [SystemEventMessage.userIds]; a missing id shows as "Unknown user"
 */
@Composable
fun systemEventText(event: SystemEventMessage, names: Map<String, UserIdentityName>): String {
    val actorName = names[event.actorId].text()
    val targetNames = event.targets.map { names[it.userId].text() }.joinToString(", ")

    return when (event.eventType) {
        SystemEventType.GROUP_CREATED -> stringResource(Res.string.system_group_created, actorName)
        SystemEventType.GROUP_MEMBER_ADDED -> stringResource(Res.string.system_member_added, actorName, targetNames)
        SystemEventType.GROUP_MEMBER_JOINED_EVENT -> stringResource(Res.string.system_member_joined_event, actorName)
        SystemEventType.GROUP_MEMBER_REMOVED -> stringResource(Res.string.system_member_removed, actorName, targetNames)
        SystemEventType.GROUP_MEMBER_LEFT -> stringResource(Res.string.system_member_left, actorName)
        SystemEventType.GROUP_ADMIN_GRANTED -> stringResource(Res.string.system_admin_granted, actorName, targetNames)
        SystemEventType.GROUP_ADMIN_REVOKED -> stringResource(Res.string.system_admin_revoked, actorName, targetNames)
        SystemEventType.GROUP_NAME_CHANGED -> stringResource(Res.string.system_group_name_changed, actorName, event.text.orEmpty())
        SystemEventType.GROUP_DESCRIPTION_CHANGED -> stringResource(Res.string.system_group_description_changed, actorName)
        SystemEventType.GROUP_PICTURE_CHANGED -> stringResource(Res.string.system_group_picture_changed, actorName)
        SystemEventType.EVENT_CHANGED -> stringResource(Res.string.system_event_changed, actorName)
        SystemEventType.EVENT_START_CHANGED -> event.timestamp?.let { start ->
            stringResource(Res.string.system_event_start_changed, actorName, millisToString(start, "dd.MM.yyyy HH:mm"))
        } ?: stringResource(Res.string.system_event_changed, actorName)
        SystemEventType.EVENT_LOCATION_CHANGED -> stringResource(Res.string.system_event_location_changed, actorName)
        SystemEventType.EVENT_CANCELLED -> stringResource(Res.string.system_event_cancelled, actorName)
        SystemEventType.GROUP_EXTENSION_CREATED -> stringResource(Res.string.system_group_extension_created, actorName, event.text.orEmpty())
        SystemEventType.GROUP_EXTENSION_RENAMED -> stringResource(Res.string.system_group_extension_renamed, actorName, event.previousText.orEmpty(), event.text.orEmpty())
        SystemEventType.GROUP_EXTENSION_DELETED -> stringResource(Res.string.system_group_extension_deleted, actorName, event.text.orEmpty())
        SystemEventType.FRIENDSHIP_ACCEPTED -> stringResource(Res.string.system_friendship_accepted)
        SystemEventType.WAKE_SENT -> if (event.text.isNullOrBlank()) {
            stringResource(Res.string.system_wake_sent, actorName)
        } else {
            stringResource(Res.string.system_wake_sent_reason, actorName, event.text)
        }
        SystemEventType.UNKNOWN -> stringResource(Res.string.system_unknown_event)
    }
}

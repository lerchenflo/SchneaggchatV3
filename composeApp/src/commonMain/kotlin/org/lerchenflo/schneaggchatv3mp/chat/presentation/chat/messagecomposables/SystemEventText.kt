package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.SystemEventMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.SystemEventType
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
 */
@Composable
fun systemEventText(event: SystemEventMessage): String {
    val targetNames = event.targets.joinToString(", ") { it.userName }

    return when (event.eventType) {
        SystemEventType.GROUP_CREATED -> stringResource(Res.string.system_group_created, event.actorName)
        SystemEventType.GROUP_MEMBER_ADDED -> stringResource(Res.string.system_member_added, event.actorName, targetNames)
        SystemEventType.GROUP_MEMBER_REMOVED -> stringResource(Res.string.system_member_removed, event.actorName, targetNames)
        SystemEventType.GROUP_MEMBER_LEFT -> stringResource(Res.string.system_member_left, event.actorName)
        SystemEventType.GROUP_ADMIN_GRANTED -> stringResource(Res.string.system_admin_granted, event.actorName, targetNames)
        SystemEventType.GROUP_ADMIN_REVOKED -> stringResource(Res.string.system_admin_revoked, event.actorName, targetNames)
        SystemEventType.GROUP_NAME_CHANGED -> stringResource(Res.string.system_group_name_changed, event.actorName, event.text.orEmpty())
        SystemEventType.GROUP_DESCRIPTION_CHANGED -> stringResource(Res.string.system_group_description_changed, event.actorName)
        SystemEventType.GROUP_PICTURE_CHANGED -> stringResource(Res.string.system_group_picture_changed, event.actorName)
        SystemEventType.EVENT_CHANGED -> stringResource(Res.string.system_event_changed, event.actorName)
        SystemEventType.FRIENDSHIP_ACCEPTED -> stringResource(Res.string.system_friendship_accepted)
        SystemEventType.WAKE_SENT -> if (event.text.isNullOrBlank()) {
            stringResource(Res.string.system_wake_sent, event.actorName)
        } else {
            stringResource(Res.string.system_wake_sent_reason, event.actorName, event.text)
        }
        SystemEventType.UNKNOWN -> stringResource(Res.string.system_unknown_event)
    }
}

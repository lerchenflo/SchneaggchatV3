@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.events.domain.icon
import org.lerchenflo.schneaggchatv3mp.events.domain.labelRes
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.event_accept
import schneaggchatv3mp.composeapp.generated.resources.event_add_to_calendar
import schneaggchatv3mp.composeapp.generated.resources.event_closes_with_date
import schneaggchatv3mp.composeapp.generated.resources.event_invite_header
import schneaggchatv3mp.composeapp.generated.resources.event_invited_users
import schneaggchatv3mp.composeapp.generated.resources.event_join
import schneaggchatv3mp.composeapp.generated.resources.event_no_group
import schneaggchatv3mp.composeapp.generated.resources.event_not_interested
import schneaggchatv3mp.composeapp.generated.resources.event_you_are_going
import schneaggchatv3mp.composeapp.generated.resources.open_chat
import kotlin.time.Clock

// Popup for a guest looking at someone else's event — everything here is read-only, so an
// outside tap can safely dismiss it (there's nothing to lose).
@Composable
fun EventJoinPopup(
    event: Event,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
    onOpenGroupChat: (String) -> Unit,
    onAccept: (String) -> Unit = {},
    onDismissEvent: (String) -> Unit = {},
    ownStatus: EventParticipationStatus? = null,
    isJoined: Boolean = false,
    isJoining: Boolean = false,
    friendsById: Map<String, User> = emptyMap(),
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = true
        ),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Header: creator avatar + "<creator> wants to <event title> at <start date>"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                EventUserAvatar(
                    userId = event.creatorId,
                    friendsById = friendsById,
                    size = 72.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        Res.string.event_invite_header,
                        event.creatorName,
                        event.title,
                        millisToString(event.startDate, "dd.MM.yyyy HH:mm")
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Type
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = event.type.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(event.type.labelRes()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = event.description,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            event.closeDate?.let { closeDate ->
                Text(
                    text = stringResource(Res.string.event_closes_with_date, millisToString(closeDate, "dd.MM.yyyy HH:mm")),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Visibility
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = event.visibility.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(event.visibility.labelRes()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Invited users
            if (event.invitedUsers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                EventUserAvatarRow(
                    label = stringResource(Res.string.event_invited_users),
                    userIds = event.invitedUsers,
                    friendsById = friendsById
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            EventParticipationOverview(
                event = event,
                friendsById = friendsById
            )

            HorizontalDivider(thickness = 2.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Big centered join/accept/open-chat button, small dismiss and cancel underneath
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isGoing = ownStatus == EventParticipationStatus.ACCEPTED

                // Group membership - not the participation status - decides Open chat: a user can
                // accept a group event without joining its chat, and a legacy member has no entry.
                if (event.groupId != null && isJoined) {
                    NormalButton(
                        text = stringResource(Res.string.open_chat),
                        onClick = { onOpenGroupChat(event.groupId) },
                        primary = true,
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(52.dp)
                    )
                } else if (event.groupId != null) {
                    NormalButton(
                        text = stringResource(Res.string.event_join),
                        onClick = { onJoin(event.id) },
                        primary = true,
                        isLoading = isJoining,
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(52.dp)
                    )
                } else if (isGoing) {
                    Text(
                        text = stringResource(Res.string.event_you_are_going),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall
                    )
                } else {
                    // No group to join, so accepting is the only way to say you are coming
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(Res.string.event_no_group),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NormalButton(
                            text = stringResource(Res.string.event_accept),
                            onClick = { onAccept(event.id) },
                            primary = true,
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(52.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val shareUtils = koinInject<ShareUtils>()
                NormalButton(
                    text = stringResource(Res.string.event_add_to_calendar),
                    onClick = {
                        shareUtils.addEventToCalendar(
                            title = event.title,
                            description = event.description,
                            location = event.location?.let { "${it.lat},${it.long}" } ?: "",
                            startDateMillis = event.startDate,
                            endDateMillis = event.closeDate
                        )
                    },
                    primary = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(52.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // A member of the event group is going by definition - the way out is leaving that
                // group, not dismissing the event, so the button is gone for them
                if (!isJoined && ownStatus != EventParticipationStatus.DISMISSED) {
                    TextButton(onClick = { onDismissEvent(event.id) }) {
                        Text(
                            text = stringResource(Res.string.event_not_interested),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(Res.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun EventJoinPopupPreview() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        EventJoinPopup(
            event = Event(
                id = "",
                creatorId = "",
                type = EventType.OTHER,
                title = "Lets go shopping",
                description = "bla bla bla",
                groupId = null,
                location = null,
                startDate = Clock.System.now().toEpochMilliseconds(),
                closeDate = null,
                invitedUsers = emptyList(),
                visibility = EventVisibility.PUBLIC,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                updatedBy = "awdawd",
                creatorName = "Flo"
            ),
            onDismiss = { },
            onJoin = { },
            onOpenGroupChat = { }
        )
    }
}

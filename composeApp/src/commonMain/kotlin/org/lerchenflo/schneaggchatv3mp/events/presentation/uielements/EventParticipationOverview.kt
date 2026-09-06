package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.goingUserIds
import org.lerchenflo.schneaggchatv3mp.events.domain.labelRes
import org.lerchenflo.schneaggchatv3mp.events.domain.participationOf
import org.lerchenflo.schneaggchatv3mp.events.domain.userIdsWithStatus
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_participation_at
import schneaggchatv3mp.composeapp.generated.resources.event_participation_count

/** Who is going, who has looked, who is not interested - one collapsible avatar row each. */
@Composable
fun EventParticipationOverview(
    event: Event,
    friendsById: Map<String, User>,
    modifier: Modifier = Modifier
) {
    val buckets = listOf(
        EventParticipationStatus.ACCEPTED to event.goingUserIds(),
        EventParticipationStatus.SEEN to event.userIdsWithStatus(EventParticipationStatus.SEEN),
        EventParticipationStatus.DISMISSED to event.userIdsWithStatus(EventParticipationStatus.DISMISSED),
    ).filter { (_, userIds) -> userIds.isNotEmpty() }

    if (buckets.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        buckets.forEach { (status, userIds) ->
            ParticipationBucket(
                status = status,
                userIds = userIds,
                friendsById = friendsById,
                respondedAtOf = { userId -> event.participationOf(userId)?.updatedAt }
            )
        }
    }
}

@Composable
private fun ParticipationBucket(
    status: EventParticipationStatus,
    userIds: List<String>,
    friendsById: Map<String, User>,
    respondedAtOf: (String) -> Long?,
) {
    // Going starts open - it is the one people look for; the other two are noise until asked for
    var expanded by remember(status) { mutableStateOf(status == EventParticipationStatus.ACCEPTED) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Text(
            text = stringResource(Res.string.event_participation_count, stringResource(status.labelRes()), userIds.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }

    AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.fillMaxWidth()) {
            EventUserAvatarRow(
                label = null,
                userIds = userIds,
                friendsById = friendsById,
                contentDescriptionFor = { userId ->
                    val name = friendsById[userId]?.displayName ?: userId
                    respondedAtOf(userId)?.let { at ->
                        stringResource(Res.string.event_participation_at, name, millisToString(at, "dd.MM.yyyy HH:mm"))
                    } ?: name
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
}

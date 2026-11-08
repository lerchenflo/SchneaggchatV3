package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import org.lerchenflo.schneaggchatv3mp.events.domain.serverNameOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.goingUserIds
import org.lerchenflo.schneaggchatv3mp.events.domain.labelRes
import org.lerchenflo.schneaggchatv3mp.events.domain.participationOf
import org.lerchenflo.schneaggchatv3mp.events.domain.unansweredUserIds
import org.lerchenflo.schneaggchatv3mp.events.domain.userIdsWithStatus
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_participation_count
import schneaggchatv3mp.composeapp.generated.resources.event_participation_no_answer
import schneaggchatv3mp.composeapp.generated.resources.event_participation_show_all
import schneaggchatv3mp.composeapp.generated.resources.unknown_user

/** Rows a group shows before "Show all". */
private const val COLLAPSED_ROW_COUNT = 5

/**
 * Who is going, who has not answered, who has looked, who is not interested - one collapsible
 * group of name rows each.
 */
@Composable
fun EventParticipationOverview(
    event: Event,
    usersById: Map<String, User>,
    modifier: Modifier = Modifier
) {
    val groups = listOf(
        ParticipationGroup(stringResource(EventParticipationStatus.ACCEPTED.labelRes()), event.goingUserIds(), startExpanded = true),
        ParticipationGroup(stringResource(Res.string.event_participation_no_answer), event.unansweredUserIds()),
        ParticipationGroup(stringResource(EventParticipationStatus.SEEN.labelRes()), event.userIdsWithStatus(EventParticipationStatus.SEEN)),
        ParticipationGroup(stringResource(EventParticipationStatus.DISMISSED.labelRes()), event.userIdsWithStatus(EventParticipationStatus.DISMISSED)),
    ).filter { it.userIds.isNotEmpty() }

    if (groups.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        groups.forEach { group ->
            ParticipationGroupSection(
                group = group,
                usersById = usersById,
                respondedAtOf = { userId -> event.participationOf(userId)?.updatedAt },
                serverNameOf = { userId -> event.serverNameOf(userId) },
            )
        }
    }
}

private data class ParticipationGroup(
    val label: String,
    val userIds: List<String>,
    // Going starts open - it is the one people look for; the others are noise until asked for
    val startExpanded: Boolean = false,
)

@Composable
private fun ParticipationGroupSection(
    group: ParticipationGroup,
    usersById: Map<String, User>,
    respondedAtOf: (String) -> Long?,
    serverNameOf: (String) -> String?,
) {
    var expanded by remember(group.label) { mutableStateOf(group.startExpanded) }
    var showAll by remember(group.label) { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = stringResource(Res.string.event_participation_count, group.label, group.userIds.size),
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
            val shownIds = if (showAll) group.userIds else group.userIds.take(COLLAPSED_ROW_COUNT)
            shownIds.forEach { userId ->
                ParticipantRow(
                    name = usersById[userId]?.displayName
                        ?: serverNameOf(userId)
                        ?: stringResource(Res.string.unknown_user),
                    respondedAt = respondedAtOf(userId),
                    userId = userId,
                    usersById = usersById,
                )
            }
            val hiddenCount = group.userIds.size - shownIds.size
            if (hiddenCount > 0) {
                TextButton(onClick = { showAll = true }) {
                    Text(stringResource(Res.string.event_participation_show_all, group.userIds.size))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun ParticipantRow(
    name: String,
    respondedAt: Long?,
    userId: String,
    usersById: Map<String, User>,
) {
    val respondedAtText = respondedAt?.let { millisToString(it, "dd.MM.yyyy HH:mm") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        EventUserAvatar(userId = userId, usersById = usersById, size = 32.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (respondedAtText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = respondedAtText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

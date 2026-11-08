package org.lerchenflo.schneaggchatv3mp.events.presentation.detail

import org.lerchenflo.schneaggchatv3mp.utilities.millisToStringWithWeekday
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.Placeholder
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.icon
import org.lerchenflo.schneaggchatv3mp.events.domain.labelRes
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventParticipationOverview
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventUserAvatar
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.StaticMapPreview
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.event_accept
import schneaggchatv3mp.composeapp.generated.resources.event_add_to_calendar
import schneaggchatv3mp.composeapp.generated.resources.event_closes_with_date
import schneaggchatv3mp.composeapp.generated.resources.event_created_by
import schneaggchatv3mp.composeapp.generated.resources.event_join
import schneaggchatv3mp.composeapp.generated.resources.event_no_group
import schneaggchatv3mp.composeapp.generated.resources.event_not_available
import schneaggchatv3mp.composeapp.generated.resources.event_not_interested
import schneaggchatv3mp.composeapp.generated.resources.event_show_on_map
import schneaggchatv3mp.composeapp.generated.resources.event_starts_label
import schneaggchatv3mp.composeapp.generated.resources.event_you_are_going
import schneaggchatv3mp.composeapp.generated.resources.open_chat

private const val DATE_FORMAT = "dd.MM.yyyy HH:mm"

@Composable
fun EventDetailScreenRoot(eventId: String) {
    val viewModel = koinViewModel<EventDetailViewModel> { parametersOf(eventId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    EventDetailScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

/** Full screen event details: everything scrolls, only "Not interested" and the main action stay at the bottom. */
@Composable
fun EventDetailScreen(
    state: EventDetailState,
    onAction: (EventDetailAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = state.event?.title.orEmpty(),
            onBackClick = { onAction(EventDetailAction.OnBackClick) },
            actions = {
                if (state.isCreator) {
                    IconButton(onClick = { onAction(EventDetailAction.OnEditClick) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.edit)
                        )
                    }
                }
            }
        )
        HorizontalDivider()

        val event = state.event
        if (event == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = stringResource(Res.string.event_not_available),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            EventDetailContent(state = state, event = event, onAction = onAction)
        }

        EventDetailActionBar(state = state, event = event, onAction = onAction)
    }
}

@Composable
private fun EventDetailContent(
    state: EventDetailState,
    event: Event,
    onAction: (EventDetailAction) -> Unit,
) {
    // Header: the event's type icon, the title, then "Created by <avatar> <creator>"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
        ) {
            Icon(
                imageVector = event.type.icon(),
                // The type is read out by the type row below
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        EventDetailHeaderText(event = event, usersById = state.usersById)
    }

    Spacer(modifier = Modifier.height(16.dp))

    EventDateRow(event = event, onAddToCalendar = { onAction(EventDetailAction.OnAddToCalendarClick) })

    Spacer(modifier = Modifier.height(12.dp))
    EventInfoRow(icon = event.type.icon(), text = stringResource(event.type.labelRes()))
    Spacer(modifier = Modifier.height(8.dp))
    EventInfoRow(icon = event.visibility.icon(), text = stringResource(event.visibility.labelRes()))

    if (event.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = event.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }

    val location = event.location
    val mapStyleUrl = state.mapStyleUrl
    if (location != null && mapStyleUrl != null) {
        Spacer(modifier = Modifier.height(16.dp))
        StaticMapPreview(
            location = location,
            styleUrl = mapStyleUrl,
            onClick = { onAction(EventDetailAction.OnShowOnMapClick) },
            contentDescription = stringResource(Res.string.event_show_on_map),
            height = 180.dp,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))

    EventParticipationOverview(
        event = event,
        usersById = state.usersById
    )
}

private const val CREATOR_NAME_MARKER = "\uE000"
private const val CREATOR_AVATAR_ID = "creatorAvatar"

/**
 * The title as headline, below it "Created by <avatar> <creator>" with the name highlighted. The
 * translation decides where the name goes, so it is put in as a marker and replaced afterwards.
 */
@Composable
private fun EventDetailHeaderText(event: Event, usersById: Map<String, User>) {
    Text(
        text = event.title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))

    val template = stringResource(Res.string.event_created_by, CREATOR_NAME_MARKER)
    val nameAt = template.indexOf(CREATOR_NAME_MARKER)
    val highlight = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    val text = buildAnnotatedString {
        if (nameAt < 0) {
            append(template)
        } else {
            append(template.substring(0, nameAt))
            appendInlineContent(CREATOR_AVATAR_ID, event.creatorName)
            append(" ")
            withStyle(highlight) { append(event.creatorName) }
            append(template.substring(nameAt + CREATOR_NAME_MARKER.length))
        }
    }
    val inlineContent = mapOf(
        CREATOR_AVATAR_ID to InlineTextContent(
            Placeholder(width = 1.6.em, height = 1.6.em, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                EventUserAvatar(userId = event.creatorId, usersById = usersById, size = maxWidth)
            }
        }
    )
    Text(
        text = text,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Start and end date, with "Add to calendar" right next to them (below them when there is no room). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventDateRow(
    event: Event,
    onAddToCalendar: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(Res.string.event_starts_label) + " " + millisToStringWithWeekday(event.startDate, DATE_FORMAT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                event.closeDate?.let { closeDate ->
                    Text(
                        text = stringResource(Res.string.event_closes_with_date, millisToStringWithWeekday(closeDate, DATE_FORMAT)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        AssistChip(
            onClick = onAddToCalendar,
            label = { Text(stringResource(Res.string.event_add_to_calendar)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun EventInfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Fixed at the bottom: "Not interested" and the main action (Join / Open chat / Accept) side by
 * side, the main action on the trailing side. Hidden when there is nothing to show.
 */
@Composable
private fun EventDetailActionBar(
    state: EventDetailState,
    event: Event,
    onAction: (EventDetailAction) -> Unit,
) {
    val isGoing = state.ownStatus == EventParticipationStatus.ACCEPTED
    val groupId = event.groupId
    // A member of the event group is going by definition - the way out is leaving that group.
    // The creator can't leave their own event either.
    val showNotInterested = !state.isCreator && !state.isJoined && state.ownStatus != EventParticipationStatus.DISMISSED
    val mainAction: (@Composable (Modifier) -> Unit)? = when {
        groupId != null && state.isJoined -> { modifier ->
            NormalButton(
                text = stringResource(Res.string.open_chat),
                onClick = { onAction(EventDetailAction.OnOpenChatClick) },
                primary = true,
                modifier = modifier,
            )
        }
        groupId != null -> { modifier ->
            NormalButton(
                text = stringResource(Res.string.event_join),
                onClick = { onAction(EventDetailAction.OnJoinClick) },
                primary = true,
                isLoading = state.isJoining,
                modifier = modifier,
            )
        }
        // No group to join, so accepting is the only way to say you are coming
        !isGoing -> { modifier ->
            NormalButton(
                text = stringResource(Res.string.event_accept),
                onClick = { onAction(EventDetailAction.OnAcceptClick) },
                primary = true,
                modifier = modifier,
            )
        }
        else -> null
    }
    val statusTextRes = when {
        groupId != null -> null
        isGoing -> Res.string.event_you_are_going
        else -> Res.string.event_no_group
    }
    if (!showNotInterested && mainAction == null && statusTextRes == null) return

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (statusTextRes != null) {
                Text(
                    text = stringResource(statusTextRes),
                    color = if (isGoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = if (isGoing) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showNotInterested || mainAction != null) Spacer(modifier = Modifier.height(8.dp))
            }
            if (showNotInterested || mainAction != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (showNotInterested) {
                        OutlinedButton(
                            onClick = { onAction(EventDetailAction.OnNotInterestedClick) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(Res.string.event_not_interested),
                                maxLines = 1
                            )
                        }
                    }
                    mainAction?.invoke(Modifier.weight(1f))
                }
            }
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.goingUserIds
import org.lerchenflo.schneaggchatv3mp.events.domain.isUnseenBy
import org.lerchenflo.schneaggchatv3mp.events.domain.statusOf
import org.lerchenflo.schneaggchatv3mp.events.presentation.CalendarBirthday
import org.lerchenflo.schneaggchatv3mp.events.presentation.birthdaysOn
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventItem
import org.lerchenflo.schneaggchatv3mp.sharedUi.DateChip
import org.lerchenflo.schneaggchatv3mp.utilities.toFormattedString
import org.lerchenflo.schneaggchatv3mp.utilities.weekdayShortResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.events_day_empty

/**
 * Single-day agenda: prev/next arrows step one day, the header label jumps back to today.
 * Same row composables as the week view so a day reads identically in both.
 */
@Composable
fun EventsDayView(
    anchorDate: LocalDate,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Event>>,
    birthdaysByMonthDay: Map<Int, List<CalendarBirthday>>,
    usersById: Map<String, User>,
    ownId: String?,
    onNavigate: (forward: Boolean) -> Unit,
    onJumpToToday: () -> Unit,
    onEventClick: (String) -> Unit,
    onBirthdayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayEvents = eventsByDate[anchorDate].orEmpty()
    val dayBirthdays = birthdaysOn(birthdaysByMonthDay, anchorDate)
    val weekdayLabel = stringResource(weekdayShortResource(anchorDate.dayOfWeek))

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "day_header") {
            CalendarNavHeader(
                label = "$weekdayLabel, ${anchorDate.toFormattedString()}",
                onPrev = { onNavigate(false) },
                onNext = { onNavigate(true) },
                onToday = onJumpToToday
            )
        }

        item(key = "day_chip") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                DateChip(anchorDate.toFormattedString(), highlighted = anchorDate == today)
            }
        }

        if (dayEvents.isEmpty() && dayBirthdays.isEmpty()) {
            item(key = "day_empty") {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = stringResource(Res.string.events_day_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@LazyColumn
        }

        items(
            items = dayBirthdays,
            key = { "birthday_${anchorDate}_${it.userId}" }
        ) { birthday ->
            CalendarBirthdayRow(
                birthday = birthday,
                date = anchorDate,
                onClick = onBirthdayClick
            )
        }
        items(
            items = dayEvents,
            key = { "event_${anchorDate}_${it.id}" }
        ) { event ->
            val creatorUser = usersById[event.creatorId]
            EventItem(
                event = event,
                creatorProfilePictureUrl = creatorUser?.profilePictureUrl,
                isOwnEvent = event.creatorId == ownId,
                onClick = { onEventClick(event.id) },
                ownStatus = ownId?.let { event.statusOf(it) },
                isUnseen = ownId != null && event.isUnseenBy(ownId),
                goingCount = event.goingUserIds().size
            )
        }
    }
}

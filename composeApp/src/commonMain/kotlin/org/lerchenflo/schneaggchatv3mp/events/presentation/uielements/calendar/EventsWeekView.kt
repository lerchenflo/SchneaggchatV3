package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.presentation.CalendarBirthday
import org.lerchenflo.schneaggchatv3mp.events.presentation.birthdaysOn
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventItem
import org.lerchenflo.schneaggchatv3mp.sharedUi.DateChip
import org.lerchenflo.schneaggchatv3mp.utilities.startOfWeek
import org.lerchenflo.schneaggchatv3mp.utilities.toFormattedString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.events_week_empty

@Composable
fun EventsWeekView(
    anchorDate: LocalDate,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Event>>,
    birthdaysByMonthDay: Map<Int, List<CalendarBirthday>>,
    friendsById: Map<String, User>,
    ownId: String?,
    onNavigate: (forward: Boolean) -> Unit,
    onJumpToToday: () -> Unit,
    onEventClick: (String) -> Unit,
    onBirthdayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekStart = startOfWeek(anchorDate)
    val weekDays = (0..6).map { weekStart.plus(DatePeriod(days = it)) }
    val isWeekEmpty = weekDays.all { eventsByDate[it].orEmpty().isEmpty() && birthdaysOn(birthdaysByMonthDay, it).isEmpty() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "week_header") {
            CalendarNavHeader(
                label = "${weekStart.toFormattedString()} - ${weekDays.last().toFormattedString()}",
                onPrev = { onNavigate(false) },
                onNext = { onNavigate(true) },
                onToday = onJumpToToday
            )
        }

        if (isWeekEmpty) {
            item(key = "week_empty") {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = stringResource(Res.string.events_week_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@LazyColumn
        }

        weekDays.forEach { day ->
            item(key = "weekday_$day") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateChip(day.toFormattedString(), highlighted = day == today)
                }
            }
            items(
                items = birthdaysOn(birthdaysByMonthDay, day),
                key = { "birthday_${day}_${it.userId}" }
            ) { birthday ->
                CalendarBirthdayRow(
                    birthday = birthday,
                    date = day,
                    onClick = onBirthdayClick
                )
            }
            items(
                items = eventsByDate[day].orEmpty(),
                key = { it.id }
            ) { event ->
                val creatorFriend = friendsById[event.creatorId]
                EventItem(
                    event = event,
                    creatorProfilePictureUrl = creatorFriend?.profilePictureUrl,
                    isOwnEvent = event.creatorId == ownId,
                    onClick = { onEventClick(event.id) }
                )
            }
        }
    }
}

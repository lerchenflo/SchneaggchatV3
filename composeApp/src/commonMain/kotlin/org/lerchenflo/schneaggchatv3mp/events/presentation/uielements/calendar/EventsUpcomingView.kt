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
import org.lerchenflo.schneaggchatv3mp.events.domain.goingUserIds
import org.lerchenflo.schneaggchatv3mp.events.domain.isUnseenBy
import org.lerchenflo.schneaggchatv3mp.events.domain.statusOf
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventItem
import org.lerchenflo.schneaggchatv3mp.sharedUi.DateChip
import org.lerchenflo.schneaggchatv3mp.utilities.toFormattedString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.events_upcoming_empty

/** How many days (today included) the "Upcoming" tab shows. */
private const val UPCOMING_WINDOW_DAYS = 10

/**
 * A rolling window of the next [UPCOMING_WINDOW_DAYS] days (today included), unlike the Month
 * view this never pages into the past or an arbitrary future range - it always reflects "what's
 * coming up" as of [today].
 */
@Composable
fun EventsUpcomingView(
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Event>>,
    birthdaysByMonthDay: Map<Int, List<CalendarBirthday>>,
    usersById: Map<String, User>,
    ownId: String?,
    onEventClick: (String) -> Unit,
    onBirthdayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val upcomingDays = (0 until UPCOMING_WINDOW_DAYS).map { today.plus(DatePeriod(days = it)) }
    val isEmpty = upcomingDays.all { eventsByDate[it].orEmpty().isEmpty() && birthdaysOn(birthdaysByMonthDay, it).isEmpty() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isEmpty) {
            item(key = "upcoming_empty") {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = stringResource(Res.string.events_upcoming_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@LazyColumn
        }

        upcomingDays.forEach { day ->
            item(key = "upcomingday_$day") {
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
                // Multi-day events sit in several day buckets, so a bare id would repeat.
                key = { "event_${day}_${it.id}" }
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
}

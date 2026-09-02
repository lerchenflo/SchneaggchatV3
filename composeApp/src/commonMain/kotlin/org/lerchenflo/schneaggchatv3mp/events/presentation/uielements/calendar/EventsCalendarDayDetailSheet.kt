@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.calendar

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.presentation.CalendarBirthday
import org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventItem
import org.lerchenflo.schneaggchatv3mp.sharedUi.DateChip
import org.lerchenflo.schneaggchatv3mp.utilities.toFormattedString

/** Bottom sheet listing a single day's birthdays and events, opened from a tapped month-view day cell. */
@Composable
fun EventsCalendarDayDetailSheet(
    date: LocalDate,
    events: List<Event>,
    birthdays: List<CalendarBirthday>,
    friendsById: Map<String, User>,
    ownId: String?,
    onDismiss: () -> Unit,
    onEventClick: (String) -> Unit,
    onBirthdayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "date_chip") {
                DateChip(date.toFormattedString())
            }
            items(items = birthdays, key = { "birthday_${it.userId}" }) { birthday ->
                CalendarBirthdayRow(
                    birthday = birthday,
                    date = date,
                    onClick = onBirthdayClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(items = events, key = { it.id }) { event ->
                val creatorFriend = friendsById[event.creatorId]
                EventItem(
                    event = event,
                    creatorProfilePictureUrl = creatorFriend?.profilePictureUrl,
                    isOwnEvent = event.creatorId == ownId,
                    onClick = { onEventClick(event.id) }
                )
            }
            item(key = "bottom_spacer") {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

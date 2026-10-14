package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.color
import org.lerchenflo.schneaggchatv3mp.events.presentation.CalendarBirthday
import org.lerchenflo.schneaggchatv3mp.events.presentation.birthdaysOn
import org.lerchenflo.schneaggchatv3mp.utilities.monthNameResource
import org.lerchenflo.schneaggchatv3mp.utilities.startOfWeek
import org.lerchenflo.schneaggchatv3mp.utilities.weekdayShortResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.birthday_label
import schneaggchatv3mp.composeapp.generated.resources.events_month_empty

@Composable
fun EventsMonthView(
    anchorDate: LocalDate,
    eventsByDate: Map<LocalDate, List<Event>>,
    birthdaysByMonthDay: Map<Int, List<CalendarBirthday>>,
    today: LocalDate,
    onNavigate: (forward: Boolean) -> Unit,
    onJumpToToday: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstOfMonth = LocalDate(anchorDate.year, anchorDate.month, 1)
    // 6 full weeks always covers a month regardless of where it starts/how many weeks it spans.
    val gridStart = startOfWeek(firstOfMonth)
    val gridDays = (0 until 42).map { gridStart.plus(DatePeriod(days = it)) }
    val isMonthEmpty = gridDays
        .filter { it.month == anchorDate.month }
        .all { eventsByDate[it].orEmpty().isEmpty() && birthdaysOn(birthdaysByMonthDay, it).isEmpty() }

    Column(modifier = modifier.fillMaxWidth()) {
        CalendarNavHeader(
            label = "${stringResource(monthNameResource(anchorDate.month.number))} ${anchorDate.year}",
            onPrev = { onNavigate(false) },
            onNext = { onNavigate(true) },
            onToday = onJumpToToday
        )

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            gridDays.take(7).forEach { day ->
                Text(
                    text = stringResource(weekdayShortResource(day.dayOfWeek)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        gridDays.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    MonthDayCell(
                        date = day,
                        isCurrentMonth = day.month == anchorDate.month,
                        isToday = day == today,
                        events = eventsByDate[day].orEmpty(),
                        birthdays = birthdaysOn(birthdaysByMonthDay, day),
                        onClick = onDayClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (isMonthEmpty) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.events_month_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    events: List<Event>,
    birthdays: List<CalendarBirthday>,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val birthdayLabel = stringResource(Res.string.birthday_label)

    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .let { if (isToday) it.background(MaterialTheme.colorScheme.primaryContainer) else it }
            .clickable(enabled = events.isNotEmpty() || birthdays.isNotEmpty()) { onClick(date) }
            .heightIn(min = 44.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
        )
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            if (birthdays.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = birthdayLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(10.dp)
                )
            }
            events.take(4).forEach { event ->
                Spacer(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(event.type.color())
                )
            }
        }
    }
}

package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.events.presentation.CalendarBirthday
import org.lerchenflo.schneaggchatv3mp.utilities.ageOnDate
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.birthday_label
import schneaggchatv3mp.composeapp.generated.resources.birthday_turns_age

/**
 * Slim, deliberately lighter-weight row for a birthday on the events calendar - distinct from the
 * full [org.lerchenflo.schneaggchatv3mp.events.presentation.uielements.EventItem] card so it never
 * reads as an event.
 */
@Composable
fun CalendarBirthdayRow(
    birthday: CalendarBirthday,
    date: LocalDate,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val age = ageOnDate(birthday.birthYear, date)
    val text = if (age != null) {
        "${birthday.displayName} · ${stringResource(Res.string.birthday_turns_age, age)}"
    } else {
        birthday.displayName
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !birthday.isOwn) { onClick(birthday.userId) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Cake,
            contentDescription = stringResource(Res.string.birthday_label),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.login.presentation.signup

import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.DateWheelPicker
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.create_account
import schneaggchatv3mp.composeapp.generated.resources.create_account_subtitle
import schneaggchatv3mp.composeapp.generated.resources.ok
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@Composable
fun SignUpHeaderText(
    alignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
){
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        ActivityTitle(
            title = stringResource(Res.string.create_account),
            onBackClick = {
                onBackClick()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.create_account_subtitle),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun BirthdatePickerPopup(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    defaultDate: LocalDate? = null
) {
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    // Avoid .minus(DateTimeUnit.YEAR), which is unstable on iOS: subtract the years by hand,
    // clamping Feb 29 to Feb 28 in non-leap years
    fun yearsBefore(years: Int): LocalDate {
        val targetYear = today.year - years
        val targetDay = if (today.month == Month.FEBRUARY &&
            today.day == 29 &&
            !LocalDate.isLeapYear(targetYear)
        ) 28 else today.day
        return LocalDate(targetYear, today.month, targetDay)
    }

    // Oldest birthday that can be picked: 80 years back
    val earliestDate = remember(today) { yearsBefore(80) }
    val safeDefaultDate = remember(defaultDate, today) {
        (defaultDate ?: yearsBefore(18)).coerceIn(earliestDate, today)
    }

    var selectedDate by remember { mutableStateOf(safeDefaultDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                // Clamp to valid range before confirming — guards against picker edge cases on iOS
                val clamped = when {
                    selectedDate < earliestDate -> earliestDate
                    // A birthday can't be in the future
                    selectedDate > today -> today
                    else -> selectedDate
                }
                onDateSelected(clamped)
            }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        text = {
            DateWheelPicker(
                startDate = selectedDate,
                onDateChange = { selectedDate = it },
                minDate = earliestDate,
                maxDate = today,
            )
        }
    )
}

// Helper — kotlinx-datetime doesn't expose this directly
private fun LocalDate.Companion.isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)


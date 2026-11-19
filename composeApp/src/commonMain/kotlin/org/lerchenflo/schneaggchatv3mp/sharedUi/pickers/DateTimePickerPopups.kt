package org.lerchenflo.schneaggchatv3mp.sharedUi.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.utilities.toFormattedString
import org.lerchenflo.schneaggchatv3mp.utilities.weekdayShortResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.picker_back
import kotlin.time.Instant

// The app's only entry points to the date and time pickers (Material3): a calendar for the date,
// then a clock dial for the time. Days outside min/max can't be tapped in the calendar; a time
// outside them on the first or last allowed day is moved to the nearest allowed value on confirm.

private val FALLBACK_MIN_DATE = LocalDate(1900, 1, 1)
private val FALLBACK_MAX_DATE = LocalDate(2100, 12, 31)

/** Calendar dialog for a date between [minDate] and [maxDate] (both included). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerPopup(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    title: String? = null,
    infoText: String? = null,
) {
    val min = minDate ?: FALLBACK_MIN_DATE
    val max = maxDate ?: FALLBACK_MAX_DATE
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.coerceIn(min, max).toUtcMillis(),
        yearRange = min.year..max.year,
        selectableDates = remember(min, max) { SelectableDateRange(min, max) },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it.utcMillisToLocalDate()) } },
                enabled = state.selectedDateMillis != null,
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    ) {
        if (title == null && infoText == null) {
            DatePicker(state = state)
        } else {
            DatePicker(
                state = state,
                title = { PickerTitle(title = title, infoText = infoText) },
            )
        }
    }
}

/**
 * Two steps: the calendar, then a clock dial for the time. "Back" on the clock returns to the
 * calendar. The result is kept between [minDateTime] and [maxDateTime].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerPopup(
    initialDateTime: LocalDateTime,
    onConfirm: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
    minDateTime: LocalDateTime? = null,
    maxDateTime: LocalDateTime? = null,
    title: String? = null,
    infoText: String? = null,
) {
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val date = pickedDate

    if (date == null) {
        DatePickerPopup(
            initialDate = initialDateTime.date,
            onConfirm = { pickedDate = it },
            onDismiss = onDismiss,
            minDate = minDateTime?.date,
            maxDate = maxDateTime?.date,
            title = title,
            infoText = infoText,
        )
        return
    }

    val timeState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        // The picked day, so the user still sees it while choosing the time
        title = { Text(stringResource(weekdayShortResource(date.dayOfWeek)) + ", " + date.toFormattedString()) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (infoText != null) {
                    Text(
                        text = infoText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                TimePicker(state = timeState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val picked = LocalDateTime(date, LocalTime(timeState.hour, timeState.minute))
                onConfirm(picked.coerceInOptional(minDateTime, maxDateTime))
            }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { pickedDate = null }) {
                Text(stringResource(Res.string.picker_back))
            }
        },
    )
}

@Composable
private fun PickerTitle(title: String?, infoText: String?) {
    // Same padding as the calendar's default title
    Column(modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) {
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
        }
        if (infoText != null) {
            Text(
                text = infoText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The calendar works on UTC midnight millis; only days from [min] to [max] can be tapped. */
@OptIn(ExperimentalMaterial3Api::class)
private class SelectableDateRange(
    private val min: LocalDate,
    private val max: LocalDate,
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis.utcMillisToLocalDate() in min..max
    override fun isSelectableYear(year: Int): Boolean = year in min.year..max.year
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.utcMillisToLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

private fun LocalDateTime.coerceInOptional(min: LocalDateTime?, max: LocalDateTime?): LocalDateTime = when {
    min != null && this < min -> min
    max != null && this > max -> max
    else -> this
}

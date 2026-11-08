package org.lerchenflo.schneaggchatv3mp.sharedUi.pickers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.WheelRows
import dev.darkokoa.datetimewheelpicker.core.format.DateFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.utilities.germanNumericDateFormatter
import org.lerchenflo.schneaggchatv3mp.utilities.toFormattedString
import org.lerchenflo.schneaggchatv3mp.utilities.weekdayShortResource

// The app's only entry points to the wheel picker library: same look everywhere, a live weekday
// line above the wheel (the wheel itself shows day numbers only), and values outside the range are
// dropped (the picker can deliver them on iOS).

private val FALLBACK_MIN_DATE = LocalDate(1900, 1, 1)
private val FALLBACK_MAX_DATE = LocalDate(2100, 12, 31)

/**
 * Date and time wheel with "Sat, 14.11.2026 18:00" above it. [onDateTimeChange] gets every
 * snapped value, while scrolling and when it settles.
 */
@Composable
fun DateTimeWheelPicker(
    startDateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    minDateTime: LocalDateTime? = null,
    maxDateTime: LocalDateTime? = null,
    rows: Int = 3,
) {
    val min = minDateTime ?: LocalDateTime(FALLBACK_MIN_DATE.year, 1, 1, 0, 0)
    val max = maxDateTime ?: LocalDateTime(FALLBACK_MAX_DATE.year, 12, 31, 23, 59)
    var shown by remember { mutableStateOf(startDateTime) }
    val onSnapped: (LocalDateTime) -> Unit = { snapped ->
        if (snapped in min..max) {
            shown = snapped
            onDateTimeChange(snapped)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        PickedLabel(
            stringResource(weekdayShortResource(shown.date.dayOfWeek)) + ", " +
                shown.date.toFormattedString() + " " +
                shown.hour.toString().padStart(2, '0') + ":" + shown.minute.toString().padStart(2, '0')
        )
        Spacer(modifier = Modifier.height(8.dp))
        WheelDateTimePicker(
            modifier = Modifier.fillMaxWidth(),
            startDateTime = startDateTime,
            minDateTime = min,
            maxDateTime = max,
            rows = WheelRows.Count(rows),
            textColor = MaterialTheme.colorScheme.onSurface,
            selectorProperties = selectorProperties(),
            onSnappedDateTimeChanged = onSnapped,
            onSnappedDateTime = onSnapped,
        )
    }
}

/**
 * Date wheel (dd.MM.yyyy numerals by default) with "Sat, 14.11.2026" above it. [onDateChange]
 * gets every snapped value, while scrolling and when it settles.
 */
@Composable
fun DateWheelPicker(
    startDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    rows: Int = 5,
    dateFormatter: DateFormatter = germanNumericDateFormatter(),
) {
    val min = minDate ?: FALLBACK_MIN_DATE
    val max = maxDate ?: FALLBACK_MAX_DATE
    var shown by remember { mutableStateOf(startDate) }
    val onSnapped: (LocalDate) -> Unit = { snapped ->
        if (snapped in min..max) {
            shown = snapped
            onDateChange(snapped)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        PickedLabel(stringResource(weekdayShortResource(shown.dayOfWeek)) + ", " + shown.toFormattedString())
        Spacer(modifier = Modifier.height(8.dp))
        WheelDatePicker(
            modifier = Modifier.fillMaxWidth(),
            startDate = startDate,
            minDate = min,
            maxDate = max,
            dateFormatter = dateFormatter,
            rows = WheelRows.Count(rows),
            textColor = MaterialTheme.colorScheme.onSurface,
            selectorProperties = selectorProperties(),
            onSnappedDateChanged = onSnapped,
            onSnappedDate = onSnapped,
        )
    }
}

@Composable
private fun selectorProperties() = WheelPickerDefaults.selectorProperties(
    enabled = true,
    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
)

@Composable
private fun PickedLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

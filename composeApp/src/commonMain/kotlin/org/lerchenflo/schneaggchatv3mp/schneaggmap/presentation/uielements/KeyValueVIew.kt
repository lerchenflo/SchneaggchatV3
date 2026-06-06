@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeDefinition
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.BoolValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.DoubleValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.IntValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.StringValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.label
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun KeyValueView(
    value: AttributeValue?,
    definition: AttributeDefinition,
    onValueChange: (AttributeValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {


        Text(
            text = definition.label() + if (definition.required) " *" else "",
        )

        Spacer(modifier = Modifier.weight(1f))

        when (definition) {
            is AttributeDefinition.BoolDef -> {
                Switch(
                    checked = (value as? BoolValue)?.value ?: false,
                    onCheckedChange = {
                        onValueChange(BoolValue(it))
                    },
                    modifier = Modifier.width(120.dp)

                )
            }

            is AttributeDefinition.DoubleDef -> {
                var rawText by remember(value) {
                    mutableStateOf(((value as? DoubleValue)?.value ?: "").toString())
                }

                val error = rawText.toDoubleOrNull().let { parsed ->
                    when {
                        rawText.isBlank() && definition.required -> "Required"
                        parsed == null && rawText.isNotBlank()   -> "Invalid number"
                        parsed != null && definition.min != null && parsed < definition.min -> "Min: ${definition.min}"
                        parsed != null && definition.max != null && parsed > definition.max -> "Max: ${definition.max}"
                        else -> null
                    }
                }

                TextField(
                    value = rawText,
                    onValueChange = { input ->
                        rawText = input
                        input.toDoubleOrNull()?.let { parsed ->
                            val clamped = when {
                                definition.min != null && parsed < definition.min -> definition.min
                                definition.max != null && parsed > definition.max -> definition.max
                                else -> parsed
                            }
                            onValueChange(DoubleValue(clamped))
                        }
                    },
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(120.dp)
                )
            }

            is AttributeDefinition.IntDef -> {
                var rawText by remember(value) {
                    mutableStateOf(((value as? IntValue)?.value ?: "").toString())
                }

                val error = rawText.toIntOrNull().let { parsed ->
                    when {
                        rawText.isBlank() && definition.required              -> "Required"
                        parsed == null && rawText.isNotBlank()                -> "Invalid number"
                        parsed != null && definition.min != null && parsed < definition.min -> "Min: ${definition.min}"
                        parsed != null && definition.max != null && parsed > definition.max -> "Max: ${definition.max}"
                        else -> null
                    }
                }

                TextField(
                    value = rawText,
                    onValueChange = { input ->
                        rawText = input
                        input.toIntOrNull()?.let { parsed ->
                            val clamped = when {
                                definition.min != null && parsed < definition.min -> definition.min
                                definition.max != null && parsed > definition.max -> definition.max
                                else -> parsed
                            }
                            onValueChange(IntValue(clamped))
                        }
                    },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp)
                )

            }

            is AttributeDefinition.StringDef -> {
                var rawText by remember(value) {
                    mutableStateOf((value as? StringValue)?.value ?: "")
                }

                val error = when {
                    rawText.isBlank() && definition.required                                    -> "Required"
                    definition.maxLength != null && rawText.length > definition.maxLength -> "Max ${definition.maxLength} chars"
                    else -> null
                }

                TextField(
                    value = rawText,
                    onValueChange = { input ->
                        val clamped = if (definition.maxLength != null) input.take(definition.maxLength) else input
                        rawText = clamped
                        onValueChange(StringValue(clamped))
                    },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )
            }

            //THis is a timestamp not a long
            is AttributeDefinition.LongDef -> {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val endOfCurrentYear = LocalDate(now.year, 12, 31)

                val currentMillis = (value as? AttributeValue.LongValue)?.value
                val initialDateTime = currentMillis
                    ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()) }
                    ?: now

                var selectedDate by remember { mutableStateOf(initialDateTime.date) }
                var selectedTime by remember { mutableStateOf(initialDateTime.time) }

                // Keep onValueChange in sync whenever date or time changes
                LaunchedEffect(selectedDate, selectedTime) {
                    val millis = LocalDateTime(date = selectedDate, time = selectedTime)
                        .toInstant(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds()
                    onValueChange(AttributeValue.LongValue(millis))
                }

                val timePickerState = rememberTimePickerState(
                    initialHour   = initialDateTime.hour,
                    initialMinute = initialDateTime.minute,
                    is24Hour      = true,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WheelDatePicker(
                        modifier = Modifier.fillMaxWidth(),
                        startDate = selectedDate,
                        minDate = LocalDate(1900, 1, 1),
                        maxDate = endOfCurrentYear,
                        rowCount = 5,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        selectorProperties = WheelPickerDefaults.selectorProperties(
                            enabled = true,
                            color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ),
                        onSnappedDate = { snappedDate ->
                            if (snappedDate >= LocalDate(1900, 1, 1) && snappedDate <= endOfCurrentYear) {
                                selectedDate = snappedDate
                            }
                        }
                    )

                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // Sync time picker state back into selectedTime
                    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                        selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, heightDp = 600)
@Composable
private fun AllKeyValueTypesPreview() {
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeyValueView(
                value = BoolValue(true),
                definition = AttributeDefinition.BoolDef(
                    key = "official",
                    required = true
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = IntValue(80),
                definition = AttributeDefinition.IntDef(
                    key = "speedLimit",
                    required = true,
                    min = 10,
                    max = 130
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = DoubleValue(15.5),
                definition = AttributeDefinition.DoubleDef(
                    key = "entryFee",
                    required = false,
                    min = 0.0,
                    max = 100.0
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = StringValue("Pizza"),
                definition = AttributeDefinition.StringDef(
                    key = "foodType",
                    required = true,
                    maxLength = 30
                ),
                onValueChange = {}
            )
        }
    }
}
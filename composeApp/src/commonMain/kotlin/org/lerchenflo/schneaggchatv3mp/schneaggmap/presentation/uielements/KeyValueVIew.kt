@file:OptIn(ExperimentalMaterial3Api::class, FormatStringsInDatetimeFormats::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeDefinition
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeKey
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.BoolValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.DoubleValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.IntValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.LongValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue.StringValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.labelRes
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.koin.mp.KoinPlatform
import kotlin.math.roundToInt
import org.lerchenflo.schneaggchatv3mp.utilities.germanNumericDateFormatter
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
            text = stringResource(definition.key.labelRes()) + if (definition.required) " *" else "",
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

            is AttributeDefinition.LongDef -> {
                var rawText by remember(value) {
                    mutableStateOf(((value as? LongValue)?.value ?: "").toString())
                }

                val error = rawText.toLongOrNull().let { parsed ->
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
                        input.toLongOrNull()?.let { parsed ->
                            val clamped = when {
                                definition.min != null && parsed < definition.min -> definition.min
                                definition.max != null && parsed > definition.max -> definition.max
                                else -> parsed
                            }
                            onValueChange(LongValue(clamped))
                        }
                    },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp)
                )
            }

            is AttributeDefinition.EnumDef -> {
                var expanded by remember { mutableStateOf(false) }
                val storedName = (value as? StringValue)?.value
                val selectedOption = definition.options.firstOrNull { it.name == storedName }

                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.width(160.dp)
                    ) {
                        Text(
                            // Unknown stored names (e.g. an option added in a newer app version) are shown raw
                            text = selectedOption?.let { stringResource(it.labelRes) } ?: storedName.orEmpty(),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        definition.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.labelRes)) },
                                onClick = {
                                    onValueChange(StringValue(option.name))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            is AttributeDefinition.PriceDef -> {
                DecimalInputField(
                    value = (value as? DoubleValue)?.value,
                    required = definition.required,
                    max = definition.max,
                    suffix = "€",
                    maxDecimals = 2,
                    onValueChange = { onValueChange(DoubleValue(it)) },
                )
            }

            is AttributeDefinition.DistanceDef -> {
                DecimalInputField(
                    value = (value as? DoubleValue)?.value,
                    required = definition.required,
                    max = definition.max,
                    suffix = definition.unit.symbol,
                    maxDecimals = null,
                    onValueChange = { onValueChange(DoubleValue(it)) },
                )
            }

            is AttributeDefinition.RatingDef -> {
                val storedRating = (value as? IntValue)?.value
                var sliderValue by remember(storedRating) {
                    mutableStateOf((storedRating ?: definition.min).coerceIn(definition.min, definition.max).toFloat())
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(200.dp)
                ) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onValueChange(IntValue(sliderValue.roundToInt())) },
                        valueRange = definition.min.toFloat()..definition.max.toFloat(),
                        steps = (definition.max - definition.min - 1).coerceAtLeast(0),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = sliderValue.roundToInt().toString(),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            is AttributeDefinition.SecretDef -> {
                var rawText by remember(value) {
                    mutableStateOf((value as? StringValue)?.value ?: "")
                }
                var secretVisible by remember { mutableStateOf(false) }

                TextField(
                    value = rawText,
                    onValueChange = { input ->
                        val clamped = if (definition.maxLength != null) input.take(definition.maxLength) else input
                        rawText = clamped
                        onValueChange(StringValue(clamped))
                    },
                    isError = rawText.isBlank() && definition.required,
                    singleLine = true,
                    visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { secretVisible = !secretVisible }) {
                                Icon(
                                    imageVector = if (secretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                            IconButton(
                                onClick = {
                                    // ShareUtils directly - copyToClipboard() would echo the secret in a snackbar
                                    KoinPlatform.getKoin().get<ShareUtils>().copyToClipboard(rawText)
                                },
                                enabled = rawText.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    modifier = Modifier.width(220.dp)
                )
            }

            is AttributeDefinition.DateTimeDef -> {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                // A few years ahead so upcoming dates (e.g. the next horse riding tournament) can be picked
                val maxPickableDate = LocalDate(now.year + 5, 12, 31)

                val currentMillis = (value as? LongValue)?.value
                val initialDateTime = currentMillis
                    ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()) }
                    ?: now

                var selectedDate by remember { mutableStateOf(initialDateTime.date) }
                var selectedTime by remember { mutableStateOf(initialDateTime.time) }
                var showDialog by remember { mutableStateOf(false) }

                // Keep onValueChange in sync whenever date or time changes
                LaunchedEffect(selectedDate, selectedTime) {
                    val millis = LocalDateTime(date = selectedDate, time = selectedTime)
                        .toInstant(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds()
                    onValueChange(LongValue(millis))
                }

                // Trigger button — shows the current value, opens dialog on click
                OutlinedButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = LocalDateTime(date = selectedDate, time = selectedTime)
                            .format(LocalDateTime.Format { byUnicodePattern("HH:mm dd.MM.yyyy") })
                    )
                }

                if (showDialog) {
                    // Dialog-local state — only committed on OK
                    var dialogDate by remember { mutableStateOf(selectedDate) }

                    val timePickerState = rememberTimePickerState(
                        initialHour   = initialDateTime.hour,
                        initialMinute = initialDateTime.minute,
                        is24Hour      = true,
                    )

                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                selectedDate = dialogDate
                                selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                                showDialog = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Cancel")
                            }
                        },
                        title = { Text("Select Date & Time") },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                WheelDatePicker(
                                    modifier = Modifier.fillMaxWidth(),
                                    startDate = dialogDate,
                                    minDate = LocalDate(1900, 1, 1),
                                    maxDate = maxPickableDate,
                                    dateFormatter = germanNumericDateFormatter(),
                                    rowCount = 5,
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    selectorProperties = WheelPickerDefaults.selectorProperties(
                                        enabled = true,
                                        color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ),
                                    onSnappedDate = { snappedDate ->
                                        if (snappedDate >= LocalDate(1900, 1, 1) && snappedDate <= maxPickableDate) {
                                            dialogDate = snappedDate
                                        }
                                    }
                                )

                                HorizontalDivider()

                                TimePicker(
                                    state = timePickerState,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}


/**
 * Non-negative decimal input with a unit [suffix]. Accepts both '.' and ',' as decimal separator and
 * rejects input with more than [maxDecimals] decimals. Only resyncs from [value] when it differs from
 * what is typed, so e.g. "3." is not overwritten with "3.0" while typing.
 */
@Composable
private fun DecimalInputField(
    value: Double?,
    required: Boolean,
    max: Double?,
    suffix: String,
    maxDecimals: Int?,
    onValueChange: (Double) -> Unit,
) {
    var rawText by remember { mutableStateOf(value?.toString() ?: "") }

    LaunchedEffect(value) {
        if (rawText.replace(',', '.').toDoubleOrNull() != value) {
            rawText = value?.toString() ?: ""
        }
    }

    val parsed = rawText.replace(',', '.').toDoubleOrNull()
    val error = when {
        rawText.isBlank() && required            -> "Required"
        parsed == null && rawText.isNotBlank()   -> "Invalid number"
        parsed != null && parsed < 0.0           -> "Min: 0"
        parsed != null && max != null && parsed > max -> "Max: $max"
        else -> null
    }

    TextField(
        value = rawText,
        onValueChange = onChange@{ input ->
            val separatorIndex = input.indexOfFirst { it == '.' || it == ',' }
            if (maxDecimals != null && separatorIndex >= 0 && input.length - separatorIndex - 1 > maxDecimals) {
                return@onChange
            }
            rawText = input
            input.replace(',', '.').toDoubleOrNull()?.let { number ->
                onValueChange(number.coerceIn(0.0, max ?: Double.MAX_VALUE))
            }
        },
        isError = error != null,
        singleLine = true,
        suffix = { Text(suffix) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.width(120.dp)
    )
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
                    key = AttributeKey.CAMPING_SITTING_POSSIBILITY,
                    required = true
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = IntValue(80),
                definition = AttributeDefinition.IntDef(
                    key = AttributeKey.RADAR_SPEED_LIMIT,
                    required = true,
                    min = 10,
                    max = 130
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = DoubleValue(15.5),
                definition = AttributeDefinition.DoubleDef(
                    key = AttributeKey.SIGHTSEEING_ENTRY_FEE,
                    required = false,
                    min = 0.0,
                    max = 100.0
                ),
                onValueChange = {}
            )

            KeyValueView(
                value = StringValue("Pizza"),
                definition = AttributeDefinition.StringDef(
                    key = AttributeKey.FOOD_OTHER_CUISINE,
                    required = true,
                    maxLength = 30
                ),
                onValueChange = {}
            )
        }
    }
}
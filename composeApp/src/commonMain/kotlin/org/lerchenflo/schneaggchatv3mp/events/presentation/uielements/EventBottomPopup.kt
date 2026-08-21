@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.icon
import org.lerchenflo.schneaggchatv3mp.events.domain.labelRes
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.event_closes_with_date
import schneaggchatv3mp.composeapp.generated.resources.event_created_by
import schneaggchatv3mp.composeapp.generated.resources.event_description_label
import schneaggchatv3mp.composeapp.generated.resources.event_has_end_time
import schneaggchatv3mp.composeapp.generated.resources.event_join
import schneaggchatv3mp.composeapp.generated.resources.event_starts_label
import schneaggchatv3mp.composeapp.generated.resources.event_starts_with_date
import schneaggchatv3mp.composeapp.generated.resources.event_title_label
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.save
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun EventBottomPopup(
    event: Event,
    onSave: (Event) -> Unit,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var currentEvent by remember(event) {
        mutableStateOf(event)
    }

    // Separate toggle state so we can flip "has end date" off without losing the picker's
    // last-set value if the user re-enables it (avoids re-deriving a default every toggle)
    var hasCloseDate by remember {
        mutableStateOf(event.closeDate != null)
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val myEvent = event.creatorId == SessionCache.requireLoggedIn()?.userId

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = true
        ),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Selectable Type label & icon
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (myEvent) {
                    OutlinedButton(
                        onClick = { typeDropdownExpanded = true }
                    ) {
                        Icon(
                            imageVector = currentEvent.type.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(currentEvent.type.labelRes()))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        EventType.entries.forEach { entry ->
                            val isSelected = entry == currentEvent.type
                            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = entry.icon(),
                                        contentDescription = null,
                                        tint = tint
                                    )
                                },
                                text = {
                                    Text(
                                        text = stringResource(entry.labelRes()),
                                        color = tint,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    currentEvent = currentEvent.copy(type = entry)
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = currentEvent.type.icon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(currentEvent.type.labelRes()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            TextField(
                value = currentEvent.title,
                onValueChange = {
                    currentEvent = currentEvent.copy(title = it)
                },
                enabled = myEvent,
                label = {
                    Text(text = stringResource(Res.string.event_title_label))
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            TextField(
                value = currentEvent.description,
                onValueChange = {
                    currentEvent = currentEvent.copy(description = it)
                },
                enabled = myEvent,
                label = {
                    Text(text = stringResource(Res.string.event_description_label))
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Start date
            if (myEvent) {
                Text(
                    text = stringResource(Res.string.event_starts_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = millisToString(currentEvent.startDate, "dd.MM.yyyy HH:mm"),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.event_starts_with_date, millisToString(currentEvent.startDate, "dd.MM.yyyy HH:mm")),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // End date (optional)
            if (myEvent) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.event_has_end_time),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = hasCloseDate,
                        onCheckedChange = { checked ->
                            hasCloseDate = checked
                            if (checked) {
                                val defaultClose = currentEvent.closeDate ?: (currentEvent.startDate + 60 * 60 * 1000L)
                                currentEvent = currentEvent.copy(closeDate = defaultClose)
                                showEndDatePicker = true
                            } else {
                                currentEvent = currentEvent.copy(closeDate = null)
                            }
                        }
                    )
                }

                if (hasCloseDate && currentEvent.closeDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = millisToString(currentEvent.closeDate!!, "dd.MM.yyyy HH:mm"),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                currentEvent.closeDate?.let { closeDate ->
                    Text(
                        text = stringResource(Res.string.event_closes_with_date, millisToString(closeDate, "dd.MM.yyyy HH:mm")),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Creator info
            Text(
                text = stringResource(Res.string.event_created_by, currentEvent.creatorName),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(thickness = 2.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel / join / save row
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                val changed = event != currentEvent

                NormalButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                    primary = false
                )

                Spacer(modifier = Modifier.weight(1f))

                if (myEvent) {
                    NormalButton(
                        text = stringResource(Res.string.save),
                        onClick = { onSave(currentEvent) },
                        disabled = !changed,
                        primary = false,
                        showOutline = true
                    )
                } else {
                    NormalButton(
                        text = stringResource(Res.string.event_join),
                        onClick = { onJoin(currentEvent.id) },
                        primary = true
                    )
                }
            }
        }
    }

    // Start Date Picker Dialog
    if (showStartDatePicker) {
        var tempStartDateTime by remember {
            mutableStateOf(
                Instant.fromEpochMilliseconds(currentEvent.startDate)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            )
        }

        AlertDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newStartMillis = tempStartDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    val newCloseDate = currentEvent.closeDate?.let { close ->
                        if (close <= newStartMillis) newStartMillis + 60 * 60 * 1000L else close
                    }
                    currentEvent = currentEvent.copy(
                        startDate = newStartMillis,
                        closeDate = newCloseDate
                    )
                    showStartDatePicker = false
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            text = {
                WheelDateTimePicker(
                    modifier = Modifier.fillMaxWidth(),
                    rowCount = 3,
                    startDateTime = tempStartDateTime,
                    minDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    textColor = MaterialTheme.colorScheme.onSurface,
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ),
                    onSnappedDateTime = { snapped: LocalDateTime ->
                        tempStartDateTime = snapped
                    }
                )
            }
        )
    }

    // End Date Picker Dialog
    if (showEndDatePicker && currentEvent.closeDate != null) {
        var tempEndDateTime by remember {
            mutableStateOf(
                Instant.fromEpochMilliseconds(currentEvent.closeDate!!)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            )
        }

        AlertDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newEndMillis = tempEndDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    currentEvent = currentEvent.copy(closeDate = newEndMillis)
                    showEndDatePicker = false
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            text = {
                WheelDateTimePicker(
                    modifier = Modifier.fillMaxWidth(),
                    rowCount = 3,
                    startDateTime = tempEndDateTime,
                    minDateTime = Instant.fromEpochMilliseconds(currentEvent.startDate)
                        .toLocalDateTime(TimeZone.currentSystemDefault()),
                    textColor = MaterialTheme.colorScheme.onSurface,
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ),
                    onSnappedDateTime = { snapped: LocalDateTime ->
                        tempEndDateTime = snapped
                    }
                )
            }
        )
    }
}

@Preview
@Composable
private fun EventBottomPopupPreview() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        EventBottomPopup(
            event = Event(
                id = "",
                creatorId = "",
                type = EventType.SHOPPING,
                title = "Lets go shopping",
                description = "bla bla bla",
                groupId = "",
                location = null,
                startDate = Clock.System.now().toEpochMilliseconds(),
                closeDate = null,
                invitedUsers = emptyList(),
                public = true,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                updatedBy = "awdawd",
                creatorName = "Flo"
            ),
            onSave = { },
            onDismiss = { },
            onJoin = { }
        )
    }
}
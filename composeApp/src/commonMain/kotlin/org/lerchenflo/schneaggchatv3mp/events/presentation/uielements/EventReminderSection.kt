package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.events.domain.EventReminders
import org.lerchenflo.schneaggchatv3mp.events.presentation.leadTimeText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_add
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_before
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_custom
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_days
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_hours
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_minutes
import schneaggchatv3mp.composeapp.generated.resources.event_reminder_remove
import schneaggchatv3mp.composeapp.generated.resources.event_reminders_label
import schneaggchatv3mp.composeapp.generated.resources.event_reminders_none

private const val MINUTES_PER_DAY = 24 * 60

/**
 * The event's reminders as removable chips plus an "Add reminder" chip. Sorted longest first,
 * at most [EventReminders.MAX_COUNT]. Reminders already past are dropped when the event is saved.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventReminderSection(
    reminderMinutes: List<Int>,
    onChange: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isAddDialogOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.event_reminders_label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (reminderMinutes.isEmpty()) {
            Text(
                text = stringResource(Res.string.event_reminders_none),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            reminderMinutes.forEach { minutes ->
                val removeLabel = stringResource(Res.string.event_reminder_remove)
                InputChip(
                    selected = false,
                    onClick = { onChange(reminderMinutes - minutes) },
                    label = { Text(stringResource(Res.string.event_reminder_before, leadTimeText(minutes))) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = removeLabel,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                )
            }
            if (reminderMinutes.size < EventReminders.MAX_COUNT) {
                AssistChip(
                    onClick = { isAddDialogOpen = true },
                    label = { Text(stringResource(Res.string.event_reminder_add)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                )
            }
        }
    }

    if (isAddDialogOpen) {
        AddReminderDialog(
            existing = reminderMinutes,
            onAdd = { minutes ->
                onChange(EventReminders.add(reminderMinutes, minutes))
                isAddDialogOpen = false
            },
            onDismiss = { isAddDialogOpen = false },
        )
    }
}

/** Presets add at once; the custom row takes days, hours and minutes (5 min steps). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddReminderDialog(
    existing: List<Int>,
    onAdd: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var days by remember { mutableIntStateOf(0) }
    var hours by remember { mutableIntStateOf(2) }
    var minutes by remember { mutableIntStateOf(30) }
    val customTotal = days * MINUTES_PER_DAY + hours * 60 + minutes
    val customAllowed = EventReminders.isValid(customTotal) && customTotal !in existing

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.event_reminder_add)) },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventReminders.PRESETS.filterNot { it in existing }.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = { onAdd(preset) },
                            label = { Text(stringResource(Res.string.event_reminder_before, leadTimeText(preset))) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.event_reminder_custom),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ReminderStepper(
                    label = stringResource(Res.string.event_reminder_days),
                    value = days,
                    onValueChange = { days = it },
                    range = 0..(EventReminders.MAX_MINUTES / MINUTES_PER_DAY),
                    step = 1,
                )
                ReminderStepper(
                    label = stringResource(Res.string.event_reminder_hours),
                    value = hours,
                    onValueChange = { hours = it },
                    range = 0..23,
                    step = 1,
                )
                ReminderStepper(
                    label = stringResource(Res.string.event_reminder_minutes),
                    value = minutes,
                    onValueChange = { minutes = it },
                    range = 0..55,
                    step = EventReminders.STEP_MINUTES,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.event_reminder_before, leadTimeText(customTotal)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (customAllowed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(customTotal) }, enabled = customAllowed) {
                Text(stringResource(Res.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun ReminderStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = value > range.first
        ) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = "$label -")
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        IconButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = value < range.last
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "$label +")
        }
    }
}

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import kotlin.time.Clock

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

    //Separate toggle state so we can flip "has end date" off without losing the picker's
    //last-set value if the user re-enables it (avoids re-deriving a default every toggle)
    var hasCloseDate by remember {
        mutableStateOf(event.closeDate != null)
    }

    val myEvent = event.creatorId == SessionCache.requireLoggedIn()?.userId

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)) {

            //Scrollable column to be able to hoist a long description / more fields later
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {

                //Type label
                Text(
                    text = currentEvent.type.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                //Title
                TextField(
                    value = currentEvent.title,
                    onValueChange = {
                        currentEvent = currentEvent.copy(title = it)
                    },
                    enabled = myEvent,
                    label = {
                        Text(text = "Title")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                //Description
                TextField(
                    value = currentEvent.description,
                    onValueChange = {
                        currentEvent = currentEvent.copy(description = it)
                    },
                    enabled = myEvent,
                    label = {
                        Text(text = "Description")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                //Start date - editable wheel picker for the owner, plain text for everyone else
                if (myEvent) {
                    Text(
                        text = "Starts:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    WheelDateTimePicker(
                        modifier = Modifier.fillMaxWidth(),
                        rowCount = 3,
                        startDateTime = Instant.fromEpochMilliseconds(currentEvent.startDate)
                            .toLocalDateTime(TimeZone.currentSystemDefault()),
                        minDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                        textColor = MaterialTheme.colorScheme.onSurface,
                        selectorProperties = WheelPickerDefaults.selectorProperties(
                            enabled = true,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ),
                        onSnappedDateTime = { snapped: LocalDateTime ->
                            currentEvent = currentEvent.copy(
                                startDate = snapped.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                            )
                        }
                    )
                } else {
                    Text(
                        text = "Starts: " + millisToTimeDateOrYesterday(currentEvent.startDate),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                //End date (optional) - owner gets a toggle + picker, others get plain text or nothing
                if (myEvent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Has end time",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = hasCloseDate,
                            onCheckedChange = { checked ->
                                hasCloseDate = checked
                                currentEvent = currentEvent.copy(
                                    closeDate = if (checked) {
                                        //Default to 1 hour after the start time when first enabled
                                        currentEvent.closeDate ?: (currentEvent.startDate + 60 * 60 * 1000L)
                                    } else {
                                        null
                                    }
                                )
                            }
                        )
                    }

                    if (hasCloseDate) {
                        Spacer(modifier = Modifier.height(4.dp))

                        WheelDateTimePicker(
                            modifier = Modifier.fillMaxWidth(),
                            rowCount = 3,
                            startDateTime = kotlin.time.Instant.fromEpochMilliseconds(
                                currentEvent.closeDate ?: currentEvent.startDate
                            ).toLocalDateTime(TimeZone.currentSystemDefault()),
                            //Can't end before it starts
                            minDateTime = kotlin.time.Instant.fromEpochMilliseconds(currentEvent.startDate)
                                .toLocalDateTime(TimeZone.currentSystemDefault()),
                            textColor = MaterialTheme.colorScheme.onSurface,
                            selectorProperties = WheelPickerDefaults.selectorProperties(
                                enabled = true,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ),
                            onSnappedDateTime = { snapped: LocalDateTime ->
                                currentEvent = currentEvent.copy(
                                    closeDate = snapped.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                                )
                            }
                        )
                    }
                } else {
                    currentEvent.closeDate?.let { closeDate ->
                        Text(
                            text = "Closes: " + millisToTimeDateOrYesterday(closeDate),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                //Creator info, mirroring the "last changed by" line from MapEntryInfoCard
                Text(
                    text = "Created by " + currentEvent.creatorName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(thickness = 4.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            //Cancel / join / save row
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                val changed = event != currentEvent

                NormalButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    primary = false
                )

                Spacer(modifier = Modifier.weight(1f))

                if (myEvent) {
                    NormalButton(
                        text = "Save",
                        onClick = { onSave(currentEvent) },
                        disabled = !changed,
                        primary = false,
                        showOutline = true
                    )
                } else {
                    NormalButton(
                        text = "Join",
                        onClick = { onJoin(currentEvent.id) },
                        primary = true
                    )
                }
            }
        }
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
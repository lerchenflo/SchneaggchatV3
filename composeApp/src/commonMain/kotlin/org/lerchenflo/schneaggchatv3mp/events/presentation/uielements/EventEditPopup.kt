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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.WheelDateTimePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.events.domain.icon
import org.lerchenflo.schneaggchatv3mp.events.domain.labelRes
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.MemberSelector
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.event_description_label
import schneaggchatv3mp.composeapp.generated.resources.event_has_end_time
import schneaggchatv3mp.composeapp.generated.resources.event_invite_friends
import schneaggchatv3mp.composeapp.generated.resources.event_invited_users
import schneaggchatv3mp.composeapp.generated.resources.event_location_label
import schneaggchatv3mp.composeapp.generated.resources.event_no_location_selected
import schneaggchatv3mp.composeapp.generated.resources.event_starts_label
import schneaggchatv3mp.composeapp.generated.resources.event_title_label
import schneaggchatv3mp.composeapp.generated.resources.error_event_title_must_not_be_empty
import schneaggchatv3mp.composeapp.generated.resources.latlong
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.save
import kotlin.time.Clock
import kotlin.time.Instant

//Keep in sync with the server-side limit in ValidationUtils.validateEventTitle (schneaggchatv3server)
private const val EVENT_TITLE_MAX_LENGTH = 200

// Popup for the event's creator - every field is editable, outside taps never dismiss it
// (an accidental tap must not silently discard in-progress edits).
@Composable
fun EventEditPopup(
    event: Event,
    onSave: (Event, ImageBitmap?) -> Unit,
    onDismiss: () -> Unit,
    friendsById: Map<String, User> = emptyMap(),
    groups: List<Group> = emptyList(),
    mapStyleUrl: String = "",
    modifier: Modifier = Modifier
) {

    var currentEvent by remember(event) {
        mutableStateOf(event)
    }

    // Only a brand-new event's group gets created with this picture - an edit must not
    // overwrite a group picture the members may have set manually since.
    val isNewEvent = event.id == ""
    val typeIconBitmap = if (isNewEvent) rememberEventTypeIconBitmap(currentEvent.type) else null

    // Separate toggle state so we can flip "has end date" off without losing the picker's
    // last-set value if the user re-enables it (avoids re-deriving a default every toggle)
    var hasCloseDate by remember {
        mutableStateOf(event.closeDate != null)
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var visibilityDropdownExpanded by remember { mutableStateOf(false) }
    var showInviteFriendsDialog by remember { mutableStateOf(false) }
    var inviteSearchTerm by remember { mutableStateOf("") }
    var showLocationPicker by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf(false) }

    LaunchedEffect(currentEvent.title) {
        if (titleError && currentEvent.title.isNotBlank()) {
            titleError = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = false
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            TextField(
                value = currentEvent.title,
                onValueChange = {
                    if (it.length <= EVENT_TITLE_MAX_LENGTH) {
                        currentEvent = currentEvent.copy(title = it)
                    }
                },
                label = {
                    Text(text = stringResource(Res.string.event_title_label))
                },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(text = stringResource(Res.string.error_event_title_must_not_be_empty)) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            TextField(
                value = currentEvent.description,
                onValueChange = {
                    currentEvent = currentEvent.copy(description = it)
                },
                label = {
                    Text(text = stringResource(Res.string.event_description_label))
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location (optional)
            Text(
                text = stringResource(Res.string.event_location_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = { showLocationPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val location = currentEvent.location
                Text(
                    text = if (location != null) {
                        stringResource(Res.string.latlong, location.lat.toString().take(8), location.long.toString().take(8))
                    } else {
                        stringResource(Res.string.event_no_location_selected)
                    },
                    modifier = Modifier.weight(1f)
                )
                if (location != null) {
                    IconButton(onClick = { currentEvent = currentEvent.copy(location = null) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start date
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

            Spacer(modifier = Modifier.height(12.dp))

            // End date (optional)
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

            Spacer(modifier = Modifier.height(12.dp))

            // Event visibility
            Box {
                OutlinedButton(
                    onClick = { visibilityDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = currentEvent.visibility.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(currentEvent.visibility.labelRes()),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = visibilityDropdownExpanded,
                    onDismissRequest = { visibilityDropdownExpanded = false }
                ) {
                    EventVisibility.entries.forEach { entry ->
                        val isSelected = entry == currentEvent.visibility
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
                                currentEvent = currentEvent.copy(visibility = entry)
                                visibilityDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Invited users
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showInviteFriendsDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(Res.string.event_invite_friends))
            }

            if (currentEvent.invitedUsers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.event_invited_users),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(currentEvent.invitedUsers, key = { it }) { userId ->
                        EventUserAvatar(
                            userId = userId,
                            friendsById = friendsById,
                            size = 40.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(thickness = 2.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel / save row
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

                NormalButton(
                    text = stringResource(Res.string.save),
                    onClick = {
                        if (currentEvent.title.isBlank()) {
                            titleError = true
                        } else {
                            onSave(currentEvent, typeIconBitmap)
                        }
                    },
                    disabled = !changed,
                    primary = false,
                    showOutline = true
                )
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

    // Invite friends dialog
    if (showInviteFriendsDialog) {
        val availableUsers = remember(friendsById, groups) {
            groups.map { it.toChatListItem() } + friendsById.values.map { it.toChatListItem() }
        }
        val selectedUsers = remember(currentEvent.invitedUsers, friendsById) {
            currentEvent.invitedUsers.mapNotNull { userId -> friendsById[userId]?.toChatListItem() }
        }

        // Members of a group that are actually invitable (i.e. also a friend) - a group can
        // contain people who aren't friends of the event creator.
        fun groupMemberIds(groupId: String): Set<String> =
            groups.find { it.id == groupId }?.members?.map { it.userId }?.toSet() ?: emptySet()

        ModalBottomSheet(
            onDismissRequest = { showInviteFriendsDialog = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.event_invite_friends),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                MemberSelector(
                    availableUsers = availableUsers,
                    selectedUsers = selectedUsers,
                    searchTerm = inviteSearchTerm,
                    onSearchTermChange = { inviteSearchTerm = it },
                    onUserSelected = { user ->
                        if (user.isGroup) {
                            val memberIds = groupMemberIds(user.id)
                            val friendIds = friendsById.keys.filter { it in memberIds }
                            currentEvent = currentEvent.copy(invitedUsers = (currentEvent.invitedUsers + friendIds).toSet().toList())
                        } else {
                            currentEvent = currentEvent.copy(invitedUsers = currentEvent.invitedUsers + user.id)
                        }
                    },
                    onUserDeselected = { user ->
                        if (user.isGroup) {
                            val memberIds = groupMemberIds(user.id)
                            currentEvent = currentEvent.copy(invitedUsers = currentEvent.invitedUsers.filterNot { it in memberIds })
                        } else {
                            currentEvent = currentEvent.copy(invitedUsers = currentEvent.invitedUsers - user.id)
                        }
                    },
                    isSelected = { user ->
                        if (user.isGroup) {
                            val memberIds = groupMemberIds(user.id)
                            val relevantIds = friendsById.keys.filter { it in memberIds }
                            relevantIds.isNotEmpty() && relevantIds.all { it in currentEvent.invitedUsers }
                        } else {
                            selectedUsers.contains(user)
                        }
                    },
                    minUsers = 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                NormalButton(
                    text = stringResource(Res.string.ok),
                    onClick = { showInviteFriendsDialog = false },
                    primary = false,
                    showOutline = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Location picker sheet
    if (showLocationPicker) {
        EventLocationPickerSheet(
            initialLocation = currentEvent.location,
            mapStyleUrl = mapStyleUrl,
            onConfirm = { location ->
                currentEvent = currentEvent.copy(location = location)
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false }
        )
    }
}

@Preview
@Composable
private fun EventEditPopupPreview() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        EventEditPopup(
            event = Event(
                id = "",
                creatorId = "",
                type = EventType.OTHER,
                title = "Lets go shopping",
                description = "bla bla bla",
                groupId = "",
                location = null,
                startDate = Clock.System.now().toEpochMilliseconds(),
                closeDate = null,
                invitedUsers = emptyList(),
                visibility = EventVisibility.PUBLIC,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                updatedBy = "awdawd",
                creatorName = "Flo"
            ),
            onSave = { _, _ -> },
            onDismiss = { }
        )
    }
}

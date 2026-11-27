package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentity
import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.DateTimePickerPopup
import org.lerchenflo.schneaggchatv3mp.utilities.millisToStringWithWeekday
import schneaggchatv3mp.composeapp.generated.resources.event_show_on_map
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.StaticMapPreview
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.EventMarkerLayer
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.toPosition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EVENT_TITLE_MAX_LENGTH
import org.lerchenflo.schneaggchatv3mp.events.domain.EventTitleError
import org.lerchenflo.schneaggchatv3mp.events.domain.validateEventTitle
import org.lerchenflo.schneaggchatv3mp.events.domain.isFree
import org.lerchenflo.schneaggchatv3mp.events.domain.isValidEntryFee
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.events.domain.GroupDeleteDelay
import org.lerchenflo.schneaggchatv3mp.events.domain.icon
import org.lerchenflo.schneaggchatv3mp.events.domain.labelRes
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.clearFocusOnTap
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.MemberSelector
import org.lerchenflo.schneaggchatv3mp.utilities.formatAmountNumber
import org.lerchenflo.schneaggchatv3mp.utilities.isValidAmountInput
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import org.lerchenflo.schneaggchatv3mp.utilities.parseAmountToCents
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.decimal_separator
import schneaggchatv3mp.composeapp.generated.resources.delete_event
import schneaggchatv3mp.composeapp.generated.resources.error_event_max_users_invalid
import schneaggchatv3mp.composeapp.generated.resources.error_event_title_must_not_be_empty
import schneaggchatv3mp.composeapp.generated.resources.error_event_title_too_long
import schneaggchatv3mp.composeapp.generated.resources.event_create_group_info
import schneaggchatv3mp.composeapp.generated.resources.event_create_group_label
import schneaggchatv3mp.composeapp.generated.resources.event_description_label
import schneaggchatv3mp.composeapp.generated.resources.event_group_delete_label
import schneaggchatv3mp.composeapp.generated.resources.event_has_end_time
import schneaggchatv3mp.composeapp.generated.resources.event_invite_friends
import schneaggchatv3mp.composeapp.generated.resources.event_invited_users
import schneaggchatv3mp.composeapp.generated.resources.event_location_label
import schneaggchatv3mp.composeapp.generated.resources.event_type_label
import schneaggchatv3mp.composeapp.generated.resources.event_max_users_hint
import schneaggchatv3mp.composeapp.generated.resources.event_max_users_info
import schneaggchatv3mp.composeapp.generated.resources.event_max_users_label
import schneaggchatv3mp.composeapp.generated.resources.event_no_location_selected
import schneaggchatv3mp.composeapp.generated.resources.event_starts_label
import schneaggchatv3mp.composeapp.generated.resources.event_title_label
import schneaggchatv3mp.composeapp.generated.resources.latlong
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.save
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

//Keep in sync with the server-side limit in ValidationUtils.validateEventMaxUsers (schneaggchatv3server)
private const val EVENT_MAX_USERS_LIMIT = 1000


// Popup for the event's creator - every field is editable, outside taps never dismiss it
// (an accidental tap must not silently discard in-progress edits).
@Composable
fun EventEditPopup(
    event: Event,
    onSave: (Event, ImageBitmap?, createGroup: Boolean) -> Unit,
    onDismiss: () -> Unit,
    friendsById: Map<String, User> = emptyMap(), // invitable users
    usersById: Map<String, UserIdentity> = emptyMap(),   // avatar/name lookup, includes the own user
    groups: List<Group> = emptyList(),
    eventGroupIds: Set<String> = emptySet(), // groups connected to an event, marked in the invite list
    onPickLocation: (Event) -> Unit = {},
    onDelete: (deleteGroup: Boolean, deleteEvent: Boolean) -> Unit = { _, _ -> },
    isSaving: Boolean = false,
    mapStyleUrl: String? = null,
    modifier: Modifier = Modifier
) {

    // Keyed on the incoming event so the draft is re-seeded whenever a different event (or the
    // same one carrying new data, e.g. the coordinates picked on the map) is delivered. An
    // unkeyed remember would keep editing the previous event's stale copy. Keying on event.id
    // alone would not work: a not-yet-saved event has id == "" both before and after the map trip.
    // Participations are left out of the key: the ViewModel tops up the creator's copy with the
    // live participations while this popup is open, and every response from another user (seen,
    // accepted, ...) would otherwise re-seed the draft from that pre-edit copy and wipe the edits.
    val draftKey = event.copy(participations = emptyList())
    var currentEvent by remember(draftKey) {
        mutableStateOf(event)
    }

    // Only a brand-new event's group gets created with this picture - an edit must not
    // overwrite a group picture the members may have set manually since.
    val isNewEvent = event.id == ""
    val typeIconBitmap = if (isNewEvent) rememberEventTypeIconBitmap(currentEvent.type) else null

    // Only shown while the event has no group yet. Defaults to on for a brand-new event (matches
    // the previous always-create-a-group behavior) and off for an existing event that was
    // detached from its group - re-adding one is then an explicit choice.
    var createGroup by remember(draftKey) { mutableStateOf(isNewEvent) }

    // Separate toggle state so we can flip "has end date" off without losing the picker's
    // last-set value if the user re-enables it (avoids re-deriving a default every toggle)
    var hasCloseDate by remember(draftKey) {
        mutableStateOf(event.closeDate != null)
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var visibilityDropdownExpanded by remember { mutableStateOf(false) }
    var groupDeleteDelayDropdownExpanded by remember { mutableStateOf(false) }
    var showInviteFriendsDialog by remember { mutableStateOf(false) }
    var inviteSearchTerm by remember { mutableStateOf("") }
    var showDeleteWarning by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf<EventTitleError?>(null) }
    var maxUsersError by remember { mutableStateOf(false) }

    // Separate toggle + raw text state so the field can hold an in-progress or invalid entry
    // without collapsing currentEvent.maxUsers to null on every keystroke.
    var limitParticipants by remember(draftKey) { mutableStateOf(event.maxUsers != null) }
    var maxUsersText by remember(draftKey) { mutableStateOf(event.maxUsers?.toString() ?: "") }

    // A too long title shows right away - an older event can already carry one. Empty is only
    // flagged by a save attempt, and every flagged error follows the title from then on.
    LaunchedEffect(currentEvent.title) {
        val error = validateEventTitle(currentEvent.title)
        titleError = error.takeIf { it == EventTitleError.TOO_LONG || titleError != null }
    }

    LaunchedEffect(maxUsersText, limitParticipants) {
        val enteredValue = maxUsersText.toIntOrNull()
        if (maxUsersError && (!limitParticipants || (enteredValue != null && enteredValue in 1..EVENT_MAX_USERS_LIMIT))) {
            maxUsersError = false
        }
    }

    // Same split as the participant limit: the toggle and the raw text survive an invalid or
    // half-typed amount, currentEvent.entryFeeCents always holds the parsed value (0 = free).
    val decimalSeparator = stringResource(Res.string.decimal_separator)
    var entryFeeError by remember { mutableStateOf(false) }
    var chargeEntryFee by remember(draftKey) { mutableStateOf(!event.isFree()) }
    var entryFeeText by remember(draftKey) {
        mutableStateOf(if (event.isFree()) "" else formatAmountNumber(event.entryFeeCents, decimalSeparator))
    }

    LaunchedEffect(currentEvent.entryFeeCents, chargeEntryFee) {
        if (entryFeeError && (!chargeEntryFee || isValidEntryFee(currentEvent.entryFeeCents))) {
            entryFeeError = false
        }
    }

    // A plain Dialog rather than a ModalBottomSheet: the sheet's anchored-drag machinery could
    // end up swallowing pointer-down events after the editor was rebuilt on returning from the
    // map's location picker, leaving it visible but inert - clicks and text-field focus dead
    // while dragging the sheet still worked.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clearFocusOnTap()
        ) {

            // Only an already-saved event can be deleted
            if (!isNewEvent) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { showDeleteWarning = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.delete_event),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Title
            OutlinedTextField(
                value = currentEvent.title,
                onValueChange = {
                    // Shortening is always allowed, so a title over the limit can be cut down
                    if (it.length <= EVENT_TITLE_MAX_LENGTH || it.length < currentEvent.title.length) {
                        currentEvent = currentEvent.copy(title = it)
                    }
                },
                label = {
                    Text(text = stringResource(Res.string.event_title_label))
                },
                isError = titleError != null,
                supportingText = {
                    Text(
                        text = when (titleError) {
                            EventTitleError.EMPTY -> stringResource(Res.string.error_event_title_must_not_be_empty)
                            EventTitleError.TOO_LONG -> stringResource(Res.string.error_event_title_too_long, EVENT_TITLE_MAX_LENGTH)
                            null -> "${currentEvent.title.length}/$EVENT_TITLE_MAX_LENGTH"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
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

            // Type, labeled and full width like the location below, so it isn't missed
            Text(
                text = stringResource(Res.string.event_type_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { typeDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = currentEvent.type.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(currentEvent.type.labelRes()),
                        modifier = Modifier.weight(1f)
                    )
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

            Spacer(modifier = Modifier.height(12.dp))

            // Location (optional)
            Text(
                text = stringResource(Res.string.event_location_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = {
                    onPickLocation(currentEvent)
                },
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
                    text = when {
                        location != null -> stringResource(Res.string.latlong, location.lat.toString().take(8), location.long.toString().take(8))
                        else -> stringResource(Res.string.event_no_location_selected)
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

            // Tap opens the location picker with the unsaved edits, like the button above
            currentEvent.location?.let { location ->
                if (mapStyleUrl != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    StaticMapPreview(
                        focus = listOf(location),
                        styleUrl = mapStyleUrl,
                        onClick = { onPickLocation(currentEvent) },
                        contentDescription = stringResource(Res.string.event_show_on_map),
                    ) {
                        // Same pin as on the Schneaggmap; only the creator edits, and they are going
                        EventMarkerLayer(
                            eventId = currentEvent.id,
                            position = location.toPosition(),
                            icon = currentEvent.type.icon(),
                            isJoined = true,
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
                    text = millisToStringWithWeekday(currentEvent.startDate),
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
                        text = millisToStringWithWeekday(currentEvent.closeDate!!),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            EventReminderSection(
                reminderMinutes = currentEvent.reminderMinutes,
                onChange = { currentEvent = currentEvent.copy(reminderMinutes = it) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Only offered while there is no group yet - an existing group can't be removed here,
            // only detached via the delete dialog.
            if (currentEvent.groupId == null) {
                SettingsSwitch(
                    modifier = Modifier.fillMaxWidth(),
                    titletext = stringResource(Res.string.event_create_group_label),
                    infotext = stringResource(Res.string.event_create_group_info),
                    switchchecked = createGroup,
                    onSwitchChange = { createGroup = it },
                    icon = null
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Group auto-delete delay (how long after start/end the connected group chat gets deleted)
            // - meaningless without a group, so hidden unless one exists or is about to be created.
            if (createGroup || currentEvent.groupId != null) {
                Text(
                    text = stringResource(Res.string.event_group_delete_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box {
                    OutlinedButton(
                        onClick = { groupDeleteDelayDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(currentEvent.groupDeleteDelay.labelRes()),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = groupDeleteDelayDropdownExpanded,
                        onDismissRequest = { groupDeleteDelayDropdownExpanded = false }
                    ) {
                        GroupDeleteDelay.entries.forEach { entry ->
                            val isSelected = entry == currentEvent.groupDeleteDelay
                            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(entry.labelRes()),
                                        color = tint,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    currentEvent = currentEvent.copy(groupDeleteDelay = entry)
                                    groupDeleteDelayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Max participants (optional cap on how many people can join)
            SettingsSwitch(
                modifier = Modifier.fillMaxWidth(),
                titletext = stringResource(Res.string.event_max_users_label),
                infotext = stringResource(Res.string.event_max_users_info),
                switchchecked = limitParticipants,
                onSwitchChange = { checked ->
                    limitParticipants = checked
                    currentEvent = currentEvent.copy(maxUsers = if (checked) maxUsersText.toIntOrNull() else null)
                },
                icon = null
            )

            if (limitParticipants) {
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = maxUsersText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(4)
                        maxUsersText = digits
                        currentEvent = currentEvent.copy(maxUsers = digits.toIntOrNull())
                    },
                    label = { Text(text = stringResource(Res.string.event_max_users_label)) },
                    placeholder = { Text(text = stringResource(Res.string.event_max_users_hint)) },
                    singleLine = true,
                    isError = maxUsersError,
                    supportingText = if (maxUsersError) {
                        { Text(text = stringResource(Res.string.error_event_max_users_invalid)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            EventEntryFeeSection(
                chargeEntryFee = chargeEntryFee,
                amountText = entryFeeText,
                isError = entryFeeError,
                onChargeEntryFeeChange = { checked ->
                    chargeEntryFee = checked
                    val enteredCents = parseAmountToCents(entryFeeText) ?: 0L
                    currentEvent = currentEvent.copy(entryFeeCents = if (checked) enteredCents else 0L)
                },
                onAmountTextChange = { input ->
                    if (isValidAmountInput(input)) {
                        entryFeeText = input
                        currentEvent = currentEvent.copy(entryFeeCents = parseAmountToCents(input) ?: 0L)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

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
                EventUserAvatarRow(
                    label = stringResource(Res.string.event_invited_users),
                    userIds = currentEvent.invitedUsers,
                    usersById = usersById
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Read-only: the creator sees how everyone responded, but never answers their own event
            EventParticipationOverview(
                event = event,
                usersById = usersById
            )

            HorizontalDivider(thickness = 2.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel / save row
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                NormalButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                    primary = false
                )

                Spacer(modifier = Modifier.weight(1f))

                NormalButton(
                    text = stringResource(Res.string.save),
                    onClick = {
                        val enteredMaxUsers = maxUsersText.toIntOrNull()
                        val maxUsersInvalid = limitParticipants && (enteredMaxUsers == null || enteredMaxUsers !in 1..EVENT_MAX_USERS_LIMIT)

                        val entryFeeInvalid = chargeEntryFee && !isValidEntryFee(currentEvent.entryFeeCents)

                        titleError = validateEventTitle(currentEvent.title)
                        maxUsersError = maxUsersInvalid
                        entryFeeError = entryFeeInvalid

                        if (titleError == null && !maxUsersInvalid && !entryFeeInvalid) {
                            onSave(currentEvent, typeIconBitmap, createGroup)
                        }
                    },
                    isLoading = isSaving,
                    primary = false,
                    showOutline = true
                )
            }
        }
    }

    if (showDeleteWarning) {
        EventDeleteDialog(
            hasGroup = currentEvent.groupId != null,
            onDismiss = { showDeleteWarning = false },
            onConfirm = { deleteGroup, deleteEvent -> onDelete(deleteGroup, deleteEvent) }
        )
    }

    // Start date picker
    if (showStartDatePicker) {
        DateTimePickerPopup(
            initialDateTime = Instant.fromEpochMilliseconds(currentEvent.startDate)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
            minDateTime = (Clock.System.now() + 5.minutes).toLocalDateTime(TimeZone.currentSystemDefault()),
            onConfirm = { pickedStart ->
                val newStartMillis = pickedStart.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                val newCloseDate = currentEvent.closeDate?.let { close ->
                    if (close <= newStartMillis) newStartMillis + 60 * 60 * 1000L else close
                }
                currentEvent = currentEvent.copy(
                    startDate = newStartMillis,
                    closeDate = newCloseDate
                )
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
        )
    }

    // End date picker
    if (showEndDatePicker && currentEvent.closeDate != null) {
        DateTimePickerPopup(
            initialDateTime = Instant.fromEpochMilliseconds(currentEvent.closeDate!!)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
            minDateTime = Instant.fromEpochMilliseconds(currentEvent.startDate)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
            onConfirm = { pickedEnd ->
                currentEvent = currentEvent.copy(
                    closeDate = pickedEnd.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                )
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
        )
    }

    // Invite friends dialog
    if (showInviteFriendsDialog) {
        val currentUserId = event.creatorId

        // Filter out groups that contain only the current user as a member
        val filteredGroups = remember(groups, currentUserId) {
            groups.filter { group ->
                val memberIds = group.members.map { it.userId }
                !(memberIds.size == 1 && memberIds.first() == currentUserId)
            }
        }

        val availableUsers = remember(filteredGroups, eventGroupIds, friendsById, inviteSearchTerm) {
            (filteredGroups.map { it.toChatListItem(hasEvent = it.id in eventGroupIds) } + friendsById.values.map { it.toChatListItem() })
                .filter { it.displayName.contains(inviteSearchTerm, ignoreCase = true) }
        }
        val selectedUsers = remember(currentEvent.invitedUsers, friendsById) {
            currentEvent.invitedUsers.mapNotNull { userId -> friendsById[userId]?.toChatListItem() }
        }

        // Members of a group that are actually invitable (i.e. also a friend)
        fun groupMemberIds(groupId: String): Set<String> =
            groups.find { it.id == groupId }?.members?.map { it.userId }?.toSet() ?: emptySet()

        Dialog(
            onDismissRequest = { showInviteFriendsDialog = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 6.dp
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
                                currentEvent = currentEvent.copy(
                                    invitedUsers = (currentEvent.invitedUsers + friendIds).toSet().toList()
                                )
                            } else {
                                currentEvent = currentEvent.copy(
                                    invitedUsers = currentEvent.invitedUsers + user.id
                                )
                            }
                        },
                        onUserDeselected = { user ->
                            if (user.isGroup) {
                                val memberIds = groupMemberIds(user.id)
                                currentEvent = currentEvent.copy(
                                    invitedUsers = currentEvent.invitedUsers.filterNot { it in memberIds }
                                )
                            } else {
                                currentEvent = currentEvent.copy(
                                    invitedUsers = currentEvent.invitedUsers - user.id
                                )
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
                groupId = null,
                location = null,
                startDate = Clock.System.now().toEpochMilliseconds(),
                closeDate = null,
                invitedUsers = emptyList(),
                visibility = EventVisibility.PUBLIC,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                updatedBy = "awdawd",
            ),
            onSave = { _, _, _ -> },
            onDismiss = { }
        )
    }
}

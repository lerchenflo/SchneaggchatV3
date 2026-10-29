@file:OptIn(ExperimentalMaterial3Api::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.utilities.copyToClipboard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeDefinition
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.drawableRes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.getValueByKey
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.sortedTypes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.stringRes
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.toSimpleLocationData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.withValueForKey
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.copy
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.latlong
import schneaggchatv3mp.composeapp.generated.resources.location_add_type
import schneaggchatv3mp.composeapp.generated.resources.open_location_in_maps
import schneaggchatv3mp.composeapp.generated.resources.save
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_attribute_required
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_delete
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_description
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_edit
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_last_changed_by
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_last_changed_label
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_new
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_title
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_type_required

@Composable
fun MapEntryInfoCard(
    entry: MapEntry,
    onDismiss: () -> Unit,
    onSave: (MapEntry) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    // Saves a change made on the read-only card (e.g. a date's Now button) and keeps the card open
    onQuickSave: (MapEntry) -> Unit = {},
) {

    // Keyed on entry so the card shows the saved state after a quick save hands back the updated entry
    var currentEntry by remember(entry) {
        mutableStateOf(entry)
    }

    //New entries have no id yet and open straight in the form, existing ones as a read-only card first
    val isNewEntry = entry.id.isEmpty()
    var isEditing by remember { mutableStateOf(isNewEntry) }

    var showDeleteConfirmationPopup by remember { mutableStateOf(false) }

    // Set by the first save attempt with a missing name or type: errors only show up after trying
    var showValidationErrors by remember { mutableStateOf(false) }

    // A plain Dialog rather than a ModalBottomSheet: the sheet's anchored-drag machinery could end
    // up swallowing pointer-down events after the card was rebuilt on a fast navigation, leaving it
    // visible but inert - clicks and text-field focus dead. Outside taps don't dismiss either, so
    // an accidental tap can't silently discard in-progress edits.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .imePadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(start = 24.dp, top = 16.dp, end = 12.dp, bottom = 20.dp)
        ) {

            EntryHeader(
                entry = currentEntry,
                isEditing = isEditing,
                isNewEntry = isNewEntry,
                onEditClick = { isEditing = true },
                onCloseClick = onDismiss,
            )

            Spacer(modifier = Modifier.height(8.dp))

            //Scrollable column to be able to hoist multiple attributevalues
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(end = 12.dp)
            ) {
                if (isEditing) {
                    EntryTitleView(
                        entry = currentEntry,
                        onChange = {
                            currentEntry = it
                        },
                        showNameError = showValidationErrors,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LocationAttributeView(
                        entry = currentEntry,
                        onChange = {
                            currentEntry = it
                        },
                        showTypeError = showValidationErrors,
                    )
                } else {
                    EntryDetailView(
                        entry = currentEntry,
                        onQuickChange = onQuickSave
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(4.dp))

                EntryMetaView(
                    entry = currentEntry,
                    showLastChanged = !isNewEntry
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                //Primary action on the trailing side, cancel next to it
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp)
                ) {
                    NormalButton(
                        text = stringResource(Res.string.cancel),
                        onClick = {
                            if (isNewEntry) {
                                onDismiss()
                            } else {
                                //Throw away the edits and go back to the read-only card
                                currentEntry = entry
                                isEditing = false
                            }
                        },
                        primary = false
                    )

                    NormalButton(
                        text = stringResource(Res.string.save),
                        onClick = {
                            //The ViewModel drops entries without a name or a type - show what is missing instead
                            if (currentEntry.name.isBlank() || currentEntry.locationData.isEmpty()) {
                                showValidationErrors = true
                            } else {
                                onSave(currentEntry)
                            }
                        },
                        disabled = currentEntry == entry
                    )
                }
            } else {
                //Delete far away from the main action on the leading side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp)
                ) {
                    IconButton(
                        onClick = { showDeleteConfirmationPopup = true }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val shareUtils = koinInject<ShareUtils>()
                    NormalButton(
                        text = stringResource(Res.string.open_location_in_maps),
                        onClick = {
                            shareUtils.openLocationInMaps(
                                currentEntry.coordinates.lat,
                                currentEntry.coordinates.long,
                                currentEntry.name
                            )
                        }
                    )
                }
            }

        }
    }

    //Kept outside the Dialog above - a dialog nested inside another one isn't reliable on all targets
    if (showDeleteConfirmationPopup) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationPopup = false },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(entry.id)
                    showDeleteConfirmationPopup = false

                }) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmationPopup = false
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            text = {
                Text(
                    text = stringResource(Res.string.schneaggmap_entry_delete)
                )
            }
        )
    }
}


/**
 * Top row of the card: the entry's types as chips (or the form title while editing), then the
 * edit and close buttons. Editing has its own cancel / save row, so it only shows the title.
 */
@Composable
private fun EntryHeader(
    entry: MapEntry,
    isEditing: Boolean,
    isNewEntry: Boolean,
    onEditClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isEditing) {
            Text(
                text = stringResource(if (isNewEntry) Res.string.schneaggmap_entry_new else Res.string.schneaggmap_entry_edit),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                entry.locationData.forEach { locationData ->
                    LocationTypeChip(locationData)
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Non-interactive label for one of the entry's types, with the same icon as its map marker. */
@Composable
private fun LocationTypeChip(locationData: LocationData) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, top = 6.dp, end = 12.dp, bottom = 6.dp)
        ) {
            Image(
                painter = painterResource(locationData.locationtype.drawableRes()),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = stringResource(locationData.locationtype.stringRes()),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Read-only body of the card: name, description and every attribute that has a value. Date rows
 * stay visible while empty, their Now button sets them in one tap via [onQuickChange].
 */
@Composable
private fun EntryDetailView(entry: MapEntry, onQuickChange: (MapEntry) -> Unit) {
    Column {
        if (entry.name.isNotBlank()) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (entry.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        //TODO: Small row with ratings

        entry.locationData.forEach { locationData ->
            val shownAttributes = locationData.schema().mapNotNull { definition ->
                val value = locationData.getValueByKey(definition.key)
                if (value != null || definition is AttributeDefinition.DateTimeDef) definition to value else null
            }

            if (shownAttributes.isEmpty()) return@forEach

            Spacer(modifier = Modifier.height(16.dp))

            //The header chips already name the type, a section title is only needed to tell several apart
            if (entry.locationData.size > 1) {
                Text(
                    text = stringResource(locationData.locationtype.stringRes()),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            shownAttributes.forEach { (definition, value) ->
                KeyValueReadOnlyView(
                    value = value,
                    definition = definition,
                    onValueChange = { newValue ->
                        onQuickChange(entry.copy(
                            locationData = entry.locationData.map {
                                if (it === locationData) locationData.withValueForKey(definition.key, newValue)
                                else it
                            }
                        ))
                    }
                )
            }
        }
    }
}

/** Coordinates with a copy button, and who changed the entry last. */
@Composable
private fun EntryMetaView(entry: MapEntry, showLastChanged: Boolean) {
    val lat = entry.coordinates.lat.toString().take(8)
    val long = entry.coordinates.long.toString().take(8)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(Res.string.latlong, lat, long),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { copyToClipboard(text = "$lat, $long") }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(Res.string.copy),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showLastChanged) {
            //The editor name is resolved server side, but old cached entries may not have one yet
            val lastChangedText = if (entry.updatedByName.isNotBlank()) {
                stringResource(
                    Res.string.schneaggmap_entry_last_changed_by,
                    entry.updatedByName,
                    millisToTimeDateOrYesterday(entry.updatedAt)
                )
            } else {
                stringResource(Res.string.schneaggmap_entry_last_changed_label) + ": " +
                        millisToTimeDateOrYesterday(entry.updatedAt)
            }

            Text(
                text = lastChangedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/** [showNameError]: a save was tried, so a blank name is marked as missing. */
@Composable
fun EntryTitleView(entry: MapEntry, onChange: (MapEntry) -> Unit, showNameError: Boolean = false) {

    Column {
        //The error goes away the moment a name is typed
        val nameError = showNameError && entry.name.isBlank()

        //Title
        OutlinedTextField(
            value = entry.name,
            onValueChange = {
                onChange(
                    entry.copy(name = it)
                )
            },
            label = {
                Text(
                    text = stringResource(Res.string.schneaggmap_entry_title) + " *"
                )
            },
            isError = nameError,
            supportingText = if (nameError) {
                { Text(stringResource(Res.string.schneaggmap_attribute_required)) }
            } else null,
            trailingIcon = if (nameError) {
                { Icon(imageVector = Icons.Default.Error, contentDescription = null) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = entry.description,
            onValueChange = {
                onChange(
                    entry.copy(description = it)
                )
            },
            label = {
                Text(
                    text = stringResource(Res.string.schneaggmap_entry_description)
                )
            },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CoordinateView(
    coordinates: LatLong,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (trailing == null) {
            Text(
                text = stringResource(Res.string.latlong, coordinates.lat.toString().take(8), coordinates.long.toString().take(8)),
                modifier = Modifier.clickable {
                    copyToClipboard(text = coordinates.lat.toString().take(8) + ", " + coordinates.long.toString().take(8))
                }
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.latlong, coordinates.lat.toString().take(8), coordinates.long.toString().take(8)),
                    modifier = Modifier.clickable {
                        copyToClipboard(text = coordinates.lat.toString().take(8) + ", " + coordinates.long.toString().take(8))
                    }
                )
                trailing()
            }
        }
    }

    /*

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {


        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.longitude),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = coordinates.long.toString().take(8),
                onValueChange = {},
                enabled = false,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(2f)
            )
        }
    }

     */
}

/** [showTypeError]: a save was tried, so an entry without a type shows why saving doesn't work. */
@Composable
fun LocationAttributeView(entry: MapEntry, onChange: (MapEntry) -> Unit, showTypeError: Boolean = false) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entry.locationData.forEach { locationData ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 4.dp, bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Image(
                            painter = painterResource(locationData.locationtype.drawableRes()),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = stringResource(locationData.locationtype.stringRes()),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp)
                        )

                        //Only allow deletion if the entry has more than one locationdata
                        if (entry.locationData.size > 1) {
                            IconButton(
                                onClick = {
                                    onChange(entry.copy(
                                        locationData = entry.locationData.filter {
                                            it.locationtype != locationData.locationtype
                                        }
                                    ))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    locationData.schema().forEach { definition ->
                        val value = locationData.getValueByKey(definition.key)

                        KeyValueView(
                            value = value,
                            definition = definition,
                            onValueChange = { newValue ->
                                onChange(entry.copy(
                                    locationData = entry.locationData.map {
                                        if (it === locationData) locationData.withValueForKey(definition.key, newValue)
                                        else it
                                    }
                                ))
                            },
                        )
                    }
                }
            }
        }
    }




    var showLocationAddDropdown by remember { mutableStateOf(false) }
    var expandedAddGroups by remember { mutableStateOf(emptySet<LocationGroup>()) }

    //Box for alignment of popup
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        NormalButton(
            text = stringResource(Res.string.location_add_type),
            primary = false,
            onClick = {
                expandedAddGroups = emptySet()
                showLocationAddDropdown = true
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = showLocationAddDropdown,
            onDismissRequest = {
                showLocationAddDropdown = false
            },
            modifier = Modifier.align(Alignment.Center)
        ) {
            LocationGroup.entries.forEach { group ->
                val expanded = group in expandedAddGroups

                //Expanded groups get a tinted card, a highlighted header and an accent line next to their types
                val sectionBackground by animateColorAsState(
                    if (expanded) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
                )
                val headerColor by animateColorAsState(
                    if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                val accentColor = MaterialTheme.colorScheme.primary

                Column(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(sectionBackground)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(group.stringRes()),
                                fontWeight = if (expanded) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        trailingIcon = {
                            val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.rotate(arrowRotation)
                            )
                        },
                        onClick = {
                            expandedAddGroups = if (expanded) expandedAddGroups - group else expandedAddGroups + group
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = headerColor,
                            trailingIconColor = headerColor,
                        ),
                    )

                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .drawBehind {
                                    val lineWidth = 3.dp.toPx()
                                    drawRoundRect(
                                        color = accentColor,
                                        topLeft = Offset(16.dp.toPx(), 0f),
                                        size = Size(lineWidth, size.height),
                                        cornerRadius = CornerRadius(lineWidth / 2),
                                    )
                                }
                        ) {
                            group.sortedTypes().forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(type.stringRes())
                                        )
                                    },
                                    onClick = {
                                        onChange(entry.copy(
                                            locationData = entry.locationData + type.toSimpleLocationData()
                                        ))
                                        showLocationAddDropdown = false
                                    },
                                    modifier = Modifier.padding(start = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTypeError && entry.locationData.isEmpty()) {
        ValidationErrorText(
            text = stringResource(Res.string.schneaggmap_entry_type_required),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }

}

/** Error line under a form element, with an icon so the error isn't shown by color alone. */
@Composable
private fun ValidationErrorText(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

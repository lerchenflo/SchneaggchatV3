package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.CoordinateView
import org.lerchenflo.schneaggchatv3mp.sharedUi.pickers.DateTimePickerPopup
import org.lerchenflo.schneaggchatv3mp.utilities.millisToLocalDateTime
import org.lerchenflo.schneaggchatv3mp.utilities.nowToTheMinute
import org.lerchenflo.schneaggchatv3mp.utilities.toEpochMillis
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.route_arrival_mode
import schneaggchatv3mp.composeapp.generated.resources.route_edit_point
import schneaggchatv3mp.composeapp.generated.resources.route_kind_label
import schneaggchatv3mp.composeapp.generated.resources.route_linked_place
import schneaggchatv3mp.composeapp.generated.resources.route_new_point
import schneaggchatv3mp.composeapp.generated.resources.route_point_description
import schneaggchatv3mp.composeapp.generated.resources.route_point_move
import schneaggchatv3mp.composeapp.generated.resources.route_point_name
import schneaggchatv3mp.composeapp.generated.resources.route_continue_mode
import schneaggchatv3mp.composeapp.generated.resources.route_main_route
import schneaggchatv3mp.composeapp.generated.resources.route_part_label
import schneaggchatv3mp.composeapp.generated.resources.route_path_start
import schneaggchatv3mp.composeapp.generated.resources.route_point_name_optional
import schneaggchatv3mp.composeapp.generated.resources.route_point_name_required
import schneaggchatv3mp.composeapp.generated.resources.route_point_time
import schneaggchatv3mp.composeapp.generated.resources.route_point_time_clear
import schneaggchatv3mp.composeapp.generated.resources.save

/**
 * Add or edit one route point: type, name, description, planned time, position. A new point
 * shows where it goes into the route. Delete only for the point's creator, the extension's
 * creator or a group admin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePointSheet(
    draft: RoutePointDraft,
    paths: List<RoutePathUi>,
    onAction: (RouteAction) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { onAction(RouteAction.OnDraftDismiss) },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded) // no half-open state
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(if (draft.itemId == null) Res.string.route_new_point else Res.string.route_edit_point),
                style = MaterialTheme.typography.titleLarge,
            )

            KindPicker(selected = draft.kind, onSelect = { onAction(RouteAction.OnDraftKindChange(it)) })

            val nameOptional = draft.kind == RoutePointKind.WAYPOINT
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onAction(RouteAction.OnDraftNameChange(it)) },
                label = {
                    Text(stringResource(if (nameOptional) Res.string.route_point_name_optional else Res.string.route_point_name))
                },
                isError = draft.showNameError,
                supportingText = if (draft.showNameError) {
                    { Text(stringResource(Res.string.route_point_name_required)) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = draft.description,
                onValueChange = { onAction(RouteAction.OnDraftDescriptionChange(it)) },
                label = { Text(stringResource(Res.string.route_point_description)) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            // Paths whose start and rejoin point exist can take points
            val openPaths = paths.filter { !it.isBroken }
            if (openPaths.isNotEmpty()) {
                RoutePartPicker(
                    selectedPathId = draft.pathId,
                    paths = openPaths,
                    onSelect = { onAction(RouteAction.OnDraftPathChange(it)) },
                )
            }

            if (!draft.isFirstPoint) {
                RouteTransportModePicker(
                    label = stringResource(Res.string.route_arrival_mode),
                    selected = draft.transportMode,
                    onSelect = { onAction(RouteAction.OnDraftTransportModeChange(it)) },
                )
            }
            if (draft.kind == RoutePointKind.TRANSFER) {
                RouteTransportModePicker(
                    label = stringResource(Res.string.route_continue_mode),
                    selected = draft.continueTransportMode,
                    onSelect = { onAction(RouteAction.OnDraftContinueModeChange(it)) },
                )
            }

            PlannedTimeRow(
                plannedAt = draft.plannedAt,
                onPickClick = { showTimePicker = true },
                onClear = { onAction(RouteAction.OnDraftPlannedAtChange(null)) },
            )

            CoordinateView(
                coordinates = draft.coordinates,
                trailing = {
                    TextButton(onClick = { onAction(RouteAction.OnMovePointClick) }) {
                        Icon(imageVector = Icons.Default.OpenWith, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.route_point_move))
                    }
                }
            )

            draft.insertHint?.let { hint ->
                Text(
                    text = insertHintText(hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (draft.mapEntryId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.route_linked_place),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Every saved point can start an alternative path, also one on a path (nested)
            if (draft.itemId != null) {
                OutlinedButton(onClick = { onAction(RouteAction.OnDraftStartPathClick) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.route_path_start))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (draft.itemId != null && draft.canDelete) {
                    TextButton(onClick = { onAction(RouteAction.OnDraftDelete) }) {
                        Text(text = stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onAction(RouteAction.OnDraftDismiss) }) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(enabled = !draft.isSaving, onClick = { onAction(RouteAction.OnDraftSave) }) {
                    Text(stringResource(Res.string.save))
                }
            }
        }
    }

    if (showTimePicker) {
        DateTimePickerPopup(
            initialDateTime = draft.plannedAt?.let(::millisToLocalDateTime) ?: nowToTheMinute(),
            onConfirm = { picked ->
                showTimePicker = false
                onAction(RouteAction.OnDraftPlannedAtChange(picked.toEpochMillis()))
            },
            onDismiss = { showTimePicker = false },
            title = stringResource(Res.string.route_point_time),
        )
    }
}

/** Labeled, full width type button with a dropdown of the four kinds and what each is for. */
@Composable
private fun KindPicker(
    selected: RoutePointKind,
    onSelect: (RoutePointKind) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(Res.string.route_kind_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(4.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                RoutePointBadge(kind = selected, stopNumber = null, size = 22.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = stringResource(selected.labelRes()), modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                RoutePointKind.entries.forEach { kind ->
                    DropdownMenuItem(
                        leadingIcon = { RoutePointBadge(kind = kind, stopNumber = null, size = 24.dp) },
                        text = {
                            Column {
                                Text(stringResource(kind.labelRes()))
                                Text(
                                    text = stringResource(kind.useCaseRes()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelect(kind)
                        }
                    )
                }
            }
        }
    }
}

/** "Belongs to": the main route or one of the alternative paths. */
@Composable
private fun RoutePartPicker(
    selectedPathId: String?,
    paths: List<RoutePathUi>,
    onSelect: (pathItemId: String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val mainRoute = stringResource(Res.string.route_main_route)

    Column {
        Text(
            text = stringResource(Res.string.route_part_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(4.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                val selected = paths.find { it.itemId == selectedPathId }
                Icon(
                    imageVector = if (selected == null) Icons.Default.Route else Icons.AutoMirrored.Filled.AltRoute,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = selected?.name ?: mainRoute, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    leadingIcon = { Icon(imageVector = Icons.Default.Route, contentDescription = null) },
                    text = { Text(mainRoute) },
                    onClick = {
                        expanded = false
                        onSelect(null)
                    }
                )
                paths.forEach { path ->
                    DropdownMenuItem(
                        leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.AltRoute, contentDescription = null) },
                        text = { Text(path.name) },
                        onClick = {
                            expanded = false
                            onSelect(path.itemId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannedTimeRow(
    plannedAt: Long?,
    onPickClick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onPickClick, modifier = Modifier.weight(1f)) {
            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = plannedAt?.let { formatPlannedDateTime(it) } ?: stringResource(Res.string.route_point_time),
                modifier = Modifier.weight(1f)
            )
        }
        if (plannedAt != null) {
            IconButton(onClick = onClear) {
                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(Res.string.route_point_time_clear))
            }
        }
    }
}

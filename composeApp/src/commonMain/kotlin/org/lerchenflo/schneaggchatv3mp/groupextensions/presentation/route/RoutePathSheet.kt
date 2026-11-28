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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.route_loop_hint
import schneaggchatv3mp.composeapp.generated.resources.route_loop_make_side_trip
import schneaggchatv3mp.composeapp.generated.resources.route_loop_new_point
import schneaggchatv3mp.composeapp.generated.resources.route_loop_text
import schneaggchatv3mp.composeapp.generated.resources.route_loop_title
import schneaggchatv3mp.composeapp.generated.resources.route_path_back_to_start
import schneaggchatv3mp.composeapp.generated.resources.route_path_delete_text
import schneaggchatv3mp.composeapp.generated.resources.route_path_delete_title
import schneaggchatv3mp.composeapp.generated.resources.route_path_edit
import schneaggchatv3mp.composeapp.generated.resources.route_path_from
import schneaggchatv3mp.composeapp.generated.resources.route_path_new
import schneaggchatv3mp.composeapp.generated.resources.route_path_to
import schneaggchatv3mp.composeapp.generated.resources.route_point_delete_paths_text
import schneaggchatv3mp.composeapp.generated.resources.route_point_delete_paths_title
import schneaggchatv3mp.composeapp.generated.resources.route_point_description
import schneaggchatv3mp.composeapp.generated.resources.route_point_name
import schneaggchatv3mp.composeapp.generated.resources.route_point_name_required
import schneaggchatv3mp.composeapp.generated.resources.route_transport_default
import schneaggchatv3mp.composeapp.generated.resources.save

/**
 * Add or edit an alternative path: name, description, mode, where it leaves its part (the main
 * route, or the path it branches off) and where it rejoins that part (a later point, or its start
 * for a side trip). Every member may edit it; delete
 * only for its creator, the extension's creator or a group admin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePathSheet(
    draft: RoutePathDraft,
    /** The points of the part the path leaves from. */
    partPoints: List<RoutePointUi>,
    onAction: (RouteAction) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(RouteAction.OnPathDraftDismiss) },
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
                text = stringResource(if (draft.itemId == null) Res.string.route_path_new else Res.string.route_path_edit),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = draft.name,
                onValueChange = { onAction(RouteAction.OnPathDraftNameChange(it)) },
                label = { Text(stringResource(Res.string.route_point_name)) },
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
                onValueChange = { onAction(RouteAction.OnPathDraftDescriptionChange(it)) },
                label = { Text(stringResource(Res.string.route_point_description)) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            RouteTransportModePicker(
                label = stringResource(Res.string.route_transport_default),
                selected = draft.mode,
                onSelect = { onAction(RouteAction.OnPathDraftModeChange(it)) },
            )

            val fromIndex = partPoints.indexOfFirst { it.itemId == draft.fromItemId }
            if (draft.movePointIds.isNotEmpty()) {
                // Closing a loop: start and end are fixed
                Text(
                    text = stringResource(Res.string.route_loop_hint, partPoints.getOrNull(fromIndex)?.name.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RoutePointPicker(
                    label = stringResource(Res.string.route_path_from),
                    selected = partPoints.getOrNull(fromIndex),
                    options = partPoints,
                    onSelect = { onAction(RouteAction.OnPathDraftFromChange(it)) },
                )
                RoutePointPicker(
                    label = stringResource(Res.string.route_path_to),
                    selected = partPoints.find { it.itemId == draft.toItemId }.takeIf { draft.toItemId != draft.fromItemId },
                    // Later points, or back to the start (a side trip, user 2026-11-26)
                    options = partPoints.drop(fromIndex + 1),
                    sideTripItemId = draft.fromItemId,
                    onSelect = { onAction(RouteAction.OnPathDraftToChange(it)) },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (draft.itemId != null && draft.canDelete) {
                    TextButton(onClick = { onAction(RouteAction.OnPathDraftDelete) }) {
                        Text(text = stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onAction(RouteAction.OnPathDraftDismiss) }) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(enabled = !draft.isSaving, onClick = { onAction(RouteAction.OnPathDraftSave) }) {
                    Text(stringResource(Res.string.save))
                }
            }
        }
    }
}

/**
 * Labeled, full width button with a dropdown of route points. With [sideTripItemId] the first
 * entry is "back to the start"; [selected] null then shows it as picked.
 */
@Composable
private fun RoutePointPicker(
    label: String,
    selected: RoutePointUi?,
    options: List<RoutePointUi>,
    onSelect: (itemId: String) -> Unit,
    sideTripItemId: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val backToStart = stringResource(Res.string.route_path_back_to_start)

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(4.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                if (selected != null) {
                    RoutePointBadge(kind = selected.kind, stopNumber = selected.stopNumber, size = 22.dp)
                } else {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = selected?.name ?: backToStart,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                sideTripItemId?.let { startId ->
                    DropdownMenuItem(
                        leadingIcon = { Icon(imageVector = Icons.Default.Replay, contentDescription = null) },
                        text = { Text(backToStart) },
                        onClick = {
                            expanded = false
                            onSelect(startId)
                        }
                    )
                }
                options.forEach { point ->
                    DropdownMenuItem(
                        leadingIcon = { RoutePointBadge(kind = point.kind, stopNumber = point.stopNumber, size = 24.dp) },
                        text = { Text(text = point.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            expanded = false
                            onSelect(point.itemId)
                        }
                    )
                }
            }
        }
    }
}

/** Asks before a delete that takes alternative paths and their points with it. */
@Composable
fun RouteDeleteConfirmDialog(
    confirm: RouteDeleteConfirm,
    onAction: (RouteAction) -> Unit,
) {
    val (title, text) = when (confirm) {
        is RouteDeleteConfirm.Path ->
            stringResource(Res.string.route_path_delete_title) to stringResource(Res.string.route_path_delete_text, confirm.name)
        is RouteDeleteConfirm.PointWithPaths ->
            stringResource(Res.string.route_point_delete_paths_title) to
                stringResource(Res.string.route_point_delete_paths_text, confirm.pathNames.joinToString(", "))
    }
    AlertDialog(
        onDismissRequest = { onAction(RouteAction.OnDeleteDismiss) },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onAction(RouteAction.OnDeleteConfirm) }) {
                Text(text = stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(RouteAction.OnDeleteDismiss) }) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/** A long press on an earlier point: make the way since then a side trip, or just add a point there. */
@Composable
fun RouteLoopDialog(
    offer: RouteLoopOffer,
    onAction: (RouteAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(RouteAction.OnLoopOfferDismiss) },
        title = { Text(stringResource(Res.string.route_loop_title)) },
        text = { Text(stringResource(Res.string.route_loop_text, offer.pointName)) },
        confirmButton = {
            TextButton(onClick = { onAction(RouteAction.OnLoopMakeSideTripClick) }) {
                Text(stringResource(Res.string.route_loop_make_side_trip))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(RouteAction.OnLoopNewPointClick) }) {
                Text(stringResource(Res.string.route_loop_new_point))
            }
        }
    )
}

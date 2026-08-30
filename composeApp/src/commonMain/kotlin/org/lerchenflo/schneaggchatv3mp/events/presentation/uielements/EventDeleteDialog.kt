package org.lerchenflo.schneaggchatv3mp.events.presentation.uielements

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.delete_event
import schneaggchatv3mp.composeapp.generated.resources.event_delete_confirm_no_group_message
import schneaggchatv3mp.composeapp.generated.resources.event_delete_detach_only
import schneaggchatv3mp.composeapp.generated.resources.event_delete_group_only
import schneaggchatv3mp.composeapp.generated.resources.event_delete_only
import schneaggchatv3mp.composeapp.generated.resources.event_delete_warning_message
import schneaggchatv3mp.composeapp.generated.resources.event_delete_with_group

/**
 * Shared delete/detach dialog for an event, used both from the event editor and from a group's
 * chat details. Breaking the event <-> group link is non-destructive by default: [onConfirm] is
 * called with (deleteGroup, deleteEvent), and both false means "unlink only" - the event and the
 * group both survive.
 *
 * When [hasGroup] is false there is no link to preserve, so the choice collapses to a single
 * delete confirmation for the event.
 */
@Composable
fun EventDeleteDialog(
    hasGroup: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (deleteGroup: Boolean, deleteEvent: Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.delete_event)) },
        text = {
            Text(
                text = stringResource(
                    if (hasGroup) Res.string.event_delete_warning_message
                    else Res.string.event_delete_confirm_no_group_message
                )
            )
        },
        confirmButton = {
            Column {
                if (hasGroup) {
                    TextButton(onClick = { onDismiss(); onConfirm(false, false) }) {
                        Text(text = stringResource(Res.string.event_delete_detach_only))
                    }
                    TextButton(onClick = { onDismiss(); onConfirm(false, true) }) {
                        Text(text = stringResource(Res.string.event_delete_only))
                    }
                    TextButton(onClick = { onDismiss(); onConfirm(true, false) }) {
                        Text(text = stringResource(Res.string.event_delete_group_only))
                    }
                    TextButton(onClick = { onDismiss(); onConfirm(true, true) }) {
                        Text(text = stringResource(Res.string.event_delete_with_group))
                    }
                } else {
                    TextButton(onClick = { onDismiss(); onConfirm(false, true) }) {
                        Text(text = stringResource(Res.string.event_delete_only))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    )
}

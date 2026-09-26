package org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.DEFAULT_QUICK_REACTIONS
import org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements.SettingsOption
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.NormalButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.popups.ReactionInputDialog
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.add_quick_reaction
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.quick_reactions
import schneaggchatv3mp.composeapp.generated.resources.quick_reactions_empty
import schneaggchatv3mp.composeapp.generated.resources.quick_reactions_info
import schneaggchatv3mp.composeapp.generated.resources.quick_reactions_reorder
import schneaggchatv3mp.composeapp.generated.resources.restore_defaults
import schneaggchatv3mp.composeapp.generated.resources.save
import sh.calvin.reorderable.ReorderableColumn

/**
 * Settings row for the quick reactions offered on a long press of a message. The row itself only
 * previews the current list - editing happens in [QuickReactionsDialog], so a long list can't push
 * the rest of the appearance settings around.
 */
@Composable
fun QuickReactionsSetting(
    reactions: List<String>,
    onSave: (List<String>) -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsOption(
        icon = Icons.Default.AddReaction,
        text = stringResource(Res.string.quick_reactions),
        subtext = reactions.joinToString(" ").ifEmpty { stringResource(Res.string.quick_reactions_empty) },
        onClick = { showDialog = true },
        modifier = modifier
    )

    if (showDialog) {
        QuickReactionsDialog(
            reactions = reactions,
            onSave = onSave,
            onRestoreDefaults = onRestoreDefaults,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Add, delete and drag-to-reorder the quick reactions. Edits only touch a local draft - nothing is
 * stored or synced until save, so a reorder doesn't fire a request per drag. Cancelling discards.
 * Removing everything is allowed; the custom reaction button in the message popup stays either way.
 */
@Composable
private fun QuickReactionsDialog(
    reactions: List<String>,
    onSave: (List<String>) -> Unit,
    onRestoreDefaults: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Seeded once when the dialog opens. If another device changes the list while it is open,
    // saving here wins - same "last write wins" the other synced settings have.
    val draft = remember { reactions.toMutableStateList() }

    // Saving the defaults has to clear the stored list back to null rather than write a copy of it,
    // otherwise the user stops following the defaults when they change in a later version. Any edit
    // made after pressing restore turns it back into an ordinary list save.
    var restoreDefaults by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.quick_reactions)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.quick_reactions_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.size(12.dp))

                if (draft.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.quick_reactions_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    // Same reorder setup as the poll option list in CreatePollPopup
                    ReorderableColumn(
                        list = draft,
                        onSettle = { from, to ->
                            draft.add(to, draft.removeAt(from))
                            restoreDefaults = false
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        },
                    ) { _, reaction, _ ->
                        ReorderableItem(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reaction,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        draft.remove(reaction)
                                        restoreDefaults = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(Res.string.delete),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                        },
                                        onDragStopped = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                        },
                                    ),
                                    onClick = {},
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DragHandle,
                                        contentDescription = stringResource(Res.string.quick_reactions_reorder),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NormalButton(
                        text = stringResource(Res.string.add_quick_reaction),
                        onClick = { showAddDialog = true },
                        primary = false
                    )

                    TextButton(
                        onClick = {
                            draft.clear()
                            draft.addAll(DEFAULT_QUICK_REACTIONS)
                            restoreDefaults = true
                        }
                    ) {
                        Text(text = stringResource(Res.string.restore_defaults))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (restoreDefaults) onRestoreDefaults() else onSave(draft.toList())
                    onDismiss()
                }
            ) {
                Text(text = stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    )

    if (showAddDialog) {
        ReactionInputDialog(
            title = stringResource(Res.string.add_quick_reaction),
            onDismiss = { showAddDialog = false },
            onConfirm = { reaction ->
                if (reaction !in draft) {
                    draft.add(reaction)
                    restoreDefaults = false
                }
                showAddDialog = false
            }
        )
    }
}

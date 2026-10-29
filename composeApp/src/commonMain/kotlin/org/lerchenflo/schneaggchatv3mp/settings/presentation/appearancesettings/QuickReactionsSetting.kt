package org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
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
import schneaggchatv3mp.composeapp.generated.resources.quick_reactions_double_tap
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
                    // The first slot is the double tap reaction. It is marked on the slot, not on the
                    // row, so a reaction dragged into it is inside the marker right away - the list
                    // itself only changes in onSettle, after the drop animation has finished.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = stringResource(Res.string.quick_reactions_double_tap),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.size(4.dp))

                    // All rows have the same height, so the first one's height is the slot height
                    var firstSlotHeight by remember { mutableIntStateOf(0) }
                    val slotColor = MaterialTheme.colorScheme.primary

                    Box(
                        modifier = Modifier.fillMaxWidth().drawBehind {
                            if (firstSlotHeight > 0) {
                                val stroke = 1.dp.toPx()
                                drawRoundRect(
                                    color = slotColor,
                                    topLeft = Offset(stroke / 2, stroke / 2),
                                    size = Size(size.width - stroke, firstSlotHeight - stroke),
                                    cornerRadius = CornerRadius(8.dp.toPx()),
                                    style = Stroke(width = stroke)
                                )
                            }
                        }
                    ) {
                        // Same reorder setup as the poll option list in CreatePollPopup. The copy matters:
                        // ReorderableColumn keys its drag state on the list, and the SnapshotStateList itself
                        // never compares unequal, so passing it directly kept stale offsets after a drop and
                        // broke on adding a reaction.
                        ReorderableColumn(
                            list = draft.toList(),
                            onMove = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            },
                            onSettle = { from, to ->
                                draft.add(to, draft.removeAt(from))
                                restoreDefaults = false
                            },
                        ) { index, reaction, _ ->
                            ReorderableItem(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { if (index == 0) firstSlotHeight = it.height }
                                        .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
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
                }

                Spacer(modifier = Modifier.size(12.dp))

                // Plus only: with a text label both buttons did not fit next to each other in the dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NormalButton(
                        text = "",
                        onClick = { showAddDialog = true },
                        primary = false,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(Res.string.add_quick_reaction)
                            )
                        }
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

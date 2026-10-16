package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.WheelRows
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.SplitMemberUi
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.CountSegmentedSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.picture.ProfilePictureView
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.germanNumericDateFormatter
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.delete
import schneaggchatv3mp.composeapp.generated.resources.ok
import schneaggchatv3mp.composeapp.generated.resources.save
import schneaggchatv3mp.composeapp.generated.resources.split_amount_for
import schneaggchatv3mp.composeapp.generated.resources.split_amount_label
import schneaggchatv3mp.composeapp.generated.resources.split_choose_icon
import schneaggchatv3mp.composeapp.generated.resources.split_date_label
import schneaggchatv3mp.composeapp.generated.resources.split_delete_expense
import schneaggchatv3mp.composeapp.generated.resources.split_delete_expense_title
import schneaggchatv3mp.composeapp.generated.resources.split_delete_message
import schneaggchatv3mp.composeapp.generated.resources.split_delete_transfer_title
import schneaggchatv3mp.composeapp.generated.resources.split_edit_expense
import schneaggchatv3mp.composeapp.generated.resources.split_edit_transfer
import schneaggchatv3mp.composeapp.generated.resources.split_icon_custom_label
import schneaggchatv3mp.composeapp.generated.resources.split_icon_remove
import schneaggchatv3mp.composeapp.generated.resources.split_icon_use
import schneaggchatv3mp.composeapp.generated.resources.split_mode_by_amount
import schneaggchatv3mp.composeapp.generated.resources.split_mode_by_shares
import schneaggchatv3mp.composeapp.generated.resources.split_mode_equally
import schneaggchatv3mp.composeapp.generated.resources.split_new_expense
import schneaggchatv3mp.composeapp.generated.resources.split_new_transfer
import schneaggchatv3mp.composeapp.generated.resources.split_paid_by_label
import schneaggchatv3mp.composeapp.generated.resources.split_required_field
import schneaggchatv3mp.composeapp.generated.resources.split_share_decrease
import schneaggchatv3mp.composeapp.generated.resources.split_share_increase
import schneaggchatv3mp.composeapp.generated.resources.split_split_between
import schneaggchatv3mp.composeapp.generated.resources.split_title_label
import schneaggchatv3mp.composeapp.generated.resources.split_transfer_from_label
import schneaggchatv3mp.composeapp.generated.resources.split_transfer_to_label

@Composable
fun SplitExpenseEditorScreenRoot(
    route: Route.SplitExpenseEditor,
) {
    val viewModel = koinViewModel<SplitExpenseEditorViewModel> {
        parametersOf(route)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    SplitExpenseEditorScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun SplitExpenseEditorScreen(
    state: SplitExpenseEditorState,
    onAction: (SplitExpenseEditorAction) -> Unit,
) {
    val screenTitle = when {
        state.isTransfer && state.isNew -> stringResource(Res.string.split_new_transfer)
        state.isTransfer -> stringResource(Res.string.split_edit_transfer)
        state.isNew -> stringResource(Res.string.split_new_expense)
        else -> stringResource(Res.string.split_edit_expense)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The app's root Scaffold already pads for system bars and keyboard
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ActivityTitle(
                title = screenTitle,
                onBackClick = { onAction(SplitExpenseEditorAction.OnBackClick) },
                actions = {
                    // Top right, far away from the save button at the bottom
                    if (!state.isNew) {
                        IconButton(onClick = { onAction(SplitExpenseEditorAction.OnDeleteClick) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.split_delete_expense)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = { onAction(SplitExpenseEditorAction.OnSaveClick) },
                    enabled = !state.isLoading && !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = 48.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(Res.string.save))
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.isTransfer) Row(verticalAlignment = Alignment.Top) {
                EmojiButton(
                    emoji = state.emoji,
                    onClick = { onAction(SplitExpenseEditorAction.OnEmojiClick) },
                    // Lines up with the text field box, not with its supporting text
                    modifier = Modifier.padding(top = 8.dp, end = 12.dp)
                )
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onAction(SplitExpenseEditorAction.OnTitleChange(it)) },
                    label = { Text(stringResource(Res.string.split_required_field, stringResource(Res.string.split_title_label))) },
                    isError = state.titleError != null,
                    supportingText = if (state.titleError != null) {
                        { FieldError(state.titleError) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { if (!it.isFocused) onAction(SplitExpenseEditorAction.OnTitleFocusLost) }
                )
            }

            OutlinedTextField(
                value = state.amount,
                onValueChange = { onAction(SplitExpenseEditorAction.OnAmountChange(it)) },
                label = { Text(stringResource(Res.string.split_required_field, stringResource(Res.string.split_amount_label))) },
                suffix = { Text(state.currencySymbol) },
                isError = state.amountError != null,
                supportingText = if (state.amountError != null) {
                    { FieldError(state.amountError) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) onAction(SplitExpenseEditorAction.OnAmountFocusLost) }
            )

            MemberPickerField(
                label = stringResource(
                    if (state.isTransfer) Res.string.split_transfer_from_label else Res.string.split_paid_by_label
                ),
                selectedName = state.paidByName,
                members = state.members,
                expanded = state.openMemberPicker == MemberPickerTarget.PAID_BY,
                error = null,
                onClick = { onAction(SplitExpenseEditorAction.OnMemberPickerClick(MemberPickerTarget.PAID_BY)) },
                onDismiss = { onAction(SplitExpenseEditorAction.OnMemberPickerDismiss) },
                onPick = { onAction(SplitExpenseEditorAction.OnMemberPicked(MemberPickerTarget.PAID_BY, it)) }
            )

            if (state.isTransfer) {
                MemberPickerField(
                    label = stringResource(Res.string.split_transfer_to_label),
                    selectedName = state.receiverName,
                    members = state.members,
                    expanded = state.openMemberPicker == MemberPickerTarget.RECEIVER,
                    error = state.receiverError,
                    onClick = { onAction(SplitExpenseEditorAction.OnMemberPickerClick(MemberPickerTarget.RECEIVER)) },
                    onDismiss = { onAction(SplitExpenseEditorAction.OnMemberPickerDismiss) },
                    onPick = { onAction(SplitExpenseEditorAction.OnMemberPicked(MemberPickerTarget.RECEIVER, it)) }
                )
            }

            ClickableField(
                label = stringResource(Res.string.split_date_label),
                value = state.dateText,
                trailingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                },
                onClick = { onAction(SplitExpenseEditorAction.OnDateClick) }
            )

            if (!state.isTransfer) {
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

                Text(
                    text = stringResource(Res.string.split_split_between),
                    style = MaterialTheme.typography.titleMedium
                )

                CountSegmentedSwitch(
                    options = SplitMode.entries,
                    selected = state.splitMode,
                    label = { mode ->
                        when (mode) {
                            SplitMode.EQUALLY -> stringResource(Res.string.split_mode_equally)
                            SplitMode.BY_SHARES -> stringResource(Res.string.split_mode_by_shares)
                            SplitMode.BY_AMOUNT -> stringResource(Res.string.split_mode_by_amount)
                        }
                    },
                    count = { null },
                    onSelect = { onAction(SplitExpenseEditorAction.OnSplitModeChange(it)) },
                    // Three labels need the room the check icon would take
                    showLeadingIcon = false
                )

                if (state.amountsStatus != null) {
                    Text(
                        text = state.amountsStatus.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.participantsError != null) {
                    FieldError(state.participantsError)
                }

                Column {
                    state.participants.forEach { participant ->
                        ParticipantRow(
                            participant = participant,
                            splitMode = state.splitMode,
                            currencySymbol = state.currencySymbol,
                            onToggle = { onAction(SplitExpenseEditorAction.OnParticipantToggle(participant.member.userId)) },
                            onWeightChange = { weight ->
                                onAction(SplitExpenseEditorAction.OnParticipantWeightChange(participant.member.userId, weight))
                            },
                            onAmountChange = { amount ->
                                onAction(SplitExpenseEditorAction.OnParticipantAmountChange(participant.member.userId, amount))
                            }
                        )
                    }
                }
            }
        }
    }

    if (state.isEmojiPickerVisible) {
        EmojiPickerSheet(
            selectedEmoji = state.emoji,
            customInput = state.customEmojiInput,
            onAction = onAction
        )
    }

    val date = state.date
    if (state.isDatePickerVisible && date != null) {
        SplitDatePickerDialog(
            initialDate = date,
            onConfirm = { onAction(SplitExpenseEditorAction.OnDateSelected(it)) },
            onDismiss = { onAction(SplitExpenseEditorAction.OnDatePickerDismiss) }
        )
    }

    if (state.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(SplitExpenseEditorAction.OnDeleteDismiss) },
            title = {
                Text(
                    stringResource(
                        if (state.isTransfer) Res.string.split_delete_transfer_title else Res.string.split_delete_expense_title
                    )
                )
            },
            text = { Text(stringResource(Res.string.split_delete_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(SplitExpenseEditorAction.OnDeleteConfirm) }) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SplitExpenseEditorAction.OnDeleteDismiss) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

// ─── Fields ───────────────────────────────────────────────────────────────────

/** Error text with an icon, so the error is not shown by color alone. */
@Composable
private fun FieldError(error: UiText) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Read-only field that reacts to a tap on its whole area (date, member pickers). */
@Composable
private fun ClickableField(
    label: String,
    value: String,
    trailingIcon: @Composable () -> Unit,
    onClick: () -> Unit,
    error: UiText? = null,
) {
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = trailingIcon,
            isError = error != null,
            supportingText = if (error != null) {
                { FieldError(error) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Covers the text field, which would otherwise take the tap for focusing
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
        )
    }
}

@Composable
private fun MemberPickerField(
    label: String,
    selectedName: String,
    members: List<SplitMemberUi>,
    expanded: Boolean,
    error: UiText?,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    Box {
        ClickableField(
            label = label,
            value = selectedName,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            onClick = onClick,
            error = error
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            members.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.name) },
                    leadingIcon = { MemberAvatar(member) },
                    onClick = { onPick(member.userId) }
                )
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: SplitParticipantUi,
    splitMode: SplitMode,
    currencySymbol: String,
    onToggle: () -> Unit,
    onWeightChange: (Int) -> Unit,
    onAmountChange: (String) -> Unit,
) {
    val amountDescription = stringResource(Res.string.split_amount_for, participant.member.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = participant.isIncluded,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = participant.isIncluded,
            onCheckedChange = null,
            modifier = Modifier.padding(end = 12.dp)
        )
        MemberAvatar(participant.member)
        Text(
            text = participant.member.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )

        if (splitMode == SplitMode.BY_SHARES && participant.isIncluded) {
            IconButton(
                onClick = { onWeightChange(participant.weight - 1) },
                enabled = participant.weight > 1
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(Res.string.split_share_decrease, participant.member.name)
                )
            }
            Text(
                text = participant.weight.toString(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 20.dp)
            )
            IconButton(onClick = { onWeightChange(participant.weight + 1) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.split_share_increase, participant.member.name)
                )
            }
        }

        if (splitMode == SplitMode.BY_AMOUNT && participant.isIncluded) {
            OutlinedTextField(
                value = participant.amountInput,
                onValueChange = onAmountChange,
                suffix = { Text(currencySymbol) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                modifier = Modifier
                    .width(136.dp)
                    .padding(vertical = 4.dp)
                    .semantics {
                        contentDescription = amountDescription
                    }
            )
        } else {
            Text(
                text = participant.shareText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 72.dp)
            )
        }
    }
}

/** Round button left of the title showing the chosen emoji, or a placeholder icon when none is set. */
@Composable
private fun EmojiButton(
    emoji: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.split_choose_icon)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        } else {
            Icon(
                imageVector = Icons.Default.AddReaction,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Bottom sheet with quick-pick emoji and a field for any other emoji from the keyboard.
 * The selected emoji is highlighted; "Remove icon" goes back to the default icon.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EmojiPickerSheet(
    selectedEmoji: String?,
    customInput: String,
    onAction: (SplitExpenseEditorAction) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(SplitExpenseEditorAction.OnEmojiPickerDismiss) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.split_choose_icon),
                style = MaterialTheme.typography.titleLarge
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SUGGESTED_EXPENSE_EMOJIS.forEach { emoji ->
                    val isSelected = emoji == selectedEmoji
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.background
                            )
                            .clickable(role = Role.Button) { onAction(SplitExpenseEditorAction.OnEmojiSelected(emoji)) }
                            .semantics { selected = isSelected },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            OutlinedTextField(
                value = customInput,
                onValueChange = { onAction(SplitExpenseEditorAction.OnCustomEmojiChange(it)) },
                label = { Text(stringResource(Res.string.split_icon_custom_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAction(SplitExpenseEditorAction.OnCustomEmojiConfirm) }),
                trailingIcon = {
                    IconButton(
                        onClick = { onAction(SplitExpenseEditorAction.OnCustomEmojiConfirm) },
                        enabled = customInput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(Res.string.split_icon_use)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedEmoji != null) {
                TextButton(onClick = { onAction(SplitExpenseEditorAction.OnEmojiRemove) }) {
                    Text(stringResource(Res.string.split_icon_remove))
                }
            }
        }
    }
}

@Composable
private fun MemberAvatar(member: SplitMemberUi) {
    ProfilePictureView(
        filepath = member.profilePictureUrl,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape),
        contentDescription = null
    )
}

/** Same wheel picker as the rest of the app; the snapped date is only committed on confirm. */
@Composable
private fun SplitDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickedDate by remember { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.split_date_label)) },
        text = {
            WheelDatePicker(
                modifier = Modifier.fillMaxWidth(),
                startDate = initialDate,
                dateFormatter = germanNumericDateFormatter(),
                rows = WheelRows.Count(5),
                textColor = MaterialTheme.colorScheme.onSurface,
                selectorProperties = WheelPickerDefaults.selectorProperties(
                    enabled = true,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ),
                onSnappedDate = { pickedDate = it }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickedDate) }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

// ─── Previews ─────────────────────────────────────────────────────────────────

private val previewMembers = listOf(
    SplitMemberUi(userId = "me", name = "Fabi (you)", isMe = true, profilePictureUrl = ""),
    SplitMemberUi(userId = "anna", name = "Anna", isMe = false, profilePictureUrl = ""),
    SplitMemberUi(userId = "ben", name = "Ben", isMe = false, profilePictureUrl = ""),
)

@Preview(showBackground = true)
@Composable
private fun SplitExpenseEditorScreenPreview() {
    SchneaggchatTheme {
        SplitExpenseEditorScreen(
            state = SplitExpenseEditorState(
                isLoading = false,
                isNew = false,
                emoji = "🍕",
                title = "Pizza Margherita",
                amount = "48,00",
                currencySymbol = "€",
                date = LocalDate(2026, 10, 16),
                dateText = "16.10.2026",
                members = previewMembers,
                paidByUserId = "me",
                paidByName = "Fabi (you)",
                splitMode = SplitMode.BY_SHARES,
                participants = listOf(
                    SplitParticipantUi(member = previewMembers[0], isIncluded = true, weight = 2, amountInput = "", shareText = "24,00 €"),
                    SplitParticipantUi(member = previewMembers[1], isIncluded = true, weight = 1, amountInput = "", shareText = "12,00 €"),
                    SplitParticipantUi(member = previewMembers[2], isIncluded = true, weight = 1, amountInput = "", shareText = "12,00 €"),
                ),
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplitExpenseEditorScreenTransferPreview() {
    SchneaggchatTheme {
        SplitExpenseEditorScreen(
            state = SplitExpenseEditorState(
                isLoading = false,
                isNew = true,
                isTransfer = true,
                amount = "28,80",
                currencySymbol = "€",
                date = LocalDate(2026, 10, 16),
                dateText = "16.10.2026",
                members = previewMembers,
                paidByUserId = "anna",
                paidByName = "Anna",
                receiverUserId = "ben",
                receiverName = "Ben",
            ),
            onAction = {}
        )
    }
}

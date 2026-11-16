package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.SplitMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.SplitEntryIcon
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.close
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.split_date_label
import schneaggchatv3mp.composeapp.generated.resources.split_delete_expense
import schneaggchatv3mp.composeapp.generated.resources.split_detail_added_by
import schneaggchatv3mp.composeapp.generated.resources.split_detail_edited_by
import schneaggchatv3mp.composeapp.generated.resources.split_detail_share_count
import schneaggchatv3mp.composeapp.generated.resources.split_mode_by_amount
import schneaggchatv3mp.composeapp.generated.resources.split_mode_by_shares
import schneaggchatv3mp.composeapp.generated.resources.split_mode_equally
import schneaggchatv3mp.composeapp.generated.resources.split_paid_by_label
import schneaggchatv3mp.composeapp.generated.resources.split_split_between
import schneaggchatv3mp.composeapp.generated.resources.split_transfer_from_label
import schneaggchatv3mp.composeapp.generated.resources.split_transfer_label
import schneaggchatv3mp.composeapp.generated.resources.split_transfer_to_label

/**
 * Read-only view of one expense or money transfer: amount, who paid, date and time, what everyone
 * owes and who added or changed it. Edit opens the editor; Delete (only for [SplitExpenseDetailUi.canDelete])
 * asks for confirmation first.
 */
@Composable
fun SplitExpenseDetailDialog(
    detail: SplitExpenseDetailUi,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailHeader(detail)

                    if (detail.isTransfer) {
                        DetailRow(label = stringResource(Res.string.split_transfer_from_label), value = detail.paidByName)
                        DetailRow(label = stringResource(Res.string.split_transfer_to_label), value = detail.receiverName)
                    } else {
                        DetailRow(label = stringResource(Res.string.split_paid_by_label), value = detail.paidByName)
                    }
                    DetailRow(label = stringResource(Res.string.split_date_label), value = detail.dateTimeText)

                    if (!detail.isTransfer) {
                        HorizontalDivider()
                        ShareList(splitMode = detail.splitMode, shares = detail.shares)
                    }

                    HorizontalDivider()
                    ChangeInfo(added = detail.added, edited = detail.edited)
                }

                DetailButtons(
                    canDelete = detail.canDelete,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(detail: SplitExpenseDetailUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SplitEntryIcon(isTransfer = detail.isTransfer, emoji = detail.emoji)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (detail.isTransfer) stringResource(Res.string.split_transfer_label) else detail.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail.amountText,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** "Split between · By shares" and one row per participant with their share count and amount. */
@Composable
private fun ShareList(
    splitMode: SplitMode,
    shares: List<SplitDetailShareUi>,
) {
    val modeLabel = when (splitMode) {
        SplitMode.EQUALLY -> stringResource(Res.string.split_mode_equally)
        SplitMode.BY_SHARES -> stringResource(Res.string.split_mode_by_shares)
        SplitMode.BY_AMOUNT -> stringResource(Res.string.split_mode_by_amount)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.split_split_between) + " · " + modeLabel,
            style = MaterialTheme.typography.titleSmall
        )
        shares.forEach { share ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = share.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (share.shareCount != null) {
                    Text(
                        text = pluralStringResource(Res.plurals.split_detail_share_count, share.shareCount, share.shareCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                Text(
                    text = share.amountText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun ChangeInfo(
    added: SplitChangeUi,
    edited: SplitChangeUi?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(Res.string.split_detail_added_by, added.name, added.atText),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (edited != null) {
            Text(
                text = stringResource(Res.string.split_detail_edited_by, edited.name, edited.atText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Delete on the leading side, away from Edit, the main action on the trailing side. */
@Composable
private fun DetailButtons(
    canDelete: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canDelete) {
            TextButton(
                onClick = onDeleteClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(Res.string.split_delete_expense))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) {
            Text(stringResource(Res.string.close))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onEditClick) {
            Text(stringResource(Res.string.edit))
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun SplitExpenseDetailDialogPreview() {
    SchneaggchatTheme {
        SplitExpenseDetailDialog(
            detail = SplitExpenseDetailUi(
                itemId = "1",
                isTransfer = false,
                emoji = "🍕",
                title = "Pizza Margherita",
                amountText = "48.00 €",
                paidByName = "Fabi (you)",
                receiverName = "",
                dateTimeText = "16.10.2026 19:30",
                splitMode = SplitMode.BY_SHARES,
                shares = listOf(
                    SplitDetailShareUi(userId = "me", name = "Fabi (you)", amountText = "24.00 €", shareCount = 2),
                    SplitDetailShareUi(userId = "anna", name = "Anna", amountText = "12.00 €", shareCount = 1),
                    SplitDetailShareUi(userId = "ben", name = "Ben", amountText = "12.00 €", shareCount = 1),
                ),
                added = SplitChangeUi(name = "Fabi (you)", atText = "16.10.2026 19:42"),
                edited = SplitChangeUi(name = "Anna", atText = "17.10.2026 08:10"),
                canDelete = true,
            ),
            onEditClick = {},
            onDeleteClick = {},
            onDismiss = {}
        )
    }
}

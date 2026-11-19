package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.detail

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.SplitMode

/** Everything the detail popup shows about one expense or money transfer. */
data class SplitExpenseDetailUi(
    val itemId: String,
    val isTransfer: Boolean,
    /** Icon chosen for the expense, null = default icon */
    val emoji: String?,
    /** Empty for transfers, the UI labels them */
    val title: String,
    val amountText: String,
    /** Payer of an expense, sender of a transfer */
    val paidByName: String,
    /** Transfers only */
    val receiverName: String,
    val dateTimeText: String,
    /** Expenses only */
    val splitMode: SplitMode,
    /** Expenses only: what each participant owes, own user first, then by name */
    val shares: List<SplitDetailShareUi>,
    val added: SplitChangeUi,
    /** Null while the entry was never changed after it was added */
    val edited: SplitChangeUi?,
    /** Item creator, split creator or group admin */
    val canDelete: Boolean,
)

data class SplitDetailShareUi(
    val userId: String,
    val name: String,
    val amountText: String,
    /** Number of shares, only set in [SplitMode.BY_SHARES] */
    val shareCount: Int?,
)

/** Who added or last changed an entry, and when. */
data class SplitChangeUi(
    val name: String,
    val atText: String,
)

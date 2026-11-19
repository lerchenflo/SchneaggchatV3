package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.detail

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.SplitExpense
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionItem
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.SplitMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.consumedCents
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.splitMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.SplitMemberUi
import org.lerchenflo.schneaggchatv3mp.utilities.DATE_TIME_FORMAT
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString

/**
 * The detail popup content of this item, which holds [expense]. [members] must contain everyone
 * the item refers to (payer, participants, creator, last editor) and gives the order of the shares;
 * [money] formats cents in the split's currency.
 */
fun GroupExtensionItem.toExpenseDetailUi(
    expense: SplitExpense,
    members: List<SplitMemberUi>,
    money: (Long) -> String,
    canDelete: Boolean,
): SplitExpenseDetailUi {
    val nameOf = members.associate { it.userId to it.name }

    return SplitExpenseDetailUi(
        itemId = id,
        isTransfer = expense.isSettlement,
        emoji = expense.emoji?.takeIf { it.isNotBlank() },
        title = expense.title,
        amountText = money(expense.amountCents),
        paidByName = nameOf[expense.paidBy].orEmpty(),
        receiverName = if (expense.isSettlement) {
            expense.participants.firstOrNull()?.let { nameOf[it] }.orEmpty()
        } else "",
        dateTimeText = millisToString(expense.date, DATE_TIME_FORMAT),
        splitMode = expense.splitMode,
        shares = if (expense.isSettlement) emptyList() else expense.detailShares(members, money),
        added = SplitChangeUi(name = nameOf[createdBy].orEmpty(), atText = millisToString(createdAt, DATE_TIME_FORMAT)),
        // The server gives both timestamps the same value when the item is added
        edited = if (updatedAt > createdAt) {
            SplitChangeUi(name = nameOf[updatedBy].orEmpty(), atText = millisToString(updatedAt, DATE_TIME_FORMAT))
        } else null,
        canDelete = canDelete,
    )
}

private fun SplitExpense.detailShares(
    members: List<SplitMemberUi>,
    money: (Long) -> String,
): List<SplitDetailShareUi> {
    val consumed = consumedCents()
    val showShareCount = splitMode == SplitMode.BY_SHARES

    return members
        .filter { it.userId in consumed }
        .map { member ->
            SplitDetailShareUi(
                userId = member.userId,
                name = member.name,
                amountText = money(consumed.getValue(member.userId)),
                shareCount = if (showShareCount) shares[member.userId]?.weight else null,
            )
        }
}

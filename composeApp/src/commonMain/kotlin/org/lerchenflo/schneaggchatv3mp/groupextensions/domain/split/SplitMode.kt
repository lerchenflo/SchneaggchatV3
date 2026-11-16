package org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.SplitExpense

/** How an expense is shared: the three plain forms the editor offers. */
enum class SplitMode { EQUALLY, BY_SHARES, BY_AMOUNT }

/**
 * The [SplitMode] that describes this expense's shares. Any fixed amount makes it [SplitMode.BY_AMOUNT]
 * (a mix of fixed amounts and weights from another client shows as amounts, which keeps the total),
 * any weight other than 1 makes it [SplitMode.BY_SHARES]. Transfers count as [SplitMode.EQUALLY].
 */
val SplitExpense.splitMode: SplitMode
    get() = when {
        isSettlement -> SplitMode.EQUALLY
        shares.values.any { it.fixedCents > 0L } -> SplitMode.BY_AMOUNT
        shares.values.any { it.weight != 1 } -> SplitMode.BY_SHARES
        else -> SplitMode.EQUALLY
    }

package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split

import androidx.compose.runtime.Stable

@Stable
data class SplitState(
    val title: String = "",
    val selectedTab: SplitTab = SplitTab.EXPENSES,
    /** Newest day first */
    val expenseSections: List<SplitExpenseSectionUi> = emptyList(),
    val expenseCount: Int = 0,
    /** What the own user consumed */
    val myTotal: String = "",
    val totalExpenses: String = "",
    val balances: List<SplitBalanceUi> = emptyList(),
    /** Transfers that settle all balances */
    val settlements: List<SplitSettlementUi> = emptyList(),
    val isLoading: Boolean = true,
    /** Loaded, but the extension is gone (deleted or never synced) */
    val extensionMissing: Boolean = false,
)

enum class SplitTab { EXPENSES, BALANCES }

data class SplitExpenseSectionUi(
    val dateText: String,
    val expenses: List<SplitExpenseUi>,
)

/** One row of the expense list - a real expense or a money transfer. */
data class SplitExpenseUi(
    val id: String,
    /** Icon chosen for the expense, null = default icon */
    val emoji: String?,
    val title: String,
    val amountText: String,
    val paidByName: String,
    val isTransfer: Boolean,
    /** Only set for transfers */
    val receiverName: String,
)

data class SplitBalanceUi(
    val userId: String,
    val name: String,
    /** Positive = gets money back, negative = owes money */
    val balanceCents: Long,
    val amountText: String,
    /** Bar length relative to the biggest balance, 0..1 */
    val barFraction: Float,
)

data class SplitSettlementUi(
    val fromUserId: String,
    val fromName: String,
    val toUserId: String,
    val toName: String,
    val amountCents: Long,
    val amountText: String,
)

sealed interface SplitAction {
    data object OnBackClick : SplitAction
    data class OnTabSelected(val tab: SplitTab) : SplitAction
    data object OnAddExpenseClick : SplitAction
    data class OnExpenseClick(val itemId: String) : SplitAction
    data class OnMarkAsPaidClick(val settlement: SplitSettlementUi) : SplitAction
}

package org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.SplitExpense

/*
 * Pure money math of the split extension. All amounts are minor units (cents) of the container
 * currency. Nothing computed here is ever stored, it is always derived from the expense list.
 */

/** One transfer that settles debts: [fromUserId] pays [amountCents] to [toUserId]. */
data class SettlementSuggestion(
    val fromUserId: String,
    val toUserId: String,
    val amountCents: Long,
)

/**
 * What each participant consumed of this expense: their fixedCents plus their part of the rest
 * (amountCents minus all fixedCents), split by weight. Cents of the rest that don't divide evenly go
 * one each to the first weighted participants, so the shares always add up to the amount. With no
 * weight left only the fixed amounts count. Empty when nobody takes part.
 */
fun SplitExpense.consumedCents(): Map<String, Long> {
    if (shares.isEmpty()) return emptyMap()

    val fixedOf = shares.mapValues { (_, share) -> share.fixedCents.coerceAtLeast(0L) }
    val weightOf = shares.mapValues { (_, share) -> share.weight.coerceAtLeast(0).toLong() }
    val totalWeight = weightOf.values.sum()
    val rest = (amountCents - fixedOf.values.sum()).coerceAtLeast(0L)
    if (totalWeight == 0L) return fixedOf

    val result = shares.keys.associateWith { fixedOf.getValue(it) + rest * weightOf.getValue(it) / totalWeight }
        .toMutableMap()
    var remainder = rest - shares.keys.sumOf { rest * weightOf.getValue(it) / totalWeight }
    for (person in shares.keys) {
        if (remainder <= 0L) break
        if (weightOf.getValue(person) == 0L) continue
        result[person] = result.getValue(person) + 1
        remainder--
    }
    return result
}

/**
 * Net balance per user over all expenses and transfers: what they paid minus what they consumed.
 * Positive = gets money back, negative = owes money. The balances always add up to 0.
 */
fun computeBalances(expenses: List<SplitExpense>): Map<String, Long> {
    val balances = mutableMapOf<String, Long>()
    expenses.forEach { expense ->
        val shares = expense.consumedCents()
        if (shares.isEmpty()) return@forEach

        // Credits what was handed out, not amountCents: the same for valid data, and fixed amounts
        // above the total (data from a buggy client) can't break the zero sum
        balances[expense.paidBy] = (balances[expense.paidBy] ?: 0L) + shares.values.sum()
        shares.forEach { (userId, share) ->
            balances[userId] = (balances[userId] ?: 0L) - share
        }
    }
    return balances
}

/**
 * Transfers that bring every balance to 0 (greedy min cash flow): the biggest debtor pays the
 * biggest creditor until one of them is even, then the next pair. Needs at most n - 1 transfers.
 */
fun suggestSettlements(balances: Map<String, Long>): List<SettlementSuggestion> {
    val debtors = balances.filterValues { it < 0L }
        .map { (userId, balance) -> userId to -balance }
        .sortedByDescending { it.second }
        .toMutableList()
    val creditors = balances.filterValues { it > 0L }
        .toList()
        .sortedByDescending { it.second }
        .toMutableList()

    val suggestions = mutableListOf<SettlementSuggestion>()
    var debtorIndex = 0
    var creditorIndex = 0
    while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
        val (debtor, owes) = debtors[debtorIndex]
        val (creditor, getsBack) = creditors[creditorIndex]
        val amount = minOf(owes, getsBack)

        suggestions += SettlementSuggestion(fromUserId = debtor, toUserId = creditor, amountCents = amount)

        debtors[debtorIndex] = debtor to owes - amount
        creditors[creditorIndex] = creditor to getsBack - amount
        if (debtors[debtorIndex].second == 0L) debtorIndex++
        if (creditors[creditorIndex].second == 0L) creditorIndex++
    }
    return suggestions
}

/** Sum of all real expenses, transfers excluded. */
fun totalExpenses(expenses: List<SplitExpense>): Long =
    expenses.filterNot { it.isSettlement }.sumOf { it.amountCents }

/** What [userId] consumed over all real expenses, transfers excluded. */
fun consumedBy(userId: String, expenses: List<SplitExpense>): Long =
    expenses.filterNot { it.isSettlement }.sumOf { it.consumedCents()[userId] ?: 0L }

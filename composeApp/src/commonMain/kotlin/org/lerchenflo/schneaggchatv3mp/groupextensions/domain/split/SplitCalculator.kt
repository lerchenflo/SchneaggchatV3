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
 * What each participant consumed of this expense: the fixed [SplitExpense.amounts] when set (a missing
 * amount counts as 0), else proportional to [SplitExpense.weights] (a missing weight counts as 1), else
 * an equal split. Cents that don't divide evenly go one each to the first participants, so weighted and
 * equal shares always add up to the amount. Empty when nobody takes part.
 */
fun SplitExpense.shares(): Map<String, Long> {
    val people = participants.distinct()
    if (people.isEmpty()) return emptyMap()

    amounts?.let { fixed ->
        return people.associateWith { (fixed[it] ?: 0L).coerceAtLeast(0L) }
    }

    val weightOf = people.associateWith { (weights?.get(it) ?: 1).coerceAtLeast(0).toLong() }
    val totalWeight = weightOf.values.sum()
    if (totalWeight == 0L) return emptyMap()

    val shares = people.associateWith { amountCents * weightOf.getValue(it) / totalWeight }.toMutableMap()
    var remainder = amountCents - shares.values.sum()
    for (person in people) {
        if (remainder <= 0L) break
        if (weightOf.getValue(person) == 0L) continue
        shares[person] = shares.getValue(person) + 1
        remainder--
    }
    return shares
}

/**
 * Net balance per user over all expenses and transfers: what they paid minus what they consumed.
 * Positive = gets money back, negative = owes money. The balances always add up to 0.
 */
fun computeBalances(expenses: List<SplitExpense>): Map<String, Long> {
    val balances = mutableMapOf<String, Long>()
    expenses.forEach { expense ->
        val shares = expense.shares()
        if (shares.isEmpty()) return@forEach

        // Credits what was handed out, not amountCents: the same for valid data, and fixed amounts
        // that don't add up to the total (data from a buggy client) can't break the zero sum
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
    expenses.filterNot { it.isSettlement }.sumOf { it.shares()[userId] ?: 0L }

package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.SplitExpense
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionItem
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.computeBalances
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.consumedBy
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.suggestSettlements
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.totalExpenses
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.editor.SplitTransferDraft
import org.lerchenflo.schneaggchatv3mp.sharedUi.DATE_CHIP_FORMAT
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.split_decimal_separator
import kotlin.math.absoluteValue

/** Backs the money split screen: expense list, totals, balances and settle-up suggestions. */
class SplitViewModel(
    private val groupExtensionRepository: GroupExtensionRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val navigator: Navigator,

    private val extensionId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(SplitState())
    val state = _state.asStateFlow()

    private data class SplitSource(
        val extension: GroupExtension,
        val items: List<GroupExtensionItem>,
        val groupMembers: List<GroupMember>,
    )

    init {
        observeSplit()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSplit() {
        viewModelScope.launch {
            val decimalSeparator = getString(Res.string.split_decimal_separator)
            val ownId = SessionCache.requireLoggedIn()?.userId

            groupExtensionRepository.getExtensionFlow(extensionId)
                .flatMapLatest { extension ->
                    if (extension == null) {
                        flowOf(null)
                    } else {
                        combine(
                            groupExtensionRepository.getItemsForExtensionFlow(extensionId),
                            groupRepository.getGroupFlow(extension.ownerId),
                        ) { items, group ->
                            SplitSource(extension, items, group?.members.orEmpty())
                        }
                    }
                }
                .collect { source ->
                    if (source == null) {
                        _state.update { it.copy(isLoading = false, extensionMissing = true) }
                    } else {
                        val content = buildState(source, ownId, decimalSeparator)
                        _state.update {
                            content.copy(selectedTab = it.selectedTab)
                        }
                    }
                }
        }
    }

    private suspend fun buildState(source: SplitSource, ownId: String?, decimalSeparator: String): SplitState {
        val currency = (source.extension.settings as? ExtensionSettings.Split)?.currency ?: DEFAULT_SPLIT_CURRENCY
        fun money(cents: Long, signed: Boolean = false) = formatMoney(cents, currency, decimalSeparator, signed)

        val expenseItems = source.items.mapNotNull { item ->
            (item.data as? SplitExpense)?.let { item to it }
        }
        val expenses = expenseItems.map { it.second }

        // Former members still show up as long as an expense refers to them
        val members = resolveSplitMembers(
            userIds = source.groupMembers.map { it.userId } + expenses.flatMap { listOf(it.paidBy) + it.participants },
            groupMembers = source.groupMembers,
            userRepository = userRepository,
            ownId = ownId,
        )
        val nameOf = members.associate { it.userId to it.name }

        val sections = expenseItems
            .sortedWith(
                compareByDescending<Pair<GroupExtensionItem, SplitExpense>> { it.second.date }
                    .thenByDescending { it.first.createdAt }
            )
            .groupBy { millisToString(it.second.date, DATE_CHIP_FORMAT) }
            .map { (dateText, dayExpenses) ->
                SplitExpenseSectionUi(
                    dateText = dateText,
                    expenses = dayExpenses.map { (item, expense) ->
                        SplitExpenseUi(
                            id = item.id,
                            emoji = expense.emoji?.takeIf { it.isNotBlank() },
                            title = expense.title,
                            amountText = money(expense.amountCents),
                            paidByName = nameOf[expense.paidBy].orEmpty(),
                            isTransfer = expense.isSettlement,
                            receiverName = if (expense.isSettlement) {
                                expense.participants.firstOrNull()?.let { nameOf[it] }.orEmpty()
                            } else "",
                        )
                    }
                )
            }

        val balanceByUser = computeBalances(expenses)
        val currentMemberIds = source.groupMembers.map { it.userId }.toSet()
        // Current members always, former ones only while they are not even
        val balanceMembers = members.filter { it.userId in currentMemberIds || (balanceByUser[it.userId] ?: 0L) != 0L }
        val biggestBalance = balanceMembers.maxOfOrNull { (balanceByUser[it.userId] ?: 0L).absoluteValue } ?: 0L

        return SplitState(
            title = source.extension.title,
            expenseSections = sections,
            expenseCount = expenses.size,
            myTotal = money(ownId?.let { consumedBy(it, expenses) } ?: 0L),
            totalExpenses = money(totalExpenses(expenses)),
            balances = balanceMembers.map { member ->
                val balance = balanceByUser[member.userId] ?: 0L
                SplitBalanceUi(
                    userId = member.userId,
                    name = member.name,
                    balanceCents = balance,
                    amountText = money(balance, signed = true),
                    barFraction = if (biggestBalance == 0L) 0f else balance.absoluteValue.toFloat() / biggestBalance,
                )
            },
            settlements = suggestSettlements(balanceByUser).map { suggestion ->
                SplitSettlementUi(
                    fromUserId = suggestion.fromUserId,
                    fromName = nameOf[suggestion.fromUserId].orEmpty(),
                    toUserId = suggestion.toUserId,
                    toName = nameOf[suggestion.toUserId].orEmpty(),
                    amountCents = suggestion.amountCents,
                    amountText = money(suggestion.amountCents),
                )
            },
            isLoading = false,
            extensionMissing = false,
        )
    }

    fun onAction(action: SplitAction) {
        when (action) {
            SplitAction.OnBackClick -> viewModelScope.launch {
                navigator.navigateBack()
            }

            is SplitAction.OnTabSelected -> _state.update { it.copy(selectedTab = action.tab) }

            SplitAction.OnAddExpenseClick -> viewModelScope.launch {
                navigator.navigate(Route.SplitExpenseEditor(extensionId = extensionId))
            }

            is SplitAction.OnExpenseClick -> viewModelScope.launch {
                navigator.navigate(Route.SplitExpenseEditor(extensionId = extensionId, itemId = action.itemId))
            }

            // Opens a prefilled transfer the user confirms (and may change, e.g. for a partial payment)
            is SplitAction.OnMarkAsPaidClick -> viewModelScope.launch {
                navigator.navigate(
                    Route.SplitExpenseEditor(
                        extensionId = extensionId,
                        transfer = SplitTransferDraft(
                            fromUserId = action.settlement.fromUserId,
                            toUserId = action.settlement.toUserId,
                            amountCents = action.settlement.amountCents,
                        )
                    )
                )
            }
        }
    }
}

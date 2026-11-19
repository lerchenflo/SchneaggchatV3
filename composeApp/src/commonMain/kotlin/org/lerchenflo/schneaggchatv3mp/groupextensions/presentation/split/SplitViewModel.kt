package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.isSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.reportErrorTo
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
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
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.detail.toExpenseDetailUi
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.editor.SplitTransferDraft
import org.lerchenflo.schneaggchatv3mp.sharedUi.DATE_CHIP_FORMAT
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.split_decimal_separator
import kotlin.math.absoluteValue

/** Backs the money split screen: expense list, totals, balances, settle-up suggestions and the detail popup. */
class SplitViewModel(
    private val errorChannel: ErrorChannel,
    private val groupExtensionRepository: GroupExtensionRepository,
    private val groupRepository: GroupRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val navigator: Navigator,

    private val extensionId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(SplitState())
    val state = _state.asStateFlow()

    /** Entry shown in the detail popup, null = popup closed */
    private val selectedItemId = MutableStateFlow<String?>(null)

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

            val sourceFlow = groupExtensionRepository.getExtensionFlow(extensionId)
                .flatMapLatest { extension ->
                    if (extension == null) {
                        flowOf(null)
                    } else {
                        groupRepository.getGroupByIdFlow(extension.ownerId).map { group ->
                            SplitSource(extension, extension.items, group?.members.orEmpty())
                        }
                    }
                }

            combine(sourceFlow, selectedItemId, ::Pair).collect { (source, selectedId) ->
                if (source == null) {
                    _state.update {
                        it.copy(isLoading = false, extensionMissing = true, detail = null, isDeleteDialogVisible = false)
                    }
                    return@collect
                }

                val content = buildState(source, ownId, decimalSeparator, selectedId)
                // The entry is gone (e.g. deleted by another member): its popup closes
                if (selectedId != null && content.detail == null) selectedItemId.value = null
                _state.update { previous ->
                    content.copy(
                        selectedTab = previous.selectedTab,
                        isDeleteDialogVisible = previous.isDeleteDialogVisible && content.detail != null,
                    )
                }
            }
        }
    }

    private suspend fun buildState(
        source: SplitSource,
        ownId: String?,
        decimalSeparator: String,
        selectedId: String?,
    ): SplitState {
        val currency = (source.extension.settings as? ExtensionSettings.Split)?.currency ?: DEFAULT_SPLIT_CURRENCY
        val money = { cents: Long -> formatMoney(cents, currency, decimalSeparator) }
        val signedMoney = { cents: Long -> formatMoney(cents, currency, decimalSeparator, signed = true) }

        val expenseItems = source.items.mapNotNull { item ->
            (item.data as? SplitExpense)?.let { item to it }
        }
        val expenses = expenseItems.map { it.second }

        // Former members still show up as long as an entry refers to them
        val members = resolveSplitMembers(
            userIds = source.groupMembers.map { it.userId } + expenseItems.flatMap { (item, expense) ->
                listOf(expense.paidBy, item.createdBy, item.updatedBy) + expense.participants
            },
            userIdentityRepository = userIdentityRepository,
            ownId = ownId,
        )
        val nameOf = members.associate { it.userId to it.name }
        val balanceByUser = computeBalances(expenses)
        val selected = expenseItems.find { (item, _) -> item.id == selectedId }

        return SplitState(
            title = source.extension.title,
            expenseSections = buildExpenseSections(expenseItems, nameOf, money),
            expenseCount = expenses.size,
            myTotal = money(ownId?.let { consumedBy(it, expenses) } ?: 0L),
            totalExpenses = money(totalExpenses(expenses)),
            balances = buildBalances(members, source.groupMembers, balanceByUser, signedMoney),
            settlements = buildSettlements(balanceByUser, nameOf, money),
            detail = selected?.let { (item, expense) ->
                item.toExpenseDetailUi(
                    expense = expense,
                    members = members,
                    money = money,
                    canDelete = canDelete(item, source, ownId),
                )
            },
            isLoading = false,
            extensionMissing = false,
        )
    }

    /** Newest day first, within a day newest entry first. */
    private fun buildExpenseSections(
        expenseItems: List<Pair<GroupExtensionItem, SplitExpense>>,
        nameOf: Map<String, String>,
        money: (Long) -> String,
    ): List<SplitExpenseSectionUi> = expenseItems
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

    /** Current members always, former ones only while they are not even. */
    private fun buildBalances(
        members: List<SplitMemberUi>,
        groupMembers: List<GroupMember>,
        balanceByUser: Map<String, Long>,
        signedMoney: (Long) -> String,
    ): List<SplitBalanceUi> {
        val currentMemberIds = groupMembers.map { it.userId }.toSet()
        val balanceMembers = members.filter { it.userId in currentMemberIds || (balanceByUser[it.userId] ?: 0L) != 0L }
        val biggestBalance = balanceMembers.maxOfOrNull { (balanceByUser[it.userId] ?: 0L).absoluteValue } ?: 0L

        return balanceMembers.map { member ->
            val balance = balanceByUser[member.userId] ?: 0L
            SplitBalanceUi(
                userId = member.userId,
                name = member.name,
                balanceCents = balance,
                amountText = signedMoney(balance),
                barFraction = if (biggestBalance == 0L) 0f else balance.absoluteValue.toFloat() / biggestBalance,
            )
        }
    }

    private fun buildSettlements(
        balanceByUser: Map<String, Long>,
        nameOf: Map<String, String>,
        money: (Long) -> String,
    ): List<SplitSettlementUi> = suggestSettlements(balanceByUser).map { suggestion ->
        SplitSettlementUi(
            fromUserId = suggestion.fromUserId,
            fromName = nameOf[suggestion.fromUserId].orEmpty(),
            toUserId = suggestion.toUserId,
            toName = nameOf[suggestion.toUserId].orEmpty(),
            amountCents = suggestion.amountCents,
            amountText = money(suggestion.amountCents),
        )
    }

    /** Item creator, split creator or group admin. */
    private fun canDelete(item: GroupExtensionItem, source: SplitSource, ownId: String?): Boolean =
        ownId != null && (
            item.createdBy == ownId ||
                source.extension.createdBy == ownId ||
                source.groupMembers.any { it.userId == ownId && it.admin }
            )

    fun onAction(action: SplitAction) {
        when (action) {
            SplitAction.OnBackClick -> viewModelScope.launch {
                navigator.navigateBack()
            }

            is SplitAction.OnTabSelected -> _state.update { it.copy(selectedTab = action.tab) }

            SplitAction.OnAddExpenseClick -> viewModelScope.launch {
                navigator.navigate(Route.SplitExpenseEditor(extensionId = extensionId))
            }

            is SplitAction.OnExpenseClick -> selectedItemId.value = action.itemId

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

            SplitAction.OnDetailDismiss -> selectedItemId.value = null

            // The popup stays open, so coming back from the editor shows the saved changes
            SplitAction.OnDetailEditClick -> {
                val itemId = selectedItemId.value ?: return
                viewModelScope.launch {
                    navigator.navigate(Route.SplitExpenseEditor(extensionId = extensionId, itemId = itemId))
                }
            }

            SplitAction.OnDetailDeleteClick -> _state.update { it.copy(isDeleteDialogVisible = true) }

            SplitAction.OnDeleteDismiss -> _state.update { it.copy(isDeleteDialogVisible = false) }

            SplitAction.OnDeleteConfirm -> deleteSelected()
        }
    }

    private fun deleteSelected() {
        val itemId = selectedItemId.value ?: return
        _state.update { it.copy(isDeleteDialogVisible = false) }

        viewModelScope.launch {
            val success = groupExtensionRepository.deleteItem(extensionId = extensionId, itemId = itemId)
                .reportErrorTo(errorChannel)
                .isSuccess
            if (success && selectedItemId.value == itemId) selectedItemId.value = null
        }
    }
}

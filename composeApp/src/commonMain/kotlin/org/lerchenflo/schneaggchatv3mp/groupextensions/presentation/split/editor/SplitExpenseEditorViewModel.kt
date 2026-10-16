package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.SplitExpense
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.shares
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.DEFAULT_SPLIT_CURRENCY
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.MAX_SPLIT_AMOUNT_CENTS
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.SplitMemberUi
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.currencySymbol
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.formatAmountNumber
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.formatMoney
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.isValidAmountInput
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.parseAmountToCents
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.resolveSplitMembers
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.millisToLocalDate
import org.lerchenflo.schneaggchatv3mp.utilities.toFormattedString
import org.lerchenflo.schneaggchatv3mp.utilities.today
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.split_amounts_complete
import schneaggchatv3mp.composeapp.generated.resources.split_amounts_left
import schneaggchatv3mp.composeapp.generated.resources.split_amounts_too_much
import schneaggchatv3mp.composeapp.generated.resources.split_decimal_separator
import schneaggchatv3mp.composeapp.generated.resources.split_delete_failed
import schneaggchatv3mp.composeapp.generated.resources.split_error_amount_required
import schneaggchatv3mp.composeapp.generated.resources.split_error_amount_too_large
import schneaggchatv3mp.composeapp.generated.resources.split_error_amounts_mismatch
import schneaggchatv3mp.composeapp.generated.resources.split_error_no_participants
import schneaggchatv3mp.composeapp.generated.resources.split_error_same_person
import schneaggchatv3mp.composeapp.generated.resources.split_error_title_required
import schneaggchatv3mp.composeapp.generated.resources.split_error_title_too_long
import schneaggchatv3mp.composeapp.generated.resources.split_item_not_found
import schneaggchatv3mp.composeapp.generated.resources.split_save_failed
import kotlin.time.ExperimentalTime

private const val MAX_TITLE_LENGTH = 100
private const val MAX_WEIGHT = 99
private const val MAX_EMOJI_LENGTH = 10

/**
 * Creates or edits one split entry: a shared expense, or a money transfer between two people
 * (opened from an existing transfer or prefilled from a settle-up suggestion).
 */
class SplitExpenseEditorViewModel(
    private val groupExtensionRepository: GroupExtensionRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val navigator: Navigator,

    private val route: Route.SplitExpenseEditor,
) : ViewModel() {

    private val _state = MutableStateFlow(SplitExpenseEditorState())
    val state = _state.asStateFlow()

    private var currency = DEFAULT_SPLIT_CURRENCY
    private var decimalSeparator = "."
    private var existingExpense: SplitExpense? = null

    // Errors are only shown once the user typed into a field (validated when leaving it)
    private var titleTouched = false
    private var amountTouched = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        decimalSeparator = getString(Res.string.split_decimal_separator)
        val ownId = SessionCache.requireLoggedIn()?.userId

        val extension = groupExtensionRepository.getExtensionFlow(route.extensionId).first()
        val existing = route.itemId?.let { itemId ->
            groupExtensionRepository.getItemsForExtensionFlow(route.extensionId).first()
                .find { it.id == itemId }?.data as? SplitExpense
        }
        // Deleted meanwhile (e.g. by another member)
        if (extension == null || (route.itemId != null && existing == null)) {
            SnackbarManager.showMessage(getString(Res.string.split_item_not_found))
            navigator.navigateBack()
            return
        }

        existingExpense = existing
        currency = (extension.settings as? ExtensionSettings.Split)?.currency ?: DEFAULT_SPLIT_CURRENCY

        val groupMembers = groupRepository.getGroupMembers(extension.ownerId)
        val currentMemberIds = groupMembers.map { it.userId }.toSet()
        val transfer = route.transfer
        val referencedIds = existing?.let { listOf(it.paidBy) + it.participants }
            ?: transfer?.let { listOf(it.fromUserId, it.toUserId) }
            ?: emptyList()
        val members = resolveSplitMembers(
            userIds = groupMembers.map { it.userId } + referencedIds,
            groupMembers = groupMembers,
            userRepository = userRepository,
            ownId = ownId,
        )

        val isTransfer = existing?.isSettlement ?: (transfer != null)
        val paidBy = existing?.paidBy
            ?: transfer?.fromUserId
            ?: members.firstOrNull { it.isMe }?.userId
            ?: members.firstOrNull()?.userId
        val receiver = if (isTransfer) existing?.participants?.firstOrNull() ?: transfer?.toUserId else null
        val amountCents = existing?.amountCents ?: transfer?.amountCents
        val date = existing?.let { millisToLocalDate(it.date) } ?: today()

        _state.update {
            it.copy(
                isLoading = false,
                isNew = existing == null,
                isTransfer = isTransfer,
                emoji = existing?.emoji?.takeIf { e -> e.isNotBlank() },
                title = existing?.title.orEmpty(),
                amount = amountCents?.let { cents -> formatAmountNumber(cents, decimalSeparator) }.orEmpty(),
                currencySymbol = currencySymbol(currency),
                date = date,
                dateText = date.toFormattedString(),
                members = members,
                paidByUserId = paidBy,
                paidByName = members.nameOf(paidBy),
                receiverUserId = receiver,
                receiverName = members.nameOf(receiver),
                splitMode = when {
                    existing?.amounts != null -> SplitMode.BY_AMOUNT
                    existing?.weights != null -> SplitMode.BY_SHARES
                    else -> SplitMode.EQUALLY
                },
                participants = members.map { member ->
                    SplitParticipantUi(
                        member = member,
                        // New expenses are shared by every current member
                        isIncluded = existing?.participants?.contains(member.userId) ?: (member.userId in currentMemberIds),
                        weight = existing?.weights?.get(member.userId) ?: 1,
                        amountInput = existing?.amounts?.get(member.userId)
                            ?.let { cents -> formatAmountNumber(cents, decimalSeparator) }
                            .orEmpty(),
                        shareText = "",
                    )
                },
            ).recalculated()
        }
    }

    fun onAction(action: SplitExpenseEditorAction) {
        when (action) {
            SplitExpenseEditorAction.OnBackClick -> viewModelScope.launch {
                navigator.navigateBack()
            }

            SplitExpenseEditorAction.OnEmojiClick -> _state.update {
                it.copy(isEmojiPickerVisible = true, customEmojiInput = "")
            }

            SplitExpenseEditorAction.OnEmojiPickerDismiss -> _state.update { it.copy(isEmojiPickerVisible = false) }

            is SplitExpenseEditorAction.OnEmojiSelected -> _state.update {
                it.copy(emoji = action.emoji, isEmojiPickerVisible = false)
            }

            is SplitExpenseEditorAction.OnCustomEmojiChange -> {
                if (action.text.length <= MAX_EMOJI_LENGTH) {
                    _state.update { it.copy(customEmojiInput = action.text) }
                }
            }

            SplitExpenseEditorAction.OnCustomEmojiConfirm -> _state.update {
                val custom = it.customEmojiInput.trim()
                if (custom.isEmpty()) it else it.copy(emoji = custom, isEmojiPickerVisible = false)
            }

            SplitExpenseEditorAction.OnEmojiRemove -> _state.update {
                it.copy(emoji = null, isEmojiPickerVisible = false)
            }

            is SplitExpenseEditorAction.OnTitleChange -> {
                titleTouched = true
                _state.update {
                    it.copy(
                        title = action.title,
                        // Clear the error the moment the input becomes valid
                        titleError = it.titleError?.let { validateTitle(action.title) },
                    )
                }
            }

            SplitExpenseEditorAction.OnTitleFocusLost -> {
                if (titleTouched) _state.update { it.copy(titleError = validateTitle(it.title)) }
            }

            is SplitExpenseEditorAction.OnAmountChange -> {
                // Ignore keystrokes that can't become an amount (letters, a third decimal, ...)
                if (!isValidAmountInput(action.amount)) return
                amountTouched = true
                _state.update {
                    it.copy(
                        amount = action.amount,
                        amountError = it.amountError?.let { validateAmount(action.amount) },
                    ).recalculated()
                }
            }

            SplitExpenseEditorAction.OnAmountFocusLost -> {
                if (amountTouched) _state.update { it.copy(amountError = validateAmount(it.amount)) }
            }

            is SplitExpenseEditorAction.OnMemberPickerClick -> _state.update { it.copy(openMemberPicker = action.target) }

            SplitExpenseEditorAction.OnMemberPickerDismiss -> _state.update { it.copy(openMemberPicker = null) }

            is SplitExpenseEditorAction.OnMemberPicked -> _state.update {
                val updated = when (action.target) {
                    MemberPickerTarget.PAID_BY -> it.copy(
                        paidByUserId = action.userId,
                        paidByName = it.members.nameOf(action.userId),
                    )
                    MemberPickerTarget.RECEIVER -> it.copy(
                        receiverUserId = action.userId,
                        receiverName = it.members.nameOf(action.userId),
                    )
                }
                updated.copy(
                    openMemberPicker = null,
                    receiverError = updated.receiverError?.let { validateReceiver(updated) },
                )
            }

            SplitExpenseEditorAction.OnDateClick -> _state.update { it.copy(isDatePickerVisible = true) }

            is SplitExpenseEditorAction.OnDateSelected -> _state.update {
                it.copy(
                    date = action.date,
                    dateText = action.date.toFormattedString(),
                    isDatePickerVisible = false,
                )
            }

            SplitExpenseEditorAction.OnDatePickerDismiss -> _state.update { it.copy(isDatePickerVisible = false) }

            is SplitExpenseEditorAction.OnSplitModeChange -> _state.update {
                val prefilled = if (action.mode == SplitMode.BY_AMOUNT) it.withAmountsPrefilled() else it
                prefilled.copy(splitMode = action.mode).let { updated ->
                    updated.copy(participantsError = updated.participantsError?.let { validateParticipants(updated) })
                }.recalculated()
            }

            is SplitExpenseEditorAction.OnParticipantToggle -> _state.update {
                val updated = it.copy(
                    participants = it.participants.map { participant ->
                        if (participant.member.userId == action.userId) {
                            participant.copy(isIncluded = !participant.isIncluded)
                        } else participant
                    }
                )
                updated.copy(
                    participantsError = updated.participantsError?.let { validateParticipants(updated) },
                ).recalculated()
            }

            is SplitExpenseEditorAction.OnParticipantWeightChange -> _state.update {
                it.copy(
                    participants = it.participants.map { participant ->
                        if (participant.member.userId == action.userId) {
                            participant.copy(weight = action.weight.coerceIn(1, MAX_WEIGHT))
                        } else participant
                    }
                ).recalculated()
            }

            is SplitExpenseEditorAction.OnParticipantAmountChange -> {
                if (!isValidAmountInput(action.amount)) return
                _state.update {
                    val updated = it.copy(
                        participants = it.participants.map { participant ->
                            if (participant.member.userId == action.userId) {
                                participant.copy(amountInput = action.amount)
                            } else participant
                        }
                    )
                    updated.copy(
                        participantsError = updated.participantsError?.let { validateParticipants(updated) },
                    ).recalculated()
                }
            }

            SplitExpenseEditorAction.OnSaveClick -> save()

            SplitExpenseEditorAction.OnDeleteClick -> _state.update { it.copy(isDeleteDialogVisible = true) }

            SplitExpenseEditorAction.OnDeleteDismiss -> _state.update { it.copy(isDeleteDialogVisible = false) }

            SplitExpenseEditorAction.OnDeleteConfirm -> delete()
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isLoading || current.isSaving) return

        val titleError = if (current.isTransfer) null else validateTitle(current.title)
        val amountError = validateAmount(current.amount)
        val receiverError = if (current.isTransfer) validateReceiver(current) else null
        val participantsError = if (current.isTransfer) null else validateParticipants(current)

        if (listOf(titleError, amountError, receiverError, participantsError).any { it != null }) {
            _state.update {
                it.copy(
                    titleError = titleError,
                    amountError = amountError,
                    receiverError = receiverError,
                    participantsError = participantsError,
                )
            }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = groupExtensionRepository.upsertItem(
                extensionId = route.extensionId,
                itemId = route.itemId,
                data = current.toExpense(dateMillis = current.dateMillis()),
            )
            if (success) {
                navigator.navigateBack()
            } else {
                _state.update { it.copy(isSaving = false) }
                SnackbarManager.showMessage(getString(Res.string.split_save_failed))
            }
        }
    }

    private fun delete() {
        val itemId = route.itemId ?: return
        _state.update { it.copy(isDeleteDialogVisible = false) }

        viewModelScope.launch {
            if (groupExtensionRepository.deleteItem(itemId)) {
                navigator.navigateBack()
            } else {
                SnackbarManager.showMessage(getString(Res.string.split_delete_failed))
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun SplitExpenseEditorState.toExpense(dateMillis: Long): SplitExpense {
        val included = participants.filter { it.isIncluded }
        return SplitExpense(
            // Transfers carry no title, the UI labels them
            title = if (isTransfer) "" else title.trim(),
            amountCents = parseAmountToCents(amount) ?: 0L,
            paidBy = paidByUserId.orEmpty(),
            participants = if (isTransfer) listOfNotNull(receiverUserId) else included.map { it.member.userId },
            weights = if (!isTransfer && splitMode == SplitMode.BY_SHARES) {
                included.associate { it.member.userId to it.weight }
            } else null,
            amounts = if (!isTransfer && splitMode == SplitMode.BY_AMOUNT) {
                included.associate { it.member.userId to (parseAmountToCents(it.amountInput) ?: 0L) }
            } else null,
            date = dateMillis,
            isSettlement = isTransfer,
            emoji = if (isTransfer) null else emoji,
        )
    }

    /** Keeps the original timestamp while the day is unchanged, so editing never moves an expense within its day. */
    @OptIn(ExperimentalTime::class)
    private fun SplitExpenseEditorState.dateMillis(): Long {
        val pickedDate = date ?: today()
        val original = existingExpense?.date
        if (original != null && millisToLocalDate(original) == pickedDate) return original
        return pickedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    /** Fills in every participant's part of the current amount and, in [SplitMode.BY_AMOUNT], the unassigned rest. */
    private fun SplitExpenseEditorState.recalculated(): SplitExpenseEditorState {
        val shares = toExpense(dateMillis = 0L).shares()
        return copy(
            participants = participants.map { participant ->
                participant.copy(
                    shareText = shares[participant.member.userId]
                        ?.let { formatMoney(it, currency, decimalSeparator) }
                        .orEmpty()
                )
            },
            amountsStatus = if (splitMode == SplitMode.BY_AMOUNT) {
                val rest = unassignedCents()
                when {
                    rest > 0L -> UiText.StringResourceText(
                        Res.string.split_amounts_left,
                        arrayOf<Any>(formatMoney(rest, currency, decimalSeparator))
                    )
                    rest < 0L -> UiText.StringResourceText(
                        Res.string.split_amounts_too_much,
                        arrayOf<Any>(formatMoney(-rest, currency, decimalSeparator))
                    )
                    else -> UiText.StringResourceText(Res.string.split_amounts_complete)
                }
            } else null,
        )
    }

    /** Total minus what the included people got assigned in [SplitMode.BY_AMOUNT]; negative = too much. */
    private fun SplitExpenseEditorState.unassignedCents(): Long =
        (parseAmountToCents(amount) ?: 0L) -
                participants.filter { it.isIncluded }.sumOf { parseAmountToCents(it.amountInput) ?: 0L }

    /**
     * Entering [SplitMode.BY_AMOUNT] with nothing typed yet starts from the current split, so the
     * amounts already add up and the user only adjusts them.
     */
    private fun SplitExpenseEditorState.withAmountsPrefilled(): SplitExpenseEditorState {
        if (participants.any { it.isIncluded && it.amountInput.isNotBlank() }) return this

        val shares = toExpense(dateMillis = 0L).shares()
        return copy(
            participants = participants.map { participant ->
                participant.copy(
                    amountInput = shares[participant.member.userId]
                        ?.let { formatAmountNumber(it, decimalSeparator) }
                        .orEmpty()
                )
            }
        )
    }

    private fun validateTitle(title: String): UiText? = when {
        title.isBlank() -> UiText.StringResourceText(Res.string.split_error_title_required)
        title.trim().length > MAX_TITLE_LENGTH -> UiText.StringResourceText(Res.string.split_error_title_too_long)
        else -> null
    }

    private fun validateAmount(amount: String): UiText? {
        val cents = parseAmountToCents(amount)
        return when {
            cents == null || cents <= 0L -> UiText.StringResourceText(Res.string.split_error_amount_required)
            cents > MAX_SPLIT_AMOUNT_CENTS -> UiText.StringResourceText(Res.string.split_error_amount_too_large)
            else -> null
        }
    }

    private fun validateReceiver(state: SplitExpenseEditorState): UiText? =
        if (state.receiverUserId == null || state.receiverUserId == state.paidByUserId) {
            UiText.StringResourceText(Res.string.split_error_same_person)
        } else null

    private fun validateParticipants(state: SplitExpenseEditorState): UiText? = when {
        state.participants.none { it.isIncluded } -> UiText.StringResourceText(Res.string.split_error_no_participants)
        state.splitMode == SplitMode.BY_AMOUNT && state.unassignedCents() != 0L ->
            UiText.StringResourceText(Res.string.split_error_amounts_mismatch)
        else -> null
    }

    private fun List<SplitMemberUi>.nameOf(userId: String?): String =
        firstOrNull { it.userId == userId }?.name.orEmpty()
}

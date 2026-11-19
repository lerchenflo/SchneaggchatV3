package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.editor

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.split.SplitMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split.SplitMemberUi
import org.lerchenflo.schneaggchatv3mp.utilities.UiText

@Stable
data class SplitExpenseEditorState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    /** Money transfer between two people instead of a shared expense */
    val isTransfer: Boolean = false,

    /** Icon of the expense, null = default icon */
    val emoji: String? = null,
    val isEmojiPickerVisible: Boolean = false,
    /** Text field in the emoji picker for any emoji that isn't suggested */
    val customEmojiInput: String = "",

    val title: String = "",
    val titleError: UiText? = null,
    val amount: String = "",
    val amountError: UiText? = null,
    val currencySymbol: String = "",

    val dateTime: LocalDateTime? = null,
    val dateTimeText: String = "",

    val members: List<SplitMemberUi> = emptyList(),
    val paidByUserId: String? = null,
    val paidByName: String = "",
    /** Transfers only */
    val receiverUserId: String? = null,
    val receiverName: String = "",
    val receiverError: UiText? = null,

    /** Expenses only */
    val splitMode: SplitMode = SplitMode.EQUALLY,
    val participants: List<SplitParticipantUi> = emptyList(),
    val participantsError: UiText? = null,
    /** [SplitMode.BY_AMOUNT] only: how much of the total is still unassigned (or too much) */
    val amountsStatus: UiText? = null,

    val openMemberPicker: MemberPickerTarget? = null,
    val isDateTimePickerVisible: Boolean = false,
    val isSaving: Boolean = false,
)

enum class MemberPickerTarget { PAID_BY, RECEIVER }

data class SplitParticipantUi(
    val member: SplitMemberUi,
    val isIncluded: Boolean,
    /** Number of shares in [SplitMode.BY_SHARES] */
    val weight: Int,
    /** Typed amount in [SplitMode.BY_AMOUNT] */
    val amountInput: String,
    /** This person's part of the current amount, empty when not included */
    val shareText: String,
)

sealed interface SplitExpenseEditorAction {
    data object OnBackClick : SplitExpenseEditorAction

    data object OnEmojiClick : SplitExpenseEditorAction
    data object OnEmojiPickerDismiss : SplitExpenseEditorAction
    data class OnEmojiSelected(val emoji: String) : SplitExpenseEditorAction
    data class OnCustomEmojiChange(val text: String) : SplitExpenseEditorAction
    data object OnCustomEmojiConfirm : SplitExpenseEditorAction
    data object OnEmojiRemove : SplitExpenseEditorAction

    data class OnTitleChange(val title: String) : SplitExpenseEditorAction
    data object OnTitleFocusLost : SplitExpenseEditorAction
    data class OnAmountChange(val amount: String) : SplitExpenseEditorAction
    data object OnAmountFocusLost : SplitExpenseEditorAction

    data class OnMemberPickerClick(val target: MemberPickerTarget) : SplitExpenseEditorAction
    data object OnMemberPickerDismiss : SplitExpenseEditorAction
    data class OnMemberPicked(val target: MemberPickerTarget, val userId: String) : SplitExpenseEditorAction

    data object OnDateTimeClick : SplitExpenseEditorAction
    data class OnDateTimeSelected(val dateTime: LocalDateTime) : SplitExpenseEditorAction
    data object OnDateTimePickerDismiss : SplitExpenseEditorAction

    data class OnSplitModeChange(val mode: SplitMode) : SplitExpenseEditorAction
    data class OnParticipantToggle(val userId: String) : SplitExpenseEditorAction
    data class OnParticipantWeightChange(val userId: String, val weight: Int) : SplitExpenseEditorAction
    data class OnParticipantAmountChange(val userId: String, val amount: String) : SplitExpenseEditorAction

    data object OnSaveClick : SplitExpenseEditorAction
}

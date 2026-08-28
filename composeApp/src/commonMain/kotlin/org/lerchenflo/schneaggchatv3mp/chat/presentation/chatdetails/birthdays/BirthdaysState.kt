package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.birthdays

data class BirthdaysState(
    val entries: List<BirthdayEntry> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface BirthdaysAction {
    data class OnEntryClick(val entry: BirthdayEntry) : BirthdaysAction
    data object OnBackClick : BirthdaysAction
}

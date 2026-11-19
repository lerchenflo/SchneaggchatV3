package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.sharedtext

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.TextWriterRun

@Stable
data class SharedTextState(
    val title: String = "",
    val text: TextFieldValue = TextFieldValue(),
    /** Who wrote which part of [text], as consecutive runs covering it */
    val writerRuns: List<TextWriterRun> = emptyList(),
    /** Everyone who wrote a part of the text, the own user first */
    val authors: List<SharedTextAuthorUi> = emptyList(),
    val showAuthorColors: Boolean = true,
    val isLoading: Boolean = true,
    /** Loaded, but the extension is gone (deleted or never synced) */
    val extensionMissing: Boolean = false,
)

data class SharedTextAuthorUi(
    val userId: String,
    val name: String,
    /** The member's name color in the group as ARGB, 0 = none set */
    val color: Int,
    val isMe: Boolean,
)

sealed interface SharedTextAction {
    data object OnBackClick : SharedTextAction
    data class OnTextChange(val value: TextFieldValue) : SharedTextAction
    data object OnToggleAuthorColors : SharedTextAction
}

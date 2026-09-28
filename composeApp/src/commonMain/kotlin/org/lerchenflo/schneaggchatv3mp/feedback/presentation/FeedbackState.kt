package org.lerchenflo.schneaggchatv3mp.feedback.presentation

import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry

enum class FeedbackSort { TOP, NEW }

data class FeedbackState(
    val selectedType: FeedbackType = FeedbackType.FEATURE,
    val features: List<FeedbackEntry> = emptyList(),
    val bugs: List<FeedbackEntry> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val viewerIsAdmin: Boolean = false,

    val sort: FeedbackSort = FeedbackSort.TOP,
    val statusFilter: FeedbackStatus? = null,
    val tagFilter: FeedbackTag? = null,
    val onlyMine: Boolean = false,

    /** Sorted + filtered list of the selected tab, recomputed whenever an input changes. */
    val visibleEntries: List<FeedbackEntry> = emptyList(),
    /** Reference time for the "new" highlight, taken when the list was loaded. */
    val nowMillis: Long = 0L,

    val detail: FeedbackDetailState? = null,
    val isCreateDialogOpen: Boolean = false,
    val isCreating: Boolean = false,
)

data class FeedbackDetailState(
    /** List entry, shown right away while the full details are still loading. */
    val entry: FeedbackEntry,
    val detail: FeedbackEntryDetail? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val commentDraft: String = "",
    val isSendingComment: Boolean = false,
)

sealed interface FeedbackAction {
    data object OnBackClick : FeedbackAction
    data object OnRetryClick : FeedbackAction
    data class OnTypeSelected(val type: FeedbackType) : FeedbackAction
    data class OnSortSelected(val sort: FeedbackSort) : FeedbackAction
    data class OnStatusFilterSelected(val status: FeedbackStatus?) : FeedbackAction
    data class OnTagFilterSelected(val tag: FeedbackTag?) : FeedbackAction
    data object OnOnlyMineToggle : FeedbackAction

    /** Tapping the vote the user already has removes it again. */
    data class OnVoteClick(val entryId: String, val vote: FeedbackVote) : FeedbackAction

    data class OnEntryClick(val entry: FeedbackEntry) : FeedbackAction
    data object OnDetailDismiss : FeedbackAction
    data object OnDetailRetryClick : FeedbackAction
    data class OnCommentDraftChange(val text: String) : FeedbackAction
    data object OnSendCommentClick : FeedbackAction
    data class OnDeleteComment(val commentId: String) : FeedbackAction
    data class OnDeleteEntry(val entryId: String) : FeedbackAction
    data class OnStatusChange(val entryId: String, val status: FeedbackStatus) : FeedbackAction

    data object OnCreateClick : FeedbackAction
    data object OnCreateDismiss : FeedbackAction
    data class OnCreateSubmit(val entry: NewFeedbackEntry) : FeedbackAction
}

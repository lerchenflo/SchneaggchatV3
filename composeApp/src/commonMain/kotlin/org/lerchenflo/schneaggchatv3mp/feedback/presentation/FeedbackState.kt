package org.lerchenflo.schneaggchatv3mp.feedback.presentation

import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryChanges
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackSection
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTab
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry

enum class FeedbackSort { TOP, NEW, UPDATED, DISCUSSED }

data class FeedbackState(
    val selectedTab: FeedbackTab = FeedbackTab.REQUESTS,
    /** Chip picked on the Games tab. Opening the tab starts on Implemented. */
    val gamesSection: FeedbackSection = FeedbackSection.IMPLEMENTED,
    /** Every stored entry, features and bugs. */
    val entries: List<FeedbackEntry> = emptyList(),
    /** Only true while nothing is stored yet and the first sync runs. */
    val isLoading: Boolean = true,
    /** The sync failed and nothing is stored to show instead. */
    val loadFailed: Boolean = false,
    val viewerIsAdmin: Boolean = false,

    val sort: FeedbackSort = FeedbackSort.TOP,
    val statusFilter: FeedbackStatus? = null,
    val tagFilter: FeedbackTag? = null,
    val onlyMine: Boolean = false,

    /** Sorted + filtered list of the selected tab, recomputed whenever an input changes. */
    val visibleEntries: List<FeedbackEntry> = emptyList(),
    /** Leading entries of [visibleEntries] pinned as new (Top sort only); the rest follows by likes. */
    val pinnedEntryCount: Int = 0,
    /** Unfiltered number of entries per tab, for the tab labels. */
    val tabCounts: Map<FeedbackTab, Int> = emptyMap(),
    /** Unfiltered number of GAMES entries per section, for the chips of the Games tab. */
    val gamesSectionCounts: Map<FeedbackSection, Int> = emptyMap(),
    /** Features still waiting for the user's vote, shown on the feature quiz button. */
    val unvotedFeatureCount: Int = 0,
    /** Reference time for the two day pin of new own entries, taken when the board opened. */
    val nowMillis: Long = 0L,
    /**
     * Server time each tab was last opened at before this visit. The New / Updated labels of an entry
     * compare against its tab's value, so they stay for the whole visit although opening the tab
     * cleared its badge. Null until it is read.
     */
    val tabLastSeenBeforeVisit: Map<FeedbackTab, Long>? = null,
    /** Per tab: entries new or updated since that tab was last opened; shown as a badge on the tab. */
    val tabBadges: Map<FeedbackTab, Int> = emptyMap(),

    val detail: FeedbackDetailState? = null,
    /** Names of the entry creators and the open entry's comment authors, by user id. */
    val userNames: Map<String, UserIdentityName> = emptyMap(),
    val isCreateDialogOpen: Boolean = false,
    val isCreating: Boolean = false,
    /**
     * Entry the list scrolls to once it is in [visibleEntries]: the one the user just created, which
     * can land above the first visible row. Null when nothing is waiting.
     */
    val scrollToEntryId: String? = null,
) {
    /** Section of the shown list: the tab's own one, or on the Games tab the picked chip. */
    val section: FeedbackSection get() = selectedTab.section ?: gamesSection
}

data class FeedbackDetailState(
    /** Stored entry, shown right away while the full details are still loading. */
    val entry: FeedbackEntry,
    val detail: FeedbackEntryDetail? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val commentDraft: String = "",
    /** Sending a new comment or saving a comment edit. */
    val isSendingComment: Boolean = false,
    /** Own comment whose text is in the comment popup for editing; null while writing a new comment. */
    val editingCommentId: String? = null,
    /** The popup for writing or editing a comment is open. */
    val isCommentPopupOpen: Boolean = false,
    /** Draft of a new comment, put aside while a comment is edited and restored afterwards. */
    val draftBeforeCommentEdit: String = "",
    /** The edit form of the entry is open. Only possible once [detail] is loaded (it holds the location). */
    val isEditDialogOpen: Boolean = false,
    val isSavingEdit: Boolean = false,
)

sealed interface FeedbackAction {
    data object OnBackClick : FeedbackAction
    data object OnFeatureQuizClick : FeedbackAction
    data object OnRetryClick : FeedbackAction
    data class OnTabSelected(val tab: FeedbackTab) : FeedbackAction
    data class OnGamesSectionSelected(val section: FeedbackSection) : FeedbackAction
    data class OnSortSelected(val sort: FeedbackSort) : FeedbackAction
    data class OnStatusFilterSelected(val status: FeedbackStatus?) : FeedbackAction
    data class OnTagFilterSelected(val tag: FeedbackTag?) : FeedbackAction
    data object OnOnlyMineToggle : FeedbackAction

    /** Tapping the vote the user already has removes it again. */
    data class OnVoteClick(val entryId: String, val vote: FeedbackVote) : FeedbackAction

    data class OnEntryClick(val entry: FeedbackEntry) : FeedbackAction
    data object OnDetailDismiss : FeedbackAction
    data object OnDetailRetryClick : FeedbackAction
    /** Opens the comment popup with the new-comment draft. */
    data object OnWriteCommentClick : FeedbackAction
    data class OnCommentDraftChange(val text: String) : FeedbackAction
    data object OnSendCommentClick : FeedbackAction
    data class OnDeleteComment(val commentId: String) : FeedbackAction
    data class OnDeleteEntry(val entryId: String) : FeedbackAction
    data class OnStatusChange(val entryId: String, val status: FeedbackStatus) : FeedbackAction

    data object OnEditEntryClick : FeedbackAction
    data object OnEditEntryDismiss : FeedbackAction
    data class OnEditEntrySubmit(val changes: FeedbackEntryChanges) : FeedbackAction
    /** Opens the comment popup with the own comment's text; sending saves the edit. */
    data class OnEditCommentClick(val commentId: String) : FeedbackAction
    /** Closes the comment popup: a new comment's draft stays, an edit ends. */
    data object OnCommentPopupDismiss : FeedbackAction

    data object OnCreateClick : FeedbackAction
    data object OnCreateDismiss : FeedbackAction
    data class OnCreateSubmit(val entry: NewFeedbackEntry) : FeedbackAction
    /** The list scrolled to [FeedbackState.scrollToEntryId]. */
    data object OnScrolledToEntry : FeedbackAction
}

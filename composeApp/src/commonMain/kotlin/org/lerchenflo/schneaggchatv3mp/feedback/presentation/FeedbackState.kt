package org.lerchenflo.schneaggchatv3mp.feedback.presentation

import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry

/** Kind of entries a list shows. Features are split into open requests and implemented ones. */
enum class FeedbackSection(
    /** The entry type a new entry created from this section gets. */
    val createType: FeedbackType,
    /** Statuses the status filter offers in this section. Empty: the section has only one status. */
    val filterStatuses: List<FeedbackStatus>,
) {
    REQUESTS(FeedbackType.FEATURE, listOf(FeedbackStatus.REQUESTED, FeedbackStatus.PLANNED)),
    BUGS(FeedbackType.BUG, FeedbackStatus.forType(FeedbackType.BUG)),
    IMPLEMENTED(FeedbackType.FEATURE, emptyList());

    fun contains(entry: FeedbackEntry): Boolean = when (this) {
        REQUESTS -> entry.type == FeedbackType.FEATURE && entry.status != FeedbackStatus.IMPLEMENTED
        BUGS -> entry.type == FeedbackType.BUG
        IMPLEMENTED -> entry.type == FeedbackType.FEATURE && entry.status == FeedbackStatus.IMPLEMENTED
    }

    companion object {
        /** The section a new or changed entry shows up in. */
        fun of(entry: FeedbackEntry): FeedbackSection = entries.first { it.contains(entry) }
    }
}

/**
 * The four lists of the board. Entries tagged GAMES only show up in the Games tab, where chips
 * pick the section; every other tab is one fixed section without the GAMES entries.
 */
enum class FeedbackTab(
    /** The section this tab always shows. Null for GAMES: its chips pick the section. */
    val section: FeedbackSection?,
) {
    REQUESTS(FeedbackSection.REQUESTS),
    BUGS(FeedbackSection.BUGS),
    IMPLEMENTED(FeedbackSection.IMPLEMENTED),
    GAMES(null);

    fun contains(entry: FeedbackEntry): Boolean =
        if (this == GAMES) entry.isGamesEntry else !entry.isGamesEntry && section?.contains(entry) == true

    companion object {
        /** The tab a new or changed entry shows up in. */
        fun of(entry: FeedbackEntry): FeedbackTab = entries.first { it.contains(entry) }
    }
}

/** Games, game wishes and game bugs: the entries of the Games tab. */
internal val FeedbackEntry.isGamesEntry: Boolean get() = FeedbackTag.GAMES in tags

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
    /** Unfiltered number of entries per tab, for the tab labels. */
    val tabCounts: Map<FeedbackTab, Int> = emptyMap(),
    /** Unfiltered number of GAMES entries per section, for the chips of the Games tab. */
    val gamesSectionCounts: Map<FeedbackSection, Int> = emptyMap(),
    /** Reference time for the two day pin of new own entries, taken when the board opened. */
    val nowMillis: Long = 0L,
    /**
     * Server time the board was last opened at before this visit. The New / Updated labels compare
     * against it, so they stay for the whole visit although opening the board cleared the badge.
     * Null until it is read.
     */
    val lastSeenBeforeVisit: Long? = null,

    val detail: FeedbackDetailState? = null,
    val isCreateDialogOpen: Boolean = false,
    val isCreating: Boolean = false,
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
    val isSendingComment: Boolean = false,
)

sealed interface FeedbackAction {
    data object OnBackClick : FeedbackAction
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
    data class OnCommentDraftChange(val text: String) : FeedbackAction
    data object OnSendCommentClick : FeedbackAction
    data class OnDeleteComment(val commentId: String) : FeedbackAction
    data class OnDeleteEntry(val entryId: String) : FeedbackAction
    data class OnStatusChange(val entryId: String, val status: FeedbackStatus) : FeedbackAction

    data object OnCreateClick : FeedbackAction
    data object OnCreateDismiss : FeedbackAction
    data class OnCreateSubmit(val entry: NewFeedbackEntry) : FeedbackAction
}

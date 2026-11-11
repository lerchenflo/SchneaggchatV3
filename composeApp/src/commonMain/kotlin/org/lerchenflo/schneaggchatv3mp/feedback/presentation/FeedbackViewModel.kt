package org.lerchenflo.schneaggchatv3mp.feedback.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.isSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.onError
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.sendError
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackRepository
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.feedback_created
import kotlin.time.Clock

/**
 * Feature / bug board. The lists come from the local store, which a sync updates every time the
 * screen opens (and the global sync on app start and resume). Opening the board clears the home
 * screen badge; the New / Updated labels stay for this visit. The full details of an entry
 * (comments, location) are fetched live only when it is opened.
 */
class FeedbackViewModel(
    private val feedbackRepository: FeedbackRepository,
    private val navigator: Navigator,
    private val applicationScope: ApplicationScope,
    private val errorChannel: ErrorChannel,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackState(nowMillis = Clock.System.now().toEpochMilliseconds()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            feedbackRepository.getEntriesFlow().collect { entries -> onEntriesChanged(entries) }
        }
        viewModelScope.launch {
            feedbackRepository.getViewerIsAdminFlow().collect { isAdmin ->
                _state.update { it.copy(viewerIsAdmin = isAdmin) }
            }
        }
        viewModelScope.launch {
            // Read before refresh() saves the new "last seen" time
            val lastSeen = feedbackRepository.getLastSeen()
            _state.update { it.copy(lastSeenBeforeVisit = lastSeen) }
            refresh()
        }
    }

    fun onAction(action: FeedbackAction) {
        when (action) {
            FeedbackAction.OnBackClick -> viewModelScope.launch { navigator.navigateBack() }
            FeedbackAction.OnRetryClick -> refresh()

            // Status sets differ between the tabs, so a status filter can't carry over
            is FeedbackAction.OnTabSelected -> updateBoard {
                it.copy(
                    selectedTab = action.tab,
                    // Opening the Games tab starts on the implemented games
                    gamesSection = if (action.tab != it.selectedTab) FeedbackSection.IMPLEMENTED else it.gamesSection,
                    statusFilter = null,
                )
            }
            is FeedbackAction.OnGamesSectionSelected -> updateBoard {
                it.copy(gamesSection = action.section, statusFilter = null)
            }
            is FeedbackAction.OnSortSelected -> updateBoard { it.copy(sort = action.sort) }
            is FeedbackAction.OnStatusFilterSelected -> updateBoard { it.copy(statusFilter = action.status) }
            is FeedbackAction.OnTagFilterSelected -> updateBoard { it.copy(tagFilter = action.tag) }
            FeedbackAction.OnOnlyMineToggle -> updateBoard { it.copy(onlyMine = !it.onlyMine) }

            is FeedbackAction.OnVoteClick -> vote(action.entryId, action.vote)

            is FeedbackAction.OnEntryClick -> openDetail(action.entry)
            FeedbackAction.OnDetailDismiss -> _state.update { it.copy(detail = null) }
            FeedbackAction.OnDetailRetryClick -> _state.value.detail?.let { openDetail(it.entry) }
            is FeedbackAction.OnCommentDraftChange -> _state.update {
                it.copy(detail = it.detail?.copy(commentDraft = action.text))
            }
            FeedbackAction.OnSendCommentClick -> sendComment()
            is FeedbackAction.OnDeleteComment -> deleteComment(action.commentId)
            is FeedbackAction.OnDeleteEntry -> deleteEntry(action.entryId)
            is FeedbackAction.OnStatusChange -> changeStatus(action.entryId, action.status)

            FeedbackAction.OnCreateClick -> _state.update { it.copy(isCreateDialogOpen = true) }
            FeedbackAction.OnCreateDismiss -> _state.update { it.copy(isCreateDialogOpen = false) }
            is FeedbackAction.OnCreateSubmit -> create(action.entry)
        }
    }

    /**
     * Syncs, then marks everything as seen. In the app scope, so the badge is cleared also when
     * the user leaves the board before the sync is done. A failed sync still marks what is stored
     * (the time of the last successful sync), since the user saw it.
     */
    private fun refresh() {
        _state.update { it.copy(loadFailed = false) }
        applicationScope.launchOnMain {
            val synced = feedbackRepository.syncFeedback().isSuccess
            feedbackRepository.markAllSeen()
            _state.update { it.copy(isLoading = false, loadFailed = !synced && it.entries.isEmpty()) }
        }
    }

    private fun onEntriesChanged(entries: List<FeedbackEntry>) {
        updateBoard { state ->
            val openDetail = state.detail
            state.copy(
                entries = entries,
                isLoading = state.isLoading && entries.isEmpty(),
                loadFailed = state.loadFailed && entries.isEmpty(),
                // The open entry follows the store; it closes when the entry was deleted
                detail = openDetail?.let { detail ->
                    val fresh = entries.firstOrNull { it.id == detail.entry.id } ?: return@let null
                    detail.copy(entry = fresh, detail = detail.detail?.copy(entry = fresh))
                },
            )
        }
    }

    private fun vote(entryId: String, vote: FeedbackVote) {
        val entry = findEntry(entryId) ?: return
        val newVote = if (entry.myVote == vote) FeedbackVote.NONE else vote

        // The repository stores the guess at once and the server answer after it
        applicationScope.launchOnMain {
            feedbackRepository.vote(entry, newVote)
                .onError { errorChannel.sendError(it) }
        }
    }

    private fun openDetail(entry: FeedbackEntry) {
        _state.update { it.copy(detail = FeedbackDetailState(entry = entry)) }
        // Opening clears the "new comments" pill also offline; the details set the exact count again
        if (entry.isOwn) {
            applicationScope.launchOnMain { feedbackRepository.markCommentsSeen(entry.id, entry.commentCount) }
        }
        loadDetail(entry.id, showErrors = true)
    }

    /** Fetches the details of [entryId]; the fresh entry that comes with them is stored too. */
    private fun loadDetail(entryId: String, showErrors: Boolean) {
        viewModelScope.launch {
            feedbackRepository.getDetails(entryId)
                .onSuccess { detail ->
                    updateDetail(entryId) { it.copy(entry = detail.entry, detail = detail, isLoading = false, loadFailed = false) }
                }
                .onError {
                    if (showErrors) updateDetail(entryId) { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun sendComment() {
        val detailState = _state.value.detail ?: return
        val text = detailState.commentDraft.trim()
        if (text.isEmpty() || detailState.isSendingComment) return

        val entryId = detailState.entry.id
        updateDetail(entryId) { it.copy(isSendingComment = true) }
        applicationScope.launchOnMain {
            feedbackRepository.comment(entryId, text, isBug = detailState.entry.type == FeedbackType.BUG)
                .onSuccess { comment ->
                    updateDetail(entryId) { detailState ->
                        detailState.copy(
                            commentDraft = "",
                            isSendingComment = false,
                            detail = detailState.detail?.let { it.copy(comments = it.comments + comment) },
                        )
                    }
                    // Comment count and dev comment of the stored entry come from the server
                    loadDetail(entryId, showErrors = false)
                }
                .onError {
                    updateDetail(entryId) { it.copy(isSendingComment = false) }
                    errorChannel.sendError(it)
                }
        }
    }

    private fun deleteComment(commentId: String) {
        val entryId = _state.value.detail?.entry?.id ?: return
        applicationScope.launchOnMain {
            feedbackRepository.deleteComment(commentId)
                .onSuccess {
                    updateDetail(entryId) { detailState ->
                        detailState.copy(
                            detail = detailState.detail?.let { detail ->
                                detail.copy(comments = detail.comments.filterNot { it.id == commentId })
                            }
                        )
                    }
                    loadDetail(entryId, showErrors = false)
                }
                .onError { errorChannel.sendError(it) }
        }
    }

    private fun deleteEntry(entryId: String) {
        applicationScope.launchOnMain {
            feedbackRepository.deleteEntry(entryId)
                .onSuccess {
                    _state.update { state -> state.copy(detail = state.detail?.takeUnless { it.entry.id == entryId }) }
                }
                .onError { errorChannel.sendError(it) }
        }
    }

    private fun changeStatus(entryId: String, status: FeedbackStatus) {
        applicationScope.launchOnMain {
            feedbackRepository.setStatus(entryId, status)
                .onError { errorChannel.sendError(it) }
        }
    }

    private fun create(newEntry: NewFeedbackEntry) {
        if (_state.value.isCreating) return
        _state.update { it.copy(isCreating = true) }
        applicationScope.launchOnMain {
            feedbackRepository.create(newEntry)
                .onSuccess { created ->
                    updateBoard { state ->
                        state.copy(
                            // Jump to the tab (and on Games the chip) the entry landed in, so the
                            // user sees it pinned on top
                            selectedTab = FeedbackTab.of(created),
                            gamesSection = FeedbackSection.of(created),
                            statusFilter = null,
                            nowMillis = Clock.System.now().toEpochMilliseconds(),
                            isCreating = false,
                            isCreateDialogOpen = false,
                        )
                    }
                    SnackbarManager.showMessage(getString(Res.string.feedback_created))
                }
                .onError {
                    _state.update { it.copy(isCreating = false) }
                    errorChannel.sendError(it)
                }
        }
    }

    private fun findEntry(entryId: String): FeedbackEntry? =
        _state.value.entries.firstOrNull { it.id == entryId }

    /** Only touches the detail sheet if it still shows [entryId] - the user may have moved on. */
    private fun updateDetail(entryId: String, block: (FeedbackDetailState) -> FeedbackDetailState) {
        _state.update { state ->
            val detail = state.detail
            if (detail == null || detail.entry.id != entryId) state else state.copy(detail = block(detail))
        }
    }

    /** Every change to the lists or filters goes through here, so the derived lists stay in sync. */
    private fun updateBoard(block: (FeedbackState) -> FeedbackState) {
        _state.update { block(it).withVisibleEntries() }
    }
}

private fun FeedbackState.withVisibleEntries(): FeedbackState {
    // The Games tab hides the tag filter (every entry there is tagged GAMES), so it is ignored
    // there and applies again on the other tabs
    val activeTagFilter = tagFilter.takeIf { selectedTab != FeedbackTab.GAMES }
    val filtered = entries.filter { entry ->
        selectedTab.contains(entry) &&
            section.contains(entry) &&
            (statusFilter == null || entry.status == statusFilter) &&
            (activeTagFilter == null || activeTagFilter in entry.tags) &&
            (!onlyMine || entry.isOwn || entry.myVote != FeedbackVote.NONE)
    }
    val gamesEntries = entries.filter { it.isGamesEntry }
    return copy(
        visibleEntries = filtered.sortedForBoard(sort, nowMillis),
        tabCounts = FeedbackTab.entries.associateWith { tab -> entries.count { tab.contains(it) } },
        gamesSectionCounts = FeedbackSection.entries.associateWith { s -> gamesEntries.count { s.contains(it) } },
    )
}

/**
 * TOP: user-created entries younger than two days stay pinned on top (newest first), everything
 * else follows by score, best first. NEW: newest first. UPDATED: last activity (status change,
 * comment) first. DISCUSSED: most comments first. Only TOP pins.
 */
internal fun List<FeedbackEntry>.sortedForBoard(sort: FeedbackSort, nowMillis: Long): List<FeedbackEntry> =
    when (sort) {
        FeedbackSort.NEW -> sortedByDescending { it.createdAt }
        FeedbackSort.UPDATED -> sortedWith(compareByDescending<FeedbackEntry> { it.activityAt }.thenByDescending { it.createdAt })
        FeedbackSort.DISCUSSED -> sortedWith(compareByDescending<FeedbackEntry> { it.commentCount }.thenByDescending { it.activityAt })
        FeedbackSort.TOP -> {
            val (pinned, rest) = partition { it.isPinned(nowMillis) }
            pinned.sortedByDescending { it.createdAt } +
                rest.sortedWith(compareByDescending<FeedbackEntry> { it.score }.thenByDescending { it.createdAt })
        }
    }

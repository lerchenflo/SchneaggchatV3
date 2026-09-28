package org.lerchenflo.schneaggchatv3mp.feedback.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.onError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.onSuccess
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.toUiText
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.data.FeedbackRepository
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.feedback_created
import kotlin.time.Clock

/**
 * Feature / bug board. Both lists are fetched live from the server every time the screen opens and
 * only kept in memory here; the full details of an entry (description, comments) are fetched only
 * when it is opened.
 */
class FeedbackViewModel(
    private val feedbackRepository: FeedbackRepository,
    private val navigator: Navigator,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackState())
    val state = _state.asStateFlow()

    init {
        loadEntries()
    }

    fun onAction(action: FeedbackAction) {
        when (action) {
            FeedbackAction.OnBackClick -> viewModelScope.launch { navigator.navigateBack() }
            FeedbackAction.OnRetryClick -> loadEntries()

            // Status sets differ between features and bugs, so a status filter can't carry over
            is FeedbackAction.OnTypeSelected -> updateBoard {
                it.copy(selectedType = action.type, statusFilter = null)
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

    private fun loadEntries() {
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val features = async { feedbackRepository.getEntries(FeedbackType.FEATURE) }
            val bugs = async { feedbackRepository.getEntries(FeedbackType.BUG) }
            val featureResult = features.await()
            val bugResult = bugs.await()

            if (featureResult is NetworkResult.Success && bugResult is NetworkResult.Success) {
                updateBoard {
                    it.copy(
                        features = featureResult.data.entries,
                        bugs = bugResult.data.entries,
                        viewerIsAdmin = featureResult.data.viewerIsAdmin,
                        isLoading = false,
                        loadFailed = false,
                        nowMillis = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    private fun vote(entryId: String, vote: FeedbackVote) {
        val entry = findEntry(entryId) ?: return
        val newVote = if (entry.myVote == vote) FeedbackVote.NONE else vote

        // Optimistic: the buttons react at once, the server answer replaces the guess
        replaceEntry(entry.withVote(newVote))
        viewModelScope.launch {
            feedbackRepository.vote(entryId, newVote)
                .onSuccess { replaceEntry(it) }
                .onError {
                    replaceEntry(entry)
                    showError(it)
                }
        }
    }

    private fun openDetail(entry: FeedbackEntry) {
        _state.update { it.copy(detail = FeedbackDetailState(entry = entry)) }
        viewModelScope.launch {
            feedbackRepository.getDetails(entry.id)
                .onSuccess { detail ->
                    replaceEntry(detail.entry)
                    updateDetail(entry.id) { it.copy(detail = detail, isLoading = false, loadFailed = false) }
                }
                .onError {
                    updateDetail(entry.id) { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun sendComment() {
        val detailState = _state.value.detail ?: return
        val text = detailState.commentDraft.trim()
        if (text.isEmpty() || detailState.isSendingComment) return

        val entryId = detailState.entry.id
        updateDetail(entryId) { it.copy(isSendingComment = true) }
        viewModelScope.launch {
            feedbackRepository.comment(entryId, text, isBug = detailState.entry.type == FeedbackType.BUG)
                .onSuccess { comment ->
                    updateDetail(entryId) { detailState ->
                        detailState.copy(
                            commentDraft = "",
                            isSendingComment = false,
                            detail = detailState.detail?.let { it.copy(comments = it.comments + comment) },
                        )
                    }
                    findEntry(entryId)?.let { entry ->
                        replaceEntry(
                            entry.copy(
                                commentCount = entry.commentCount + 1,
                                latestDevComment = if (comment.isDevComment) comment.text else entry.latestDevComment,
                            )
                        )
                    }
                }
                .onError {
                    updateDetail(entryId) { it.copy(isSendingComment = false) }
                    showError(it)
                }
        }
    }

    private fun deleteComment(commentId: String) {
        val entryId = _state.value.detail?.entry?.id ?: return
        viewModelScope.launch {
            feedbackRepository.deleteComment(commentId)
                .onSuccess {
                    var remainingDevComment: String? = null
                    updateDetail(entryId) { detailState ->
                        val remaining = detailState.detail?.comments.orEmpty().filterNot { it.id == commentId }
                        remainingDevComment = remaining.lastOrNull { it.isDevComment }?.text
                        detailState.copy(detail = detailState.detail?.copy(comments = remaining))
                    }
                    findEntry(entryId)?.let { entry ->
                        replaceEntry(
                            entry.copy(
                                commentCount = (entry.commentCount - 1).coerceAtLeast(0),
                                latestDevComment = remainingDevComment,
                            )
                        )
                    }
                }
                .onError { showError(it) }
        }
    }

    private fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            feedbackRepository.deleteEntry(entryId)
                .onSuccess {
                    updateBoard { state ->
                        state.copy(
                            features = state.features.filterNot { it.id == entryId },
                            bugs = state.bugs.filterNot { it.id == entryId },
                            detail = state.detail?.takeUnless { it.entry.id == entryId },
                        )
                    }
                }
                .onError { showError(it) }
        }
    }

    private fun changeStatus(entryId: String, status: FeedbackStatus) {
        viewModelScope.launch {
            feedbackRepository.setStatus(entryId, status)
                .onSuccess { replaceEntry(it) }
                .onError { showError(it) }
        }
    }

    private fun create(newEntry: NewFeedbackEntry) {
        if (_state.value.isCreating) return
        _state.update { it.copy(isCreating = true) }
        viewModelScope.launch {
            feedbackRepository.create(newEntry)
                .onSuccess { created ->
                    updateBoard { state ->
                        state.copy(
                            features = if (created.type == FeedbackType.FEATURE) state.features + created else state.features,
                            bugs = if (created.type == FeedbackType.BUG) state.bugs + created else state.bugs,
                            // Jump to the tab the entry landed in, so the user sees it pinned on top
                            selectedType = created.type,
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
                    showError(it)
                }
        }
    }

    private fun findEntry(entryId: String): FeedbackEntry? =
        _state.value.let { state -> (state.features + state.bugs).firstOrNull { it.id == entryId } }

    /** Swap one entry everywhere it is shown: its list and, if open, the detail sheet. */
    private fun replaceEntry(updated: FeedbackEntry) {
        updateBoard { state ->
            state.copy(
                features = state.features.map { if (it.id == updated.id) updated else it },
                bugs = state.bugs.map { if (it.id == updated.id) updated else it },
                detail = state.detail?.let { detail ->
                    if (detail.entry.id != updated.id) detail
                    else detail.copy(
                        entry = updated,
                        detail = detail.detail?.copy(entry = updated),
                    )
                },
            )
        }
    }

    /** Only touches the detail sheet if it still shows [entryId] - the user may have moved on. */
    private fun updateDetail(entryId: String, block: (FeedbackDetailState) -> FeedbackDetailState) {
        _state.update { state ->
            val detail = state.detail
            if (detail == null || detail.entry.id != entryId) state else state.copy(detail = block(detail))
        }
    }

    /** Every change to the lists or filters goes through here, so [FeedbackState.visibleEntries] stays in sync. */
    private fun updateBoard(block: (FeedbackState) -> FeedbackState) {
        _state.update { block(it).withVisibleEntries() }
    }

    private suspend fun showError(error: NetworkingError) {
        val message = when (val text = error.toUiText()) {
            is UiText.DynamicString -> text.value
            is UiText.StringResourceText -> getString(text.resId, *text.args)
        }
        SnackbarManager.showMessage(message)
    }
}

private fun FeedbackState.withVisibleEntries(): FeedbackState {
    val source = if (selectedType == FeedbackType.FEATURE) features else bugs
    val filtered = source.filter { entry ->
        (statusFilter == null || entry.status == statusFilter) &&
            (tagFilter == null || tagFilter in entry.tags) &&
            (!onlyMine || entry.isOwn || entry.myVote != FeedbackVote.NONE)
    }
    return copy(visibleEntries = filtered.sortedForBoard(sort, nowMillis))
}

/**
 * TOP: user-created entries younger than two days stay pinned on top (newest first), everything
 * else follows by score, best first. NEW: newest first, no pinning.
 */
internal fun List<FeedbackEntry>.sortedForBoard(sort: FeedbackSort, nowMillis: Long): List<FeedbackEntry> =
    when (sort) {
        FeedbackSort.NEW -> sortedByDescending { it.createdAt }
        FeedbackSort.TOP -> {
            val (pinned, rest) = partition { it.isPinned(nowMillis) }
            pinned.sortedByDescending { it.createdAt } +
                rest.sortedWith(compareByDescending<FeedbackEntry> { it.score }.thenByDescending { it.createdAt })
        }
    }

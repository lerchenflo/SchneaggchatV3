package org.lerchenflo.schneaggchatv3mp.feedback.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.domain.map
import org.lerchenflo.schneaggchatv3mp.app.domain.onError
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.feedback.data.local.FeedbackLocalDataSource
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackTab
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.FeedbackRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCommentRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCommentEditRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCreateRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackEditRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackStatusRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackVoteRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.toFeedbackComment
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.toFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.toFeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.toNetworkFeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.toNetworkFeedbackTag
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.toNetworkFeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.toNetworkFeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackComment
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryChanges
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackRepository
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion

/**
 * [FeedbackRepository] on top of the local [FeedbackLocalDataSource] and the server via
 * [FeedbackRemoteDataSource]. The server sends no socket message for feedback, so the local store
 * only changes through [syncFeedback] (app start, resume, board open) and this user's own writes.
 * App version and platform are attached to bug reports and comments automatically.
 */
class OfflineFirstFeedbackRepository(
    private val feedbackLocalDataSource: FeedbackLocalDataSource,
    private val feedbackRemoteDataSource: FeedbackRemoteDataSource,
    private val preferencemanager: Preferencemanager,
    private val appVersion: AppVersion,
    private val loggingRepository: Logger,
) : FeedbackRepository {

    private val versionName get() = appVersion.getVersionName()
    private val platformName get() = appVersion.getDeviceType().name

    // The global sync and the board open can start a sync at the same time
    private val syncMutex = Mutex()

    override fun getEntriesFlow(): Flow<List<FeedbackEntry>> =
        combine(
            feedbackLocalDataSource.getAllFeedbackEntriesFlow(),
            feedbackLocalDataSource.getSeenCommentCountsFlow(),
        ) { entries, seenCounts ->
            entries.map { entry ->
                // No seen count yet: nothing is new (the baseline is written with the entry)
                val seen = seenCounts[entry.id] ?: entry.commentCount
                if (!entry.isOwn || entry.commentCount <= seen) entry
                else entry.copy(newCommentCount = entry.commentCount - seen)
            }
        }

    override fun getViewerIsAdminFlow(): Flow<Boolean> =
        preferencemanager.getFeedbackViewerIsAdminFlow()

    // ─── Sync (the FEEDBACK job of the global dataSync) ─────────────────────────

    override suspend fun syncFeedback(): EmptyDataResult<DataError> = syncMutex.withLock {
        SessionCache.requireLoggedIn() ?: return@withLock DataResult.Success(Unit)

        // The same client list goes with every page, as the server pages over its own diff
        val clientEntries = feedbackLocalDataSource.getFeedbackEntryChangeIds()
            .map { IdTimeStamp(it.id, it.updatedAt) }

        // Server time of page 0: read before any entry, so a change that lands while the pages
        // load is newer than it and still counts as unseen later
        var serverTime: Long? = null
        var viewerIsAdmin = false
        var page = 0
        while (true) {
            when (val response = feedbackRemoteDataSource.feedbackSync(clientEntries, page).trackConnectivity()) {
                is DataResult.Error -> {
                    loggingRepository.logWarning("Feedback sync error: ${response.error}")
                    return@withLock response
                }
                is DataResult.Success -> {
                    val data = response.data
                    if (serverTime == null) serverTime = data.serverTime
                    viewerIsAdmin = data.viewerIsAdmin
                    feedbackLocalDataSource.upsertFeedbackEntries(data.updatedEntries.map { it.toFeedbackEntry() })
                    feedbackLocalDataSource.deleteFeedbackEntries(data.deletedEntries)
                    if (!data.moreEntries) break
                    page++
                }
            }
        }

        serverTime.let { preferencemanager.saveFeedbackSyncResult(serverTime = it, viewerIsAdmin = viewerIsAdmin) }
        DataResult.Success(Unit)
    }

    // ─── Badge ────────────────────────────────────────────────────────────────

    override fun getUnseenCountFlow(): Flow<Int> =
        getTabUnseenCountsFlow().map { counts -> counts.values.sum() }.distinctUntilChanged()

    override fun getTabUnseenCountsFlow(): Flow<Map<FeedbackTab, Int>> =
        combine(
            feedbackLocalDataSource.getAllFeedbackEntriesFlow(),
            tabsLastSeenFlow(),
        ) { entries, lastSeen ->
            // Every entry is in exactly one tab, so the sum counts each entry once
            FeedbackTab.entries.associateWith { tab ->
                entries.count { tab.contains(it) && it.markSince(lastSeen.getValue(tab)) != null }
            }
        }.distinctUntilChanged()

    override suspend fun getTabsLastSeen(): Map<FeedbackTab, Long> = tabsLastSeenFlow().first()

    private fun tabsLastSeenFlow(): Flow<Map<FeedbackTab, Long>> =
        preferencemanager.getFeedbackTabsLastSeenFlow(FeedbackTab.entries.map { it.name })
            .map { byName -> FeedbackTab.entries.associateWith { byName[it.name] ?: 0L } }

    override suspend fun markTabSeen(tab: FeedbackTab) {
        // Server time, never the device clock: the marks compare server times
        val lastSyncServerTime = preferencemanager.getFeedbackLastSyncServerTime() ?: return
        if (lastSyncServerTime > getTabsLastSeen().getValue(tab)) {
            preferencemanager.saveFeedbackTabLastSeen(tab.name, lastSyncServerTime)
        }
    }

    // ─── Board CRUD ───────────────────────────────────────────────────────────
    // No socket broadcast for feedback: every write stores the server's answer itself.

    override suspend fun markCommentsSeen(entryId: String, commentCount: Int) =
        feedbackLocalDataSource.setSeenCommentCount(entryId, commentCount)

    override suspend fun getDetails(entryId: String): DataResult<FeedbackEntryDetail, DataError> =
        feedbackRemoteDataSource.getFeedbackDetails(entryId)
            .map { it.toFeedbackEntryDetail() }
            .onSuccess {
                feedbackLocalDataSource.upsertFeedbackEntries(listOf(it.entry))
                // The details show every comment, so all of them are seen now
                if (it.entry.isOwn) feedbackLocalDataSource.setSeenCommentCount(it.entry.id, it.entry.commentCount)
            }

    override suspend fun create(entry: NewFeedbackEntry): DataResult<FeedbackEntry, DataError> {
        val isBug = entry.type == FeedbackType.BUG
        return feedbackRemoteDataSource.createFeedback(
            FeedbackCreateRequest(
                type = entry.type.toNetworkFeedbackType(),
                title = entry.title.trim(),
                description = entry.description.trim(),
                tags = entry.tags.map { it.toNetworkFeedbackTag() },
                // "Where to find it" only makes sense for features
                location = entry.location?.trim()?.takeIf { !isBug && it.isNotEmpty() },
                appVersion = versionName.takeIf { isBug },
                platform = platformName.takeIf { isBug },
                premium = entry.isPremium && !isBug,
            )
        ).map { it.toFeedbackEntry() }
            .onSuccess { feedbackLocalDataSource.upsertFeedbackEntries(listOf(it)) }
    }

    override suspend fun edit(entry: FeedbackEntry, changes: FeedbackEntryChanges): DataResult<FeedbackEntry, DataError> =
        feedbackRemoteDataSource.editFeedback(
            FeedbackEditRequest(
                entryId = entry.id,
                type = changes.type.toNetworkFeedbackType(),
                title = changes.title.trim(),
                description = changes.description.trim(),
                tags = changes.tags.map { it.toNetworkFeedbackTag() },
                location = changes.storedLocation,
                premium = changes.premiumChange(entry),
            )
        ).map { it.toFeedbackEntry() }
            .onSuccess { feedbackLocalDataSource.upsertFeedbackEntries(listOf(it)) }

    override suspend fun vote(entry: FeedbackEntry, vote: FeedbackVote): DataResult<FeedbackEntry, DataError> {
        // Optimistic: the buttons react at once. The guess keeps the old updatedAt, so a sync
        // before the answer arrives still fetches the real counts.
        feedbackLocalDataSource.upsertFeedbackEntries(listOf(entry.withVote(vote)))
        return feedbackRemoteDataSource.voteFeedback(FeedbackVoteRequest(entryId = entry.id, vote = vote.toNetworkFeedbackVote()))
            .map { it.toFeedbackEntry() }
            .onSuccess { feedbackLocalDataSource.upsertFeedbackEntries(listOf(it)) }
            .onError { feedbackLocalDataSource.upsertFeedbackEntries(listOf(entry)) }
    }

    /** [isBug] attaches app version and platform, which only matter for bug reports. */
    override suspend fun comment(entryId: String, text: String, isBug: Boolean): DataResult<FeedbackComment, DataError> =
        feedbackRemoteDataSource.commentFeedback(
            FeedbackCommentRequest(
                entryId = entryId,
                text = text.trim(),
                appVersion = versionName.takeIf { isBug },
                platform = platformName.takeIf { isBug },
            )
        ).map { it.toFeedbackComment() }
            .onSuccess { feedbackLocalDataSource.incrementSeenCommentCount(entryId) }

    override suspend fun deleteEntry(entryId: String): EmptyDataResult<DataError> =
        feedbackRemoteDataSource.deleteFeedback(entryId)
            .onSuccess { feedbackLocalDataSource.deleteFeedbackEntries(listOf(entryId)) }

    override suspend fun deleteComment(commentId: String): EmptyDataResult<DataError> =
        feedbackRemoteDataSource.deleteFeedbackComment(commentId)

    override suspend fun editComment(commentId: String, text: String): DataResult<FeedbackComment, DataError> =
        feedbackRemoteDataSource.editFeedbackComment(FeedbackCommentEditRequest(commentId = commentId, text = text.trim()))
            .map { it.toFeedbackComment() }

    override suspend fun setStatus(entryId: String, status: FeedbackStatus): DataResult<FeedbackEntry, DataError> =
        feedbackRemoteDataSource.setFeedbackStatus(FeedbackStatusRequest(entryId = entryId, status = status.toNetworkFeedbackStatus()))
            .map { it.toFeedbackEntry() }
            .onSuccess { feedbackLocalDataSource.upsertFeedbackEntries(listOf(it)) }
}

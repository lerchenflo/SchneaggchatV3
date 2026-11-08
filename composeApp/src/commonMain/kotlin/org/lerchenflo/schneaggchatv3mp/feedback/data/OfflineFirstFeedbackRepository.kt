package org.lerchenflo.schneaggchatv3mp.feedback.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.FeedbackRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCommentRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCreateRequest
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
        feedbackLocalDataSource.getAllFeedbackEntriesFlow()

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

        serverTime?.let { preferencemanager.saveFeedbackSyncResult(serverTime = it, viewerIsAdmin = viewerIsAdmin) }
        DataResult.Success(Unit)
    }

    // ─── Badge ────────────────────────────────────────────────────────────────

    override fun getUnseenCountFlow(): Flow<Int> =
        combine(
            feedbackLocalDataSource.getAllFeedbackEntriesFlow(),
            preferencemanager.getFeedbackLastSeenFlow(),
        ) { entries, lastSeen ->
            entries.count { it.markSince(lastSeen) != null }
        }.distinctUntilChanged()

    override suspend fun getLastSeen(): Long =
        preferencemanager.getFeedbackLastSeenFlow().first()

    override suspend fun markAllSeen() {
        // Server time, never the device clock: the marks compare server times
        val lastSyncServerTime = preferencemanager.getFeedbackLastSyncServerTime() ?: return
        if (lastSyncServerTime > getLastSeen()) {
            preferencemanager.saveFeedbackLastSeen(lastSyncServerTime)
        }
    }

    // ─── Board CRUD ───────────────────────────────────────────────────────────
    // No socket broadcast for feedback: every write stores the server's answer itself.

    override suspend fun getDetails(entryId: String): DataResult<FeedbackEntryDetail, DataError> =
        feedbackRemoteDataSource.getFeedbackDetails(entryId)
            .map { it.toFeedbackEntryDetail() }
            .onSuccess { feedbackLocalDataSource.upsertFeedbackEntries(listOf(it.entry)) }

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

    override suspend fun deleteEntry(entryId: String): EmptyDataResult<DataError> =
        feedbackRemoteDataSource.deleteFeedback(entryId)
            .onSuccess { feedbackLocalDataSource.deleteFeedbackEntries(listOf(entryId)) }

    override suspend fun deleteComment(commentId: String): EmptyDataResult<DataError> =
        feedbackRemoteDataSource.deleteFeedbackComment(commentId)

    override suspend fun setStatus(entryId: String, status: FeedbackStatus): DataResult<FeedbackEntry, DataError> =
        feedbackRemoteDataSource.setFeedbackStatus(FeedbackStatusRequest(entryId = entryId, status = status.toNetworkFeedbackStatus()))
            .map { it.toFeedbackEntry() }
            .onSuccess { feedbackLocalDataSource.upsertFeedbackEntries(listOf(it)) }
}

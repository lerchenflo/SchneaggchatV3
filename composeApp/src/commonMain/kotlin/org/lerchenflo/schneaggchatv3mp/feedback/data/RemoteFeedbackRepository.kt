package org.lerchenflo.schneaggchatv3mp.feedback.data

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackCommentRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackCreateRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackStatusRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackVoteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toFeedbackComment
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toFeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.toFeedbackList
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.EmptyResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.map
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackComment
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntryDetail
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackList
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackRepository
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.FeedbackRemoteDataSource

/**
 * Talks straight to the server - the board is always fetched live and never stored in the
 * database. App version and platform are attached to bug reports and comments automatically.
 */
class RemoteFeedbackRepository(
    private val feedbackRemoteDataSource: FeedbackRemoteDataSource,
    private val appVersion: AppVersion,
) : FeedbackRepository {

    private val versionName get() = appVersion.getVersionName()
    private val platformName get() = appVersion.getDeviceType().name

    override suspend fun getEntries(type: FeedbackType): NetworkResult<FeedbackList, NetworkingError> =
        feedbackRemoteDataSource.getFeedbackList(type).map { it.toFeedbackList() }

    override suspend fun getDetails(entryId: String): NetworkResult<FeedbackEntryDetail, NetworkingError> =
        feedbackRemoteDataSource.getFeedbackDetails(entryId).map { it.toFeedbackEntryDetail() }

    override suspend fun create(entry: NewFeedbackEntry): NetworkResult<FeedbackEntry, NetworkingError> {
        val isBug = entry.type == FeedbackType.BUG
        return feedbackRemoteDataSource.createFeedback(
            FeedbackCreateRequest(
                type = entry.type,
                title = entry.title.trim(),
                description = entry.description.trim(),
                tags = entry.tags,
                // "Where to find it" only makes sense for features
                location = entry.location?.trim()?.takeIf { !isBug && it.isNotEmpty() },
                appVersion = versionName.takeIf { isBug },
                platform = platformName.takeIf { isBug },
            )
        ).map { it.toFeedbackEntry() }
    }

    override suspend fun vote(entryId: String, vote: FeedbackVote): NetworkResult<FeedbackEntry, NetworkingError> =
        feedbackRemoteDataSource.voteFeedback(FeedbackVoteRequest(entryId = entryId, vote = vote))
            .map { it.toFeedbackEntry() }

    /** [isBug] attaches app version and platform, which only matter for bug reports. */
    override suspend fun comment(entryId: String, text: String, isBug: Boolean): NetworkResult<FeedbackComment, NetworkingError> =
        feedbackRemoteDataSource.commentFeedback(
            FeedbackCommentRequest(
                entryId = entryId,
                text = text.trim(),
                appVersion = versionName.takeIf { isBug },
                platform = platformName.takeIf { isBug },
            )
        ).map { it.toFeedbackComment() }

    override suspend fun deleteEntry(entryId: String): EmptyResult<NetworkingError> =
        feedbackRemoteDataSource.deleteFeedback(entryId)

    override suspend fun deleteComment(commentId: String): EmptyResult<NetworkingError> =
        feedbackRemoteDataSource.deleteFeedbackComment(commentId)

    override suspend fun setStatus(entryId: String, status: FeedbackStatus): NetworkResult<FeedbackEntry, NetworkingError> =
        feedbackRemoteDataSource.setFeedbackStatus(FeedbackStatusRequest(entryId = entryId, status = status))
            .map { it.toFeedbackEntry() }
}

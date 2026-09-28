package org.lerchenflo.schneaggchatv3mp.feedback.data

import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
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
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.feedback.domain.NewFeedbackEntry
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion

/**
 * Talks straight to the server - the board is always fetched live and never stored in the
 * database. App version and platform are attached to bug reports and comments automatically.
 */
class FeedbackRepository(
    private val networkUtils: NetworkUtils,
    private val appVersion: AppVersion,
) {

    private val versionName get() = appVersion.getVersionName()
    private val platformName get() = appVersion.getDeviceType().name

    suspend fun getEntries(type: FeedbackType): NetworkResult<FeedbackList, NetworkingError> =
        networkUtils.getFeedbackList(type).map { it.toFeedbackList() }

    suspend fun getDetails(entryId: String): NetworkResult<FeedbackEntryDetail, NetworkingError> =
        networkUtils.getFeedbackDetails(entryId).map { it.toFeedbackEntryDetail() }

    suspend fun create(entry: NewFeedbackEntry): NetworkResult<FeedbackEntry, NetworkingError> {
        val isBug = entry.type == FeedbackType.BUG
        return networkUtils.createFeedback(
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

    suspend fun vote(entryId: String, vote: FeedbackVote): NetworkResult<FeedbackEntry, NetworkingError> =
        networkUtils.voteFeedback(FeedbackVoteRequest(entryId = entryId, vote = vote))
            .map { it.toFeedbackEntry() }

    /** [isBug] attaches app version and platform, which only matter for bug reports. */
    suspend fun comment(entryId: String, text: String, isBug: Boolean): NetworkResult<FeedbackComment, NetworkingError> =
        networkUtils.commentFeedback(
            FeedbackCommentRequest(
                entryId = entryId,
                text = text.trim(),
                appVersion = versionName.takeIf { isBug },
                platform = platformName.takeIf { isBug },
            )
        ).map { it.toFeedbackComment() }

    suspend fun deleteEntry(entryId: String): EmptyResult<NetworkingError> =
        networkUtils.deleteFeedback(entryId)

    suspend fun deleteComment(commentId: String): EmptyResult<NetworkingError> =
        networkUtils.deleteFeedbackComment(commentId)

    suspend fun setStatus(entryId: String, status: FeedbackStatus): NetworkResult<FeedbackEntry, NetworkingError> =
        networkUtils.setFeedbackStatus(FeedbackStatusRequest(entryId = entryId, status = status))
            .map { it.toFeedbackEntry() }
}

package org.lerchenflo.schneaggchatv3mp.feedback.domain

import org.lerchenflo.schneaggchatv3mp.datasource.network.util.EmptyResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/** The feedback board (feature requests and bug reports), always read live from the server. */
interface FeedbackRepository {

    suspend fun getEntries(type: FeedbackType): NetworkResult<FeedbackList, NetworkingError>

    suspend fun getDetails(entryId: String): NetworkResult<FeedbackEntryDetail, NetworkingError>

    suspend fun create(entry: NewFeedbackEntry): NetworkResult<FeedbackEntry, NetworkingError>

    suspend fun vote(entryId: String, vote: FeedbackVote): NetworkResult<FeedbackEntry, NetworkingError>

    /** [isBug] attaches app version and platform, which only matter for bug reports. */
    suspend fun comment(entryId: String, text: String, isBug: Boolean): NetworkResult<FeedbackComment, NetworkingError>

    suspend fun deleteEntry(entryId: String): EmptyResult<NetworkingError>

    suspend fun deleteComment(commentId: String): EmptyResult<NetworkingError>

    suspend fun setStatus(entryId: String, status: FeedbackStatus): NetworkResult<FeedbackEntry, NetworkingError>
}

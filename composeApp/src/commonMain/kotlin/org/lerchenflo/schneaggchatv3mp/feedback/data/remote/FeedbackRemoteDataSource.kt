package org.lerchenflo.schneaggchatv3mp.feedback.data.remote

import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCommentRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCommentResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCreateRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackEntryDetailResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackEntryResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackListResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackStatusRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackVoteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.NetworkFeedbackType

/** Server calls for the feedback board. */
interface FeedbackRemoteDataSource {

    suspend fun getFeedbackList(type: NetworkFeedbackType): NetworkResult<FeedbackListResponse, NetworkingError>

    suspend fun getFeedbackDetails(entryId: String): NetworkResult<FeedbackEntryDetailResponse, NetworkingError>

    suspend fun createFeedback(request: FeedbackCreateRequest): NetworkResult<FeedbackEntryResponse, NetworkingError>

    suspend fun voteFeedback(request: FeedbackVoteRequest): NetworkResult<FeedbackEntryResponse, NetworkingError>

    suspend fun commentFeedback(request: FeedbackCommentRequest): NetworkResult<FeedbackCommentResponse, NetworkingError>

    suspend fun deleteFeedback(entryId: String): NetworkResult<Unit, NetworkingError>

    suspend fun deleteFeedbackComment(commentId: String): NetworkResult<Unit, NetworkingError>

    suspend fun setFeedbackStatus(request: FeedbackStatusRequest): NetworkResult<FeedbackEntryResponse, NetworkingError>
}

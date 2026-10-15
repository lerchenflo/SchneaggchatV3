package org.lerchenflo.schneaggchatv3mp.feedback.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackCommentRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackCommentResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackCreateRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackEntryDetailResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackEntryResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackListResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackStatusRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses.FeedbackVoteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType

/** Server calls for the feedback board. */
interface FeedbackRemoteDataSource {

    suspend fun getFeedbackList(type: FeedbackType): NetworkResult<FeedbackListResponse, NetworkingError>

    suspend fun getFeedbackDetails(entryId: String): NetworkResult<FeedbackEntryDetailResponse, NetworkingError>

    suspend fun createFeedback(request: FeedbackCreateRequest): NetworkResult<FeedbackEntryResponse, NetworkingError>

    suspend fun voteFeedback(request: FeedbackVoteRequest): NetworkResult<FeedbackEntryResponse, NetworkingError>

    suspend fun commentFeedback(request: FeedbackCommentRequest): NetworkResult<FeedbackCommentResponse, NetworkingError>

    suspend fun deleteFeedback(entryId: String): NetworkResult<Unit, NetworkingError>

    suspend fun deleteFeedbackComment(commentId: String): NetworkResult<Unit, NetworkingError>

    suspend fun setFeedbackStatus(request: FeedbackStatusRequest): NetworkResult<FeedbackEntryResponse, NetworkingError>
}

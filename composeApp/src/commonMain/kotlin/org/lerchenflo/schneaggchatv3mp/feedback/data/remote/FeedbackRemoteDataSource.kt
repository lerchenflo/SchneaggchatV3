package org.lerchenflo.schneaggchatv3mp.feedback.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCommentRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCommentResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCreateRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackEntryDetailResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackEntryResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackStatusRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackVoteRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackSyncResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.IdTimeStamp

/** Server calls for the feedback board. */
interface FeedbackRemoteDataSource {

    /** One page of the delta sync: [entries] are all stored entries with their `updatedAt`. */
    suspend fun feedbackSync(entries: List<IdTimeStamp>, page: Int): DataResult<FeedbackSyncResponse, DataError>

    suspend fun getFeedbackDetails(entryId: String): DataResult<FeedbackEntryDetailResponse, DataError>

    suspend fun createFeedback(request: FeedbackCreateRequest): DataResult<FeedbackEntryResponse, DataError>

    suspend fun voteFeedback(request: FeedbackVoteRequest): DataResult<FeedbackEntryResponse, DataError>

    suspend fun commentFeedback(request: FeedbackCommentRequest): DataResult<FeedbackCommentResponse, DataError>

    suspend fun deleteFeedback(entryId: String): DataResult<Unit, DataError>

    suspend fun deleteFeedbackComment(commentId: String): DataResult<Unit, DataError>

    suspend fun setFeedbackStatus(request: FeedbackStatusRequest): DataResult<FeedbackEntryResponse, DataError>
}

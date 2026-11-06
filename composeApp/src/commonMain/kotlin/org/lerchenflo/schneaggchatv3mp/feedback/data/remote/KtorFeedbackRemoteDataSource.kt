package org.lerchenflo.schneaggchatv3mp.feedback.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCommentRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCommentResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackCreateRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackEntryDetailResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackEntryResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackListResponse
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackStatusRequest
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.FeedbackVoteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.NetworkFeedbackType

/** [FeedbackRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorFeedbackRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : FeedbackRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun getFeedbackList(type: NetworkFeedbackType): DataResult<FeedbackListResponse, DataError> {
        return server.safeGet(endpoint = "/feedback/list?type=${type.name}")
    }

    override suspend fun getFeedbackDetails(entryId: String): DataResult<FeedbackEntryDetailResponse, DataError> {
        return server.safeGet(endpoint = "/feedback/details?entryid=$entryId")
    }

    override suspend fun createFeedback(request: FeedbackCreateRequest): DataResult<FeedbackEntryResponse, DataError> {
        return server.safePost(endpoint = "/feedback/create", body = request)
    }

    override suspend fun voteFeedback(request: FeedbackVoteRequest): DataResult<FeedbackEntryResponse, DataError> {
        return server.safePost(endpoint = "/feedback/vote", body = request)
    }

    override suspend fun commentFeedback(request: FeedbackCommentRequest): DataResult<FeedbackCommentResponse, DataError> {
        return server.safePost(endpoint = "/feedback/comment", body = request)
    }

    override suspend fun deleteFeedback(entryId: String): DataResult<Unit, DataError> {
        return server.safeDelete(endpoint = "/feedback/delete?entryid=$entryId")
    }

    override suspend fun deleteFeedbackComment(commentId: String): DataResult<Unit, DataError> {
        return server.safeDelete(endpoint = "/feedback/deletecomment?commentid=$commentId")
    }

    override suspend fun setFeedbackStatus(request: FeedbackStatusRequest): DataResult<FeedbackEntryResponse, DataError> {
        return server.safePost(endpoint = "/feedback/status", body = request)
    }
}

package org.lerchenflo.schneaggchatv3mp.feedback.data.remote

import io.ktor.client.HttpClient
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
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.dto.NetworkFeedbackType

/** [FeedbackRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorFeedbackRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : FeedbackRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun getFeedbackList(type: NetworkFeedbackType): NetworkResult<FeedbackListResponse, NetworkingError> {
        return server.safeGet(endpoint = "/feedback/list?type=${type.name}")
    }

    override suspend fun getFeedbackDetails(entryId: String): NetworkResult<FeedbackEntryDetailResponse, NetworkingError> {
        return server.safeGet(endpoint = "/feedback/details?entryid=$entryId")
    }

    override suspend fun createFeedback(request: FeedbackCreateRequest): NetworkResult<FeedbackEntryResponse, NetworkingError> {
        return server.safePost(endpoint = "/feedback/create", body = request)
    }

    override suspend fun voteFeedback(request: FeedbackVoteRequest): NetworkResult<FeedbackEntryResponse, NetworkingError> {
        return server.safePost(endpoint = "/feedback/vote", body = request)
    }

    override suspend fun commentFeedback(request: FeedbackCommentRequest): NetworkResult<FeedbackCommentResponse, NetworkingError> {
        return server.safePost(endpoint = "/feedback/comment", body = request)
    }

    override suspend fun deleteFeedback(entryId: String): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/feedback/delete?entryid=$entryId")
    }

    override suspend fun deleteFeedbackComment(commentId: String): NetworkResult<Unit, NetworkingError> {
        return server.safeDelete(endpoint = "/feedback/deletecomment?commentid=$commentId")
    }

    override suspend fun setFeedbackStatus(request: FeedbackStatusRequest): NetworkResult<FeedbackEntryResponse, NetworkingError> {
        return server.safePost(endpoint = "/feedback/status", body = request)
    }
}

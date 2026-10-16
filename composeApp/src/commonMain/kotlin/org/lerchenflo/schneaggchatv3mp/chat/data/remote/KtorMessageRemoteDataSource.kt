package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import io.ktor.client.HttpClient
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.network.ServerClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.appendFile
import org.lerchenflo.schneaggchatv3mp.datasource.network.appendJsonPart
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.AudioMessageRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.EditMessageRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.ImageMessageRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.MessageRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.MessageResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.MessageSyncResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollCreateRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollMessageRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollOptionDeleteRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollVoteRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.ReactionRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager

/** [MessageRemoteDataSource] over Ktor, against the server URL from [Preferencemanager]. */
class KtorMessageRemoteDataSource(
    httpClient: HttpClient,
    preferenceManager: Preferencemanager,
    loggingRepository: Logger,
) : MessageRemoteDataSource {

    private val server = ServerClient(httpClient, preferenceManager, loggingRepository)

    override suspend fun votePoll(pollVoteRequest: PollVoteRequest) : NetworkResult<MessageResponse, NetworkingError>{
        return server.safePost(
            endpoint = "/messages/pollvote",
            body = pollVoteRequest,
        )
    }

    override suspend fun deletePollOption(request: PollOptionDeleteRequest) : NetworkResult<MessageResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/messages/polloption/delete",
            body = request,
        )
    }

    override suspend fun sendTextMessageToServer(messageId: String?, empfaenger: String, gruppe: Boolean, content: String, answerid: String?, clientMessageId: String) : NetworkResult<MessageResponse, NetworkingError> {
        val messageRequest = MessageRequest(
            messageId = messageId,
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.TEXT,
            content = content,
            answerId = answerid,
            clientMessageId = clientMessageId,
        )

        println("MessageRequest: $messageRequest")

        return server.safePost(
            endpoint = "/messages/send/text",
            body = messageRequest
        )
    }

    override suspend fun sendImageMessageToServer(
        empfaenger: String,
        gruppe: Boolean,
        image: ByteArray,
        text: String,
        answerid: String?,
        clientMessageId: String
    ): NetworkResult<MessageResponse, NetworkingError> {

        val messageRequest = ImageMessageRequest(
            messageId = null,
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.IMAGE,
            content = text,
            answerId = answerid,
            clientMessageId = clientMessageId,
        )

        return server.safePostMultipart(
            endpoint = "/messages/send/image",
            parts = {
                appendFile(key = "image", bytes = image, fileName = "image.jpg", contentType = "image/jpeg")
                appendJsonPart(key = "request", serializer = ImageMessageRequest.serializer(), value = messageRequest)
            }
        )
    }

    override suspend fun sendAudioMessageToServer(
        empfaenger: String,
        gruppe: Boolean,
        audio: ByteArray,
        answerid: String?,
        clientMessageId: String
    ): NetworkResult<MessageResponse, NetworkingError> {

        val messageRequest = AudioMessageRequest(
            messageId = null,
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.AUDIO,
            answerId = answerid,
            clientMessageId = clientMessageId,
        )

        return server.safePostMultipart(
            endpoint = "/messages/send/audio",
            parts = {
                appendFile(key = "audio", bytes = audio, fileName = "message_audio.m4a", contentType = "audio/mp4")
                appendJsonPart(key = "request", serializer = AudioMessageRequest.serializer(), value = messageRequest)
            }
        )
    }

    override suspend fun sendPollMessageToServer(empfaenger: String, gruppe: Boolean, content: PollCreateRequest, answerid: String?, clientMessageId: String) : NetworkResult<MessageResponse, NetworkingError> {
        val pollRequest = PollMessageRequest(
            receiverId = empfaenger,
            groupMessage = gruppe,
            msgType = MessageType.POLL,
            answerId = answerid,
            poll = content,
            clientMessageId = clientMessageId,
        )

        println("PollMessageRequest: $pollRequest")

        return server.safePost(
            endpoint = "/messages/send/poll",
            body = pollRequest
        )
    }

    override suspend fun messageSync(since: Long) : NetworkResult<MessageSyncResponse, NetworkingError>{
        return server.safeGet(
            endpoint = "/messages/sync?since=$since&page_size=400",
        )
    }

    override suspend fun setMessagesRead(chatId: String, group: Boolean, timeStamp: Long) : NetworkResult<String, NetworkingError>{
        return server.safePost(
            endpoint = "/messages/setread?userid=$chatId&group=$group&timestamp=$timeStamp",
            body = "",
        )
    }

    override suspend fun editMessage(messageId: String, newContent: String): NetworkResult<MessageResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/messages/edit",
            body = EditMessageRequest(
                messageId = messageId,
                newContent = newContent
            )
        )
    }

    override suspend fun deleteMessage(messageId: String): NetworkResult<Any, NetworkingError> {
        return server.safeDelete(
            endpoint = "/messages/delete?messageid=$messageId"
        )
    }

    override suspend fun reactToMessage(messageId: String, content: String): NetworkResult<MessageResponse, NetworkingError> {
        return server.safePost(
            endpoint = "/messages/react",
            body = ReactionRequest(messageId = messageId, content = content)
        )
    }

    override suspend fun getImageForImageMessage(messageId: String) : NetworkResult<ByteArray, NetworkingError> {
        return server.safeGet(
            endpoint = "/messages/images/$messageId"
        )
    }

    override suspend fun getAudioForAudioMessage(messageId: String) : NetworkResult<ByteArray, NetworkingError> {
        return server.safeGet(
            endpoint = "/messages/audios/$messageId"
        )
    }
}

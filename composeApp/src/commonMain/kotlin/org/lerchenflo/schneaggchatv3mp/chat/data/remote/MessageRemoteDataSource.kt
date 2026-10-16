package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.MessageResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.MessageSyncResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollCreateRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollOptionDeleteRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollVoteRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

/** Server calls for messages: send, sync, read state, edit/delete/react, polls and media. */
interface MessageRemoteDataSource {

    suspend fun votePoll(pollVoteRequest: PollVoteRequest) : NetworkResult<MessageResponse, NetworkingError>

    suspend fun deletePollOption(request: PollOptionDeleteRequest) : NetworkResult<MessageResponse, NetworkingError>

    suspend fun sendTextMessageToServer(messageId: String?, empfaenger: String, gruppe: Boolean, content: String, answerid: String?, clientMessageId: String) : NetworkResult<MessageResponse, NetworkingError>

    suspend fun sendImageMessageToServer(
        empfaenger: String,
        gruppe: Boolean,
        image: ByteArray,
        text: String,
        answerid: String?,
        clientMessageId: String
    ): NetworkResult<MessageResponse, NetworkingError>

    suspend fun sendAudioMessageToServer(
        empfaenger: String,
        gruppe: Boolean,
        audio: ByteArray,
        answerid: String?,
        clientMessageId: String
    ): NetworkResult<MessageResponse, NetworkingError>

    suspend fun sendPollMessageToServer(empfaenger: String, gruppe: Boolean, content: PollCreateRequest, answerid: String?, clientMessageId: String) : NetworkResult<MessageResponse, NetworkingError>

    suspend fun messageSync(since: Long) : NetworkResult<MessageSyncResponse, NetworkingError>

    suspend fun setMessagesRead(chatId: String, group: Boolean, timeStamp: Long) : NetworkResult<String, NetworkingError>

    suspend fun editMessage(messageId: String, newContent: String): NetworkResult<MessageResponse, NetworkingError>

    suspend fun deleteMessage(messageId: String): NetworkResult<Any, NetworkingError>

    suspend fun reactToMessage(messageId: String, content: String): NetworkResult<MessageResponse, NetworkingError>

    suspend fun getImageForImageMessage(messageId: String) : NetworkResult<ByteArray, NetworkingError>

    suspend fun getAudioForAudioMessage(messageId: String) : NetworkResult<ByteArray, NetworkingError>
}

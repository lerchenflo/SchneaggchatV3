package org.lerchenflo.schneaggchatv3mp.chat.data.remote

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.MessageResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.MessageSyncResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollCreateRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollOptionDeleteRequest
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.PollVoteRequest

/** Server calls for messages: send, sync, read state, edit/delete/react, polls and media. */
interface MessageRemoteDataSource {

    suspend fun votePoll(pollVoteRequest: PollVoteRequest) : DataResult<MessageResponse, DataError>

    suspend fun deletePollOption(request: PollOptionDeleteRequest) : DataResult<MessageResponse, DataError>

    suspend fun sendTextMessageToServer(messageId: String?, empfaenger: String, gruppe: Boolean, content: String, answerid: String?, clientMessageId: String) : DataResult<MessageResponse, DataError>

    suspend fun sendImageMessageToServer(
        empfaenger: String,
        gruppe: Boolean,
        image: ByteArray,
        text: String,
        answerid: String?,
        clientMessageId: String
    ): DataResult<MessageResponse, DataError>

    suspend fun sendAudioMessageToServer(
        empfaenger: String,
        gruppe: Boolean,
        audio: ByteArray,
        answerid: String?,
        clientMessageId: String
    ): DataResult<MessageResponse, DataError>

    suspend fun sendPollMessageToServer(empfaenger: String, gruppe: Boolean, content: PollCreateRequest, answerid: String?, clientMessageId: String) : DataResult<MessageResponse, DataError>

    suspend fun messageSync(since: Long) : DataResult<MessageSyncResponse, DataError>

    suspend fun setMessagesRead(chatId: String, group: Boolean, timeStamp: Long) : DataResult<String, DataError>

    suspend fun editMessage(messageId: String, newContent: String): DataResult<MessageResponse, DataError>

    suspend fun deleteMessage(messageId: String): DataResult<Any, DataError>

    suspend fun reactToMessage(messageId: String, content: String): DataResult<MessageResponse, DataError>

    suspend fun getImageForImageMessage(messageId: String) : DataResult<ByteArray, DataError>

    suspend fun getAudioForAudioMessage(messageId: String) : DataResult<ByteArray, DataError>
}

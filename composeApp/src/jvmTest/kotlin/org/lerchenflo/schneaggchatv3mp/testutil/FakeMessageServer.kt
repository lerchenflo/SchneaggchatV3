package org.lerchenflo.schneaggchatv3mp.testutil

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.lerchenflo.schneaggchatv3mp.datasource.network.AppJson
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.MessageRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.MessageResponse
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto.MessageSyncResponse

/** Server clock of the first stored message; later ones get +1 ms each. */
private const val SERVER_TIME_BASE = 1_700_000_000_000L

/** Server version of the first stored message; later ones get +1 each. */
private const val SERVER_VERSION_BASE = 100L

/**
 * The server's message endpoints, answered in memory through a Ktor [MockEngine] ([engine]):
 * - `POST /messages/send/text`: every body is recorded in [textSendRequests]; [onTextSend]
 *   decides the reply (default: [accept] it).
 * - `GET /messages/sync`: hands out the pages queued with [enqueueSyncPage], one per call. With
 *   no page queued it answers "nothing new".
 *
 * Any other request is recorded in [unexpectedRequests] and answered with 404. Tests check that
 * list is empty, so a path that starts calling a new endpoint fails loudly instead of quietly
 * getting a 404.
 *
 * Request handlers run on Ktor's IO threads, so all state is thread-safe.
 */
class FakeMessageServer(private val ownUserId: String) {

    /** What the server answers to one send. */
    sealed interface Reply {
        /** HTTP 200 with this message as body. */
        data class Ok(val message: MessageResponse) : Reply

        /** An HTTP error status; the client maps it to a `NetworkingError`. */
        data class Fail(val status: HttpStatusCode, val body: String = status.description) : Reply

        /** The connection breaks before a response arrives (the client sees an `IOException`). */
        data object ConnectionDrop : Reply
    }

    private val _textSendRequests = CopyOnWriteArrayList<MessageRequest>()
    private val _syncSinceParams = CopyOnWriteArrayList<Long>()
    private val _unexpectedRequests = CopyOnWriteArrayList<String>()
    private val storedMessages = CopyOnWriteArrayList<MessageResponse>()
    private val syncPages = ConcurrentLinkedQueue<MessageSyncResponse>()

    /** Decoded bodies of every `POST /messages/send/text`, in arrival order. */
    val textSendRequests: List<MessageRequest> get() = _textSendRequests.toList()

    /** The `since` parameter of every `GET /messages/sync`, in order. */
    val syncSinceParams: List<Long> get() = _syncSinceParams.toList()

    /** `METHOD url` of every request this fake does not serve. */
    val unexpectedRequests: List<String> get() = _unexpectedRequests.toList()

    /** Reply to the next text send. Swap it per test; it may suspend to hold the response back. */
    @Volatile
    var onTextSend: suspend (MessageRequest) -> Reply = { accept(it) }

    /**
     * Stores [request] the way the real server does and returns the stored copy:
     * - a new server id (`srv-1`, `srv-2`, …), server clock and server version;
     * - `clientMessageId` echoed back, as the server does for the sender's own messages
     *   (`MESSAGE_SEND_SYNC_REBUILD_PLAN.md` §1);
     * - a retry with an already stored `clientMessageId` returns the stored message instead of a
     *   second one (server-side send idempotency).
     */
    @Synchronized
    fun accept(request: MessageRequest): Reply.Ok {
        storedMessages.firstOrNull { it.clientMessageId == request.clientMessageId }
            ?.let { return Reply.Ok(it) }

        val number = storedMessages.size + 1
        val message = MessageResponse(
            messageId = "srv-$number",
            senderId = ownUserId,
            receiverId = request.receiverId,
            groupMessage = request.groupMessage,
            msgType = request.msgType,
            content = request.content,
            pollResponse = null,
            answerId = request.answerId,
            sendDate = SERVER_TIME_BASE + number,
            lastChanged = SERVER_TIME_BASE + number,
            deleted = false,
            readers = emptyList(),
            version = SERVER_VERSION_BASE + number,
            clientMessageId = request.clientMessageId,
        )
        storedMessages += message
        return Reply.Ok(message)
    }

    /** The stored message with server id [messageId]. */
    fun storedMessage(messageId: String): MessageResponse =
        storedMessages.single { it.messageId == messageId }

    /** Queues one `/messages/sync` page. By default its watermark is the highest version in it. */
    fun enqueueSyncPage(
        updated: List<MessageResponse>,
        deleted: List<String> = emptyList(),
        newVersion: Long = updated.maxOfOrNull { it.version } ?: 0L,
        moreMessages: Boolean = false,
    ) {
        syncPages += MessageSyncResponse(
            updatedMessages = updated,
            deletedMessages = deleted,
            newVersion = newVersion,
            moreMessages = moreMessages,
        )
    }

    /** Forgets the recorded requests (not the stored messages or queued pages). */
    fun clearRecordedRequests() {
        _textSendRequests.clear()
        _syncSinceParams.clear()
    }

    val engine: MockEngine = MockEngine { request ->
        val path = request.url.encodedPath
        when {
            request.method == HttpMethod.Post && path == "/messages/send/text" -> {
                val body = AppJson.instance.decodeFromString(
                    MessageRequest.serializer(),
                    request.body.toByteArray().decodeToString(),
                )
                _textSendRequests += body
                when (val reply = onTextSend(body)) {
                    is Reply.Ok -> respondJson(AppJson.instance.encodeToString(MessageResponse.serializer(), reply.message))
                    is Reply.Fail -> respond(reply.body, reply.status)
                    Reply.ConnectionDrop -> throw java.io.IOException("Connection reset (FakeMessageServer)")
                }
            }

            request.method == HttpMethod.Get && path == "/messages/sync" -> {
                val since = requireNotNull(request.url.parameters["since"]) { "sync without since" }.toLong()
                _syncSinceParams += since
                val page = syncPages.poll() ?: MessageSyncResponse(
                    updatedMessages = emptyList(),
                    deletedMessages = emptyList(),
                    newVersion = since,
                    moreMessages = false,
                )
                respondJson(AppJson.instance.encodeToString(MessageSyncResponse.serializer(), page))
            }

            else -> {
                _unexpectedRequests += "${request.method.value} ${request.url}"
                respond("Not served by FakeMessageServer", HttpStatusCode.NotFound)
            }
        }
    }

    private fun MockRequestHandleScope.respondJson(json: String): HttpResponseData =
        respond(
            content = json,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
}

package org.lerchenflo.schneaggchatv3mp.chat.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.matches
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.Message
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.testutil.FRIEND_ID
import org.lerchenflo.schneaggchatv3mp.testutil.FakeMessageServer.Reply
import org.lerchenflo.schneaggchatv3mp.testutil.OWN_USER_ID
import org.lerchenflo.schneaggchatv3mp.testutil.SendPathFixture
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private val UUID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

/**
 * Characterization tests for sending one text message (plan Phase 4.5): what lands in the local
 * table before and after the server answers, and what each kind of server error does to the row.
 * They pin today's behavior, including the odd parts (see the KDoc of each test).
 *
 * Today the code under test is `AppRepository.sendMessage`; after Phase 5 it is
 * `OfflineFirstMessageRepository.sendMessage` behind the same calls ([SendPathFixture]).
 */
class MessageSendTest {

    private lateinit var fixture: SendPathFixture

    @BeforeTest
    fun setUp() {
        resetGlobalState()
        fixture = SendPathFixture()
    }

    @AfterTest
    fun tearDown() {
        try {
            assertThat(fixture.server.unexpectedRequests).isEmpty()
        } finally {
            fixture.close()
            resetGlobalState()
        }
    }

    /**
     * Also pins two things the send path does on purpose or by accident:
     * - the server's `version` from the send response is not stored (the row keeps 0), so a send
     *   never moves the `/messages/sync` cursor (`MAX(version)`);
     * - the stored row loses its `clientMessageId` once the server copy is written (the success
     *   upsert does not copy it over). Harmless today, see `plans/Todo.md`.
     */
    @Test
    fun `text send inserts a pending row first, then writes the server copy onto that row`() =
        runTest(StandardTestDispatcher()) {
            var rowsWhileRequestInFlight: List<Message> = emptyList()
            fixture.server.onTextSend = { request ->
                rowsWhileRequestInFlight = fixture.storedMessages()
                fixture.server.accept(request)
            }

            fixture.sendText("Hello", answerId = "srv-older")

            val request = fixture.server.textSendRequests.single()
            assertThat(request.receiverId).isEqualTo(FRIEND_ID)
            assertThat(request.groupMessage).isFalse()
            assertThat(request.msgType).isEqualTo(MessageType.TEXT)
            assertThat(request.content).isEqualTo("Hello")
            assertThat(request.answerId).isEqualTo("srv-older")
            assertThat(request.messageId).isNull()
            assertThat(request.clientMessageId).matches(UUID_PATTERN)

            val pending = rowsWhileRequestInFlight.single()
            assertThat(pending.id).isNull()
            assertThat(pending.sent).isFalse()
            assertThat(pending.myMessage).isTrue()
            assertThat(pending.senderId).isEqualTo(OWN_USER_ID)
            assertThat(pending.receiverId).isEqualTo(FRIEND_ID)
            assertThat(pending.content).isEqualTo("Hello")
            assertThat(pending.answerId).isEqualTo("srv-older")
            assertThat(pending.clientMessageId).isEqualTo(request.clientMessageId)

            val serverCopy = fixture.server.storedMessage("srv-1")
            val row = fixture.storedMessages().single()
            assertThat(row.localPK).isEqualTo(pending.localPK)
            assertThat(row.id).isEqualTo("srv-1")
            assertThat(row.sent).isTrue()
            assertThat(row.myMessage).isTrue()
            assertThat(row.readByMe).isTrue()
            assertThat(row.content).isEqualTo("Hello")
            assertThat(row.answerId).isEqualTo("srv-older")
            assertThat(row.sendDate).isEqualTo(serverCopy.sendDate.toString())
            assertThat(row.version).isEqualTo(0L)
            assertThat(row.clientMessageId).isNull()
        }

    @Test
    fun `server error keeps the row queued with its clientMessageId and reports the error`() =
        runTest(StandardTestDispatcher()) {
            fixture.server.onTextSend = { Reply.Fail(HttpStatusCode.InternalServerError) }

            fixture.sendText("Hello")

            val request = fixture.server.textSendRequests.single()
            val row = fixture.storedMessages().single()
            assertThat(row.id).isNull()
            assertThat(row.sent).isFalse()
            assertThat(row.content).isEqualTo("Hello")
            assertThat(row.clientMessageId).isEqualTo(request.clientMessageId)

            fixture.errorChannel.errors.test {
                assertThat(awaitItem().error).isNotNull().isInstanceOf<NetworkingError.ServerError>()
            }
        }

    @Test
    fun `dropped connection keeps the row queued and reports no internet`() =
        runTest(StandardTestDispatcher()) {
            fixture.server.onTextSend = { Reply.ConnectionDrop }

            fixture.sendText("Hello")

            val row = fixture.storedMessages().single()
            assertThat(row.sent).isFalse()
            assertThat(row.id).isNull()
            fixture.errorChannel.errors.test {
                assertThat(awaitItem().error).isEqualTo(NetworkingError.NoInternetConnection)
            }
        }

    @Test
    fun `bad request deletes the local row`() = runTest(StandardTestDispatcher()) {
        fixture.server.onTextSend = { Reply.Fail(HttpStatusCode.BadRequest) }

        fixture.sendText("Hello")

        assertThat(fixture.server.textSendRequests).hasSize(1)
        assertThat(fixture.storedMessages()).isEmpty()
        fixture.errorChannel.errors.test {
            assertThat(awaitItem().error).isNotNull().isInstanceOf<NetworkingError.BadRequest>()
        }
    }

    @Test
    fun `conflict deletes the local row`() = runTest(StandardTestDispatcher()) {
        fixture.server.onTextSend = { Reply.Fail(HttpStatusCode.Conflict) }

        fixture.sendText("Hello")

        assertThat(fixture.server.textSendRequests).hasSize(1)
        assertThat(fixture.storedMessages()).isEmpty()
        fixture.errorChannel.errors.test {
            assertThat(awaitItem().error).isNotNull().isInstanceOf<NetworkingError.Conflict>()
        }
    }
}

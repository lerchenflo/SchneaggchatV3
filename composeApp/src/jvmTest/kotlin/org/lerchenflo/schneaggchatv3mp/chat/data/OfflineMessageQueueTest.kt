package org.lerchenflo.schneaggchatv3mp.chat.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageRequest
import org.lerchenflo.schneaggchatv3mp.testutil.FRIEND_ID
import org.lerchenflo.schneaggchatv3mp.testutil.FakeMessageServer.Reply
import org.lerchenflo.schneaggchatv3mp.testutil.OWN_USER_ID
import org.lerchenflo.schneaggchatv3mp.testutil.SendPathFixture
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Characterization tests for the offline queue (plan Phase 4.5): `sendOfflineMessages` resends
 * the unsent rows one by one, head first, and stops as soon as the head is still unsent after
 * an attempt.
 *
 * The queue is filled the way the app fills it: sends that fail with a retryable server error.
 *
 * The code under test is `OfflineFirstMessageRepository.sendOfflineMessages`
 * (`AppRepository.sendOfflineMessages` until Phase 5b), behind the call of [SendPathFixture].
 */
class OfflineMessageQueueTest {

    private lateinit var fixture: SendPathFixture

    @BeforeTest
    fun setUp() {
        resetGlobalState()
    }

    /**
     * `runTest` on a [StandardTestDispatcher] that first builds [fixture] inside the test, so the
     * repository's `externalScope` is this test's `backgroundScope` (plan R2).
     */
    private fun runSendPathTest(testBody: suspend TestScope.() -> Unit) = runTest(StandardTestDispatcher()) {
        fixture = SendPathFixture(this)
        testBody()
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
     * Queues one row per text through failed sends. Returns each text's clientMessageId and
     * leaves the server accepting sends again, with no recorded requests.
     */
    private suspend fun queueWhileServerIsDown(vararg texts: String): Map<String, String> {
        fixture.server.onTextSend = { Reply.Fail(HttpStatusCode.ServiceUnavailable) }
        texts.forEach { fixture.sendText(it) }

        val clientMessageIds = fixture.server.textSendRequests.associate { it.content to it.clientMessageId }
        assertThat(fixture.storedMessages().count { it.content in texts && !it.sent }).isEqualTo(texts.size)

        fixture.server.onTextSend = { fixture.server.accept(it) }
        fixture.server.clearRecordedRequests()
        return clientMessageIds
    }

    /**
     * "Oldest first" means lowest local row first. `getUnsentMessages()` has no ORDER BY, so this
     * order comes from SQLite walking the `sent` index (see `plans/Todo.md`).
     */
    @Test
    fun `drains the queue oldest row first and reuses each row's clientMessageId`() =
        runSendPathTest {
            val clientMessageIds = queueWhileServerIsDown("first", "second", "third")

            fixture.sendOfflineMessages()

            val retries = fixture.server.textSendRequests
            assertThat(retries.map { it.content }).containsExactly("first", "second", "third")
            retries.forEach { retry ->
                assertThat(retry.clientMessageId).isEqualTo(clientMessageIds.getValue(retry.content))
                assertThat(retry.receiverId).isEqualTo(FRIEND_ID)
                assertThat(retry.groupMessage).isFalse()
                assertThat(retry.messageId).isNull()
            }

            val rows = fixture.storedMessages()
            assertThat(rows.map { it.content }).containsExactly("first", "second", "third")
            assertThat(rows.map { it.id }).containsExactly("srv-1", "srv-2", "srv-3")
            assertThat(rows.all { it.sent }).isTrue()
        }

    @Test
    fun `stops at the first failing row and leaves it and every later row unsent`() =
        runSendPathTest {
            val clientMessageIds = queueWhileServerIsDown("first", "second", "third")
            fixture.server.onTextSend = { request ->
                if (request.content == "second") Reply.Fail(HttpStatusCode.InternalServerError)
                else fixture.server.accept(request)
            }

            fixture.sendOfflineMessages()

            assertThat(fixture.server.textSendRequests.map { it.content }).containsExactly("first", "second")

            val (first, second, third) = fixture.storedMessages()
            assertThat(first.content).isEqualTo("first")
            assertThat(first.sent).isTrue()
            assertThat(first.id).isEqualTo("srv-1")

            assertThat(second.content).isEqualTo("second")
            assertThat(second.sent).isFalse()
            assertThat(second.id).isNull()
            assertThat(second.clientMessageId).isEqualTo(clientMessageIds.getValue("second"))

            assertThat(third.content).isEqualTo("third")
            assertThat(third.sent).isFalse()
            assertThat(third.id).isNull()
            assertThat(third.clientMessageId).isEqualTo(clientMessageIds.getValue("third"))
        }

    /** A rejected head is deleted (like a fresh send), so the head changes and the drain goes on. */
    @Test
    fun `a row the server rejects with bad request is deleted and the drain continues`() =
        runSendPathTest {
            queueWhileServerIsDown("rejected", "fine")
            fixture.server.onTextSend = { request ->
                if (request.content == "rejected") Reply.Fail(HttpStatusCode.BadRequest)
                else fixture.server.accept(request)
            }

            fixture.sendOfflineMessages()

            assertThat(fixture.server.textSendRequests.map { it.content }).containsExactly("rejected", "fine")
            val row = fixture.storedMessages().single()
            assertThat(row.content).isEqualTo("fine")
            assertThat(row.sent).isTrue()
        }

    /** Such a row could never be sent and would block the queue forever, so it is deleted unsent. */
    @Test
    fun `a poll row whose poll no longer decodes is dropped without a request`() =
        runSendPathTest {
            fixture.database.messageDao().insertMessageDto(
                MessageDto(
                    msgType = MessageType.POLL,
                    poll = null,
                    senderId = OWN_USER_ID,
                    receiverId = FRIEND_ID,
                    sent = false,
                    myMessage = true,
                    readByMe = true,
                    clientMessageId = "broken-poll",
                )
            )
            queueWhileServerIsDown("after the poll")

            fixture.sendOfflineMessages()

            assertThat(fixture.server.textSendRequests.map { it.content }).containsExactly("after the poll")
            val row = fixture.storedMessages().single()
            assertThat(row.content).isEqualTo("after the poll")
            assertThat(row.sent).isTrue()
        }

    /**
     * While a send holds `sendMessageLock`, its row is still unsent. The drain waits for the lock
     * (polling every 15 ms) instead of picking that row up and sending it a second time.
     */
    @Test
    @OptIn(ExperimentalCoroutinesApi::class) // advanceTimeBy
    fun `drain waits for a send in flight instead of resending its row`() =
        runSendPathTest {
            val requestArrived = CompletableDeferred<MessageRequest>()
            val releaseResponse = CompletableDeferred<Unit>()
            fixture.server.onTextSend = { request ->
                requestArrived.complete(request)
                releaseResponse.await()
                fixture.server.accept(request)
            }

            val send = launch { fixture.sendText("Hello") }
            requestArrived.await()

            val drain = launch { fixture.sendOfflineMessages() }
            advanceTimeBy(1_000)
            assertThat(fixture.server.textSendRequests).hasSize(1)
            assertThat(fixture.storedMessages().single().sent).isFalse()

            releaseResponse.complete(Unit)
            send.join()
            drain.join()

            assertThat(fixture.server.textSendRequests).hasSize(1)
            val row = fixture.storedMessages().single()
            assertThat(row.id).isEqualTo("srv-1")
            assertThat(row.sent).isTrue()
        }
}

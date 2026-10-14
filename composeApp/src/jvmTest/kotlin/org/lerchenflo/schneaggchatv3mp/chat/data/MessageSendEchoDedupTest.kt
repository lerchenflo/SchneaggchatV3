package org.lerchenflo.schneaggchatv3mp.chat.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.MessageRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.testutil.FakeMessageServer.Reply
import org.lerchenflo.schneaggchatv3mp.testutil.OWN_USER_ID
import org.lerchenflo.schneaggchatv3mp.testutil.SendPathFixture
import org.lerchenflo.schneaggchatv3mp.testutil.loginTestUser
import org.lerchenflo.schneaggchatv3mp.testutil.resetGlobalState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Characterization tests for the send echo (plan Phase 4.5, `MESSAGE_SEND_SYNC_REBUILD_PLAN.md`):
 * the message sync brings our own sent message back, with its `clientMessageId` echoed. However
 * the sync and the send response interleave, the chat must end up with exactly one row.
 *
 * The code under test is `OfflineFirstMessageRepository.sendMessage` / `syncMessages` /
 * `sendOfflineMessages` (on `AppRepository` until Phase 5b, `syncMessages` was `messageIdSync`),
 * behind the calls of [SendPathFixture].
 */
class MessageSendEchoDedupTest {

    private lateinit var fixture: SendPathFixture

    @BeforeTest
    fun setUp() {
        resetGlobalState()
        loginTestUser(OWN_USER_ID) // syncMessages needs a session
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

    /** Also pins that the send left the sync cursor at 0: the sync asks `since=0`. */
    @Test
    fun `sync after the send response matches the row by server id`() =
        runSendPathTest {
            fixture.sendText("Hello")
            val sentRow = fixture.storedMessages().single()

            fixture.server.enqueueSyncPage(listOf(fixture.server.storedMessage("srv-1")))
            fixture.syncMessages()

            assertThat(fixture.server.syncSinceParams).containsExactly(0L)
            val row = fixture.storedMessages().single()
            assertThat(row.localPK).isEqualTo(sentRow.localPK)
            assertThat(row.id).isEqualTo("srv-1")
            assertThat(row.sent).isTrue()
            assertThat(row.content).isEqualTo("Hello")
            assertThat(row.version).isEqualTo(fixture.server.storedMessage("srv-1").version)
        }

    /**
     * The race the rebuild plan fixed: the sync pulls our message while the send still waits for
     * its response. The sync merges into the pending row by `clientMessageId`; the late send
     * response then finds that row by server id. The row keeps the sync's version (the send
     * response never writes its own).
     */
    @Test
    fun `sync before the send response merges into the pending row by clientMessageId`() =
        runSendPathTest {
            val requestArrived = CompletableDeferred<MessageRequest>()
            val releaseResponse = CompletableDeferred<Unit>()
            fixture.server.onTextSend = { request ->
                val stored = fixture.server.accept(request) // committed on the server ...
                requestArrived.complete(request)
                releaseResponse.await() // ... but the response is still on its way
                stored
            }

            val send = launch { fixture.sendText("Hello") }
            val request = requestArrived.await()

            val pending = fixture.storedMessages().single()
            assertThat(pending.sent).isFalse()
            assertThat(pending.id).isNull()
            assertThat(pending.clientMessageId).isEqualTo(request.clientMessageId)

            fixture.server.enqueueSyncPage(listOf(fixture.server.storedMessage("srv-1")))
            fixture.syncMessages()

            val merged = fixture.storedMessages().single()
            assertThat(merged.localPK).isEqualTo(pending.localPK)
            assertThat(merged.id).isEqualTo("srv-1")
            assertThat(merged.sent).isTrue()
            assertThat(merged.content).isEqualTo("Hello")

            releaseResponse.complete(Unit)
            send.join()

            val row = fixture.storedMessages().single()
            assertThat(row.localPK).isEqualTo(pending.localPK)
            assertThat(row.id).isEqualTo("srv-1")
            assertThat(row.sent).isTrue()
            assertThat(row.version).isEqualTo(fixture.server.storedMessage("srv-1").version)
            assertThat(fixture.server.textSendRequests).hasSize(1)
        }

    /**
     * The server stored the message but the response never arrived, so the row stays queued.
     * The next sync completes it by `clientMessageId`, and the offline queue has nothing left
     * to resend. Before the rebuild this row was orphaned and blocked the queue forever.
     */
    @Test
    fun `sync completes a send whose response was lost, so the offline queue sends nothing`() =
        runSendPathTest {
            fixture.server.onTextSend = { request ->
                fixture.server.accept(request)
                Reply.ConnectionDrop
            }

            fixture.sendText("Hello")

            val queued = fixture.storedMessages().single()
            assertThat(queued.sent).isFalse()
            assertThat(queued.id).isNull()
            fixture.errorChannel.errors.test {
                assertThat(awaitItem().error).isEqualTo(NetworkingError.NoInternetConnection)
            }

            fixture.server.enqueueSyncPage(listOf(fixture.server.storedMessage("srv-1")))
            fixture.syncMessages()

            val row = fixture.storedMessages().single()
            assertThat(row.localPK).isEqualTo(queued.localPK)
            assertThat(row.id).isEqualTo("srv-1")
            assertThat(row.sent).isTrue()
            assertThat(row.clientMessageId).isNull()

            fixture.sendOfflineMessages()

            assertThat(fixture.server.textSendRequests).hasSize(1)
            val afterDrain = fixture.storedMessages().single()
            assertThat(afterDrain.localPK).isEqualTo(queued.localPK)
            assertThat(afterDrain.sent).isTrue()
        }
}

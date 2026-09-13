package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.advance
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** R5, case 18. */
class TimeoutTest {

    @Test
    fun hangingRefreshResolvesRetryableAtTheRefreshTimeout() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        h.api.enqueueHang()

        val pending = async { h.manager.refresh(RefreshReason.Reactive401) }

        advance(11.seconds + 999.milliseconds)
        assertFalse(pending.isCompleted, "still waiting just before the 12 s refresh timeout")

        advance(1.milliseconds)
        assertTrue(pending.isCompleted, "resolved at 12 s, far below the 30 s request timeout")

        val outcome = assertIs<RefreshOutcome.Retryable>(pending.await())
        assertIs<NetworkingError.NetworkTimeout>(outcome.error)
        assertIs<AuthSessionState.Degraded>(h.manager.state.value)
        assertTrue(h.store.stored != null, "a timeout keeps the credentials")
    }

    @Test
    fun timeoutArmsBackoffAndTheSchedulerRetries() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = (-1).minutes))
        h.api.enqueueHang()
        val fresh = h.pair()
        h.api.enqueueSuccess(fresh)

        val outcome = assertIs<RefreshOutcome.Retryable>(h.manager.refresh(RefreshReason.Reactive401))
        assertEquals(2.seconds, outcome.retryAfter)
        assertEquals(1, h.api.calls.size)

        advance(2.seconds)
        assertEquals(2, h.api.calls.size, "the manager's own timer retried after the backoff")
        assertEquals(fresh, h.store.stored)
        assertIs<AuthSessionState.Active>(h.manager.state.value)
    }
}

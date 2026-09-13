package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes.AuthTestHarness
import org.lerchenflo.schneaggchatv3mp.datasource.network.createAuthenticatedHttpClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * R9, D15, cases 1, 46: the real Ktor `Auth` plugin as configured in `createAuthenticatedHttpClient`,
 * against a `MockEngine` that plays the server, with the manager on fakes underneath.
 */
class KtorIntegrationTest {

    /**
     * A server that accepts exactly the access tokens in [validAccessTokens]. The engine runs on
     * the test scheduler: with a real dispatcher the test body would block on a thread and
     * `runTest` would skip virtual time into the manager's proactive timer meanwhile.
     */
    private class FakeServer(private val scope: TestScope) {
        val validAccessTokens = mutableSetOf<String>()

        fun engine() = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(scope.testScheduler)
            addHandler { request -> handle(request) }
        })

        private fun io.ktor.client.engine.mock.MockRequestHandleScope.handle(request: io.ktor.client.request.HttpRequestData) =
            when (request.url.encodedPath) {
                "/api/data" -> {
                    val presented = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
                    if (presented != null && presented in validAccessTokens) {
                        respond("""{"ok":true}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                    } else {
                        respond("", HttpStatusCode.Unauthorized)
                    }
                }
                "/api/limited" -> respond(
                    """{"error":"rate_limited","retryAfterSeconds":5}""",
                    HttpStatusCode.TooManyRequests,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
    }

    private fun MockEngine.authorizationHeaders(): List<String?> = requestHistory.map { it.headers[HttpHeaders.Authorization] }

    @Test
    fun one401TriggersOneRefreshAndOneRetry() = runTest {
        val h = AuthTestHarness(this)
        val p1 = h.storeSession(h.pair(accessValidFor = 10.hours))
        val server = FakeServer(this)                        // p1 unknown to the server: revoked / secret rotated
        val p2 = h.pair(accessValidFor = 10.hours)
        h.api.enqueue { server.validAccessTokens += p2.accessToken; NetworkResult.Success(p2) }
        val engine = server.engine()
        val client = createAuthenticatedHttpClient(engine, h.manager)

        val response = client.get("http://test/api/data")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf("Bearer ${p1.accessToken}", "Bearer ${p2.accessToken}"), engine.authorizationHeaders())
        assertEquals(1, h.api.calls.size)
        assertEquals(p2, h.store.stored)
        client.close()
    }

    @Test
    fun aSecond401OnTheRetryDoesNotLoop() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = 10.hours))
        val server = FakeServer(this)
        h.api.enqueueSuccess(h.pair(accessValidFor = 10.hours))   // server still rejects the new token
        val engine = server.engine()
        val client = createAuthenticatedHttpClient(engine, h.manager)

        val response = client.get("http://test/api/data")

        assertEquals(HttpStatusCode.Unauthorized, response.status, "surfaced to the caller, not turned into a logout (R9)")
        assertEquals(2, engine.requestHistory.size)
        assertEquals(1, h.api.calls.size)
        assertTrue(h.sink.invalidations.isEmpty())
        client.close()
    }

    @Test
    fun parallel401sShareOneRefresh() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = 10.hours))
        val server = FakeServer(this)
        val p2 = h.pair(accessValidFor = 10.hours)
        h.api.enqueue { server.validAccessTokens += p2.accessToken; NetworkResult.Success(p2) }
        val engine = server.engine()
        val client = createAuthenticatedHttpClient(engine, h.manager)

        val statuses = (1..5).map { async { client.get("http://test/api/data").status } }.awaitAll()

        assertTrue(statuses.all { it == HttpStatusCode.OK }, "$statuses")
        assertEquals(1, h.api.calls.size, "five 401s, one rotation")
        assertEquals(10, engine.requestHistory.size)
        client.close()
    }

    @Test
    fun a429OnANormalEndpointTouchesNothing() = runTest {
        val h = AuthTestHarness(this)
        val p1 = h.storeSession(h.pair(accessValidFor = 10.hours))
        val server = FakeServer(this)
        val engine = server.engine()
        val client = createAuthenticatedHttpClient(engine, h.manager)

        val response = client.get("http://test/api/limited")

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertTrue(h.api.calls.isEmpty())
        assertEquals(1, engine.requestHistory.size)
        assertEquals(p1, h.manager.currentTokens())
        assertEquals(0, h.backoff.attempt)
        client.close()
    }

    @Test
    fun tokensRotatedOutsideKtorAreUsedOnTheNextRequestWithoutA401() = runTest {
        val h = AuthTestHarness(this)
        val p1 = h.storeSession(h.pair(accessValidFor = 10.hours))
        val server = FakeServer(this).apply { validAccessTokens += p1.accessToken }
        val engine = server.engine()
        val client = createAuthenticatedHttpClient(engine, h.manager)
        assertEquals(HttpStatusCode.OK, client.get("http://test/api/data").status)

        // Socket handshake path / scheduler rotates while Ktor is idle; the server forgets p1.
        val p2 = h.pair(accessValidFor = 10.hours)
        h.api.enqueue { server.validAccessTokens.clear(); server.validAccessTokens += p2.accessToken; NetworkResult.Success(p2) }
        assertEquals(RefreshOutcome.Success, h.manager.refresh(RefreshReason.Manual))

        assertEquals(HttpStatusCode.OK, client.get("http://test/api/data").status)

        // No stale cache, no 401 round trip (D15).
        assertEquals(listOf("Bearer ${p1.accessToken}", "Bearer ${p2.accessToken}"), engine.authorizationHeaders())
        client.close()
    }

    @Test
    fun aFailedRefreshReturnsThe401ToTheCallerAndKeepsTheSession() = runTest {
        val h = AuthTestHarness(this)
        val p1 = h.storeSession(h.pair(accessValidFor = 10.hours))
        val server = FakeServer(this)                        // api fallback: offline
        val engine = server.engine()
        val client = createAuthenticatedHttpClient(engine, h.manager)

        val response = client.get("http://test/api/data")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(1, engine.requestHistory.size, "no retry with the same stale token")
        assertEquals(1, h.api.calls.size)
        assertEquals(p1, h.store.stored)
        assertIs<AuthSessionState.Degraded>(h.manager.state.value)
        client.close()
    }

    @Test
    fun aRejectedRefreshInvalidatesOnceAndLaterRequestsStaySilent() = runTest {
        val h = AuthTestHarness(this)
        h.storeSession(h.pair(accessValidFor = 10.hours))
        assertIs<SessionCheck.Active>(h.manager.ensureSession())
        val server = FakeServer(this)
        h.api.fallback = { NetworkResult.Error(NetworkingError.Unauthorized()) }
        val engine = server.engine()
        val client = createAuthenticatedHttpClient(engine, h.manager)

        assertEquals(HttpStatusCode.Unauthorized, client.get("http://test/api/data").status)
        assertNull(h.store.stored)
        assertEquals(listOf(InvalidationReason.RejectedByServer), h.sink.invalidations)

        // A request racing the logout: no token to send, 401, no session -> no second invalidation.
        assertEquals(HttpStatusCode.Unauthorized, client.get("http://test/api/data").status)
        assertNull(engine.requestHistory.last().headers[HttpHeaders.Authorization])
        assertEquals(1, h.sink.invalidations.size)
        assertEquals(1, h.api.calls.size)
        client.close()
    }

    @Test
    fun aSuccessfulRequestResetsTheRefreshBackoff() = runTest {
        val h = AuthTestHarness(this)
        val p1 = h.storeSession(h.pair(accessValidFor = 10.hours))
        val server = FakeServer(this).apply { validAccessTokens += p1.accessToken }
        h.manager.refresh(RefreshReason.Reactive401)      // offline fallback -> Degraded, attempt 1
        assertEquals(1, h.backoff.attempt)
        val engine = server.engine()
        val client = createAuthenticatedHttpClient(engine, h.manager)

        assertEquals(HttpStatusCode.OK, client.get("http://test/api/data").status)
        runCurrent()

        assertEquals(0, h.backoff.attempt, "R7: a working authenticated request resets the backoff")
        client.close()
    }
}

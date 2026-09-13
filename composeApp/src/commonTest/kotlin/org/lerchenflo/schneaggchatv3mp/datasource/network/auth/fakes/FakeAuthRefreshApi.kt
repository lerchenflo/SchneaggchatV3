package org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthRefreshApi
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

typealias RefreshResult = NetworkResult<TokenPair, NetworkingError>

/**
 * Scriptable `/auth/refresh`. Records every call with the presented token and the virtual time,
 * which is what proves single-flight and backoff timing.
 */
class FakeAuthRefreshApi(
    private val nowEpochMillis: () -> Long = { 0L },
) : AuthRefreshApi {

    data class Call(val refreshToken: String, val atEpochMillis: Long)

    val calls = mutableListOf<Call>()

    private val scripted = ArrayDeque<suspend () -> RefreshResult>()

    /** Used once the script is exhausted. Defaults to "offline". */
    var fallback: suspend () -> RefreshResult = { NetworkResult.Error(NetworkingError.NoInternetConnection) }

    fun enqueue(block: suspend () -> RefreshResult) {
        scripted += block
    }

    fun enqueueSuccess(pair: TokenPair) = enqueue { NetworkResult.Success(pair) }

    fun enqueueError(error: NetworkingError) = enqueue { NetworkResult.Error(error) }

    /** The call suspends until [gate] is completed - lets a test hold a refresh open. */
    fun enqueueGate(gate: CompletableDeferred<RefreshResult>) = enqueue { gate.await() }

    /** The call never returns on its own (only the manager's timeout ends it). */
    fun enqueueHang() = enqueue { awaitCancellation() }

    fun enqueueThrow(throwable: Throwable) = enqueue { throw throwable }

    override suspend fun refresh(refreshToken: String): RefreshResult {
        calls += Call(refreshToken, nowEpochMillis())
        val block = scripted.removeFirstOrNull() ?: fallback
        return block()
    }
}

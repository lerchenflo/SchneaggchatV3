package org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes

import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthEventSink
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.InvalidationReason
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

class RecordingAuthEventSink : AuthEventSink {
    val active = mutableListOf<TokenPair>()
    val invalidations = mutableListOf<InvalidationReason>()
    val unreachable = mutableListOf<NetworkingError>()

    override fun onSessionActive(tokens: TokenPair) {
        active += tokens
    }

    override fun onSessionInvalidated(reason: InvalidationReason) {
        invalidations += reason
    }

    override fun onServerUnreachable(error: NetworkingError) {
        unreachable += error
    }
}

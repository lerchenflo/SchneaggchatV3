package org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes

import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthEventSink
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.InvalidationReason
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

class RecordingAuthEventSink : AuthEventSink {
    val active = mutableListOf<TokenPair>()
    val invalidations = mutableListOf<InvalidationReason>()
    val unreachable = mutableListOf<NetworkingError>()

    /** When set, [onSessionActive] throws - models a derived view that fails to update. */
    var failOnSessionActive = false

    override fun onSessionActive(tokens: TokenPair) {
        if (failOnSessionActive) throw IllegalStateException("fake sink failure")
        active += tokens
    }

    override fun onSessionInvalidated(reason: InvalidationReason) {
        invalidations += reason
    }

    override fun onServerUnreachable(error: NetworkingError) {
        unreachable += error
    }
}

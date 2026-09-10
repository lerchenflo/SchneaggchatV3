package org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes

import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthSessionStore

class FakeAuthSessionStore(initial: TokenPair? = null) : AuthSessionStore {
    var stored: TokenPair? = initial

    var throwOnRead = false
    var throwOnWrite = false
    var throwOnClear = false

    var reads = 0
    var writes = 0
    var clears = 0
    val writtenPairs = mutableListOf<TokenPair>()

    override suspend fun read(): TokenPair? {
        reads++
        if (throwOnRead) throw IllegalStateException("fake storage read failure")
        return stored
    }

    override suspend fun write(tokens: TokenPair) {
        writes++
        if (throwOnWrite) throw IllegalStateException("fake storage write failure")
        stored = tokens
        writtenPairs += tokens
    }

    override suspend fun clear() {
        clears++
        if (throwOnClear) throw IllegalStateException("fake storage clear failure")
        stored = null
    }
}

package org.lerchenflo.schneaggchatv3mp.app.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError

//Errorchannel for global error events (Show in every screen)
class DefaultErrorChannel : ErrorChannel {

    private val _channel = Channel<ErrorEvent>(capacity = Channel.BUFFERED)
    override val errors: Flow<ErrorEvent> = _channel.receiveAsFlow()

    override suspend fun sendErrorSuspend(event: ErrorEvent) {
        if (event.isSessionError()) return
        _channel.send(event) // suspending send
    }

    override fun trySendError(event: ErrorEvent) {
        if (event.isSessionError()) return
        _channel.trySend(event).onFailure {
            println("Error when adding error event: $it")
        }
    }

    /**
     * A 401 from an authenticated request is the session's business, not the caller's: the
     * request already failed after the auth layer could not refresh (offline, backoff, server
     * busy) and the session stays logged in. Only a real invalidation shows a popup, together
     * with the logout (`AppAuthEventSink.onSessionInvalidated`).
     */
    private fun ErrorEvent.isSessionError(): Boolean = error is NetworkingError.Unauthorized
}

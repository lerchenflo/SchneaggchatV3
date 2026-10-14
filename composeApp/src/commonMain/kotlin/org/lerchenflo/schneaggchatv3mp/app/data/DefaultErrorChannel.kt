package org.lerchenflo.schneaggchatv3mp.app.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent

//Errorchannel for global error events (Show in every screen)
class DefaultErrorChannel : ErrorChannel {

    private val _channel = Channel<ErrorEvent>(capacity = Channel.BUFFERED)
    override val errors: Flow<ErrorEvent> = _channel.receiveAsFlow()

    override suspend fun sendErrorSuspend(event: ErrorEvent) {
        _channel.send(event) // suspending send
    }

    override fun trySendError(event: ErrorEvent) {
        _channel.trySend(event).onFailure {
            println("Error when adding error event: $it")
        }
    }
}

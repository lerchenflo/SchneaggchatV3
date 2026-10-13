package org.lerchenflo.schneaggchatv3mp.app.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ActionChannel
import org.lerchenflo.schneaggchatv3mp.datasource.domain.ActionEvent

class DefaultActionChannel : ActionChannel {

    private val _channel = Channel<ActionEvent>(capacity = Channel.BUFFERED)
    override val actions: Flow<ActionEvent> = _channel.receiveAsFlow()

    override suspend fun sendActionSuspend(event: ActionEvent) {
        _channel.send(event) // suspending send
    }

    override fun trySendAction(event: ActionEvent) {
        _channel.trySend(event).onFailure {
            println("Error when adding action event: $it")
        }
    }
}

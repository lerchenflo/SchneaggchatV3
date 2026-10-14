package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.lerchenflo.schneaggchatv3mp.app.domain.ActionChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ActionEvent

/**
 * [ActionChannel] that records every event in [sent] (whether or not anybody collects [actions]).
 * Unlike the real channel its buffer is unlimited, so a send never suspends or drops.
 */
class FakeActionChannel : ActionChannel {

    private val channel = Channel<ActionEvent>(Channel.UNLIMITED)

    val sent = CallLog<ActionEvent>()

    override val actions: Flow<ActionEvent> = channel.receiveAsFlow()

    override suspend fun sendActionSuspend(event: ActionEvent) = trySendAction(event)

    override fun trySendAction(event: ActionEvent) {
        sent.record(event)
        channel.trySend(event)
    }
}

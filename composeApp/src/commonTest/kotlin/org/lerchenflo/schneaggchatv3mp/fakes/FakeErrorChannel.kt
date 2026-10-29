package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent

/**
 * [ErrorChannel] that records every event in [sent] (whether or not anybody collects [errors]).
 * Unlike the real channel its buffer is unlimited, so a send never suspends or drops.
 */
class FakeErrorChannel : ErrorChannel {

    private val channel = Channel<ErrorEvent>(Channel.UNLIMITED)

    val sent = CallLog<ErrorEvent>()

    override val errors: Flow<ErrorEvent> = channel.receiveAsFlow()

    override suspend fun sendErrorSuspend(event: ErrorEvent) = trySendError(event)

    override fun trySendError(event: ErrorEvent) {
        sent.record(event)
        channel.trySend(event)
    }
}

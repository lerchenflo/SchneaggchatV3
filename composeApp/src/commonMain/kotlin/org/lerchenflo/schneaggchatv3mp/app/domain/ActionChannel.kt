package org.lerchenflo.schneaggchatv3mp.app.domain

import kotlinx.coroutines.flow.Flow

/** App-wide stream of action events that `App.kt` reacts to (e.g. logout on [ActionEvent.AuthInvalidated]). */
interface ActionChannel {
    val actions: Flow<ActionEvent>

    suspend fun sendActionSuspend(event: ActionEvent)

    /** Non-suspending send; a full buffer drops the event and prints why. */
    fun trySendAction(event: ActionEvent)
}

sealed interface ActionEvent {
    /** Raised by AppAuthEventSink when the session died while it was active: toast, wipe, navigate to Login. */
    data object AuthInvalidated : ActionEvent
}

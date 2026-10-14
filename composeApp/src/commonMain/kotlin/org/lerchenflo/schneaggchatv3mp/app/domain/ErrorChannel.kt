package org.lerchenflo.schneaggchatv3mp.app.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.utilities.UiText

/**
 * App-wide stream of error events. `App.kt` collects [errors] and shows each one as a popup on
 * top of every screen.
 *
 * Repositories writing into this channel is temporary: once they return `Result<T, DataError>`,
 * the caller maps the error to UI instead (see `plans/Todo.md`).
 */
interface ErrorChannel {
    val errors: Flow<ErrorEvent>

    suspend fun sendErrorSuspend(event: ErrorEvent)

    /** Non-suspending send; a full buffer drops the event and prints why. */
    fun trySendError(event: ErrorEvent)
}

/** One global error popup. Rendered to text in the UI layer, see `ErrorEvent.toStringComposable()`. */
data class ErrorEvent(
    val errorCode: Int? = null,
    val errorMessage: String? = null,
    val error: NetworkingError? = null,
    val errorMessageUiText: UiText? = null,
    val duration: Long = 5000L
)

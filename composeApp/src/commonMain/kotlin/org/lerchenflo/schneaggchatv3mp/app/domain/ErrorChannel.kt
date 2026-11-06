package org.lerchenflo.schneaggchatv3mp.app.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.utilities.UiText

/**
 * App-wide stream of error events. `App.kt` collects [errors] and shows each one as a popup on
 * top of every screen. This popup is the only place for errors: `SnackbarManager` is for success
 * and info messages. ViewModels send the errors their repository calls return, with
 * [reportErrorTo]. The session layer sends its own, because no caller waits for them.
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
    val error: DataError? = null,
    val errorMessageUiText: UiText? = null,
    val duration: Long = 5000L
)

/** Shows [error] in the global popup. */
suspend fun ErrorChannel.sendError(error: DataError) {
    sendErrorSuspend(ErrorEvent(error = error))
}

/** Shows [text] in the global popup, for failures without a [DataError], such as input checks. */
suspend fun ErrorChannel.sendError(text: UiText) {
    sendErrorSuspend(ErrorEvent(errorMessageUiText = text))
}

/** Shows this result's error in the global popup and returns the result for further handling. */
suspend fun <T, E : DataError> DataResult<T, E>.reportErrorTo(channel: ErrorChannel): DataResult<T, E> =
    onError { channel.sendError(it) }

/** Like [reportErrorTo], but silent for transient errors: a retry or the offline bar covers them. */
suspend fun <T, E : DataError> DataResult<T, E>.reportLastingErrorTo(channel: ErrorChannel): DataResult<T, E> =
    onError { if (!it.isTransient()) channel.sendError(it) }

/** One batch of writes gets one popup: shows only the first error of these results. */
suspend fun Iterable<DataResult<*, DataError>>.reportFirstErrorTo(channel: ErrorChannel) {
    firstNotNullOfOrNull { (it as? DataResult.Error)?.error }?.let { channel.sendError(it) }
}

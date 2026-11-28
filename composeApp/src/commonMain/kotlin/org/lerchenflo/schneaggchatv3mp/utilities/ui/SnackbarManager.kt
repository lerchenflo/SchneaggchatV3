package org.lerchenflo.schneaggchatv3mp.utilities.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Success and info messages only. Errors go to the global popup, see `ErrorChannel`. */
object SnackbarManager {
    private var scope: CoroutineScope? = null

    fun init(scope: CoroutineScope) {
        this.scope = scope
    }


    data class SnackbarEvent(
        val showTime: Long = 3000,

        val message: String,
        /** Optional button next to the text, e.g. "Undo". */
        val action: SnackbarAction? = null,
    )

    /** A button in the snackbar; [onClick] runs once, then the snackbar closes. */
    data class SnackbarAction(
        val label: String,
        val onClick: () -> Unit,
    )

    private val _channel = Channel<SnackbarEvent>(capacity = Channel.BUFFERED)
    val snackbars = _channel.receiveAsFlow()



    fun showMessage(message: String, showTime: Long = 3000, action: SnackbarAction? = null) {
        scope?.launch {
            _channel.send(SnackbarEvent(
                showTime = showTime,
                message = message,
                action = action,
            ))
        }
    }

    /** Test-only (plan D19): forgets the scope from [init] and drops every queued snackbar. Never call it from app code. */
    internal fun resetForTest() {
        scope = null
        // Each tryReceive removes one queued snackbar; stop once the channel is empty
        do {
            val received = _channel.tryReceive()
        } while (received.isSuccess)
    }




}

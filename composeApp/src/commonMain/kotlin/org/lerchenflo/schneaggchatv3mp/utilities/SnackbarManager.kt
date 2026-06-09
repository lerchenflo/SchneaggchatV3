package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

object SnackbarManager {
    private var scope: CoroutineScope? = null

    fun init(scope: CoroutineScope) {
        this.scope = scope
    }


    data class SnackbarEvent(
        val showTime: Long = 3000,

        val message: String,
    )

    private val _channel = Channel<SnackbarEvent>(capacity = Channel.BUFFERED)
    val snackbars = _channel.receiveAsFlow()



    fun showMessage(message: String, showTime: Long = 3000) {
        scope?.launch {
            scope?.launch {
                _channel.send(SnackbarEvent(
                    showTime = showTime,
                    message = message
                ))
            }
        }
    }




}
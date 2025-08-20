package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object SnackbarManager {
    private var hostState: SnackbarHostState? = null
    private var scope: CoroutineScope? = null

    fun init(hostState: SnackbarHostState, scope: CoroutineScope) {
        this.hostState = hostState
        this.scope = scope
    }

    fun showMessage(message: String) {
        scope?.launch {
            hostState?.showSnackbar(message)
        }
    }
}
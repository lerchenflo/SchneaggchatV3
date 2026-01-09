package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object SnackbarManager {
    private var hostState: SnackbarHostState? = null
    private var scope: CoroutineScope? = null
    private var currentSnackbarJob: Job? = null

    fun init(hostState: SnackbarHostState, scope: CoroutineScope) {
        this.hostState = hostState
        this.scope = scope
    }

    fun showMessage(message: String) {
        // Cancel the previous snackbar if it's still showing
        currentSnackbarJob?.cancel()

        // Dismiss current snackbar immediately
        hostState?.currentSnackbarData?.dismiss()

        // Show the new snackbar
        currentSnackbarJob = scope?.launch {
            hostState?.showSnackbar(message)
        }
    }
}
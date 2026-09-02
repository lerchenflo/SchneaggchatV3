package org.lerchenflo.schneaggchatv3mp.datasource.network.util

import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.SessionCache

/**
 * Updates the app-wide online/offline state from this result and returns it unchanged, so it can
 * be chained at the call site. A network-shaped error (timeout, no connection) only flips the app
 * offline while it is in the foreground - a backgrounded app's connectivity is irrelevant to the UI.
 */
fun <D, E : NetworkingError> NetworkResult<D, E>.trackConnectivity(): NetworkResult<D, E> {
    val isNetworkFailure = this is NetworkResult.Error && error.isNetworkerror()
    if (!isNetworkFailure) {
        SessionCache.updateOnline(true)
    } else if (AppLifecycleManager.isAppInForeground) {
        SessionCache.updateOnline(false)
    }
    return this
}

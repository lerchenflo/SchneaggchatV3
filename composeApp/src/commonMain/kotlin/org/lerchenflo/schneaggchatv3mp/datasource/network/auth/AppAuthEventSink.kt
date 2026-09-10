package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.TokenPair
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_server_unreachable

/**
 * The app's [AuthEventSink]: keeps `SessionCache` in step with the session manager and raises the
 * app-wide action / error events the UI already reacts to.
 */
class AppAuthEventSink : AuthEventSink {

    override fun onSessionActive(tokens: TokenPair) {
        SessionCache.updateTokens(tokens)
    }

    override fun onSessionInvalidated(reason: InvalidationReason) {
        SessionCache.logout()
        // App.kt handles this: toast, full logout (server session kill is skipped, the token is
        // already gone), navigation to Login.
        AppRepository.ActionChannel.trySendAction(AppRepository.ActionChannel.ActionEvent.AuthInvalidated)
    }

    override fun onServerUnreachable(error: NetworkingError) {
        AppRepository.ErrorChannel.trySendError(
            AppRepository.ErrorChannel.ErrorEvent(
                error = error,
                errorMessageUiText = UiText.StringResourceText(Res.string.error_server_unreachable),
                duration = 8000L,
            )
        )
    }
}

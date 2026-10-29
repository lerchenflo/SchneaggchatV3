package org.lerchenflo.schneaggchatv3mp.datasource.network.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.ActionChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ActionEvent
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.app.domain.SessionTerminator
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkingError
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_not_permitted
import schneaggchatv3mp.composeapp.generated.resources.error_server_unreachable
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.TokenPair

/**
 * The app's [AuthEventSink]: keeps `SessionCache` in step with the session manager, wipes the
 * device when the session is invalidated and raises the app-wide action / error events the UI
 * reacts to.
 *
 * [sessionTerminator] is lazy because the terminator needs the session manager, which needs this sink.
 */
class AppAuthEventSink(
    private val actionChannel: ActionChannel,
    private val errorChannel: ErrorChannel,
    private val sessionTerminator: Lazy<SessionTerminator>,
    private val loggingRepository: Logger,
    private val externalScope: CoroutineScope,
) : AuthEventSink {

    override fun onSessionActive(tokens: TokenPair) {
        SessionCache.updateTokens(tokens)
    }

    override fun onSessionInvalidated(reason: InvalidationReason) {
        SessionCache.logout()
        errorChannel.trySendError(
            ErrorEvent(
                401,
                errorMessageUiText = UiText.StringResourceText(Res.string.error_access_not_permitted),
                duration = 5000L,
            )
        )
        // Called inside the session manager's lock, and the wipe needs that lock: run it in the app
        // scope. Login opens only once the wipe is done, so a quick re-login cannot be erased by a
        // wipe still running; a failed wipe still opens Login. No "logged out" snackbar, the popup
        // above already tells the user. endSessionOnServer stays true so the server drops the push token.
        externalScope.launch {
            try {
                sessionTerminator.value.logout(endSessionOnServer = true, showSnackbar = false)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                loggingRepository.logError("Forced logout: wipe failed (${e.message}), opening Login anyway")
            } finally {
                actionChannel.trySendAction(ActionEvent.AuthInvalidated) // App.kt navigates to Login
            }
        }
    }

    override fun onServerUnreachable(error: NetworkingError) {
        errorChannel.trySendError(
            ErrorEvent(
                error = error,
                errorMessageUiText = UiText.StringResourceText(Res.string.error_server_unreachable),
                duration = 8000L,
            )
        )
    }
}

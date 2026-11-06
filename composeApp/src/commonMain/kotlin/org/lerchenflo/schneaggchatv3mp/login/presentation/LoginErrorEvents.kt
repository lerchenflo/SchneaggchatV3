package org.lerchenflo.schneaggchatv3mp.login.presentation

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorEvent
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.error_access_expired
import schneaggchatv3mp.composeapp.generated.resources.error_invalid_credentials
import schneaggchatv3mp.composeapp.generated.resources.error_login_server_error
import schneaggchatv3mp.composeapp.generated.resources.error_login_too_many_requests
import schneaggchatv3mp.composeapp.generated.resources.offline

/**
 * Only a genuine 401 means wrong credentials - rate limiting, server errors and being offline
 * are not the user's fault and shouldn't be reported as such.
 */
fun DataError.toLoginErrorEvent(): ErrorEvent {
    val messageRes = when (this) {
        is DataError.TooManyRequests -> Res.string.error_login_too_many_requests
        is DataError.NoInternetConnection,
        is DataError.NetworkTimeout -> Res.string.offline
        is DataError.ServerError -> Res.string.error_login_server_error
        else -> Res.string.error_invalid_credentials
    }

    return ErrorEvent(
        errorMessageUiText = UiText.StringResourceText(messageRes),
        duration = 5000L,
    )
}

/** The saved session can't be restored, the user has to log in again. */
fun sessionExpiredErrorEvent(): ErrorEvent = ErrorEvent(
    errorCode = 401,
    errorMessageUiText = UiText.StringResourceText(Res.string.error_access_expired),
    duration = 5000L,
)

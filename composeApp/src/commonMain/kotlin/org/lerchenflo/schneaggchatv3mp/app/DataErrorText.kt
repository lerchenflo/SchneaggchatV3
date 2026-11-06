package org.lerchenflo.schneaggchatv3mp.app

import androidx.compose.runtime.Composable
import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.network_error_bad_request
import schneaggchatv3mp.composeapp.generated.resources.network_error_conflict
import schneaggchatv3mp.composeapp.generated.resources.network_error_forbidden
import schneaggchatv3mp.composeapp.generated.resources.network_error_no_internet
import schneaggchatv3mp.composeapp.generated.resources.network_error_not_found
import schneaggchatv3mp.composeapp.generated.resources.network_error_payload_too_large
import schneaggchatv3mp.composeapp.generated.resources.network_error_serialization
import schneaggchatv3mp.composeapp.generated.resources.network_error_server
import schneaggchatv3mp.composeapp.generated.resources.network_error_timeout
import schneaggchatv3mp.composeapp.generated.resources.network_error_too_many_requests
import schneaggchatv3mp.composeapp.generated.resources.network_error_unauthorized
import schneaggchatv3mp.composeapp.generated.resources.unknown_error

fun errorCodeToUiText(code: Int?): UiText = UiText.StringResourceText(
    when (code) {
        400 -> Res.string.network_error_bad_request
        401 -> Res.string.network_error_unauthorized
        403 -> Res.string.network_error_forbidden
        404 -> Res.string.network_error_not_found
        408 -> Res.string.network_error_timeout
        409 -> Res.string.network_error_conflict
        413 -> Res.string.network_error_payload_too_large
        429 -> Res.string.network_error_too_many_requests
        in 500..599 -> Res.string.network_error_server
        0 -> Res.string.network_error_no_internet
        -1 -> Res.string.network_error_serialization
        else -> Res.string.unknown_error
    }
)

fun DataError.toUiText(): UiText = errorCodeToUiText(errorCode)

@Composable
fun errorCodeToMessage(code: Int?): String = errorCodeToUiText(code).asString()

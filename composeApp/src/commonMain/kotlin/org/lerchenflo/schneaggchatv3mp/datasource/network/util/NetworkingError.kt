package org.lerchenflo.schneaggchatv3mp.datasource.network.util

import androidx.compose.runtime.Composable
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

sealed interface NetworkingError {
    val errorCode: Int
    val message: String?


    data class BadRequest(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = 400
    }

    data class Unauthorized(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = 401
    }

    data class Forbidden(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = 403
    }

    data class NotFound(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = 404
    }

    data class Conflict(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = 409
    }

    data class PayloadTooLarge(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = 413
    }

    data class TooManyRequests(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = 429
    }

    data class NetworkTimeout(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = 408
    }

    data class ServerError(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = 500
    }

    data object NoInternetConnection : NetworkingError {
        override val errorCode: Int = 0
        override val message: String? = null
    }

    data class SerializationError(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = -1
    }

    data class Unknown(
        override val message: String? = null
    ) : NetworkingError {
        override val errorCode: Int = -2
    }
}

fun NetworkingError.isNetworkerror() : Boolean {
    return this is NetworkingError.NetworkTimeout || this is NetworkingError.NoInternetConnection
}




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
        500 -> Res.string.network_error_server
        0 -> Res.string.network_error_no_internet
        -1 -> Res.string.network_error_serialization
        else -> Res.string.unknown_error
    }
)

fun NetworkingError.toUiText(): UiText = errorCodeToUiText(errorCode)

@Composable
fun errorCodeToMessage(code: Int?): String = errorCodeToUiText(code).asString()
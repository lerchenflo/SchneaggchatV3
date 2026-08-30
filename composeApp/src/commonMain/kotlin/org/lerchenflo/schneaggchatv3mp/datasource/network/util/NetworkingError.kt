package org.lerchenflo.schneaggchatv3mp.datasource.network.util

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




fun errorCodeToMessage(code: Int?): String = when (code) {
    400 -> "Bad request"
    401 -> "Access denied (invalid credentials)"
    403 -> "Not allowed"
    404 -> "Resource not found"
    408 -> "Request timed out"
    409 -> "Conflict (resource already exists or invalid state)"
    413 -> "Payload too large"
    429 -> "Too many requests (rate limit exceeded)"
    500 -> "Server error (internal server issue)"
    0   -> "No internet connection"
    -1  -> "Serialization error"
    -2  -> "Unknown error"
    else -> "Unknown error"
}